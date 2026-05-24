package com.example.nutrisee.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodLog: FoodLog)

    // Semua log user untuk satu hari tertentu (Flow untuk reactive)
    @Query("""
        SELECT * FROM food_log 
        WHERE userId = :userId 
        AND tanggal BETWEEN :startOfDay AND :endOfDay 
        ORDER BY tanggal DESC
    """)
    fun getByTanggal(userId: Int, startOfDay: Long, endOfDay: Long): Flow<List<FoodLog>>

    // Semua log user (Flow untuk reactive)
    @Query("SELECT * FROM food_log WHERE userId = :userId ORDER BY tanggal DESC")
    fun getAll(userId: Int): Flow<List<FoodLog>>

    // ─── SUSPEND FUNCTIONS (One-time query) ─────────────────────────────

    // Semua log user hari ini (suspend)
    @Query("""
        SELECT * FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startOfDay
        ORDER BY tanggal DESC
    """)
    suspend fun getTodayLogs(userId: Int, startOfDay: Long): List<FoodLog>

    // Total kalori hari ini
    @Query("""
        SELECT COALESCE(SUM(kalori), 0) FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startOfDay
    """)
    suspend fun getTodayCalories(userId: Int, startOfDay: Long): Float

    // Total karbo hari ini
    @Query("""
        SELECT COALESCE(SUM(karbo), 0) FROM food_log
        WHERE userId = :userId AND tanggal >= :startOfDay
    """)
    suspend fun getTodayCarbs(userId: Int, startOfDay: Long): Float

    // Total protein hari ini
    @Query("""
        SELECT COALESCE(SUM(protein), 0) FROM food_log
        WHERE userId = :userId AND tanggal >= :startOfDay
    """)
    suspend fun getTodayProtein(userId: Int, startOfDay: Long): Float

    // Total lemak hari ini
    @Query("""
        SELECT COALESCE(SUM(lemak), 0) FROM food_log
        WHERE userId = :userId AND tanggal >= :startOfDay
    """)
    suspend fun getTodayFat(userId: Int, startOfDay: Long): Float

    // ─── QUERY UNTUK RENTANG WAKTU (WEEKLY CHART) ───────────────────────

    // Kalori dalam rentang waktu
    @Query("""
        SELECT COALESCE(SUM(kalori), 0) FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startMs
        AND tanggal < :endMs
    """)
    suspend fun getCaloriesInRange(userId: Int, startMs: Long, endMs: Long): Float

    // Karbo dalam rentang waktu
    @Query("""
        SELECT COALESCE(SUM(karbo), 0) FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startMs
        AND tanggal < :endMs
    """)
    suspend fun getCarbsInRange(userId: Int, startMs: Long, endMs: Long): Float

    // Protein dalam rentang waktu
    @Query("""
        SELECT COALESCE(SUM(protein), 0) FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startMs
        AND tanggal < :endMs
    """)
    suspend fun getProteinInRange(userId: Int, startMs: Long, endMs: Long): Float

    @Query("""
        SELECT COALESCE(SUM(lemak), 0) FROM food_log
        WHERE userId = :userId
        AND tanggal >= :startMs
        AND tanggal < :endMs
    """)
    suspend fun getFatInRange(userId: Int, startMs: Long, endMs: Long): Float

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM food_log WHERE userId = :userId")
    suspend fun clearAll(userId: Int)

    @Query("DELETE FROM food_log WHERE userId = :userId AND tanggal BETWEEN :startOfDay AND :endOfDay")
    suspend fun deleteByDateRange(userId: Int, startOfDay: Long, endOfDay: Long)
}