package com.example.nutrisee.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "makanan_entries")
data class MakananEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val kategori: String,
    val kalori: Int,
    val karbo: Int,
    val protein: Int,
    val lemak: Int,
    val serat: Int,
    val porsi: Int,
    val waktuMs: Long = System.currentTimeMillis(),
    val tanggal: String
)