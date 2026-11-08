package com.example.mtgstatapp

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

enum class NavigationScreens(
    val label : String,
    @DrawableRes val icon : Int,
    val contentDescription : String
){
    HOME(label = "Home", icon = R.drawable.rounded_dashboard_24, contentDescription = "Home Screen"),
    LEADERBOARD(label = "Leaderboard", icon = R.drawable.rounded_leaderboard_24, contentDescription = "Leaderboard Screen")
}
