package com.cab9.driver.data.repos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cab9.driver.data.models.ChatMessagesEntity
import com.cab9.driver.data.models.RemoteKey
import com.squareup.moshi.Moshi
import javax.inject.Inject

@Database(entities = [ChatMessagesEntity::class, RemoteKey::class], version = 1, exportSchema = false)
@TypeConverters(RoomTypeConverter::class)
abstract class ChatRoomDataBase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: ChatRoomDataBase? = null

        fun getDatabase(context: Context): ChatRoomDataBase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatRoomDataBase::class.java,
                    "chat_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}