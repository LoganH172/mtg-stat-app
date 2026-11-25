package com.example.mtgstatapp

import androidx.annotation.DrawableRes

/*Class holding data associated with NavScreen. Some enum values are not currently used, and are deprecated.
* Labels may be removed depending on design limitations*/
enum class NavBarScreens(
    val label : String,
    @param:DrawableRes val icon : Int,
    val contentDescription : String
){
    HOME(label = "Home", icon = R.drawable.rounded_person_24, contentDescription = "Home Screen"),
    //LEADERBOARD(label = "Leaderboard", icon = R.drawable.rounded_leaderboard_24, contentDescription = "Leaderboard Screen"),
    COMMANDERS(label = "My Commanders", icon = R.drawable.rounded_crown_24, contentDescription = "My Commander Screen"),
    GAMES(label = "Games", icon = R.drawable.rounded_note_add_24, contentDescription = "Game Screen"),
    PLAYGROUP(label = "Playgroup", icon = R.drawable.rounded_group_24, contentDescription = "Playgroup Screen"),
    //PLAYER(label = "Player", icon = R.drawable.rounded_person_24, contentDescription = "Player Screen")
}
