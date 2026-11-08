package edu.csci4420.commander;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.concurrent.TimeUnit;

public final class GreetingServer {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 50051 : Integer.parseInt(args[0]);
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
