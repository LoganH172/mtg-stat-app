package com.example.mtgstatapp.client

import android.content.Context
import edu.csci4420.commander.proto.GreeterGrpcKt
import edu.csci4420.commander.proto.HelloRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.android.AndroidChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.TimeUnit

/*Client Stub. Will update with final proto to handle methods. Passed into viewModel for separation*/
class ClientService(
    context: Context,
    host : String,
    port : Int
): Closeable {
    private val channel: ManagedChannel = AndroidChannelBuilder
        .forAddress(host, port)
        .context(context.applicationContext)
        .usePlaintext()
        .build()
    private val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    suspend fun sayHello(userName: String): String = withContext(Dispatchers.IO) {
        val request = HelloRequest.newBuilder()
            .setName(userName)
            .build()

        val response = stub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .sayHello(request)

        response.message
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}