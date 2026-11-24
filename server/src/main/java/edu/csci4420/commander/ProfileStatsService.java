package edu.csci4420.commander;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import edu.csci4420.commander.proto.ColorIdentity;
import edu.csci4420.commander.proto.ColorIdentityRecord;
import edu.csci4420.commander.proto.GetProfileStatsReply;
import edu.csci4420.commander.proto.GetProfileStatsRequest;
import edu.csci4420.commander.proto.ManaColor;
import edu.csci4420.commander.proto.PlayerProfile;
import edu.csci4420.commander.proto.StatsServiceGrpc;
import edu.csci4420.commander.proto.WinLossRecord;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProfileStatsService extends StatsServiceGrpc.StatsServiceImplBase {
    private final Database database;

    ProfileStatsService(Database database) {
        this.database = database;
    }

    @Override
    public void getProfileStats(GetProfileStatsRequest request, StreamObserver<GetProfileStatsReply> response) {
        PlayerProfile player = SessionAuthInterceptor.CURRENT_PLAYER.get();
        if (player == null) {
            response.onError(Status.UNAUTHENTICATED.asRuntimeException());
            return;
        }

        GetProfileStatsReply reply;
        try {
            reply = loadStats(player);
        } catch (SQLException e) {
            response.onError(Database.failure("Profile stats", e).asRuntimeException());
            return;
        }
        response.onNext(reply);
        response.onCompleted();
    }

    private GetProfileStatsReply loadStats(PlayerProfile player) throws SQLException {
        GameCounts overall = new GameCounts();
        // The query reads newest matches first, so insertion order breaks color-identity ties.
        Map<ColorIdentity, GameCounts> byIdentity = new LinkedHashMap<>();
        try (Connection connection = database.open();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT member.won_game, card.color_identity
                     FROM game_members member
                     JOIN games game ON game.uuid = member.game_id
                     JOIN commander_card_data card ON card.uuid = member.commander_id
                     WHERE member.player_id = ?
                       AND EXISTS (
                           SELECT 1 FROM game_members winner
                           WHERE winner.game_id = game.uuid AND winner.won_game = b'1'
                       )
                     ORDER BY game.timestamp DESC, game.uuid
                     """)) {
            select.setString(1, player.getPlayerId());
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    boolean won = rows.getBoolean("won_game");
                    overall.add(won);
                    ColorIdentity identity = parseColorIdentity(rows.getString("color_identity"));
                    if (identity != null) {
                        byIdentity.computeIfAbsent(identity, ignored -> new GameCounts()).add(won);
                    }
                }
            }
        }

        GetProfileStatsReply.Builder reply = GetProfileStatsReply.newBuilder()
                .setPlayer(player)
                .setOverall(overall.toProto());
        long mostPlayedGames = 0;
        for (var entry : byIdentity.entrySet()) {
            ColorIdentity identity = entry.getKey();
            WinLossRecord record = entry.getValue().toProto();
            reply.addRecordsByColorIdentity(ColorIdentityRecord.newBuilder()
                    .setColorIdentity(identity)
                    .setRecord(record));
            if (record.getGamesPlayed() > mostPlayedGames) {
                mostPlayedGames = record.getGamesPlayed();
                reply.setMostPlayedColorIdentity(identity);
            }
        }
        return reply.build();
    }

    private static ColorIdentity parseColorIdentity(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            JsonElement json = JsonParser.parseString(stored);
            if (!json.isJsonArray()) {
                return null;
            }
            // Enum order matches the proto's white, blue, black, red, green order.
            EnumSet<ManaColor> colors = EnumSet.noneOf(ManaColor.class);
            for (JsonElement element : json.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    return null;
                }
                ManaColor color = switch (element.getAsString()) {
                    case "W" -> ManaColor.MANA_COLOR_WHITE;
                    case "U" -> ManaColor.MANA_COLOR_BLUE;
                    case "B" -> ManaColor.MANA_COLOR_BLACK;
                    case "R" -> ManaColor.MANA_COLOR_RED;
                    case "G" -> ManaColor.MANA_COLOR_GREEN;
                    default -> ManaColor.MANA_COLOR_UNSPECIFIED;
                };
                if (color == ManaColor.MANA_COLOR_UNSPECIFIED) {
                    return null;
                }
                colors.add(color);
            }
            return ColorIdentity.newBuilder().addAllColors(colors).build();
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static final class GameCounts {
        private long wins;
        private long losses;

        void add(boolean won) {
            if (won) {
                wins++;
            } else {
                losses++;
            }
        }

        WinLossRecord toProto() {
            long games = wins + losses;
            return WinLossRecord.newBuilder()
                    .setWins(wins)
                    .setLosses(losses)
                    .setGamesPlayed(games)
                    .setWinPercentage(games == 0 ? 0 : 100.0 * wins / games)
                    .build();
        }
    }
}
