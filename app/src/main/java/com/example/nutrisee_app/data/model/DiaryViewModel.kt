package com.example.nutrisee.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutrisee.FirebaseManager
import com.example.nutrisee.data.model.DiaryItem
import com.example.nutrisee.data.repository.GoogleFitRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    val googleFitRepo = GoogleFitRepository(application)
    private val sdf   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val uid   get() = FirebaseManager.auth.currentUser?.uid ?: ""

    private val _selectedDate     = MutableLiveData(getTodayString())
    val selectedDate: LiveData<String> = _selectedDate

    private val _diaryItems       = MutableLiveData<List<DiaryItem>>(emptyList())
    val diaryItems: LiveData<List<DiaryItem>> = _diaryItems

    private val _kaloriDikonsumsi = MutableLiveData(0)
    val kaloriDikonsumsi: LiveData<Int> = _kaloriDikonsumsi

    private val _kaloriDibakar    = MutableLiveData(0)
    val kaloriDibakar: LiveData<Int> = _kaloriDibakar

    private val _netKalori        = MutableLiveData(0)
    val netKalori: LiveData<Int> = _netKalori

    private val _syncStatus       = MutableLiveData<SyncStatus>(SyncStatus.Idle)
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private var foodListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null

    private var cachedFoodItems: List<DiaryItem.Makanan>       = emptyList()
    private var cachedActivityItems: List<DiaryItem.Aktivitas> = emptyList()

    init {
        selectDate(getTodayString())
    }

    fun getTodayString(): String = sdf.format(Date())

    fun selectDate(tanggal: String) {
        _selectedDate.value = tanggal
        listenFoodLogs(tanggal)
        listenActivityLogs(tanggal)
    }

    private fun dateStringToRange(tanggal: String): Pair<Long, Long> {
        val date = sdf.parse(tanggal) ?: Date()
        val cal  = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0);  cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);       cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59);      cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    private fun listenFoodLogs(tanggal: String) {
        foodListener?.remove()
        if (uid.isEmpty()) return

        val (start, end) = dateStringToRange(tanggal)

        foodListener = FirebaseManager.db
            .collection("users")
            .document(uid)
            .collection("foodLogs")
            .whereGreaterThanOrEqualTo("tanggal", start)
            .whereLessThanOrEqualTo("tanggal", end)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                cachedFoodItems = snap.documents.mapNotNull { doc ->
                    try {
                        DiaryItem.Makanan(
                            id       = doc.getLong("id")?.toLong() ?: 0L,
                            nama     = doc.getString("nama") ?: "",
                            kategori = doc.getString("kategori") ?: "",
                            kalori   = doc.getDouble("kalori") ?: 0.0,
                            porsi    = (doc.getLong("porsi") ?: 1L).toInt(),
                            waktuMs  = doc.getLong("tanggal") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                mergeAndEmit()
            }
    }

    private fun listenActivityLogs(tanggal: String) {
        activityListener?.remove()
        if (uid.isEmpty()) return

        activityListener = FirebaseManager.db
            .collection("users")
            .document(uid)
            .collection("activityLogs")
            .whereEqualTo("tanggal", tanggal)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                cachedActivityItems = snap.documents.mapNotNull { doc ->
                    try {
                        DiaryItem.Aktivitas(
                            id             = doc.getLong("id")?.toLong() ?: 0L,
                            nama           = doc.getString("nama") ?: "",
                            icon           = doc.getString("icon") ?: "",
                            kaloriTerbakar = doc.getDouble("kaloriTerbakar") ?: 0.0,
                            durasiMenit    = (doc.getLong("durasiMenit") ?: 0L).toInt(),
                            waktuMs        = doc.getLong("waktuMulai") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                mergeAndEmit()
            }
    }

    private fun mergeAndEmit() {
        val all = (cachedFoodItems + cachedActivityItems)
            .sortedByDescending { it.waktuMs }
        _diaryItems.postValue(all)

        val masuk  = cachedFoodItems.sumOf { it.kalori }.toInt()
        val keluar = cachedActivityItems.sumOf { it.kaloriTerbakar }.toInt()
        _kaloriDikonsumsi.postValue(masuk)
        _kaloriDibakar.postValue(keluar)
        _netKalori.postValue(masuk - keluar)
    }

    fun syncGoogleFit(tanggal: String = getTodayString()) {
        if (!googleFitRepo.isConnected()) {
            _syncStatus.value = SyncStatus.NotConnected
            return
        }
        _syncStatus.value = SyncStatus.Loading

        viewModelScope.launch {
            val result = googleFitRepo.syncAktivitasHariIni(tanggal)
            result.fold(
                onSuccess = { count ->
                    syncActivityRoomToFirestore(tanggal)
                    _syncStatus.value = SyncStatus.Success(count)
                },
                onFailure = { e ->
                    _syncStatus.value = SyncStatus.Error(e.message ?: "Gagal sync")
                }
            )
        }
    }

    private suspend fun syncActivityRoomToFirestore(tanggal: String) {
        try {
            val context = getApplication<Application>()
            val db      = com.example.nutrisee.data.AppDatabase.getInstance(context)
            val logs    = db.activityLogDao().getByTanggal(
                userId  = 1,
                tanggal = tanggal
            )

            var collected = false
            logs.collect { activities ->
                if (collected) return@collect
                collected = true

                activities.forEach { activity ->
                    val docId = "${activity.waktuMulai}_${activity.waktuSelesai}"
                    val data  = hashMapOf(
                        "id"             to activity.id,
                        "nama"           to activity.nama,
                        "icon"           to activity.icon,
                        "kaloriTerbakar" to activity.kaloriTerbakar,
                        "durasiMenit"    to activity.durasiMenit,
                        "waktuMulai"     to activity.waktuMulai,
                        "waktuSelesai"   to activity.waktuSelesai,
                        "tanggal"        to tanggal
                    )
                    FirebaseManager.db
                        .collection("users")
                        .document(uid)
                        .collection("activityLogs")
                        .document(docId)
                        .set(data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteAllForDate(tanggal: String) {
        if (uid.isEmpty()) return
        val (start, end) = dateStringToRange(tanggal)

        FirebaseManager.db
            .collection("users")
            .document(uid)
            .collection("foodLogs")
            .whereGreaterThanOrEqualTo("tanggal", start)
            .whereLessThanOrEqualTo("tanggal", end)
            .get()
            .addOnSuccessListener { snap ->
                val batch = FirebaseManager.db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }

        FirebaseManager.db
            .collection("users")
            .document(uid)
            .collection("activityLogs")
            .whereEqualTo("tanggal", tanggal)
            .get()
            .addOnSuccessListener { snap ->
                val batch = FirebaseManager.db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    override fun onCleared() {
        super.onCleared()
        foodListener?.remove()
        activityListener?.remove()
    }

    sealed class SyncStatus {
        object Idle         : SyncStatus()
        object Loading      : SyncStatus()
        object NotConnected : SyncStatus()
        data class Success(val count: Int)    : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}