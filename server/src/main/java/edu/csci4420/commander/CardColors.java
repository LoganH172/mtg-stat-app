package edu.csci4420.commander;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import edu.csci4420.commander.proto.ColorIdentity;
import edu.csci4420.commander.proto.ManaColor;
import java.util.EnumSet;

final class CardColors {
    private CardColors() {}

    static ColorIdentity parse(String stored) {
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
}
