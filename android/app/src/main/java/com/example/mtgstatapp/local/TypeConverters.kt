package com.example.mtgstatapp.local

import edu.csci4420.commander.proto.PlayerProfile

/*Eventually will be populated with all type converters for all entities. Local Cache is still work in progress, as
* we do clean-up with server-database and other data */
fun PlayerProfile.toEntity() : cPlayerEntity {
    return cPlayerEntity(
        this.playerId,
        this.username
    )
}

fun cPlayerEntity.toProto() : PlayerProfile{
    return PlayerProfile.newBuilder()
        .setPlayerId(this.id)
        .setUsername(this.username)
        .build()
}