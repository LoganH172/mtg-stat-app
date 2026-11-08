package edu.csci4420.commander;

import edu.csci4420.commander.proto.GreeterGrpc;
import edu.csci4420.commander.proto.HelloReply;
import edu.csci4420.commander.proto.HelloRequest;
import io.grpc.stub.StreamObserver;

public final class GreetingService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> observer) {
        String name = request.getName().trim();
        if (name.isEmpty()) name = "Android";
        observer.onNext(HelloReply.newBuilder()
                .setMessage("Hello " + name + "! Connection successful.").build());
        observer.onCompleted();
    }
}
