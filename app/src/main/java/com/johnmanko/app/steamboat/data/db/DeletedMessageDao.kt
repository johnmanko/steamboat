package com.johnmanko.app.steamboat.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface DeletedMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<DeletedMessageEntity>)

    @Query("SELECT providerKey FROM deleted_messages WHERE providerKey = :providerKey LIMIT 1")
    suspend fun find(providerKey: String): String?
}
