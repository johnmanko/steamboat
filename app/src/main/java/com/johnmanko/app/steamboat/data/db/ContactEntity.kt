package com.johnmanko.app.steamboat.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "contacts",
    primaryKeys = ["lookupKey", "matchKey"],
    indices = [Index(value = ["matchKey"])],
)
data class ContactEntity(
    val lookupKey: String,
    val displayName: String,
    val phoneNumber: String,
    val matchKey: String,
    val photoUri: String?,
)
