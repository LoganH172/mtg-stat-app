package com.example.mtgstatapp.composables


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mtgstatapp.R

/*Intention is to have the home screen act as the player data screen.This way specific game data can be another screen
* with more granularity. The home screen will only show the most recent commander, date of last game, wins, losses, winrate,
* and possibly commander color breakdown if possible through Scryfall integration.*/
@Composable
fun HomeScreen(){
    //Future goal -> Break all complex elements into own composable function and let main call handle layout
    Box(
        modifier = Modifier
            .background(Color(0xFF1C4DA4))
            .fillMaxSize()
    )
    {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.size(0.dp, 50.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    painter = painterResource(R.drawable.test_image),
                    contentDescription = "LAST PLAYED COMMANDER"
                )
                Canvas(
                    modifier = Modifier.size(200.dp)
                )
                {
                    inset(30F, { drawCircle(color = Color(0xFFFFFFFF)) })

                }
            }
            Spacer(modifier = Modifier.size(0.dp, 100.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Canvas(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                {
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F)
                            )
                        })
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F),
                                topLeft = Offset(0F, 250F)
                            )
                        })
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F),
                                topLeft = Offset(0F, 500F)
                            )
                        })

                }

            }

        }
        //Works adjust layout later
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FloatingDrawer()
        }
    }
}
//Composable for floating action drawer when player clicks the floating action add button
@Composable
fun FloatingDrawer(){
    var expand by remember{mutableStateOf(false)}
    FloatingActionButtonMenu(
        expanded = expand,
        button =
            {
                ToggleFloatingActionButton(
                    checked = expand,
                    onCheckedChange = {expand = !expand})
                {
                    Icon(painter = painterResource(R.drawable.rounded_add_24), contentDescription = "add")
                }
            })
    {
        FloatingActionButtonMenuItem(
            onClick = {},
            text = {Text("Create new game")},
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_note_add_24),
                    contentDescription = "Add game") }
        )
        FloatingActionButtonMenuItem(
            onClick = {},
            text = {Text("Create a new playgroup")},
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_group_24),
                    contentDescription = "Add playgroup") }
        )
        FloatingActionButtonMenuItem(
            onClick = {},
            text = {Text("Join a playgroup")},
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_group_24),
                    contentDescription = "Join playgroup") }
        )
    }
}