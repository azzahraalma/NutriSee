package com.example.nutrisee.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrisee.FirebaseManager
import com.example.nutrisee.LoginActivity
import com.example.nutrisee.R
import com.example.nutrisee.SessionManager
import com.example.nutrisee.databinding.ActivityProfilBinding

class ProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding
    private var userId: Int = -1
    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) userId = SessionManager.getUserId(this)
        uid = FirebaseManager.auth.currentUser?.uid ?: ""

        loadProfile()
        setupNav()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        if (uid.isEmpty()) return

        FirebaseManager.db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                binding.txtEmail.text = doc.getString("email") ?: "-"
            }

        FirebaseManager.db.collection("users")
            .document(uid)
            .collection("profile")
            .document("data")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.txtNama.text         = doc.getString("nama") ?: "-"
                    binding.txtTinggi.text       = "${doc.getDouble("tinggi")?.toInt() ?: "-"} cm"
                    binding.txtBerat.text        = "${doc.getDouble("berat")?.toInt() ?: "-"} kg"
                    binding.txtTarget.text       = "${doc.getDouble("targetBerat")?.toInt() ?: "-"} kg"
                    binding.txtTanggalLahir.text = doc.getString("tanggalLahir") ?: "-"

                    val fotoPath = doc.getString("fotoPath")
                    if (!fotoPath.isNullOrEmpty()) {
                        binding.ivProfilePhoto.setImageURI(Uri.parse(fotoPath))
                    } else {
                        binding.ivProfilePhoto.setImageResource(R.drawable.foto_profil_default)
                    }
                }
            }
    }

    private fun setupNav() {
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, EditProfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("OPEN_TAMBAH", true)
            startActivity(intent)
        }

        binding.navBeranda.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        binding.navProfil.setOnClickListener { }

        // TOMBOL LOGOUT
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Apakah kamu yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ ->
                    FirebaseManager.auth.signOut()
                    SessionManager.clearSession(this)
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}