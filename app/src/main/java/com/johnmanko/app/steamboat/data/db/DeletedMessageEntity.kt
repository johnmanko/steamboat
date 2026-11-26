package com.johnmanko.app.steamboat.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "deleted_messages",
    indices = [Index(value = ["providerKey"], unique = true)],
)
data class DeletedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerKey: String,
)
