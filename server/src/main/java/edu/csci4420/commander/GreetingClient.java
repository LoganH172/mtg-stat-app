package edu.csci4420.commander;

import edu.csci4420.commander.proto.GreeterGrpc;
import edu.csci4420.commander.proto.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

public final class GreetingClient {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 50051;
        String name = args.length > 2 ? args[2] : "Android";
        // Using plaintext for the local Milestone 1 demo.
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        try {
            System.out.println(GreeterGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
