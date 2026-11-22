package com.example.mtgstatapp.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import javax.annotation.Nonnull

@Entity(tableName = "players")
data class cPlayerEntity(
    @PrimaryKey (autoGenerate = false)
    val id : String,
    @param:Nonnull
    val username: String,
    @param:Nonnull
    val session_token: String,
    @param:Nonnull
    val token_exp : Long
    )
@Entity(tableName = "commander_cards")
data class cCommanderCardEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val name : String,
    val color_id : String
)
@Entity(
    tableName = "commander_and_player",
    foreignKeys = [
        ForeignKey(
            entity = cPlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = cCommanderCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["commanderId"],
        )
    ],
    indices = [
        Index(value = ["commanderId"]),
    ],
    primaryKeys = ["playerId", "commanderId"])
data class cCommander2PlayerEntity(
    val playerId : String,
    val commanderId : String
)

@Entity(tableName = "games")
data class cGameEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val timestamp : Long
)

@Entity(
    tableName = "game_members",
    foreignKeys = [
        ForeignKey(
            entity = cPlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = cCommanderCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["commanderId"],
        ),
        ForeignKey(
            entity = cPlaygroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["playgroupId"],
        ),
        ForeignKey(
            entity = cGameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
        )
    ],
    primaryKeys = ["gameId", "playerId"])
data class cGameDataEnity(
    val gameId : String,
    val playerId: String,
    val playgroupId : String,
    val commanderId: String,
    val won : Boolean,
    val standoutCardId : String
)

@Entity(tableName = "playgroup")
data class cPlaygroupEntity(
    @PrimaryKey(autoGenerate = false)
    val id : String,
    val name: String
)

@Entity(
    tableName = "playgroup_members",
    foreignKeys = [
        ForeignKey(
            entity = cPlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = cPlaygroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["playgroupId"],
            onDelete = CASCADE
        )
    ],
    primaryKeys = ["playgroupId", "playerId"])
data class cPlaygroupDataEntity(
    val playgroupId: String,
    val playerId: String,
    val role : String
)

