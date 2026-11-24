package com.example.mtgstatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgstatapp.client.ClientService
import edu.csci4420.commander.proto.Card
import edu.csci4420.commander.proto.ColorIdentity
import edu.csci4420.commander.proto.ColorIdentityRecord
import edu.csci4420.commander.proto.LeaderboardEntry
import edu.csci4420.commander.proto.LosingCard
import edu.csci4420.commander.proto.Match
import edu.csci4420.commander.proto.PlayerProfile
import edu.csci4420.commander.proto.WinLossRecord
import edu.csci4420.commander.proto.mostPlayedColorIdentityOrNull
import io.grpc.StatusException
import kotlinx.coroutines.launch
import java.util.UUID

class ViewModel(application: Application): AndroidViewModel(application) {
    /*Had to do some debugging and figuring, but my own local network somehow claimed the 10.0.0.0/24 host
    * which caused some issues. Feel free to change to 10.0.2.2. Alternatively you can port forward to fix
    * some issues and change host to localhost. I included some commands to help port forward in the updated README.
    * Otherwise this is the layout fot the viewModel, which will handle data from gRPC calls and pass into UI elements
    * for updating. */
    private val client = ClientService(application, "10.0.0.193", 50051)

    //For state of UI on LoginScreen
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var newUserRegister by mutableStateOf(true)
    var loginScreenState by mutableStateOf("Login")

    //Takes data from gRPC calls to pass into UI elements across app
    var currentPlayer by mutableStateOf(PlayerProfile.getDefaultInstance())
    //Breaks the GetProfileStatsReply into its parts for easier parsing
    var currentPlayerWLRecord by mutableStateOf(WinLossRecord.getDefaultInstance())
    var currentPlayerColorIdentityRecord by mutableStateOf<List<ColorIdentityRecord>>(emptyList())
    var currentPlayerMostPlayerColorIdentity: ColorIdentity? by mutableStateOf(ColorIdentity.getDefaultInstance())

    var pageSize by mutableStateOf(0)
    var pageToken by mutableStateOf("")
    var leaderboardPage by mutableStateOf<List<LeaderboardEntry>>(emptyList())
    var currentPlayerMatchHistory by mutableStateOf<List<Match>>(emptyList())
    var storedRequestID by mutableStateOf("")
    var serverMatch by mutableStateOf(Match.getDefaultInstance())

    var winningPlayerID by mutableStateOf("")

    var losingCards by mutableStateOf<List<LosingCard>>(emptyList())

    var commanderID by mutableStateOf("")

    var selectedMatchByCommander by mutableStateOf(Match.getDefaultInstance())

    var roomCode by mutableStateOf("")




    fun greet(name: String) {
        viewModelScope.launch {
            try {
                val reply = client.sayHello(name)
                Log.d("CommanderGRPC", "Server response: $reply")
            } catch (e: StatusException) {
                Log.e("CommanderGRPC", "RPC failed: ${e.status}", e)
            }
        }
    }

    /*Will add session passing at later date*/
    fun playerLogin(username : String, password: String)
    {
        viewModelScope.launch {
            try {
                currentPlayer = client.loginExistingPlayer(username, password).player
                Log.d("CommanderGRPC", "User logged in")
            }
            catch (e: StatusException){
                Log.e("CommanderGRPC", "Login failed: ${e.status}", e)}
        }
    }
    fun playerRegister(username : String, password: String)
    {
        viewModelScope.launch {
            try {
                currentPlayer = client.registerNewPlayer(username, password).player
                Log.d("CommanderGRPC", "User registered")
            }
            catch (e: StatusException){
                Log.e("CommanderGRPC", "Registration failed: ${e.status}", e)}
        }
    }

    fun playerStats()
    {
        viewModelScope.launch {
         try {
             val currentPlayerStats = client.getProfileStats()
             currentPlayerWLRecord = currentPlayerStats.overall
             currentPlayerColorIdentityRecord = currentPlayerStats.recordsByColorIdentityList
             currentPlayerMostPlayerColorIdentity = currentPlayerStats.mostPlayedColorIdentityOrNull
             Log.d("CommanderGRPC", "Player stats gathered and stored")
         }
         catch (e: StatusException){
             Log.e("CommanderGRPC", "Getting player stats failed: ${e.status}", e)
         }
        }
    }

    fun leaderboard(pageLimit : Int, pageInfo : String){
        viewModelScope.launch {
            try {
                leaderboardPage = client.leaderboardList(pageLimit, pageInfo).entriesList
            }
            catch (e: StatusException)
            {
                Log.e("CommanderGRPC", "Retrieving leaderboard failed: ${e.status}", e)
            }
        }
    }

    fun createMatch(){
        viewModelScope.launch {
            try {
                storedRequestID = UUID.randomUUID().toString()
                serverMatch = client.matchCreation(storedRequestID)
                Log.d("CommanderGRPC", "Created Match:  ${serverMatch.matchId}")

            }
            catch (e: StatusException)
            {
                Log.e("CommanderGRPC", "Creating match failed: ${e.status}", e)
            }
        }
    }
    fun joinMatch(roomCode : String) {
        viewModelScope.launch {
            try {
                val joinedMatch = client.matchJoin(roomCode)
                Log.d("CommanderGRPC", "Joined Match:  ${joinedMatch.matchId}")
            } catch (e: StatusException) {
                Log.e("CommanderGRPC", "Joining match failed: ${e.status}", e)
            }
        }
    }

    fun getMatch(matchID : String) {
        viewModelScope.launch {
            try {
                val retrievedMatch = client.matchGet(matchID)
                Log.d("CommanderGRPC", "Retrieved Match: ${retrievedMatch.matchId}")

            } catch (e: StatusException) {
                Log.e("CommanderGRPC", "Retrieving match failed: ${e.status}", e)
            }
        }
    }
        fun selectCommander(matchID: String, commanderID : String) {
            viewModelScope.launch {
                try {
                    selectedMatchByCommander = client.matchCommanderSelect(matchID, commanderID)
                    Log.d("CommanderGRPC", "Match with commander found: ${selectedMatchByCommander.matchId}")
                }
                catch (e: StatusException)
                {
                    Log.e("CommanderGRPC", "Unable to find match with commander: ${e.status}", e)
                }
            }
        }
        fun submitMatch(matchID: String, winningPlayerID: String, losingCards: List<LosingCard>) {
            viewModelScope.launch {
                try {
                    val submittedMatch = client.matchSubmit(matchID, winningPlayerID, losingCards)
                    Log.d("CommanderGRPC", "Submitted match successfully")
                }
                catch (e: StatusException)
                {
                    Log.e("CommanderGRPC", "Submitting match failed: ${e.status}", e)
                }
            }
        }

        fun matchHistory(pageLimit: Int, pageInfo: String) {
            viewModelScope.launch {
                try {
                    currentPlayerMatchHistory = client.matchHistory(pageLimit, pageInfo).matchesList
                } catch (e: StatusException) {
                    Log.e("CommanderGRPC", "Retrieving match history failed: ${e.status}", e)
                }
            }
        }


    //Passes client logout to viewModel for ease of use.
    fun logout(){
        client.logout()
    }


    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
