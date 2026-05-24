package com.example.nutrisee.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nutrisee.FirebaseManager
import com.example.nutrisee.data.model.FoodSearchItem
import com.example.nutrisee.data.model.ParsedNutrition
import com.example.nutrisee.data.repository.FoodRepository
import com.example.nutrisee.databinding.FragmentTambahDataBinding
import com.example.nutrisee.ui.adapter.FoodSearchAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TambahDataFragment : Fragment() {

    private var _binding: FragmentTambahDataBinding? = null
    private val binding get() = _binding!!

    private val repository = FoodRepository()

    private var userId: Int = -1
    private var uid: String = ""
    private var porsi = 1
    private var selectedKategori = "Makan Siang"
    private var selectedNutrition: ParsedNutrition? = null
    private var searchJob: Job? = null
    private var isSelectingItem = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = requireActivity().intent.getIntExtra("USER_ID", -1)
        uid    = FirebaseManager.auth.currentUser?.uid ?: ""

        setupKategoriChips()
        setupPorsiControl()
        setupSearchInput()
        setupTambahButton()
        setupBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBottomNav() {
        binding.navBeranda.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.navProfil.setOnClickListener {
            val intent = Intent(requireContext(), ProfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
        binding.fabAdd.setOnClickListener { }
    }

    private fun setupKategoriChips() {
        val chips = listOf(
            binding.chipSarapan    to "Sarapan",
            binding.chipMakanSiang to "Makan Siang",
            binding.chipMakanMalam to "Makan Malam",
            binding.chipCamilan    to "Camilan"
        )
        chips.forEach { (chip, label) ->
            chip.setOnClickListener {
                selectedKategori = label
                updateChipSelection(chips, chip)
            }
        }
    }

    private fun updateChipSelection(chips: List<Pair<TextView, String>>, selected: TextView) {
        chips.forEach { (chip, _) ->
            if (chip == selected) {
                chip.setBackgroundResource(com.example.nutrisee.R.drawable.bg_chip_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(com.example.nutrisee.R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.parseColor("#0D7A45"))
            }
        }
    }

    private fun setupPorsiControl() {
        binding.btnPlus.setOnClickListener {
            porsi++
            binding.txtPorsiCount.text = porsi.toString()
            updateNutritionDisplay()
        }
        binding.btnMinus.setOnClickListener {
            if (porsi > 1) {
                porsi--
                binding.txtPorsiCount.text = porsi.toString()
                updateNutritionDisplay()
            }
        }
    }

    private fun setupSearchInput() {
        binding.etNamaMakanan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSelectingItem) return
                val query = s?.toString()?.trim() ?: ""
                selectedNutrition = null
                hideNutritionCards()
                if (query.length < 2) {
                    binding.lvSearchResults.visibility = View.GONE
                    return
                }
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(600)
                    searchFood(query)
                }
            }
        })

        binding.lvSearchResults.setOnItemClickListener { _, _, position, _ ->
            val adapter = binding.lvSearchResults.adapter as? FoodSearchAdapter
            val item = adapter?.getItem(position) ?: return@setOnItemClickListener
            onFoodSelected(item)
        }
    }

    private fun searchFood(query: String) {
        binding.pbSearch.visibility = View.VISIBLE
        binding.lvSearchResults.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.searchFood(query)
            binding.pbSearch.visibility = View.GONE

            result.onSuccess { items ->
                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "Makanan tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                val adapter = FoodSearchAdapter(requireContext(), items)
                binding.lvSearchResults.adapter = adapter
                binding.lvSearchResults.visibility = View.VISIBLE
                setListViewHeightBasedOnChildren(binding.lvSearchResults)
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Gagal mencari makanan. Cek koneksi internet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onFoodSelected(item: FoodSearchItem) {
        isSelectingItem = true
        binding.lvSearchResults.visibility = View.GONE
        binding.lvSearchResults.adapter = null
        val p = binding.lvSearchResults.layoutParams
        p.height = 0
        binding.lvSearchResults.layoutParams = p
        binding.lvSearchResults.requestLayout()

        binding.etNamaMakanan.setText(item.title)
        binding.etNamaMakanan.setSelection(item.title.length)
        isSelectingItem = false

        binding.pbSearch.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getNutrition(item.title)
            binding.pbSearch.visibility = View.GONE
            result.onSuccess { nutrition ->
                selectedNutrition = nutrition
                updateNutritionDisplay()
                showNutritionCards()
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Gagal memuat nutrisi. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNutritionDisplay() {
        val base = selectedNutrition ?: return
        val scaled = base.scaled(porsi)
        binding.txtKalori.text      = scaled.calories.toInt().toString()
        binding.txtKaloriLabel.text = "🔥 ${scaled.kaloriLabel()}"
        binding.txtKarbo.text       = scaled.carbs.toInt().toString()
        binding.txtProtein.text     = scaled.protein.toInt().toString()
        binding.txtLemak.text       = scaled.fat.toInt().toString()
        binding.txtSerat.text       = scaled.fiber.toInt().toString()
        binding.pbKarbo.progress   = (scaled.carbs   / 3f).toInt().coerceIn(0, 100)
        binding.pbProtein.progress = (scaled.protein / 1.5f).toInt().coerceIn(0, 100)
        binding.pbLemak.progress   = (scaled.fat     / 1f).toInt().coerceIn(0, 100)
        binding.pbSerat.progress   = (scaled.fiber   / 0.5f).toInt().coerceIn(0, 100)
    }

    private fun showNutritionCards() {
        binding.cardKalori.visibility   = View.VISIBLE
        binding.cardNutrisi.visibility  = View.VISIBLE
        binding.cardNutrisi2.visibility = View.VISIBLE
    }

    private fun hideNutritionCards() {
        binding.cardKalori.visibility   = View.GONE
        binding.cardNutrisi.visibility  = View.GONE
        binding.cardNutrisi2.visibility = View.GONE
    }

    private fun setupTambahButton() {
        binding.btnTambahData.setOnClickListener {
            val nama = binding.etNamaMakanan.text.toString().trim()
            if (nama.isEmpty()) {
                binding.etNamaMakanan.error = "Nama makanan tidak boleh kosong"
                return@setOnClickListener
            }
            if (uid.isEmpty()) {
                Toast.makeText(requireContext(), "Session error, silakan login ulang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nutrition = selectedNutrition ?: run {
                Toast.makeText(requireContext(), "Pilih makanan dari hasil pencarian terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val scaled = nutrition.scaled(porsi)
            val logId  = FirebaseManager.db
                .collection("users")
                .document(uid)
                .collection("foodLogs")
                .document().id

            val data = hashMapOf(
                "id"       to logId,
                "nama"     to scaled.foodName,
                "kategori" to selectedKategori,
                "porsi"    to porsi,
                "kalori"   to scaled.calories,
                "karbo"    to scaled.carbs,
                "protein"  to scaled.protein,
                "lemak"    to scaled.fat,
                "serat"    to scaled.fiber,
                "tanggal"  to System.currentTimeMillis()
            )

            FirebaseManager.db
                .collection("users")
                .document(uid)
                .collection("foodLogs")
                .document(logId)
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "✅ ${scaled.foodName} ($porsi porsi) berhasil ditambahkan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setListViewHeightBasedOnChildren(listView: ListView) {
        listView.post {
            val adapter = listView.adapter ?: return@post
            var totalHeight = 0
            val maxItems = minOf(adapter.count, 5)
            for (i in 0 until maxItems) {
                val item = adapter.getView(i, null, listView)
                item.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.UNSPECIFIED
                )
                totalHeight += item.measuredHeight
            }
            val params = listView.layoutParams
            params.height = totalHeight + (listView.dividerHeight * (maxItems - 1))
            listView.layoutParams = params
            listView.requestLayout()
        }
    }
}