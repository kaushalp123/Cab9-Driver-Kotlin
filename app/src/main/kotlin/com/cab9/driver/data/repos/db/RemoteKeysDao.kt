package com.cab9.driver.data.repos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cab9.driver.data.models.RemoteKey

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrReplace(remoteKey: RemoteKey)

    @Query("SELECT * FROM remote_keys")
    suspend fun remoteKeyByQuery(): List<RemoteKey>

    @Query("DELETE FROM remote_keys")
    suspend fun deleteByQuery()

}