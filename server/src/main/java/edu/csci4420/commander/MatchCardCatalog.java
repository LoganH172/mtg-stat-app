package edu.csci4420.commander;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.grpc.Status;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class MatchCardCatalog {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final long REQUEST_SPACING = TimeUnit.MILLISECONDS.toNanos(100);
    private static final Status UNAVAILABLE = Status.UNAVAILABLE.withDescription("Scryfall card lookup is unavailable.");

    private final Database database;
    private final HttpClient httpClient;
    private final URI cardsEndpoint;
    private long lastRequest = System.nanoTime() - REQUEST_SPACING;
    private long retryAfter = System.nanoTime();

    MatchCardCatalog(Database database) {
        this(database, URI.create("https://api.scryfall.com/cards/"));
    }

    MatchCardCatalog(Database database, URI cardsEndpoint) {
        this.database = database;
        this.cardsEndpoint = cardsEndpoint;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    }

    void ensureCard(String printingId) throws SQLException {
        try (Connection connection = database.open();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT card_name, color_identity FROM commander_card_data WHERE uuid = ?
                     """)) {
            select.setString(1, printingId);
            try (ResultSet rows = select.executeQuery()) {
                if (rows.next() && rows.getString("card_name") != null
                        && !rows.getString("card_name").isBlank()
                        && CardColors.parse(rows.getString("color_identity")) != null) {
                    return;
                }
            }
        }

        CardData card = fetchCard(printingId);
        try (Connection connection = database.open();
             PreparedStatement upsert = connection.prepareStatement("""
                     INSERT INTO commander_card_data (uuid, card_name, color_identity) VALUES (?, ?, ?)
                     ON DUPLICATE KEY UPDATE card_name = ?, color_identity = ?
                     """)) {
            upsert.setString(1, printingId);
            upsert.setString(2, card.name());
            upsert.setString(3, card.colorIdentity());
            upsert.setString(4, card.name());
            upsert.setString(5, card.colorIdentity());
            upsert.executeUpdate();
        }
    }

    private synchronized CardData fetchCard(String printingId) {
        if (System.nanoTime() - retryAfter < 0) {
            throw UNAVAILABLE.asRuntimeException();
        }
        HttpRequest request = HttpRequest.newBuilder(cardsEndpoint.resolve(printingId))
                .header("User-Agent", "CommanderLedger/1.0")
                .header("Accept", "application/json")
                .timeout(HTTP_TIMEOUT)
                .build();
        try {
            long remainingWait = REQUEST_SPACING - (System.nanoTime() - lastRequest);
            if (remainingWait > 0) {
                TimeUnit.NANOSECONDS.sleep(remainingWait);
            }
            lastRequest = System.nanoTime();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw Status.INVALID_ARGUMENT.withDescription("Scryfall printing ID was not found.")
                        .asRuntimeException();
            }
            if (response.statusCode() == 429) {
                retryAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            }
            if (response.statusCode() != 200) {
                throw UNAVAILABLE.asRuntimeException();
            }
            JsonObject card = JsonParser.parseString(response.body()).getAsJsonObject();
            String name = card.get("name").getAsString();
            String identity = card.get("color_identity").toString();
            if (!printingId.equals(card.get("id").getAsString()) || name.isBlank()
                    || CardColors.parse(identity) == null) {
                throw UNAVAILABLE.asRuntimeException();
            }
            return new CardData(name, identity);
        } catch (IOException | JsonParseException | IllegalStateException
                 | NullPointerException | UnsupportedOperationException e) {
            throw UNAVAILABLE.asRuntimeException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Status.CANCELLED.withDescription("Card lookup was interrupted.").asRuntimeException();
        }
    }

    private record CardData(String name, String colorIdentity) {}
}
