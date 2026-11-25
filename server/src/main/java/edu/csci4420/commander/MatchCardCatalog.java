package edu.csci4420.commander;

import io.grpc.Status;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class MatchCardCatalog {
    private final Database database;
    private final ScryfallClient scryfall;

    MatchCardCatalog(Database database) {
        this(database, new ScryfallClient());
    }

    MatchCardCatalog(Database database, URI cardsEndpoint) {
        this(database, new ScryfallClient(cardsEndpoint));
    }

    MatchCardCatalog(Database database, ScryfallClient scryfall) {
        this.database = database;
        this.scryfall = scryfall;
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

        ScryfallClient.Printing printing = scryfall.lookup(printingId);
        if (printing == null) {
            throw Status.INVALID_ARGUMENT.withDescription("Scryfall printing ID was not found.").asRuntimeException();
        }
        String name = printing.card().getName();
        String identity = printing.storedColorIdentity();
        try (Connection connection = database.open();
             PreparedStatement upsert = connection.prepareStatement("""
                     INSERT INTO commander_card_data (uuid, card_name, color_identity) VALUES (?, ?, ?)
                     ON DUPLICATE KEY UPDATE card_name = ?, color_identity = ?
                     """)) {
            upsert.setString(1, printingId);
            upsert.setString(2, name);
            upsert.setString(3, identity);
            upsert.setString(4, name);
            upsert.setString(5, identity);
            upsert.executeUpdate();
        }
    }
}
