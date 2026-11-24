package com.johnmanko.app.steamboat.data.themes

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "theme_bundles")
data class ThemeBundleEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val framework: Int,
    val name: String,
    val origin: String,
    val account: String?,
    val installedAt: Long,
)

@Entity(
    tableName = "themes",
    primaryKeys = ["bundleId", "themeId"],
    foreignKeys = [
        ForeignKey(
            entity = ThemeBundleEntity::class,
            parentColumns = ["id"],
            childColumns = ["bundleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bundleId")],
)
data class ThemeEntity(
    val bundleId: String,
    val themeId: String,
    val version: Int,
    val name: String,
    val background: String,
    val foreground: String,
    val cursor: String,
    val colors: String,
    val previewOutgoing: String,
    val previewIncoming: String,
    val source: String,
    val framework: Int,
)

@Dao
interface ThemeDao {
    @Query("SELECT version FROM theme_bundles WHERE id = :id")
    suspend fun bundleVersion(id: String): Int?

    @Query("SELECT themeId FROM themes WHERE bundleId = :bundleId AND source = :source")
    suspend fun themeIds(bundleId: String, source: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBundle(bundle: ThemeBundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThemes(themes: List<ThemeEntity>)

    @Query("DELETE FROM themes WHERE bundleId = :bundleId AND source = :source AND themeId NOT IN (:keep)")
    suspend fun deleteSourceExcept(bundleId: String, source: String, keep: List<String>)

    @Query("DELETE FROM themes WHERE bundleId = :bundleId AND source = :source")
    suspend fun deleteSource(bundleId: String, source: String)

    @Query("SELECT * FROM themes")
    suspend fun listAll(): List<ThemeEntity>

    @Query("SELECT * FROM themes")
    fun observeAll(): Flow<List<ThemeEntity>>
}

internal fun ThemeEntity.toInstalled(): InstalledTheme = InstalledTheme(
    bundleId = bundleId,
    themeId = themeId,
    version = version,
    name = name,
    background = background,
    foreground = foreground,
    cursor = cursor,
    colors = colors.split(' '),
    previewOutgoing = previewOutgoing,
    previewIncoming = previewIncoming,
    source = source,
    framework = framework,
)
