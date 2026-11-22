package com.example.mtgstatapp.client

import android.content.Context
import edu.csci4420.commander.proto.AuthServiceGrpcKt
import edu.csci4420.commander.proto.GreeterGrpcKt
import edu.csci4420.commander.proto.HelloRequest
import edu.csci4420.commander.proto.LoginRequest
import edu.csci4420.commander.proto.PlayerProfile
import edu.csci4420.commander.proto.RegisterRequest
import edu.csci4420.commander.proto.playerOrNull
import io.grpc.ManagedChannel
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
    private val testStub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    private val authenticatorStub = AuthServiceGrpcKt.AuthServiceCoroutineStub(channel)

    //Server call to test connection
    suspend fun sayHello(userName: String): String = withContext(Dispatchers.IO) {
        val request = HelloRequest.newBuilder()
            .setName(userName)
            .build()

        val response = testStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .sayHello(request)

        response.message
    }

    suspend fun registerNewPlayer(username : String, password: String): PlayerProfile? = withContext(Dispatchers.IO){
        val request = RegisterRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()
        val response = authenticatorStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .register(request)

        response.playerOrNull
    }

    suspend fun loginExistingPlayer(username : String, password: String): PlayerProfile? = withContext(Dispatchers.IO){
        val request = LoginRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()
        val response = authenticatorStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .login(request)

        response.playerOrNull
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}