package edu.csci4420.commander;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import java.util.concurrent.TimeUnit;

public final class GreetingServer {
    public static void main(String[] args) throws Exception {
        int port = 50051;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Enter a whole number from 1 to 65535.");
                System.err.println("Example: gradlew.bat run --args=50052");
                System.exit(1);
                return;
            }
        }
        ServerBuilder<?> builder = ServerBuilder.forPort(port).addService(new GreetingService());
        String dbUrl = System.getenv("COMMANDER_DB_URL");
        if (dbUrl != null) {
            String dbUser = System.getenv("COMMANDER_DB_USER");
            String dbPassword = System.getenv("COMMANDER_DB_PASSWORD");
            if (dbUrl.isBlank() || dbUser == null || dbUser.isBlank() || dbPassword == null) {
                throw new IllegalArgumentException(
                        "Set COMMANDER_DB_URL, COMMANDER_DB_USER, and COMMANDER_DB_PASSWORD to enable auth.");
            }
            Database database = new Database(dbUrl, dbUser, dbPassword);
            SessionAuthInterceptor sessionAuth = new SessionAuthInterceptor(database);
            ScryfallClient scryfall = new ScryfallClient();
            builder.addService(new AuthService(database));
            builder.addService(ServerInterceptors.intercept(new ProfileStatsService(database), sessionAuth));
            builder.addService(ServerInterceptors.intercept(new MatchRoomService(database,
                    new MatchCardCatalog(database, scryfall)), sessionAuth));
            builder.addService(ServerInterceptors.intercept(new ScryfallCardService(scryfall), sessionAuth));
        }
        Server server = builder.build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) server.shutdownNow();
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
        System.out.println("Commander server listening on port " + server.getPort()
                + (dbUrl == null ? " (greeting only)" : " (greeting, auth, profile stats, match rooms, and cards)"));
        server.awaitTermination();
    }
}
