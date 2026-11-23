package com.example.mtgstatapp.client

import android.content.Context
import com.google.common.primitives.Shorts
import edu.csci4420.commander.proto.AuthReply
import edu.csci4420.commander.proto.AuthServiceGrpcKt
import edu.csci4420.commander.proto.CardServiceGrpcKt
import edu.csci4420.commander.proto.CreateMatchRequest
import edu.csci4420.commander.proto.GetCardRequest
import edu.csci4420.commander.proto.GetMatchRequest
import edu.csci4420.commander.proto.GetProfileStatsReply
import edu.csci4420.commander.proto.GetProfileStatsRequest
import edu.csci4420.commander.proto.GreeterGrpcKt
import edu.csci4420.commander.proto.HelloRequest
import edu.csci4420.commander.proto.JoinMatchRequest
import edu.csci4420.commander.proto.ListLeaderboardReply
import edu.csci4420.commander.proto.ListLeaderboardRequest
import edu.csci4420.commander.proto.ListMatchHistoryReply
import edu.csci4420.commander.proto.ListMatchHistoryRequest
import edu.csci4420.commander.proto.LoginRequest
import edu.csci4420.commander.proto.LosingCard
import edu.csci4420.commander.proto.Match
import edu.csci4420.commander.proto.MatchServiceGrpcKt
import edu.csci4420.commander.proto.PlayerProfile
import edu.csci4420.commander.proto.RegisterRequest
import edu.csci4420.commander.proto.SelectCommanderRequest
import edu.csci4420.commander.proto.StatsServiceGrpcKt
import edu.csci4420.commander.proto.SubmitMatchResultRequest
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
    private val cardStub = CardServiceGrpcKt.CardServiceCoroutineStub(channel)
    private val matchStub = MatchServiceGrpcKt.MatchServiceCoroutineStub(channel)
    private val statsStub = StatsServiceGrpcKt.StatsServiceCoroutineStub(channel)

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

    suspend fun registerNewPlayer(username : String, password: String): AuthReply = withContext(Dispatchers.IO){
        val request = RegisterRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()
        val response = authenticatorStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .register(request)

        response
    }

    suspend fun loginExistingPlayer(username : String, password: String): AuthReply = withContext(Dispatchers.IO){
        val request = LoginRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()
        val response = authenticatorStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .login(request)

        response
    }

    suspend fun getProfileStats(): GetProfileStatsReply = withContext(Dispatchers.IO){
        val request = GetProfileStatsRequest.newBuilder().build()
        val response = statsStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .getProfileStats(request)

        response
    }

    suspend fun leaderboardList(pageLimit : Int, pageToken : String): ListLeaderboardReply = withContext(Dispatchers.IO){
        val request = ListLeaderboardRequest.newBuilder()
            .setPageSize(pageLimit)
            .setPageToken(pageToken)
            .build()
        val response = statsStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .listLeaderboard(request)
        response
    }

    //Optional field left out
    suspend fun matchCreation(requestID : String): Match = withContext(Dispatchers.IO){
        val request = CreateMatchRequest.newBuilder()
            .setRequestId(requestID)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .createMatch(request)
        response
    }
    //Assuming room code is hashed request/match ID
    suspend fun matchJoin(roomCode : String): Match = withContext(Dispatchers.IO){
        val request = JoinMatchRequest.newBuilder()
            .setRoomCode(roomCode)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .joinMatch(request)
        response
    }

    suspend fun matchGet(matchID : String): Match = withContext(Dispatchers.IO){
        val request = GetMatchRequest.newBuilder()
            .setMatchId(matchID)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .getMatch(request)
        response
    }

    //Optional fields left out
    suspend fun matchSubmit(matchID : String, winningPlayerID : String, losingCards : List<LosingCard>): Match = withContext(Dispatchers.IO){
        val request = SubmitMatchResultRequest.newBuilder()
            .setMatchId(matchID)
            .setWinnerPlayerId(winningPlayerID)
            .addAllLosingCards(losingCards)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .submitMatchResult(request)
        response
    }

    suspend fun matchCommanderSelect(matchID: String, commanderScryfallID : String): Match = withContext(Dispatchers.IO){
        val request = SelectCommanderRequest.newBuilder()
            .setMatchId(matchID)
            .setCommanderScryfallId(commanderScryfallID)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .selectCommander(request)
        response
    }

    suspend fun matchHistory(pageLimit : Int, pageToken : String): ListMatchHistoryReply = withContext(Dispatchers.IO){
        val request = ListMatchHistoryRequest.newBuilder()
            .setPageSize(pageLimit)
            .setPageToken(pageToken)
            .build()
        val response = matchStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .listMatchHistory(request)
        response
    }
    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}