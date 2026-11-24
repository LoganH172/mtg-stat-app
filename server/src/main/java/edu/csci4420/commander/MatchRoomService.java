package edu.csci4420.commander;

import edu.csci4420.commander.proto.CreateMatchRequest;
import edu.csci4420.commander.proto.GetMatchRequest;
import edu.csci4420.commander.proto.JoinMatchRequest;
import edu.csci4420.commander.proto.Match;
import edu.csci4420.commander.proto.MatchParticipant;
import edu.csci4420.commander.proto.MatchServiceGrpc;
import edu.csci4420.commander.proto.PlayerProfile;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class MatchRoomService extends MatchServiceGrpc.MatchServiceImplBase {
    private static final String ROOM_CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 8;
    private static final Pattern ROOM_CODE_PATTERN =
            Pattern.compile("[" + ROOM_CODE_CHARACTERS + "]{" + ROOM_CODE_LENGTH + "}");
    private static final int MAX_CREATE_ATTEMPTS = 5;
    private static final int DUPLICATE_ENTRY = 1062;
    private static final String SELECT_ROOM = """
            SELECT match_id, room_code, created_by_player_id, playgroup_id,
                   UNIX_TIMESTAMP(created_at) AS created_at_seconds
            FROM match_rooms
            """;

    private final SecureRandom random = new SecureRandom();
    private final Database database;

    MatchRoomService(Database database) {
        this.database = database;
    }

    @Override
    public void createMatch(CreateMatchRequest request, StreamObserver<Match> response) {
        reply(response, "Match creation", playerId -> {
            String requestId = validatedUuid(request.getRequestId(), "Request ID");
            String playgroupId = request.hasPlaygroupId() ? request.getPlaygroupId() : null;
            if (playgroupId != null && (playgroupId.isBlank()
                    || playgroupId.codePointCount(0, playgroupId.length()) > 255)) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid playgroup ID.").asRuntimeException();
            }
            return createRoom(playerId, requestId, playgroupId);
        });
    }

    @Override
    public void joinMatch(JoinMatchRequest request, StreamObserver<Match> response) {
        reply(response, "Joining match", playerId -> {
            String roomCode = request.getRoomCode().strip().toUpperCase(Locale.ROOT);
            if (!ROOM_CODE_PATTERN.matcher(roomCode).matches()) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid room code.").asRuntimeException();
            }
            return inTransaction(connection -> {
                // Serialize changes to one room so its membership and reply agree.
                Room room = findRoom(connection, "WHERE room_code = ? FOR UPDATE", roomCode);
                if (room == null) {
                    throw Status.NOT_FOUND.withDescription("Room code not found.").asRuntimeException();
                }
                requirePlaygroupMember(connection, room.playgroupId(), playerId);
                addMember(connection, room.matchId(), playerId);
                return readMatch(connection, room);
            });
        });
    }

    @Override
    public void getMatch(GetMatchRequest request, StreamObserver<Match> response) {
        reply(response, "Reading match", playerId -> {
            String matchId = validatedUuid(request.getMatchId(), "Match ID");
            return inTransaction(connection -> {
                Room room = findRoom(connection, "WHERE match_id = ?", matchId);
                if (room == null) {
                    throw Status.NOT_FOUND.withDescription("Match not found.").asRuntimeException();
                }
                if (!hasMembership(connection, """
                        SELECT 1 FROM match_room_members WHERE match_id = ? AND player_id = ?
                        """, matchId, playerId)) {
                    throw Status.PERMISSION_DENIED.withDescription("Join the match before reading it.").asRuntimeException();
                }
                return readMatch(connection, room);
            });
        });
    }

    private Match createRoom(String playerId, String requestId, String playgroupId) throws SQLException {
        for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
            try {
                return inTransaction(connection -> {
                    // Requests from this player wait until the first creation has committed.
                    lockCreator(connection, playerId);
                    Room existing = findRoom(connection,
                            "WHERE created_by_player_id = ? AND request_id = ?", playerId, requestId);
                    if (existing != null) {
                        if (!Objects.equals(existing.playgroupId(), playgroupId)) {
                            throw Status.ALREADY_EXISTS
                                    .withDescription("Request ID was already used with a different playgroup.")
                                    .asRuntimeException();
                        }
                        return readMatch(connection, existing);
                    }
                    requirePlaygroupMember(connection, playgroupId, playerId);
                    String matchId = UUID.randomUUID().toString();
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO match_rooms (match_id, room_code, created_by_player_id, request_id, playgroup_id)
                            VALUES (?, ?, ?, ?, ?)
                            """)) {
                        insert.setString(1, matchId);
                        insert.setString(2, newRoomCode());
                        insert.setString(3, playerId);
                        insert.setString(4, requestId);
                        insert.setString(5, playgroupId);
                        insert.executeUpdate();
                    }
                    addMember(connection, matchId, playerId);
                    return readMatch(connection, findRoom(connection, "WHERE match_id = ?", matchId));
                });
            } catch (SQLException e) {
                if (e.getErrorCode() != DUPLICATE_ENTRY) {
                    throw e;
                }
                // Another room used the generated code, so try again with a fresh code.
            }
        }
        throw Status.UNAVAILABLE.withDescription("Could not allocate a room code. Retry the request.")
                .asRuntimeException();
    }

    private String newRoomCode() {
        StringBuilder code = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            code.append(ROOM_CODE_CHARACTERS.charAt(random.nextInt(ROOM_CODE_CHARACTERS.length())));
        }
        return code.toString();
    }

    private static void lockCreator(Connection connection, String playerId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT uuid FROM players WHERE uuid = ? FOR UPDATE")) {
            select.setString(1, playerId);
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) {
                    throw Status.UNAUTHENTICATED.withDescription("Player account no longer exists.").asRuntimeException();
                }
            }
        }
    }

    private static Room findRoom(Connection connection, String condition, String... parameters) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(SELECT_ROOM + condition)) {
            for (int i = 0; i < parameters.length; i++) {
                select.setString(i + 1, parameters[i]);
            }
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                return new Room(rows.getString("match_id"), rows.getString("room_code"),
                        rows.getString("created_by_player_id"), rows.getString("playgroup_id"),
                        rows.getLong("created_at_seconds"));
            }
        }
    }

    private static void requirePlaygroupMember(Connection connection, String playgroupId, String playerId)
            throws SQLException {
        if (playgroupId != null && !hasMembership(connection, """
                SELECT 1 FROM playgroup_members WHERE playgroup_id = ? AND player_id = ? FOR SHARE
                """, playgroupId, playerId)) {
            throw Status.PERMISSION_DENIED.withDescription("Playgroup membership is required.").asRuntimeException();
        }
    }

    private static boolean hasMembership(Connection connection, String query, String matchOrPlaygroupId, String playerId)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(query)) {
            select.setString(1, matchOrPlaygroupId);
            select.setString(2, playerId);
            try (ResultSet rows = select.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static void addMember(Connection connection, String matchId, String playerId) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO match_room_members (match_id, player_id) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE player_id = match_room_members.player_id
                """)) {
            insert.setString(1, matchId);
            insert.setString(2, playerId);
            insert.executeUpdate();
        }
    }

    private static Match readMatch(Connection connection, Room room) throws SQLException {
        Match.Builder reply = Match.newBuilder()
                .setMatchId(room.matchId())
                .setRoomCode(room.roomCode())
                .setCreatedByPlayerId(room.creatorId())
                .setCreatedAtUnixSeconds(room.createdAtSeconds());
        if (room.playgroupId() != null) {
            reply.setPlaygroupId(room.playgroupId());
        }
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT player.uuid, player.username
                FROM match_room_members member
                JOIN players player ON player.uuid = member.player_id
                WHERE member.match_id = ?
                ORDER BY (player.uuid = ?) DESC, player.uuid
                """)) {
            select.setString(1, room.matchId());
            select.setString(2, room.creatorId());
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    reply.addParticipants(MatchParticipant.newBuilder().setPlayer(PlayerProfile.newBuilder()
                            .setPlayerId(rows.getString("uuid")).setUsername(rows.getString("username"))));
                }
            }
        }
        return reply.build();
    }

    private Match inTransaction(RoomOperation operation) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                Match match = operation.run(connection);
                connection.commit();
                return match;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    private static String validatedUuid(String value, String label) {
        try {
            String canonical = UUID.fromString(value).toString();
            if (canonical.equalsIgnoreCase(value)) {
                return canonical;
            }
        } catch (IllegalArgumentException ignored) {
            // Report the same error for any malformed UUID.
        }
        throw Status.INVALID_ARGUMENT.withDescription(label + " must be a UUID.").asRuntimeException();
    }

    private static void reply(StreamObserver<Match> response, String operation, MatchRequest request) {
        PlayerProfile player = SessionAuthInterceptor.CURRENT_PLAYER.get();
        if (player == null) {
            response.onError(Status.UNAUTHENTICATED.asRuntimeException());
            return;
        }
        Match match;
        try {
            match = request.run(player.getPlayerId());
        } catch (StatusRuntimeException e) {
            response.onError(e);
            return;
        } catch (SQLException e) {
            response.onError(Database.failure(operation, e).asRuntimeException());
            return;
        }
        response.onNext(match);
        response.onCompleted();
    }

    private record Room(String matchId, String roomCode, String creatorId, String playgroupId, long createdAtSeconds) {}

    @FunctionalInterface
    private interface RoomOperation {
        Match run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface MatchRequest {
        Match run(String playerId) throws SQLException;
    }
}
