package com.example.nutrisee.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: ActivityLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<ActivityLog>)

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND tanggal = :tanggal ORDER BY waktuMulai DESC")
    fun getByTanggal(userId: Int, tanggal: String): Flow<List<ActivityLog>>

    @Query("SELECT COUNT(*) FROM activity_logs WHERE userId = :userId AND waktuMulai = :waktuMulai AND waktuSelesai = :waktuSelesai")
    suspend fun countByTimeRange(userId: Int, waktuMulai: Long, waktuSelesai: Long): Int


    @Query("DELETE FROM activity_logs WHERE userId = :userId AND tanggal = :tanggal")
    suspend fun deleteByTanggal(userId: Int, tanggal: String)
}