package com.example.mtgstatapp.composables

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mtgstatapp.viewmodel.ViewModel

/*Since either registration or loggining in occured, currentPlayer should be populated, and since the header
* will let the server know who is sending the request it should match who just logged in.*/
@Composable
fun TEST_SCREEN(viewModel: ViewModel){
    LaunchedEffect(viewModel.currentPlayer.username)
    {
        if (viewModel.currentPlayer.username.isNotEmpty())
            viewModel.playerStats()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly)
    {
        Text("${viewModel.currentPlayer.username} is currently logged in"  )
        Log.d("CommanderGRPC", "Player ID: ${viewModel.currentPlayer.playerId}")
        Text("Player Wins: ${viewModel.currentPlayerWLRecord.wins} wins\n" +
                "Player Losses: ${viewModel.currentPlayerWLRecord.losses} losses\n" +
                "Player Winrate: ${viewModel.currentPlayerWLRecord.winPercentage}%")

        viewModel.currentPlayerMatchHistory.forEach { match ->
            Text("Match Data: \n${match.matchId} \n${match.roomCode} \n${match.participantsList} \n${match.result} ")
        }
    }
}