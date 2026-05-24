package com.example.nutrisee

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import com.example.nutrisee.databinding.ActivityRegisterBinding

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpannable()
        setupClickListeners()

        binding.tvTerms.paintFlags =
            binding.tvTerms.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
    }

    private fun setupSpannable() {
        val text = "Sudah Punya Akun? Masuk"
        val spannable = SpannableString(text)
        val boldStart = text.indexOf("Masuk")
        if (boldStart >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                boldStart, text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvGoLoginSpan.text = spannable
    }

    private fun setupClickListeners() {
        binding.tvTerms.setOnClickListener { showTermsDialog() }

        binding.btnSignUp.setOnClickListener {
            val nama     = binding.etNama.text.toString().trim()
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val konfirm  = binding.etKonfirmPassword.text.toString().trim()

            when {
                nama.isEmpty() || email.isEmpty() || password.isEmpty() || konfirm.isEmpty() -> {
                    Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
                }
                password != konfirm -> {
                    Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                }
                !binding.cbTerms.isChecked -> {
                    Toast.makeText(this, "Setujui Terms & Condition dulu!", Toast.LENGTH_SHORT).show()
                }
                else -> doRegister(nama, email, password)
            }
        }

        binding.tvGoLoginSpan.setOnClickListener {
            navigateTo(Intent(this, LoginActivity::class.java), finish = true)
        }
    }

    private fun doRegister(nama: String, email: String, password: String) {
        FirebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = hashMapOf(
                    "namaLengkap" to nama,
                    "email"       to email
                )

                FirebaseManager.db.collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        SessionManager.saveSession(this, uid.hashCode())
                        Toast.makeText(this, "Registrasi berhasil! Silakan lengkapi profil.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, LengkapiProfilActivity::class.java)
                        intent.putExtra("USER_ID", uid.hashCode())
                        navigateTo(intent, finish = true)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal simpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registrasi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTermsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_terms)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}