package com.example.nutrisee.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrisee.FirebaseManager
import com.example.nutrisee.R
import com.example.nutrisee.databinding.ActivityHomeBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.firestore.Query
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var userId: Int = -1
    private var uid: String = ""
    private var selectedNutritionWeek: Int = 0
    private var selectedWeightDays: Int = 7

    private var targetCalories: Int = 1500
    private var targetBerat: Float  = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) { finish(); return }
        uid = FirebaseManager.auth.currentUser?.uid ?: run { finish(); return }

        loadProfile()
        setupWeightTabs()
        setupNutritionTabs()
        setupBottomNav()

        if (intent.getBooleanExtra("OPEN_TAMBAH", false)) openTambahData()

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.fragmentContainer.visibility = View.GONE
                loadProfile()
                loadNutritionWeekChart(selectedNutritionWeek)
                loadWeightChart(selectedWeightDays)
            }
        }

        binding.fabAdd.setOnClickListener { openTambahData() }
        binding.btnTambahBerat.setOnClickListener { showTambahBeratDialog() }
        binding.btnDiary.setOnClickListener { openDiary() }
    }

    override fun onResume() {
        super.onResume()
        if (uid.isNotEmpty()) {
            loadProfile()
            loadNutritionWeekChart(selectedNutritionWeek)
            loadWeightChart(selectedWeightDays)
        }
    }

    private fun setupBottomNav() {
        binding.navBeranda.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                binding.fragmentContainer.visibility = View.GONE
            }
        }
        binding.navProfil.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java).apply {
                putExtra("USER_ID", userId)
            })
        }
    }

    private fun openTambahData() {
        binding.fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, TambahDataFragment())
            .addToBackStack("tambah_data")
            .commit()
    }

    private fun openDiary() {
        binding.fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, DiaryFragment())
            .addToBackStack("diary")
            .commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            binding.fragmentContainer.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun loadProfile() {
        FirebaseManager.db.collection("users").document(uid)
            .collection("profile").document("data")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val nama        = doc.getString("nama") ?: "-"
                val tinggi      = (doc.getDouble("tinggi") ?: 0.0).toFloat()
                val berat       = (doc.getDouble("berat") ?: 0.0).toFloat()
                val target      = (doc.getDouble("targetBerat") ?: 0.0).toFloat()
                val tglLahir    = doc.getString("tanggalLahir") ?: ""
                val jenisKelamin = doc.getString("jenisKelamin") ?: "Laki-laki"

                targetBerat = target

                val umur = hitungUmur(tglLahir)

                val bmr = if (jenisKelamin.contains("perempuan", ignoreCase = true) ||
                    jenisKelamin.contains("wanita", ignoreCase = true)) {
                    (10 * berat + 6.25f * tinggi - 5 * umur - 161).toInt()
                } else {
                    (10 * berat + 6.25f * tinggi - 5 * umur + 5).toInt()
                }
                targetCalories = (bmr * 1.2f - 300).toInt().coerceAtLeast(1200)

                val bmi = if (tinggi > 0) berat / ((tinggi / 100f) * (tinggi / 100f)) else 0f
                val bmiCategory = when {
                    bmi < 18.5f -> "Kurus"
                    bmi < 25.0f -> "Normal"
                    bmi < 30.0f -> "Kelebihan Berat"
                    else        -> "Obesitas"
                }
                val bmiColor = when {
                    bmi < 18.5f -> "#2196F3"
                    bmi < 25.0f -> "#4CAF50"
                    bmi < 30.0f -> "#FF9800"
                    else        -> "#F44336"
                }

                binding.txtName.text   = nama
                binding.txtAge.text    = "${umur}th"
                binding.txtHeight.text = "${tinggi.toInt()}cm"
                binding.txtWeight.text = "${berat.toInt()}kg"
                binding.txtTarget.text = "${target.toInt()}kg"
                binding.txtCalories.text = "$targetCalories cal"

                binding.txtBmi.text       = String.format("%.1f", bmi)
                binding.txtBmiStatus.text = bmiCategory
                val bg = binding.txtBmiStatus.background
                if (bg is GradientDrawable) bg.setColor(Color.parseColor(bmiColor))
                setupBmiIndicator(bmi)

                loadTodayNutrition()
                ensureInitialWeightSeeded(berat)
                loadNutritionWeekChart(selectedNutritionWeek)
            }
    }

    private fun hitungUmur(tanggalLahir: String): Int {
        return try {
            val sdf   = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            val birth = sdf.parse(tanggalLahir) ?: return 20
            val today = Calendar.getInstance()
            val bCal  = Calendar.getInstance().apply { time = birth }
            var age   = today.get(Calendar.YEAR) - bCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < bCal.get(Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) { 20 }
    }

    private fun loadTodayNutrition() {
        val startOfDay = getStartOfDayMillis()
        val endOfDay   = startOfDay + 24 * 60 * 60 * 1000L

        FirebaseManager.db.collection("users").document(uid)
            .collection("foodLogs")
            .whereGreaterThanOrEqualTo("tanggal", startOfDay)
            .whereLessThan("tanggal", endOfDay)
            .get()
            .addOnSuccessListener { snap ->
                var totalKalori  = 0f
                var totalKarbo   = 0f
                var totalProtein = 0f
                var totalLemak   = 0f

                snap.documents.forEach { doc ->
                    totalKalori  += (doc.getDouble("kalori")  ?: 0.0).toFloat()
                    totalKarbo   += (doc.getDouble("karbo")   ?: 0.0).toFloat()
                    totalProtein += (doc.getDouble("protein") ?: 0.0).toFloat()
                    totalLemak   += (doc.getDouble("lemak")   ?: 0.0).toFloat()
                }

                val consumed = totalKalori.toInt()
                binding.txtConsumed.text        = consumed.toString()
                binding.txtBurned.text          = "0"
                binding.txtCurrentCalories.text = consumed.toString()

                val calorieProgress = (totalKalori / targetCalories.toFloat()).coerceIn(0f, 1f)
                animateNumber(binding.calorieProgress.progress, (calorieProgress * 100).toInt()) {
                    binding.calorieProgress.progress = it
                }

                val carbTarget    = (targetCalories * 0.55f / 4).toInt().coerceAtLeast(1)
                val proteinTarget = (targetCalories * 0.20f / 4).toInt().coerceAtLeast(1)
                val fatTarget     = (targetCalories * 0.25f / 9).toInt().coerceAtLeast(1)

                binding.txtCarbs.text   = "${totalKarbo.toInt()}/${carbTarget}g"
                binding.txtProtein.text = "${totalProtein.toInt()}/${proteinTarget}g"
                binding.txtFat.text     = "${totalLemak.toInt()}/${fatTarget}g"

                setupNutritionProgress(totalKarbo.toInt(),   carbTarget,    binding.carbsProgress)
                setupNutritionProgress(totalProtein.toInt(), proteinTarget, binding.proteinProgress)
                setupNutritionProgress(totalLemak.toInt(),   fatTarget,     binding.fatProgress)

                val avgNutrition = if (consumed == 0) 0f else listOf(
                    totalKarbo   / carbTarget,
                    totalProtein / proteinTarget,
                    totalLemak   / fatTarget
                ).average().toFloat().coerceIn(0f, 1.5f)

                binding.txtNutritionStatus.text = when {
                    consumed == 0        -> "Belum ada data"
                    avgNutrition < 0.3f  -> "Kurang"
                    avgNutrition < 0.7f  -> "Cukup"
                    avgNutrition <= 1.0f -> "Bagus"
                    else                 -> "Berlebih"
                }
                binding.txtNutritionStatus.setTextColor(when {
                    consumed == 0        -> Color.parseColor("#999999")
                    avgNutrition < 0.3f  -> Color.parseColor("#F44336")
                    avgNutrition < 0.7f  -> Color.parseColor("#FFC107")
                    avgNutrition <= 1.0f -> Color.parseColor("#4CAF50")
                    else                 -> Color.parseColor("#FF5722")
                })
            }
    }

    private fun ensureInitialWeightSeeded(profileWeight: Float) {
        if (profileWeight <= 0f) return
        FirebaseManager.db.collection("users").document(uid)
            .collection("weightHistory")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    val data = hashMapOf(
                        "weight" to profileWeight,
                        "date"   to System.currentTimeMillis()
                    )
                    FirebaseManager.db.collection("users").document(uid)
                        .collection("weightHistory")
                        .add(data)
                }
            }
    }

    private fun showTambahBeratDialog() {
        val dp = resources.displayMetrics.density

        fun makeInputBg(strokeColor: String) = GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke((2*dp).toInt(), Color.parseColor(strokeColor))
            cornerRadius = 12f * dp
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28f * dp
            }
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (16*dp).toInt())
        }
        headerLayout.addView(TextView(this).apply {
            text     = "Catat Berat Badan"
            textSize = 20f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerLayout.addView(TextView(this).apply {
            text     = "Masukkan berat badanmu hari ini"
            textSize = 13f
            setTextColor(Color.parseColor("#7CB99A"))
            setPadding(0, (4*dp).toInt(), 0, 0)
        })

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1*dp).toInt())
            setBackgroundColor(Color.parseColor("#E8F5EE"))
        }

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*dp).toInt(), (20*dp).toInt(), (24*dp).toInt(), (8*dp).toInt())
        }
        inputLayout.addView(TextView(this).apply {
            text     = "Berat Badan (kg)"
            textSize = 13f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, (8*dp).toInt())
        })

        val etBerat = EditText(this).apply {
            hint      = "Contoh: 70.5"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize  = 18f
            setTextColor(Color.parseColor("#1A1A1A"))
            setHintTextColor(Color.parseColor("#AACABB"))
            setPadding((16*dp).toInt(), (14*dp).toInt(), (16*dp).toInt(), (14*dp).toInt())
            background = makeInputBg("#0D7A45")
        }
        inputLayout.addView(etBerat)

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.END
            weightSum   = 2f
            setPadding((24*dp).toInt(), (16*dp).toInt(), (24*dp).toInt(), (24*dp).toInt())
        }
        val btnBatal = TextView(this).apply {
            text     = "Batal"
            textSize = 15f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke((2*dp).toInt(), Color.parseColor("#0D7A45"))
                cornerRadius = 50f * dp
            }
            layoutParams = LinearLayout.LayoutParams(0, (48*dp).toInt(), 1f).apply {
                marginEnd = (8*dp).toInt()
            }
        }
        val btnSimpan = TextView(this).apply {
            text     = "Simpan"
            textSize = 15f
            gravity  = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0D7A45"))
                cornerRadius = 50f * dp
            }
            layoutParams = LinearLayout.LayoutParams(0, (48*dp).toInt(), 1f).apply {
                marginStart = (8*dp).toInt()
            }
        }
        btnLayout.addView(btnBatal)
        btnLayout.addView(btnSimpan)

        rootLayout.addView(headerLayout)
        rootLayout.addView(divider)
        rootLayout.addView(inputLayout)
        rootLayout.addView(btnLayout)

        val dialog = AlertDialog.Builder(this).setView(rootLayout).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnSimpan.setOnClickListener {
            val berat = etBerat.text.toString().trim().toFloatOrNull()
            if (berat == null || berat <= 0f) {
                etBerat.background = makeInputBg("#F44336")
                Toast.makeText(this, "Masukkan berat badan yang valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val data = hashMapOf(
                "weight" to berat,
                "date"   to System.currentTimeMillis()
            )
            FirebaseManager.db.collection("users").document(uid)
                .collection("weightHistory")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Berat ${berat}kg berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    loadWeightChart(selectedWeightDays)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setupNutritionTabs() {
        selectNutritionTab(0)
        binding.tabNutriWeek0.setOnClickListener { selectNutritionTab(0) }
        binding.tabNutriWeek1.setOnClickListener { selectNutritionTab(1) }
        binding.tabNutriWeek2.setOnClickListener { selectNutritionTab(2) }
        binding.tabNutriWeek3.setOnClickListener { selectNutritionTab(3) }
    }

    private fun selectNutritionTab(weekOffset: Int) {
        selectedNutritionWeek = weekOffset
        listOf(binding.tabNutriWeek0, binding.tabNutriWeek1, binding.tabNutriWeek2, binding.tabNutriWeek3)
            .forEachIndexed { index, tv ->
                if (index == weekOffset) {
                    tv.setBackgroundResource(R.drawable.bg_selected_tab)
                    tv.setTextColor(Color.WHITE)
                } else {
                    tv.background = null
                    tv.setTextColor(Color.parseColor("#0D7A45"))
                }
            }
        loadNutritionWeekChart(weekOffset)
    }

    private fun loadNutritionWeekChart(weekOffset: Int) {
        val cal = Calendar.getInstance()
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        cal.add(Calendar.WEEK_OF_YEAR, -weekOffset)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)

        val weekStart  = cal.timeInMillis
        val weekEnd    = weekStart + 7 * 24 * 60 * 60 * 1000L

        FirebaseManager.db.collection("users").document(uid)
            .collection("foodLogs")
            .whereGreaterThanOrEqualTo("tanggal", weekStart)
            .whereLessThan("tanggal", weekEnd)
            .get()
            .addOnSuccessListener { snap ->
                val oneDayMs    = 24L * 60 * 60 * 1000
                val calorieData = FloatArray(7)
                val carbData    = FloatArray(7)
                val proteinData = FloatArray(7)
                val fatData     = FloatArray(7)

                snap.documents.forEach { doc ->
                    val tanggal = doc.getLong("tanggal") ?: return@forEach
                    val idx     = ((tanggal - weekStart) / oneDayMs).toInt().coerceIn(0, 6)
                    calorieData[idx] += (doc.getDouble("kalori")  ?: 0.0).toFloat()
                    carbData[idx]    += (doc.getDouble("karbo")   ?: 0.0).toFloat()
                    proteinData[idx] += (doc.getDouble("protein") ?: 0.0).toFloat()
                    fatData[idx]     += (doc.getDouble("lemak")   ?: 0.0).toFloat()
                }

                val total = calorieData.sum().toInt()
                binding.txtNutriTotal.text = total.toString()
                binding.txtNutriAvg.text   = (if (total > 0) total / 7 else 0).toString()

                val entries = ArrayList<BarEntry>()
                for (i in 0..6) {
                    val pK = proteinData[i] * 4f
                    val cK = carbData[i] * 4f
                    val fK = fatData[i] * 9f
                    val rK = (calorieData[i] - pK - cK - fK).coerceAtLeast(0f)
                    entries.add(BarEntry(i.toFloat(), floatArrayOf(pK, cK, fK, rK)))
                }

                val dataSet = BarDataSet(entries, "").apply {
                    colors = listOf(
                        Color.parseColor("#F48FB1"),
                        Color.parseColor("#A5D6A7"),
                        Color.parseColor("#90CAF9"),
                        Color.parseColor("#212121")
                    )
                    stackLabels = arrayOf("Protein", "Karbo", "Lemak", "Kalori")
                    setDrawValues(false)
                }

                binding.nutritionChart.apply {
                    data = BarData(dataSet).apply { barWidth = 0.55f }
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(false)
                    description.isEnabled = false
                    legend.isEnabled      = true
                    legend.textColor      = Color.parseColor("#333333")
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    legend.verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
                    legend.orientation         = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    xAxis.valueFormatter  = IndexAxisValueFormatter(listOf("S","S","R","K","J","S","M"))
                    xAxis.position        = XAxis.XAxisPosition.BOTTOM
                    xAxis.axisMinimum     = -0.5f
                    xAxis.axisMaximum     = 6.5f
                    xAxis.granularity     = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor       = Color.parseColor("#333333")
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor    = Color.parseColor("#EEEEEE")
                    axisLeft.textColor    = Color.parseColor("#333333")
                    axisLeft.axisMinimum  = 0f
                    axisRight.isEnabled   = false
                    setTouchEnabled(false)
                    setScaleEnabled(false)
                    animateY(900)
                    invalidate()
                }
            }
    }

    private fun setupWeightTabs() {
        binding.tabWeek.setBackgroundResource(R.drawable.bg_selected_tab)
        binding.tabWeek.setTextColor(Color.WHITE)
        loadWeightChart(7)

        binding.tabWeek.setOnClickListener     { selectWeightTab(binding.tabWeek);     selectedWeightDays = 7;   loadWeightChart(7) }
        binding.tabMonth.setOnClickListener    { selectWeightTab(binding.tabMonth);    selectedWeightDays = 30;  loadWeightChart(30) }
        binding.tabSixMonth.setOnClickListener { selectWeightTab(binding.tabSixMonth); selectedWeightDays = 180; loadWeightChart(180) }
    }

    private fun selectWeightTab(selected: TextView) {
        listOf(binding.tabWeek, binding.tabMonth, binding.tabSixMonth).forEach {
            it.background = null
            it.setTextColor(Color.parseColor("#0D7A45"))
        }
        selected.setBackgroundResource(R.drawable.bg_selected_tab)
        selected.setTextColor(Color.WHITE)
    }

    private fun loadWeightChart(days: Int) {
        val startDate = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)

        FirebaseManager.db.collection("users").document(uid)
            .collection("weightHistory")
            .whereGreaterThanOrEqualTo("date", startDate)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    binding.weightChart.clear()
                    binding.weightChart.setNoDataText("Belum ada riwayat berat badan")
                    binding.weightChart.setNoDataTextColor(Color.parseColor("#999999"))
                    binding.weightChart.invalidate()
                    binding.txtWeightChange.text = "-"
                    binding.txtWeightChange.setTextColor(Color.parseColor("#999999"))
                    return@addOnSuccessListener
                }

                val entries = ArrayList<Entry>()
                snap.documents.forEachIndexed { i, doc ->
                    val w = (doc.getDouble("weight") ?: 0.0).toFloat()
                    entries.add(Entry(i.toFloat(), w))
                }

                val actualDataSet = LineDataSet(entries, "Berat Aktual").apply {
                    color        = Color.parseColor("#0D7A45")
                    lineWidth    = 3f
                    circleRadius = 5f
                    setCircleColor(Color.parseColor("#0D7A45"))
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#DDF5E7")
                    mode      = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawValues(false)
                }

                val dataSets = ArrayList<ILineDataSet>().apply { add(actualDataSet) }

                if (targetBerat > 0f) {
                    dataSets.add(LineDataSet(arrayListOf(
                        Entry(0f, targetBerat),
                        Entry((entries.size - 1).toFloat(), targetBerat)
                    ), "Target").apply {
                        color     = Color.parseColor("#FF7043")
                        lineWidth = 2f
                        enableDashedLine(14f, 7f, 0f)
                        setDrawCircles(false)
                        setDrawFilled(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.LINEAR
                    })
                }

                binding.weightChart.apply {
                    data = LineData(dataSets)
                    description.isEnabled = false
                    legend.isEnabled      = true
                    legend.textColor      = Color.parseColor("#444444")
                    legend.textSize       = 11f
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    legend.verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
                    legend.orientation         = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    axisRight.isEnabled   = false
                    axisLeft.setDrawGridLines(false)
                    axisLeft.textColor    = Color.parseColor("#444444")
                    xAxis.position        = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor       = Color.parseColor("#444444")
                    xAxis.setDrawLabels(false)
                    setTouchEnabled(false)
                    setScaleEnabled(false)
                    animateX(900)
                    invalidate()
                }

                if (entries.size >= 2) {
                    val diff = entries.last().y - entries.first().y
                    binding.txtWeightChange.text = if (diff < 0) "${String.format("%.1f", diff)}kg" else "+${String.format("%.1f", diff)}kg"
                    binding.txtWeightChange.setTextColor(if (diff < 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                } else {
                    binding.txtWeightChange.text = "${entries.first().y.toInt()}kg"
                    binding.txtWeightChange.setTextColor(Color.parseColor("#999999"))
                }
            }
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun setupBmiIndicator(bmi: Float) {
        val progress = ((bmi - 15f) / (40f - 15f)).coerceIn(0f, 1f)
        binding.bmiBar.post {
            val maxWidth = binding.bmiBar.width - binding.bmiIndicator.width
            binding.bmiIndicator.animate()
                .translationX(maxWidth * progress)
                .setDuration(900).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun animateNumber(start: Int, end: Int, onUpdate: (Int) -> Unit) {
        ValueAnimator.ofInt(start, end).apply {
            duration = 900; interpolator = DecelerateInterpolator()
            addUpdateListener { onUpdate(it.animatedValue as Int) }
            start()
        }
    }

    private fun setupNutritionProgress(current: Int, target: Int, view: View) {
        if (target <= 0) return
        val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        view.post {
            val parent = view.parent as? View ?: return@post
            val targetWidth = (parent.width * progress).toInt()
            ValueAnimator.ofInt(0, targetWidth).apply {
                duration = 900; interpolator = DecelerateInterpolator()
                addUpdateListener {
                    view.layoutParams.width = it.animatedValue as Int
                    view.requestLayout()
                }
                start()
            }
        }
    }
}