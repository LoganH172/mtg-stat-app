package com.example.mtgstatapp.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/*Playgroup Screen will look similar to that of the Home Screen, but will analyze a playgroup's metrics.
* Color breakdown of group, last game played, how often you win in specific group, how often commander wins. */
@Composable
fun PlaygroupScreen(){
    Column(
        modifier = Modifier
            .background(Color(0xFF1C4DA4))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Playgroup",
            fontSize = 40.sp,
            color = Color(0xFFFFFFFF)
        )
    }
}