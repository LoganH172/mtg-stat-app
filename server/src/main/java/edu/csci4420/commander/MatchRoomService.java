package edu.csci4420.commander;

import edu.csci4420.commander.proto.Card;
import edu.csci4420.commander.proto.ColorIdentity;
import edu.csci4420.commander.proto.CreateMatchRequest;
import edu.csci4420.commander.proto.GetMatchRequest;
import edu.csci4420.commander.proto.JoinMatchRequest;
import edu.csci4420.commander.proto.LosingCard;
import edu.csci4420.commander.proto.Match;
import edu.csci4420.commander.proto.MatchParticipant;
import edu.csci4420.commander.proto.MatchResult;
import edu.csci4420.commander.proto.MatchServiceGrpc;
import edu.csci4420.commander.proto.PlayerProfile;
import edu.csci4420.commander.proto.SelectCommanderRequest;
import edu.csci4420.commander.proto.SubmitMatchResultRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
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
    private final MatchCardCatalog cards;

    MatchRoomService(Database database) {
        this(database, new MatchCardCatalog(database));
    }

    MatchRoomService(Database database, MatchCardCatalog cards) {
        this.database = database;
        this.cards = cards;
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
                requireOpen(connection, room.matchId());
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
                Room room = requireRoom(connection, matchId, false);
                requireRoomMember(connection, matchId, playerId);
                return readMatch(connection, room);
            });
        });
    }

    @Override
    public void selectCommander(SelectCommanderRequest request, StreamObserver<Match> response) {
        reply(response, "Commander selection", playerId -> {
            String matchId = validatedUuid(request.getMatchId(), "Match ID");
            String commanderId = validatedUuid(request.getCommanderScryfallId(), "Commander printing ID");

            // Check access before a cache miss can send a request to Scryfall.
            try (Connection connection = database.open()) {
                requireRoom(connection, matchId, false);
                requireRoomMember(connection, matchId, playerId);
                requireOpen(connection, matchId);
            }
            cards.ensureCard(commanderId);
            return inTransaction(connection -> {
                Room room = requireRoom(connection, matchId, true);
                requireRoomMember(connection, matchId, playerId);
                requireOpen(connection, matchId);
                try (PreparedStatement selectCommander = connection.prepareStatement("""
                        UPDATE match_room_members SET commander_id = ?
                        WHERE match_id = ? AND player_id = ?
                        """)) {
                    selectCommander.setString(1, commanderId);
                    selectCommander.setString(2, matchId);
                    selectCommander.setString(3, playerId);
                    selectCommander.executeUpdate();
                }
                return readMatch(connection, room);
            });
        });
    }

    @Override
    public void submitMatchResult(SubmitMatchResultRequest request, StreamObserver<Match> response) {
        reply(response, "Match submission", playerId -> {
            String matchId = validatedUuid(request.getMatchId(), "Match ID");
            ResultSelection selection = ResultSelection.fromRequest(request);
            Match current = inTransaction(connection -> {
                Room room = requireRoom(connection, matchId, false);
                return checkSubmission(connection, room, playerId, selection);
            });
            if (current.hasResult()) {
                return current;
            }

            // Resolve optional cards before locking the room for finalization.
            var optionalCards = new LinkedHashSet<>(selection.losingCards().values());
            if (selection.mostValuableCard() != null) {
                optionalCards.add(selection.mostValuableCard());
            }
            for (String cardId : optionalCards) {
                cards.ensureCard(cardId);
            }

            return inTransaction(connection -> {
                Room room = requireRoom(connection, matchId, true);
                Match match = checkSubmission(connection, room, playerId, selection);
                if (match.hasResult()) {
                    return match;
                }
                saveResult(connection, room, match, selection);
                return readMatch(connection, room);
            });
        });
    }

    private static Match checkSubmission(
            Connection connection, Room room, String playerId, ResultSelection selection) throws SQLException {
        if (!room.creatorId().equals(playerId)) {
            throw Status.PERMISSION_DENIED.withDescription("Only the match creator can submit a result.")
                    .asRuntimeException();
        }
        Match match = readMatch(connection, room);
        if (match.hasResult()) {
            if (!selection.equals(ResultSelection.fromResult(match.getResult()))) {
                throw Status.FAILED_PRECONDITION.withDescription("The match already has a different result.")
                        .asRuntimeException();
            }
            return match;
        }

        var participants = new LinkedHashSet<String>();
        for (MatchParticipant member : match.getParticipantsList()) {
            participants.add(member.getPlayer().getPlayerId());
            if (!member.hasCommander()) {
                throw Status.FAILED_PRECONDITION.withDescription("Every participant must select a commander.")
                        .asRuntimeException();
            }
        }
        if (!participants.contains(selection.winnerId()) || !participants.containsAll(selection.losingCards().keySet())) {
            throw Status.INVALID_ARGUMENT.withDescription("Result players must be match participants.")
                    .asRuntimeException();
        }
        return match;
    }

    private static void saveResult(Connection connection, Room room, Match match, ResultSelection selection)
            throws SQLException {
        try (PreparedStatement game = connection.prepareStatement(
                "INSERT INTO games (uuid, timestamp) VALUES (?, CURRENT_TIMESTAMP)")) {
            game.setString(1, room.matchId());
            game.executeUpdate();
        }
        try (PreparedStatement member = connection.prepareStatement("""
                INSERT INTO game_members (game_id, player_id, won_game, commander_id, standout_card_id, playgroup_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (MatchParticipant participant : match.getParticipantsList()) {
                String playerId = participant.getPlayer().getPlayerId();
                boolean won = playerId.equals(selection.winnerId());
                member.setString(1, room.matchId());
                member.setString(2, playerId);
                member.setBoolean(3, won);
                member.setString(4, participant.getCommander().getScryfallId());
                member.setString(5, won ? selection.mostValuableCard() : selection.losingCards().get(playerId));
                member.setString(6, room.playgroupId());
                member.addBatch();
            }
            member.executeBatch();
        }
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

    private static Room requireRoom(Connection connection, String matchId, boolean lock) throws SQLException {
        Room room = findRoom(connection, lock ? "WHERE match_id = ? FOR UPDATE" : "WHERE match_id = ?", matchId);
        if (room == null) {
            throw Status.NOT_FOUND.withDescription("Match not found.").asRuntimeException();
        }
        return room;
    }

    private static void requireRoomMember(Connection connection, String matchId, String playerId) throws SQLException {
        if (!hasMembership(connection, """
                SELECT 1 FROM match_room_members WHERE match_id = ? AND player_id = ?
                """, matchId, playerId)) {
            throw Status.PERMISSION_DENIED.withDescription("Join the match before accessing it.").asRuntimeException();
        }
    }

    private static void requireOpen(Connection connection, String matchId) throws SQLException {
        if (findResult(connection, matchId) != null) {
            throw Status.FAILED_PRECONDITION.withDescription("The match is already complete.").asRuntimeException();
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
                .setCreatedByPlayerId(room.creatorId())
                .setCreatedAtUnixSeconds(room.createdAtSeconds());
        MatchResult result = findResult(connection, room.matchId());
        if (result == null) {
            reply.setRoomCode(room.roomCode());
        } else {
            reply.setResult(result);
        }
        if (room.playgroupId() != null) {
            reply.setPlaygroupId(room.playgroupId());
        }
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT player.uuid, player.username, card.uuid AS commander_uuid, card.card_name, card.color_identity
                FROM match_room_members member
                JOIN players player ON player.uuid = member.player_id
                LEFT JOIN commander_card_data card ON card.uuid = member.commander_id
                WHERE member.match_id = ?
                ORDER BY (player.uuid = ?) DESC, player.uuid
                """)) {
            select.setString(1, room.matchId());
            select.setString(2, room.creatorId());
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    MatchParticipant.Builder participant = MatchParticipant.newBuilder()
                            .setPlayer(PlayerProfile.newBuilder().setPlayerId(rows.getString("uuid"))
                                    .setUsername(rows.getString("username")));
                    String commanderId = rows.getString("commander_uuid");
                    if (commanderId != null) {
                        Card.Builder card = Card.newBuilder().setScryfallId(commanderId);
                        String name = rows.getString("card_name");
                        if (name != null) {
                            card.setName(name);
                        }
                        ColorIdentity colors = CardColors.parse(rows.getString("color_identity"));
                        if (colors != null) {
                            card.setColorIdentity(colors);
                        }
                        participant.setCommander(card);
                    }
                    reply.addParticipants(participant);
                }
            }
        }
        return reply.build();
    }

    private static MatchResult findResult(Connection connection, String matchId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT UNIX_TIMESTAMP(game.timestamp) AS recorded_at, member.player_id, member.won_game,
                       member.standout_card_id
                FROM games game
                LEFT JOIN game_members member ON member.game_id = game.uuid
                WHERE game.uuid = ?
                ORDER BY member.player_id
                """)) {
            select.setString(1, matchId);
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                MatchResult.Builder result = MatchResult.newBuilder().setRecordedAtUnixSeconds(rows.getLong("recorded_at"));
                do {
                    String cardId = rows.getString("standout_card_id");
                    if (rows.getBoolean("won_game")) {
                        result.setWinnerPlayerId(rows.getString("player_id"));
                        if (cardId != null) {
                            result.setMostValuableCardScryfallId(cardId);
                        }
                    } else if (cardId != null) {
                        result.addLosingCards(LosingCard.newBuilder()
                                .setPlayerId(rows.getString("player_id")).setScryfallId(cardId));
                    }
                } while (rows.next());
                return result.build();
            }
        }
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

    private record ResultSelection(String winnerId, String mostValuableCard, Map<String, String> losingCards) {
        static ResultSelection fromRequest(SubmitMatchResultRequest request) {
            String winner = validatedUuid(request.getWinnerPlayerId(), "Winner player ID");
            String mvc = request.hasMostValuableCardScryfallId()
                    ? validatedUuid(request.getMostValuableCardScryfallId(), "Most Valuable Card printing ID")
                    : null;
            Map<String, String> losing = new LinkedHashMap<>();
            for (LosingCard card : request.getLosingCardsList()) {
                String player = validatedUuid(card.getPlayerId(), "Losing player ID");
                String printing = validatedUuid(card.getScryfallId(), "Losing Card printing ID");
                if (player.equals(winner) || losing.putIfAbsent(player, printing) != null) {
                    throw Status.INVALID_ARGUMENT.withDescription("Use one Losing Card per eliminated player.")
                            .asRuntimeException();
                }
            }
            return new ResultSelection(winner, mvc, losing);
        }

        static ResultSelection fromResult(MatchResult result) {
            Map<String, String> losing = new LinkedHashMap<>();
            for (LosingCard card : result.getLosingCardsList()) {
                losing.put(card.getPlayerId(), card.getScryfallId());
            }
            return new ResultSelection(result.getWinnerPlayerId(),
                    result.hasMostValuableCardScryfallId() ? result.getMostValuableCardScryfallId() : null, losing);
        }
    }

    @FunctionalInterface
    private interface RoomOperation {
        Match run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface MatchRequest {
        Match run(String playerId) throws SQLException;
    }
}
