package edu.csci4420.commander;

import edu.csci4420.commander.proto.Card;
import edu.csci4420.commander.proto.CardFinish;
import edu.csci4420.commander.proto.CardPrice;
import edu.csci4420.commander.proto.CardServiceGrpc;
import edu.csci4420.commander.proto.GetCardReply;
import edu.csci4420.commander.proto.GetCardRequest;
import edu.csci4420.commander.proto.SearchCardsRequest;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

final class ScryfallCardService extends CardServiceGrpc.CardServiceImplBase {
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    // Scryfall returns up to 175 cards in the first search page.
    private static final int MAX_SEARCH_LIMIT = 100;
    private static final int MAX_QUERY_LENGTH = 1_000;

    private final ScryfallClient scryfall;

    ScryfallCardService(ScryfallClient scryfall) {
        this.scryfall = scryfall;
    }

    @Override
    public void searchCards(SearchCardsRequest request, StreamObserver<Card> response) {
        try {
            String query = request.getQuery().strip();
            if (request.getCommandersOnly()) {
                query = query.isEmpty() ? "is:commander" : "(" + query + ") is:commander";
            }
            if (query.isEmpty() || query.codePointCount(0, query.length()) > MAX_QUERY_LENGTH) {
                throw Status.INVALID_ARGUMENT.withDescription("Provide a card search query of up to 1,000 characters.")
                        .asRuntimeException();
            }
            long requested = Integer.toUnsignedLong(request.getLimit());
            int limit = requested == 0 ? DEFAULT_SEARCH_LIMIT : (int) Math.min(requested, MAX_SEARCH_LIMIT);
            for (Card card : scryfall.search(query, limit)) {
                if (Context.current().isCancelled()) {
                    return;
                }
                response.onNext(card);
            }
            response.onCompleted();
        } catch (StatusRuntimeException error) {
            response.onError(error);
        }
    }

    @Override
    public void getCard(GetCardRequest request, StreamObserver<GetCardReply> response) {
        try {
            CardFinish finish = request.getFinish();
            if (request.hasFinish() && (finish == CardFinish.CARD_FINISH_UNSPECIFIED || finish == CardFinish.UNRECOGNIZED)) {
                throw Status.INVALID_ARGUMENT.withDescription("Provide a specific card finish or omit it.")
                        .asRuntimeException();
            }
            ScryfallClient.Printing printing = scryfall.lookup(request.getScryfallId());
            if (printing == null) {
                throw Status.NOT_FOUND.withDescription("Scryfall printing ID was not found.").asRuntimeException();
            }
            GetCardReply.Builder reply = GetCardReply.newBuilder().setCard(printing.card());
            if (request.hasFinish()) {
                CardPrice price = printing.price(finish);
                if (price != null) {
                    reply.setPrice(price);
                }
            }
            response.onNext(reply.build());
            response.onCompleted();
        } catch (StatusRuntimeException error) {
            response.onError(error);
        }
    }
}
