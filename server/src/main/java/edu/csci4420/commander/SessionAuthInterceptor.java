package edu.csci4420.commander;

import edu.csci4420.commander.proto.PlayerProfile;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SessionAuthInterceptor implements ServerInterceptor {
    static final Context.Key<PlayerProfile> CURRENT_PLAYER = Context.key("current-player");

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Pattern BEARER = Pattern.compile(
            "(?i:Bearer) (([0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})\\.[A-Za-z0-9_-]{43})");
    private static final Status INVALID_SESSION =
            Status.UNAUTHENTICATED.withDescription("A valid session token is required.");

    private final Database database;

    SessionAuthInterceptor(Database database) {
        this.database = database;
    }

    @Override
    public <Request, Reply> ServerCall.Listener<Request> interceptCall(
            ServerCall<Request, Reply> call, Metadata headers, ServerCallHandler<Request, Reply> next) {
        Iterable<String> values = headers.getAll(AUTHORIZATION);
        if (values == null) {
            return reject(call, INVALID_SESSION);
        }
        Iterator<String> credentials = values.iterator();
        Matcher bearer = BEARER.matcher(credentials.next());
        if (credentials.hasNext() || !bearer.matches()) {
            return reject(call, INVALID_SESSION);
        }

        PlayerProfile player;
        try {
            player = findPlayer(bearer.group(2), bearer.group(1));
        } catch (SQLException e) {
            return reject(call, Database.failure("Session validation", e));
        }
        if (player == null) {
            return reject(call, INVALID_SESSION);
        }
        return Contexts.interceptCall(Context.current().withValue(CURRENT_PLAYER, player), call, headers, next);
    }

    private PlayerProfile findPlayer(String playerId, String token) throws SQLException {
        // The UUID narrows the lookup. The stored hash and expiration authenticate the player.
        try (Connection connection = database.open();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT p.uuid, p.username
                     FROM sessions s
                     JOIN players p ON p.uuid = s.session_player
                     WHERE s.session_player = ?
                       AND s.session_token = SHA2(CONCAT(s.salt, ?), 256)
                       AND s.session_expiration > CURRENT_TIMESTAMP
                     """)) {
            select.setString(1, playerId);
            select.setString(2, token);
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                return PlayerProfile.newBuilder()
                        .setPlayerId(rows.getString("uuid"))
                        .setUsername(rows.getString("username"))
                        .build();
            }
        }
    }

    private static <Request, Reply> ServerCall.Listener<Request> reject(ServerCall<Request, Reply> call, Status status) {
        call.close(status, new Metadata());
        return new ServerCall.Listener<>() {};
    }
}
