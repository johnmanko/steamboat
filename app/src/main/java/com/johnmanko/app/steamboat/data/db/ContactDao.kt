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
           OR phoneNumber LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY displayName COLLATE NOCASE, phoneNumber
        """,
    )
    fun search(query: String): PagingSource<Int, ContactEntity>

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
