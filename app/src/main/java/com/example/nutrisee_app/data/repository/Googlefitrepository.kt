package com.example.nutrisee.data.repository

import android.content.Context
import android.util.Log
import com.example.nutrisee.data.ActivityLog
import com.example.nutrisee.data.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GoogleFitRepository(private val context: Context) {

    private val TAG = "GoogleFitRepository"

    private val beratBadanKg = 60.0

    val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .build()


    fun isConnected(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    suspend fun syncAktivitasHariIni(tanggal: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Akun Google belum login"))

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                return@withContext Result.failure(Exception("Izin Google Fit belum diberikan"))
            }

            // Rentang waktu: 00:00:00 sampai 23:59:59 hari ini
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val endTime = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val startTime = cal.timeInMillis

            val readRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            val db = AppDatabase.getInstance(context)
            val dao = db.activityLogDao()

            // Hapus data hari ini dulu supaya tidak duplikat saat sync ulang
            dao.deleteByTanggal(userId = 1, tanggal = tanggal)

            val logs = mutableListOf<ActivityLog>()

            val dataSet = response.getDataSet(DataType.TYPE_ACTIVITY_SEGMENT)

            for (dp in dataSet.dataPoints) {

                val activityType = dp.getValue(Field.FIELD_ACTIVITY).asInt()

                val activityName = getActivityName(activityType)

                val startMs = dp.getStartTime(TimeUnit.MILLISECONDS)
                val endMs   = dp.getEndTime(TimeUnit.MILLISECONDS)
                val durasiMenit = ((endMs - startMs) / 60_000L).toInt()

                if (durasiMenit < 2) continue
                if (activityName == "UNKNOWN") continue

                logs.add(
                    ActivityLog(
                        userId          = 1,
                        nama            = mapNamaAktivitas(activityName),
                        icon            = mapIconAktivitas(activityName),
                        kaloriTerbakar  = hitungKalori(activityName, durasiMenit),
                        durasiMenit     = durasiMenit,
                        waktuMulai      = startMs,
                        waktuSelesai    = endMs,
                        tanggal         = tanggal
                    )
                )
            }

            if (logs.isNotEmpty()) {
                dao.insertAll(logs)
            }
            Log.d(TAG, "Sync selesai: ${logs.size} aktivitas disimpan untuk $tanggal")
            Result.success(logs.size)

        } catch (e: Exception) {
            Log.e(TAG, "Sync gagal: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getActivityName(type: Int): String {
        return when (type) {
            7   -> "WALKING"
            8   -> "RUNNING"
            1   -> "BIKING"
            9   -> "SWIMMING"
            3   -> "STILL"
            0   -> "IN_VEHICLE"
            9   -> "AEROBICS"
            14  -> "YOGA"
            11  -> "STRENGTH_TRAINING"
            13  -> "STAIR_CLIMBING"
            else -> "UNKNOWN"
        }
    }

    private fun mapNamaAktivitas(type: String): String = when (type) {
        "WALKING"           -> "Jalan Kaki"
        "RUNNING"           -> "Berlari"
        "BIKING"            -> "Bersepeda"
        "SWIMMING"          -> "Berenang"
        "STILL"             -> "Diam"
        "IN_VEHICLE"        -> "Berkendara"
        "AEROBICS"          -> "Aerobik"
        "YOGA"              -> "Yoga"
        "STRENGTH_TRAINING" -> "Latihan Kekuatan"
        "STAIR_CLIMBING"    -> "Naik Tangga"
        else -> type.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
    }

    private fun mapIconAktivitas(type: String): String = when (type) {
        "WALKING"           -> "🚶"
        "RUNNING"           -> "🏃"
        "BIKING"            -> "🚴"
        "SWIMMING"          -> "🏊"
        "STILL"             -> "🧘"
        "IN_VEHICLE"        -> "🚗"
        "AEROBICS"          -> "🤸"
        "YOGA"              -> "🧘"
        "STRENGTH_TRAINING" -> "🏋️"
        "STAIR_CLIMBING"    -> "🪜"
        else -> "⚡"
    }

    private fun hitungKalori(type: String, durasiMenit: Int): Double {
        val met = when (type) {
            "WALKING"           -> 3.5
            "RUNNING"           -> 8.0
            "BIKING"            -> 6.0
            "SWIMMING"          -> 7.0
            "AEROBICS"          -> 6.5
            "YOGA"              -> 3.0
            "STRENGTH_TRAINING" -> 5.0
            "STAIR_CLIMBING"    -> 4.0
            "STILL"             -> 1.0
            "IN_VEHICLE"        -> 1.5
            else                -> 2.0
        }
        val durasiJam = durasiMenit / 60.0
        return met * beratBadanKg * durasiJam
    }
}