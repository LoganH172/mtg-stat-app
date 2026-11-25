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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtgstatapp.NavigationRoutes
import com.example.mtgstatapp.viewmodel.ViewModel
import edu.csci4420.commander.proto.Match

/*Since either registration or loggining in occured, currentPlayer should be populated, and since the header
* will let the server know who is sending the request it should match who just logged in.*/

@Composable
fun TEST_SCREEN(viewModel: ViewModel, navHostController: NavHostController){
    //Test commander UUIDs, from Scryfall
    val yshtola = "c7f2c2d5-e052-49e8-b5de-712858c2ea78"
    val edgar = "8d94b8ec-ecda-43c8-a60e-1ba33e6a54a4"
    val urdragon = "7e78b70b-0c67-4f14-8ad7-c9f8e3f59743"
    val atraxa = "c374a73c-2d23-4834-aca9-efe079da717b"

    //On launch executes, and only executes again upon username being updated.
    LaunchedEffect(viewModel.currentPlayer.username)
    {
        if (viewModel.currentPlayer.username.isNotEmpty())
            viewModel.playerStats()
    }

    //Waits until data is populated before showing information screen
    if (viewModel.currentPlayer.username.isEmpty())
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

                //Generates and locally stores Match data from server
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

                //Shows number of participants
                Text("Players in ${viewModel.roomCode}: ${viewModel.serverMatch.participantsList.count()}")

                //Select commander per player using dropdown
                var isDropdown by remember{mutableStateOf(false)}
                Button(onClick = {isDropdown = true}) {Text("Click to Select Commander") }
                DropdownMenu(
                    expanded = isDropdown,
                    onDismissRequest = {isDropdown = !isDropdown}
                ) {
                    //Each item holds text, based on whats in that text call selectCommander for the match
                    //Since calls are authenticated, this should attribute a player to a commander for the match
                    DropdownMenuItem(
                        text = {Text("Edgar Markov")},
                        onClick = {
                            viewModel.commanderID = edgar
                            viewModel.selectCommander(viewModel.serverMatch.matchId, viewModel.commanderID)
                        }
                    )
                    DropdownMenuItem(
                        text = {Text("The Ur-Dragon")},
                        onClick = {
                            viewModel.commanderID = urdragon
                            viewModel.selectCommander(viewModel.serverMatch.matchId, viewModel.commanderID)
                        }
                    )
                    DropdownMenuItem(
                        text = {Text("Y'shtola, Night's Blessed")},
                        onClick = {
                            viewModel.commanderID = yshtola
                            viewModel.selectCommander(viewModel.serverMatch.matchId, viewModel.commanderID)
                        }
                    )
                    DropdownMenuItem(
                        text = {Text("Atraxa, Grand Purifier")},
                        onClick = {
                            viewModel.commanderID = atraxa
                            viewModel.selectCommander(viewModel.serverMatch.matchId, viewModel.commanderID)
                        }
                    )
                }
            }
            //Assuming client can only have one open game at a time. It should be stored in the viewModel
            //Gets the lists of participants (members of room, makes it into a useable list (since proto lists or immutable)
            val playersInMatch = viewModel.serverMatch.participantsList.toMutableList()
            //Select a random player to "win"
            val winningPlayer = playersInMatch.randomOrNull()
            viewModel.winningPlayerID = winningPlayer?.player?.playerId.toString()
            //Sets information from user to pass to call if is the player who created the match and the match data has been set
            if (viewModel.currentPlayer.playerId.equals(viewModel.serverMatch.createdByPlayerId) &&
                !viewModel.serverMatch.equals(Match.getDefaultInstance()))
            {
                Button(onClick = {
                    viewModel.submitMatch(
                        viewModel.serverMatch.matchId,
                        viewModel.winningPlayerID,
                        viewModel.losingCards)
                    //Clean data on submission to prevent duplicates
                    viewModel.serverMatch = Match.getDefaultInstance()
                    viewModel.roomCode = ""
                }){Text("Submit Match")}
            }
            //Match History Information
            viewModel.matchHistory(0, "")
            Text("Total number of matches: ${viewModel.currentPlayerMatchHistory.size}")
            viewModel.currentPlayerMatchHistory.forEach { match ->
                Text("Match Data: \n${match.matchId} \n${match.roomCode} \n${match.participantsList} \n${match.result} ")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Button(onClick = {
                    navHostController.navigate(NavigationRoutes.SCRYFALL.name) })
                {
                    Text("To Scryfall Test")
                }
                //Logs player out by setting token to null also pops back to login
                Button(onClick = {
                    viewModel.logout()
                    navHostController.popBackStack(route = NavigationRoutes.LOGIN.name, inclusive = false)
                    //Cleanup viewModel so that users don't have persisting data from old users
                    viewModel.roomCode = ""
                    viewModel.serverMatch = Match.getDefaultInstance()
                }) {
                    Text("Logout")
                }
            }

        }
    }
}
