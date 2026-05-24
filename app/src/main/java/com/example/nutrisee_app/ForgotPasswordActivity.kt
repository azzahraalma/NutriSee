package com.example.nutrisee

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.nutrisee.data.AppDatabase
import com.example.nutrisee.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val db = AppDatabase.getInstance(this)

        binding.btnReset.setOnClickListener {
            val email        = binding.etEmail.text.toString().trim()
            val passwordBaru = binding.etPasswordBaru.text.toString().trim()
            val konfirm      = binding.etKonfirmPasswordBaru.text.toString().trim()

            when {
                email.isEmpty() || passwordBaru.isEmpty() || konfirm.isEmpty() -> {
                    Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
                }
                passwordBaru != konfirm -> {
                    Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    lifecycleScope.launch {
                        val existing = db.userDao().getUserByEmail(email)
                        if (existing == null) {
                            Toast.makeText(
                                this@ForgotPasswordActivity,
                                "Email tidak terdaftar!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            db.userDao().updatePassword(email, passwordBaru)
                            Toast.makeText(
                                this@ForgotPasswordActivity,
                                "Password berhasil direset!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateTo(Intent(this@ForgotPasswordActivity, LoginActivity::class.java), finish = true)
                        }
                    }
                }
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        startAnimations()
    }

    private fun startAnimations() {
        binding.ivGreenBg.translationY  = 800f
        binding.forgotPanel.translationY = 800f
        binding.tvTitle1.apply { scaleX = 0.5f; scaleY = 0.5f; alpha = 0f }
        binding.tvTitle2.apply { scaleX = 0.5f; scaleY = 0.5f; alpha = 0f }
        binding.ivStripe.apply { scaleX = 0.5f; scaleY = 0.5f; alpha = 0f }

        val greenBgY = ObjectAnimator.ofFloat(binding.ivGreenBg, "translationY", 800f, 0f).apply {
            duration = 700; interpolator = DecelerateInterpolator()
        }
        val panelY = ObjectAnimator.ofFloat(binding.forgotPanel, "translationY", 800f, 0f).apply {
            duration = 600; startDelay = 400; interpolator = DecelerateInterpolator()
        }

        fun makePopAnimators(view: android.view.View, delay: Long): List<ObjectAnimator> = listOf(
            ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f).apply {
                duration = 400; startDelay = delay; interpolator = OvershootInterpolator()
            },
            ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f).apply {
                duration = 400; startDelay = delay; interpolator = OvershootInterpolator()
            },
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 400; startDelay = delay
            }
        )

        AnimatorSet().apply {
            playTogether(
                greenBgY, panelY,
                *makePopAnimators(binding.tvTitle1, 700L).toTypedArray(),
                *makePopAnimators(binding.tvTitle2, 850L).toTypedArray(),
                *makePopAnimators(binding.ivStripe, 1000L).toTypedArray()
            )
            start()
        }
    }
}