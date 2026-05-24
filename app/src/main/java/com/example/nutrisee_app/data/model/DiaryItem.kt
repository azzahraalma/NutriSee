package com.example.nutrisee.data.model

import com.example.nutrisee.data.ActivityLog
import com.example.nutrisee.data.FoodLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DiaryItem {

    abstract val waktuMs: Long

    val waktuFormatted: String
        get() = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(waktuMs))

    // ---- Makanan ----//
    data class Makanan(
        val id: Long,
        val nama: String,
        val kategori: String,
        val kalori: Double,
        val porsi: Int,
        override val waktuMs: Long
    ) : DiaryItem()

    // ---- Aktivitas ----//
    data class Aktivitas(
        val id: Long,
        val nama: String,
        val icon: String,
        val kaloriTerbakar: Double,
        val durasiMenit: Int,
        override val waktuMs: Long
    ) : DiaryItem()
}

fun FoodLog.toDiaryItem() = DiaryItem.Makanan(
    id       = id.toLong(),
    nama     = nama,
    kategori = kategori,
    kalori   = kalori.toDouble(),
    porsi    = porsi,
    waktuMs  = tanggal
)

fun ActivityLog.toDiaryItem() = DiaryItem.Aktivitas(
    id              = id.toLong(),
    nama            = nama,
    icon            = icon,
    kaloriTerbakar  = kaloriTerbakar.toDouble(),
    durasiMenit     = durasiMenit,
    waktuMs         = waktuMulai
)