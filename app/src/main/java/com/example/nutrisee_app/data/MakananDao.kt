package com.example.nutrisee.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutrisee.data.entity.MakananEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MakananDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MakananEntry)

    @Query("""
        SELECT * FROM makanan_entries
        WHERE tanggal = :tanggal
        ORDER BY waktuMs DESC
    """)
    fun getByTanggal(tanggal: String): Flow<List<MakananEntry>>

    @Delete
    suspend fun delete(entry: MakananEntry)

    @Query("DELETE FROM makanan_entries WHERE tanggal = :tanggal")
    suspend fun deleteByTanggal(tanggal: String)
}