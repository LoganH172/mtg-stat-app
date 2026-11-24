package edu.csci4420.commander;

import edu.csci4420.commander.proto.AuthReply;
import edu.csci4420.commander.proto.AuthServiceGrpc;
import edu.csci4420.commander.proto.LoginRequest;
import edu.csci4420.commander.proto.PlayerProfile;
import edu.csci4420.commander.proto.RegisterRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

final class AuthService extends AuthServiceGrpc.AuthServiceImplBase {
    private static final Duration SESSION_DURATION = Duration.ofHours(24);
    private static final int DUPLICATE_ENTRY = 1062;

    private final SecureRandom random = new SecureRandom();
    private final Database database;

    AuthService(Database database) {
        this.database = database;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthReply> response) {
        String username = request.getUsername().strip();
        String password = request.getPassword();
        if (!validCredentials(username, password, response)) {
            return;
        }

        PlayerProfile player = playerProfile(UUID.randomUUID().toString(), username);
        AuthReply reply;
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                insertPlayer(connection, player, password);
                reply = createSession(connection, player);
                connection.commit();
            } catch (SQLException | StatusRuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (StatusRuntimeException e) {
            response.onError(e);
            return;
        } catch (SQLException e) {
            response.onError(Database.failure("Registration", e).asRuntimeException());
            return;
        }
        response.onNext(reply);
        response.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthReply> response) {
        String username = request.getUsername().strip();
        String password = request.getPassword();
        if (!validCredentials(username, password, response)) {
            return;
        }

        AuthReply reply;
        try (Connection connection = database.open()) {
            PlayerProfile player;
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT uuid, username, password, SHA2(CONCAT(salt, ?), 256) AS candidate_hash
                    FROM players WHERE username = ?
                    """)) {
                select.setString(1, password);
                select.setString(2, username);
                try (ResultSet rows = select.executeQuery()) {
                    if (!rows.next() || !matchesPassword(rows)) {
                        throw Status.UNAUTHENTICATED
                                .withDescription("Invalid username or password.").asRuntimeException();
                    }
                    player = playerProfile(rows.getString("uuid"), rows.getString("username"));
                }
            }
            reply = createSession(connection, player);
        } catch (StatusRuntimeException e) {
            response.onError(e);
            return;
        } catch (SQLException e) {
            response.onError(Database.failure("Login", e).asRuntimeException());
            return;
        }
        response.onNext(reply);
        response.onCompleted();
    }

    private static void insertPlayer(Connection connection, PlayerProfile player, String password) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO players (uuid, username, password) VALUES (?, ?, ?)")) {
            insert.setString(1, player.getPlayerId());
            insert.setString(2, player.getUsername());
            // MySQL generates a random salt and stores the password hash when a player registers.
            insert.setString(3, password);
            insert.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_ENTRY) {
                throw Status.ALREADY_EXISTS.withDescription("Username is taken.").asRuntimeException();
            }
            throw e;
        }
    }

    private AuthReply createSession(Connection connection, PlayerProfile player) throws SQLException {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        // The player ID lets later lookups use the sessions table's player index.
        String token = player.getPlayerId() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        long expiresAt = Instant.now().plus(SESSION_DURATION).getEpochSecond();
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO sessions (session_player, session_token, session_expiration)
                VALUES (?, ?, FROM_UNIXTIME(?))
                """)) {
            insert.setString(1, player.getPlayerId());
            // The MySQL insert trigger stores a salted hash of the bearer token.
            insert.setString(2, token);
            insert.setLong(3, expiresAt);
            insert.executeUpdate();
        }
        return AuthReply.newBuilder()
                .setPlayer(player)
                .setSessionToken(token)
                .setExpiresAtUnixSeconds(expiresAt)
                .build();
    }

    private static boolean matchesPassword(ResultSet row) throws SQLException {
        byte[] candidate = row.getBytes("candidate_hash");
        return candidate != null && MessageDigest.isEqual(row.getBytes("password"), candidate);
    }

    private static boolean validCredentials(String username, String password, StreamObserver<AuthReply> response) {
        if (username.isEmpty() || username.codePointCount(0, username.length()) > 32) {
            response.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username must contain 1 to 32 characters.").asRuntimeException());
            return false;
        }
        if (password.isBlank() || password.codePointCount(0, password.length()) > 255) {
            response.onError(Status.INVALID_ARGUMENT
                    .withDescription("Password must contain 1 to 255 characters and cannot be blank.").asRuntimeException());
            return false;
        }
        return true;
    }

    private static PlayerProfile playerProfile(String playerId, String username) {
        return PlayerProfile.newBuilder().setPlayerId(playerId).setUsername(username).build();
    }
}
