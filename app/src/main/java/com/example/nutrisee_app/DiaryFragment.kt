package com.example.nutrisee.ui

import com.example.nutrisee_app.ui.adapter.WeekDateAdapter
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutrisee.R
import com.example.nutrisee.databinding.FragmentDiaryBinding
import com.example.nutrisee.ui.adapter.DiaryAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels()
    private val adapter = DiaryAdapter()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkGoogleFitOAuth()
        } else {
            Toast.makeText(requireContext(), "Izin aktivitas fisik diperlukan untuk deteksi otomatis", Toast.LENGTH_LONG).show()
        }
    }

    private val tambahMakananLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (viewModel.googleFitRepo.isConnected()) {
                viewModel.syncGoogleFit(viewModel.selectedDate.value ?: viewModel.getTodayString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideFab()

        setupRecyclerView()
        setupWeekDatePicker()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnMenu.setOnClickListener {
            showDeleteAllMenu()
        }

        checkActivityPermission()
    }

    override fun onResume() {
        super.onResume()
        hideFab()
        if (viewModel.googleFitRepo.isConnected()) {
            viewModel.syncGoogleFit(viewModel.selectedDate.value ?: viewModel.getTodayString())
        }
    }

    override fun onPause() {
        super.onPause()
        showFab()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showFab()
        _binding = null
    }

    private fun hideFab() {
        activity?.findViewById<FloatingActionButton>(R.id.fabAdd)?.visibility = View.GONE
    }

    private fun showFab() {
        activity?.findViewById<FloatingActionButton>(R.id.fabAdd)?.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        binding.rvDiary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDiary.adapter = adapter
        binding.rvDiary.isNestedScrollingEnabled = false
    }

    private fun setupWeekDatePicker() {
        val sdf      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNames = listOf("MIN", "SEN", "SEL", "RAB", "KAM", "JUM", "SAB")
        val cal      = Calendar.getInstance()

        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val days = mutableListOf<Pair<String, String>>()
        repeat(7) {
            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dayNum  = cal.get(Calendar.DAY_OF_MONTH).toString()
            days.add("$dayName\n$dayNum" to sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val weekAdapter = WeekDateAdapter(
            days         = days,
            selectedDate = viewModel.selectedDate.value ?: sdf.format(Date())
        ) { tanggal ->
            viewModel.selectDate(tanggal)
            if (viewModel.googleFitRepo.isConnected()) {
                viewModel.syncGoogleFit(tanggal)
            }
        }

        binding.rvWeekDates.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvWeekDates.adapter = weekAdapter
    }

    private fun observeViewModel() {
        viewModel.diaryItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.layoutDiaryEmpty.visibility =
                if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.netKalori.observe(viewLifecycleOwner) { net ->
            binding.txtNetKalori.text = net.toString()
        }
        viewModel.kaloriDikonsumsi.observe(viewLifecycleOwner) { masuk ->
            binding.txtKaloriDikonsumsi.text = masuk.toString()
        }
        viewModel.kaloriDibakar.observe(viewLifecycleOwner) { bakar ->
            binding.txtKaloriDibakar.text = bakar.toString()
        }

        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is DiaryViewModel.SyncStatus.Loading -> {
                    binding.pbDiaryLoading.visibility = View.VISIBLE
                }
                is DiaryViewModel.SyncStatus.Success -> {
                    binding.pbDiaryLoading.visibility = View.GONE
                }
                is DiaryViewModel.SyncStatus.Error -> {
                    binding.pbDiaryLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gagal sync: ${status.message}", Toast.LENGTH_SHORT).show()
                }
                is DiaryViewModel.SyncStatus.NotConnected -> {
                    binding.pbDiaryLoading.visibility = View.GONE
                    checkActivityPermission()
                }
                else -> {
                    binding.pbDiaryLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun showDeleteAllMenu() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Semua Riwayat")
            .setMessage("Yakin ingin menghapus semua riwayat aktivitas dan makanan hari ini?")
            .setPositiveButton("Hapus") { _, _ ->
                val tanggal = viewModel.selectedDate.value ?: viewModel.getTodayString()
                viewModel.deleteAllForDate(tanggal)
                Toast.makeText(requireContext(), "Semua riwayat dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                checkGoogleFitOAuth()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            checkGoogleFitOAuth()
        }
    }

    private fun checkGoogleFitOAuth() {
        val fitnessOptions = viewModel.googleFitRepo.fitnessOptions
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())

        if (account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            viewModel.syncGoogleFit()
            return
        }

        val prefs = requireContext().getSharedPreferences("nutrisee_prefs", Context.MODE_PRIVATE)
        val alreadyDeclined = prefs.getBoolean("google_fit_declined", false)
        if (alreadyDeclined) return

        GoogleSignIn.requestPermissions(
            this,
            GOOGLE_FIT_REQUEST_CODE,
            GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions),
            fitnessOptions
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.syncGoogleFit()
            } else {
                requireContext()
                    .getSharedPreferences("nutrisee_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("google_fit_declined", true)
                    .apply()
            }
        }
    }

    companion object {
        private const val GOOGLE_FIT_REQUEST_CODE = 1001
    }
}