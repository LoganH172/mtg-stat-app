package com.example.mtgstatapp.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Upsert

@Dao
interface CacheDAO {
    //TODO - Implement final local queries (queries present are for testing and demonstrating a local cache)

    @Upsert
    suspend fun cachePlayerWithToken(player : cPlayerEntity)



}