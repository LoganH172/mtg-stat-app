package edu.csci4420.commander;

import io.grpc.Server;
import io.grpc.ServerBuilder;
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
        Server server = ServerBuilder.forPort(port).addService(new GreetingService()).build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) server.shutdownNow();
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
        System.out.println("Greeting server listening on port " + server.getPort());
        server.awaitTermination();
    }
}
