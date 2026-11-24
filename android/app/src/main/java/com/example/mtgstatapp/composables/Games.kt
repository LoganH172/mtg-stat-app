package com.example.mtgstatapp.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

/*Currently placeholder text, but will show game data sourced from database in LazyList with Card composables
* for each game. Data to include would be players, their associated commanders and who won. Playgroups should be
* implemented to allow for filtering by playgroup.*/

@Composable
fun GameScreen(){
    Column(
        modifier = Modifier
            .background(Color(0xFF1C4DA4))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Game",
            fontSize = 40.sp,
            color = Color(0xFFFFFFFF)
        )
    }
}

//Reusable and mutable game card to be called from LazyColumn, for each game of current player
//Can "dropdown" for more granular view
@Composable
fun GameCard(){
    Card() {Text("Game:") }
}