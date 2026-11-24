package com.example.mtgstatapp.composables

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtgstatapp.NavigationRoutes
import com.example.mtgstatapp.viewmodel.ViewModel

/*Since either registration or loggining in occured, currentPlayer should be populated, and since the header
* will let the server know who is sending the request it should match who just logged in.*/
@Composable
fun TEST_SCREEN(viewModel: ViewModel, navHostController: NavHostController){
    LaunchedEffect(viewModel.currentPlayer.username)
    {
        if (viewModel.currentPlayer.username.isNotEmpty())
            viewModel.playerStats()
    }

    if (viewModel.currentPlayer.username.isNotEmpty())
    {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {

            Text("Waiting for player information...")
        }

    }
    else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        )
        {
            //Player Stats Information
            Text("${viewModel.currentPlayer.username} is currently logged in")
            Log.d("CommanderGRPC", "Player ID: ${viewModel.currentPlayer.playerId}")
            Text(
                "Player Wins: ${viewModel.currentPlayerWLRecord.wins} wins\n" +
                        "Player Losses: ${viewModel.currentPlayerWLRecord.losses} losses\n" +
                        "Player Winrate: ${viewModel.currentPlayerWLRecord.winPercentage}%"
            )

            //Match Making Information
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally)
            {

                //Generates and locally stores match from server
                Button(onClick = {viewModel.createMatch()}) {Text("Create Match")}
                //Selection Container allows for copying
                Text("Room Code:")
                SelectionContainer{Text("${viewModel.serverMatch.roomCode}")}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    //Joins newly created match.
                    TextField(
                        value = viewModel.roomCode,
                        onValueChange = {viewModel.roomCode = it},
                        label = {Text("Enter room code")})
                    Button(onClick = {viewModel.joinMatch(viewModel.roomCode.trim())}){Text("Join Match")}
                }

                //Sets information from user to pass to call
                Button(onClick = {}){Text("Submit Match")}

            }

            //Match History Information
            Text("Total number of matches: ${viewModel.currentPlayerMatchHistory.size}")
            viewModel.currentPlayerMatchHistory.forEach { match ->
                Text("Match Data: \n${match.matchId} \n${match.roomCode} \n${match.participantsList} \n${match.result} ")
            }

            //Logs player out by setting token to null also pops back to login
            Button(onClick = {
                viewModel.logout()
                navHostController.popBackStack(route = NavigationRoutes.LOGIN.name, inclusive = false)
            }) {
                Text("Logout")
            }
        }
    }
}
