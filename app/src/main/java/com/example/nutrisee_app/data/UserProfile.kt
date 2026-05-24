package com.example.nutrisee.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val nama: String,
    val jenisKelamin: String,
    val tanggalLahir: String,
    val berat: Float,
    val tinggi: Float,
    val targetBerat: Float,
    val fotoPath: String? = null
) {
    fun hitungUmur(): Int {
        return try {
            val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = format.parse(tanggalLahir) ?: return 20
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) {
            20
        }
    }

    fun bmi(): Float {
        if (tinggi <= 0) return 0f
        val tinggiM = tinggi / 100f
        return berat / (tinggiM * tinggiM)
    }

    fun bmiCategory(): String {
        return when {
            bmi() < 18.5f -> "Kurus"
            bmi() < 25.0f -> "Normal"
            bmi() < 30.0f -> "Kelebihan Berat"
            else -> "Obesitas"
        }
    }

    fun bmiCategoryColor(): Int {
        return when {
            bmi() < 18.5f -> android.graphics.Color.parseColor("#2196F3")
            bmi() < 25.0f -> android.graphics.Color.parseColor("#4CAF50")
            bmi() < 30.0f -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#F44336")
        }
    }

    fun targetCalories(): Int {
        val umur = hitungUmur()
        if (tinggi <= 0 || berat <= 0 || umur <= 0) return 1500
        val bmr = if (jenisKelamin.lowercase().contains("perempuan") || jenisKelamin.lowercase().contains("wanita")) {
            (10 * berat + 6.25f * tinggi - 5 * umur - 161).toInt()
        } else {
            (10 * berat + 6.25f * tinggi - 5 * umur + 5).toInt()
        }
        return (bmr * 1.2f - 300).toInt().coerceAtLeast(1200)
    }
}