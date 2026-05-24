package com.example.nutrisee

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.nutrisee.databinding.ActivityLengkapiProfilBinding
import com.example.nutrisee.ui.HomeActivity
import java.util.Calendar

class LengkapiProfilActivity : BaseActivity() {

    private lateinit var binding: ActivityLengkapiProfilBinding
    private var selectedPhotoUri: Uri? = null
    private var userId: Int = -1
    private var uid: String = ""

    private val jenisKelaminOptions = arrayOf("Laki-laki", "Perempuan")

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPhotoUri = it
            binding.ivProfilePhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLengkapiProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) userId = SessionManager.getUserId(this)

        // Ambil uid Firebase dari current user
        uid = FirebaseManager.auth.currentUser?.uid ?: run {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setupClickListeners()
        loadExistingProfile()
    }

    private fun setupClickListeners() {
        binding.btnAddPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.ivProfilePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.tvJenisKelamin.setOnClickListener {
            val currentSelection = jenisKelaminOptions.indexOf(binding.tvJenisKelamin.text.toString())
            AlertDialog.Builder(this)
                .setTitle("Pilih Jenis Kelamin")
                .setSingleChoiceItems(jenisKelaminOptions, currentSelection.coerceAtLeast(0)) { dialog, which ->
                    binding.tvJenisKelamin.text = jenisKelaminOptions[which]
                    binding.tvJenisKelamin.setTextColor(getColor(android.R.color.black))
                    dialog.dismiss()
                }
                .show()
        }

        binding.tvTanggalLahir.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.tvTanggalLahir.text = String.format("%02d-%02d-%04d", day, month + 1, year)
                },
                calendar.get(Calendar.YEAR) - 20,
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSimpan.setOnClickListener { saveProfile() }
    }

    private fun loadExistingProfile() {
        FirebaseManager.db.collection("users")
            .document(uid)
            .collection("profile")
            .document("data")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.etNama.setText(doc.getString("nama") ?: "")
                    binding.tvTanggalLahir.text = doc.getString("tanggalLahir") ?: ""
                    binding.etBerat.setText(doc.getDouble("berat")?.toString() ?: "")
                    binding.etTinggi.setText(doc.getDouble("tinggi")?.toString() ?: "")
                    binding.etTargetBerat.setText(doc.getDouble("targetBerat")?.toString() ?: "")
                    binding.tvJenisKelamin.text = doc.getString("jenisKelamin") ?: "Laki-laki"
                }
            }
    }

    private fun saveProfile() {
        val nama         = binding.etNama.text.toString().trim()
        val tanggal      = binding.tvTanggalLahir.text.toString()
        val beratStr     = binding.etBerat.text.toString().trim()
        val tinggiStr    = binding.etTinggi.text.toString().trim()
        val targetStr    = binding.etTargetBerat.text.toString().trim()
        val jenisKelamin = binding.tvJenisKelamin.text.toString()

        when {
            nama.isEmpty()                    -> { Toast.makeText(this, "Nama wajib diisi!", Toast.LENGTH_SHORT).show(); return }
            tanggal == "00-00-0000"           -> { Toast.makeText(this, "Pilih tanggal lahir!", Toast.LENGTH_SHORT).show(); return }
            jenisKelamin !in jenisKelaminOptions -> { Toast.makeText(this, "Pilih jenis kelamin!", Toast.LENGTH_SHORT).show(); return }
            beratStr.isEmpty()                -> { Toast.makeText(this, "Berat wajib diisi!", Toast.LENGTH_SHORT).show(); return }
            tinggiStr.isEmpty()               -> { Toast.makeText(this, "Tinggi wajib diisi!", Toast.LENGTH_SHORT).show(); return }
            targetStr.isEmpty()               -> { Toast.makeText(this, "Target berat wajib diisi!", Toast.LENGTH_SHORT).show(); return }
        }

        val profileData = hashMapOf(
            "nama"         to nama,
            "jenisKelamin" to jenisKelamin,
            "tanggalLahir" to tanggal,
            "berat"        to beratStr.toFloat(),
            "tinggi"       to tinggiStr.toFloat(),
            "targetBerat"  to targetStr.toFloat(),
            "fotoPath"     to (selectedPhotoUri?.toString() ?: "")
        )

        FirebaseManager.db.collection("users")
            .document(uid)
            .collection("profile")
            .document("data")
            .set(profileData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal simpan profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}