package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteMangaEntity(
    @PrimaryKey val id: String, // e.g. "200719"
    val title: String,
    val img: String?,
    val type: String?,
    val score: String?,
    val status: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryMangaEntity(
    @PrimaryKey val id: String, // e.g. "200719" (so each manga has one history entry)
    val mangaTitle: String,
    val img: String?,
    val chapterTitle: String, // e.g. "Chapter 41"
    val chapterId: String, // e.g. "425726"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MangaDao {
    // Favorites
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteMangaEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id LIMIT 1)")
    fun isFavoriteFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id LIMIT 1)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(manga: FavoriteMangaEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryMangaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryMangaEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Database(entities = [FavoriteMangaEntity::class, HistoryMangaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nefusoft_manga_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
