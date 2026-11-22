package com.example.mtgstatapp.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        cPlayerEntity::class,
        cGameEntity::class,
        cPlaygroupEntity::class,
        cCommanderCardEntity::class,
        cGameDataEnity::class,
        cCommander2PlayerEntity::class,
        cPlaygroupDataEntity::class],
    version = 1)
abstract class  LocalCache : RoomDatabase() {
    abstract fun cacheDAO() : CacheDAO
    companion object {
        @Volatile private var INSTANCE: LocalCache? = null

        fun getInstance(context: Context): LocalCache {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LocalCache::class.java,
                    "local_cache"
                ).build().also { INSTANCE = it }
            }
        }
    }
}