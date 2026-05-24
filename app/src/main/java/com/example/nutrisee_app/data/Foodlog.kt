package com.example.nutrisee.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_log")
data class FoodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val nama: String,
    val kategori: String,
    val porsi: Int,
    val kalori: Float,
    val karbo: Float,
    val protein: Float,
    val lemak: Float,
    val serat: Float,
    val tanggal: Long
)