package com.johnmanko.app.steamboat.data.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface ContactDao {
    @Query(
        """
        SELECT * FROM contacts
        ORDER BY displayName COLLATE NOCASE, phoneNumber
        """,
    )
    fun paging(): PagingSource<Int, ContactEntity>

    @Query(
        """
        SELECT * FROM contacts
        WHERE displayName LIKE '%' || :query || '%' ESCAPE '\'
           OR (:digits != '' AND matchKey LIKE '%' || :digits || '%')
        ORDER BY displayName COLLATE NOCASE, phoneNumber
        """,
    )
    fun search(query: String, digits: String): PagingSource<Int, ContactEntity>

    @Query("SELECT * FROM contacts WHERE matchKey = :matchKey LIMIT 1")
    suspend fun findByMatchKey(matchKey: String): ContactEntity?

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Transaction
    suspend fun replaceAll(contacts: List<ContactEntity>) {
        deleteAll()
        insertAll(contacts)
    }
}
