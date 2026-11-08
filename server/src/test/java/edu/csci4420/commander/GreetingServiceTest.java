package edu.csci4420.commander;

import edu.csci4420.commander.proto.GreeterGrpc;
import edu.csci4420.commander.proto.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

// These tests send real gRPC requests to a local server.
// Port 0 lets the server pick an available port so tests don't conflict.
public class GreetingServiceTest {
    private Server server;
    private ManagedChannel channel;

    @Before public void start() throws Exception {
        server = ServerBuilder.forPort(0).addService(new GreetingService()).build().start();
        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
    }
    @After public void stop() throws Exception {
        if (channel != null) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        if (server != null) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    private String greet(String name) {
        return GreeterGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS)
                .sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
    }
    @Test public void greetsNamedClient() {
        assertEquals("Hello Cayden! Connection successful.", greet("Cayden"));
    }
    @Test public void defaultsBlankName() {
        assertEquals("Hello Android! Connection successful.", greet("   "));
    }
}
