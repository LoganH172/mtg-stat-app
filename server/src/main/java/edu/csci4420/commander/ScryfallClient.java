package edu.csci4420.commander;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import edu.csci4420.commander.proto.Card;
import edu.csci4420.commander.proto.CardFinish;
import edu.csci4420.commander.proto.CardPrice;
import io.grpc.Context;
import io.grpc.Status;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class ScryfallClient {
    private static final URI CARDS_ENDPOINT = URI.create("https://api.scryfall.com/cards/");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_DURATION = Duration.ofDays(1);
    private static final int MAX_CACHED_RESPONSES = 64;
    private static final long LOOKUP_SPACING = TimeUnit.MILLISECONDS.toNanos(100);
    private static final long SEARCH_SPACING = TimeUnit.MILLISECONDS.toNanos(500);
    private static final Status UNAVAILABLE = Status.UNAVAILABLE.withDescription("Scryfall is unavailable.");

    private final URI cardsEndpoint;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    private final Map<URI, RetrievedCardData> cache = new LinkedHashMap<>(16, .75f, true);
    private long lastRequest = System.nanoTime() - LOOKUP_SPACING;
    private long lastSearch = System.nanoTime() - SEARCH_SPACING;
    private long retryAfter = System.nanoTime();

    ScryfallClient() {
        this(CARDS_ENDPOINT);
    }

    ScryfallClient(URI cardsEndpoint) {
        this.cardsEndpoint = cardsEndpoint;
    }

    List<Card> search(String query, int limit) {
        URI uri = cardsEndpoint.resolve("search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        return request(uri, true, response -> readSearchResults(response.body(), limit));
    }

    private static List<Card> readSearchResults(JsonObject body, int limit) {
        if (body == null) {
            return List.of();
        }
        JsonArray results = array(body, "data");
        if (results == null) {
            throw UNAVAILABLE.asRuntimeException();
        }
        int count = Math.min(limit, results.size());
        List<Card> cards = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            JsonElement element = results.get(index);
            if (!element.isJsonObject()) {
                throw UNAVAILABLE.asRuntimeException();
            }
            cards.add(readCard(element.getAsJsonObject()));
        }
        return cards;
    }

    Printing lookup(String printingId) {
        String canonical = normalizePrintingId(printingId);
        return request(cardsEndpoint.resolve(canonical), false, response -> {
            JsonObject body = response.body();
            if (body == null) {
                return null;
            }
            Card card = readCard(body);
            if (!canonical.equals(card.getScryfallId())) {
                throw UNAVAILABLE.asRuntimeException();
            }
            return new Printing(card, body.get("color_identity").toString(),
                    object(body, "prices"), response.retrievedAtSeconds());
        });
    }

    private synchronized <T> T request(URI uri, boolean search, Function<RetrievedCardData, T> decode) {
        checkCancelled();
        RetrievedCardData cached = cache.get(uri);
        if (cached != null && Instant.now().getEpochSecond() - cached.retrievedAtSeconds() < CACHE_DURATION.toSeconds()) {
            return decode.apply(cached);
        }
        if (System.nanoTime() - retryAfter < 0) {
            throw UNAVAILABLE.asRuntimeException();
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "CommanderLedger/1.0")
                .header("Accept", "application/json")
                .timeout(HTTP_TIMEOUT)
                .build();
        try {
            long now = System.nanoTime();
            long wait = LOOKUP_SPACING - (now - lastRequest);
            if (search) {
                wait = Math.max(wait, SEARCH_SPACING - (now - lastSearch));
            }
            if (wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            checkCancelled();
            lastRequest = System.nanoTime();
            if (search) {
                lastSearch = lastRequest;
            }
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long retrievedAt = Instant.now().getEpochSecond();
            if (response.statusCode() == 404) {
                return decode.apply(new RetrievedCardData(null, retrievedAt));
            }
            if (search && (response.statusCode() == 400 || response.statusCode() == 422)) {
                throw Status.INVALID_ARGUMENT.withDescription("Scryfall rejected the card search query.").asRuntimeException();
            }
            if (response.statusCode() == 429) {
                retryAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            }
            if (response.statusCode() != 200) {
                throw UNAVAILABLE.asRuntimeException();
            }
            var data = new RetrievedCardData(JsonParser.parseString(response.body()).getAsJsonObject(), retrievedAt);
            T result = decode.apply(data);
            cache.put(uri, data);
            if (cache.size() > MAX_CACHED_RESPONSES) {
                cache.remove(cache.keySet().iterator().next());
            }
            return result;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw UNAVAILABLE.asRuntimeException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Status.CANCELLED.withDescription("The Scryfall request was interrupted.").asRuntimeException();
        }
    }

    private static void checkCancelled() {
        if (Context.current().isCancelled()) {
            throw Status.CANCELLED.withDescription("The Scryfall request was cancelled.").asRuntimeException();
        }
    }

    private static String normalizePrintingId(String value) {
        try {
            String canonical = UUID.fromString(value).toString();
            if (canonical.equalsIgnoreCase(value)) {
                return canonical;
            }
        } catch (IllegalArgumentException ignored) {
            // The canonical comparison also rejects abbreviated UUIDs.
        }
        throw Status.INVALID_ARGUMENT.withDescription("Scryfall printing ID must be a UUID.").asRuntimeException();
    }

    private static Card readCard(JsonObject source) {
        String id = text(source, "id");
        String name = text(source, "name");
        JsonArray colors = array(source, "color_identity");
        var identity = colors == null ? null : CardColors.parse(colors.toString());
        if (id == null || name == null || identity == null) {
            throw UNAVAILABLE.asRuntimeException();
        }
        Card.Builder card = Card.newBuilder().setScryfallId(id).setName(name).setColorIdentity(identity);
        String image = imageUrl(source);
        if (image != null) {
            card.addImageUrls(image);
        } else {
            JsonArray faces = array(source, "card_faces");
            if (faces != null) {
                for (JsonElement face : faces) {
                    if (face.isJsonObject()) {
                        String faceImage = imageUrl(face.getAsJsonObject());
                        if (faceImage != null) {
                            card.addImageUrls(faceImage);
                        }
                    }
                }
            }
        }
        JsonArray finishes = array(source, "finishes");
        if (finishes != null) {
            for (JsonElement finish : finishes) {
                String finishName = text(finish);
                if (finishName == null) {
                    continue;
                }
                CardFinish mapped = switch (finishName) {
                    case "nonfoil" -> CardFinish.CARD_FINISH_NONFOIL;
                    case "foil" -> CardFinish.CARD_FINISH_FOIL;
                    case "etched" -> CardFinish.CARD_FINISH_ETCHED;
                    default -> CardFinish.CARD_FINISH_UNSPECIFIED;
                };
                if (mapped != CardFinish.CARD_FINISH_UNSPECIFIED) {
                    card.addAvailableFinishes(mapped);
                }
            }
        }
        return card.build();
    }

    private static String imageUrl(JsonObject source) {
        JsonObject images = object(source, "image_uris");
        String display = text(images, "display");
        return display == null ? text(images, "normal") : display;
    }

    private static JsonObject object(JsonObject source, String key) {
        JsonElement value = source.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject source, String key) {
        JsonElement value = source.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static String text(JsonObject source, String key) {
        return source == null ? null : text(source.get(key));
    }

    private static String text(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        String content = value.getAsString();
        return content.isBlank() ? null : content;
    }

    private record RetrievedCardData(JsonObject body, long retrievedAtSeconds) {}

    record Printing(Card card, String storedColorIdentity, JsonObject prices, long retrievedAtSeconds) {
        CardPrice price(CardFinish finish) {
            String suffix = switch (finish) {
                case CARD_FINISH_NONFOIL -> "";
                case CARD_FINISH_FOIL -> "_foil";
                case CARD_FINISH_ETCHED -> "_etched";
                default -> throw new IllegalArgumentException("A specific finish is required.");
            };
            String currency = "USD";
            String amount = text(prices, "usd" + suffix);
            if (amount == null) {
                currency = "EUR";
                amount = text(prices, "eur" + suffix);
            }
            return amount == null ? null : CardPrice.newBuilder().setFinish(finish).setAmount(amount)
                    .setCurrencyCode(currency).setRetrievedAtUnixSeconds(retrievedAtSeconds).build();
        }
    }
}
