package com.example.nutrisee

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nutrisee.data.AppDatabase
import com.example.nutrisee.ui.HomeActivity
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val userId = SessionManager.getUserId(this)

        if (userId == -1) {
            findViewById<Button>(R.id.btnMulai).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            }
        } else {
            findViewById<Button>(R.id.btnMulai).visibility = View.GONE
            checkSession(userId)
        }
    }

    private fun checkSession(userId: Int) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val profile = db.userProfileDao().getProfileByUserId(userId)
            val intent = if (profile != null) {
                Intent(this@SplashActivity, HomeActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            } else {
                Intent(this@SplashActivity, LengkapiProfilActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            startActivity(intent)
            finish()
        }
    }
}