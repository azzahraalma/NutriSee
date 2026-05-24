package com.example.nutrisee.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Int = 1,

    val nama: String,

    val icon: String,

    val kaloriTerbakar: Double,
    val durasiMenit: Int,
    val waktuMulai: Long,
    val waktuSelesai: Long,
    val tanggal: String
)