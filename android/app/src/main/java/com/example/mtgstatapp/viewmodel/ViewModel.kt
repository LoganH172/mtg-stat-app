package com.example.mtgstatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgstatapp.client.ClientService
import edu.csci4420.commander.proto.ColorIdentity
import edu.csci4420.commander.proto.ColorIdentityRecord
import edu.csci4420.commander.proto.PlayerProfile
import edu.csci4420.commander.proto.WinLossRecord
import edu.csci4420.commander.proto.mostPlayedColorIdentityOrNull
import io.grpc.StatusException
import kotlinx.coroutines.launch

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
    var currentPlayerMostPlayerColorIdentity by mutableStateOf(ColorIdentity.getDefaultInstance())


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

    fun createMatch(){}
    fun joinMatch(){}
    fun getMatch(){}
    fun selectCommander(){}
    fun submitMatch(){}
    fun matchHistory(){}



    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
