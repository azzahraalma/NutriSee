package com.example.nutrisee.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeightHistoryDao {

    @Insert
    suspend fun insert(history: WeightHistory)

    @Query("""
        SELECT * FROM weight_history
        WHERE userId = :userId
        ORDER BY date ASC
    """)
    suspend fun getAll(
        userId: Int
    ): List<WeightHistory>

    @Query("""
        SELECT * FROM weight_history
        WHERE userId = :userId
        AND date >= :startDate
        ORDER BY date ASC
    """)
    suspend fun getFromDate(
        userId: Int,
        startDate: Long
    ): List<WeightHistory>
}