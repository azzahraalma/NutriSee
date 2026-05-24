package com.example.nutrisee

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.Toast
import com.example.nutrisee.databinding.ActivityLoginBinding
import com.example.nutrisee.ui.HomeActivity

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpannable()
        setupClickListeners()
    }

    private fun setupSpannable() {
        val text = "Belum Punya Akun? Daftar"
        val spannable = SpannableString(text)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            text.indexOf("Daftar"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvGoRegisterSpan.text = spannable
    }

    private fun setupClickListeners() {
        binding.tvForgotPassword.setOnClickListener {
            navigateTo(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnSignIn.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(email, password)
        }

        binding.tvGoRegisterSpan.setOnClickListener {
            navigateTo(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin(email: String, password: String) {
        FirebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                SessionManager.saveSession(this, uid.hashCode())

                // Cek profil di Firestore
                FirebaseManager.db.collection("users")
                    .document(uid)
                    .collection("profile")
                    .document("data")
                    .get()
                    .addOnSuccessListener { doc ->
                        val intent = if (doc.exists()) {
                            Toast.makeText(this, "Selamat datang kembali!", Toast.LENGTH_SHORT).show()
                            Intent(this, HomeActivity::class.java)
                        } else {
                            Toast.makeText(this, "Silakan lengkapi profil dulu!", Toast.LENGTH_LONG).show()
                            Intent(this, LengkapiProfilActivity::class.java)
                        }
                        intent.putExtra("USER_ID", uid.hashCode())
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Email atau password salah!", Toast.LENGTH_SHORT).show()
            }
    }
}