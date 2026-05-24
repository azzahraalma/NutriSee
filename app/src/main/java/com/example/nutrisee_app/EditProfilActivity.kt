package com.example.nutrisee.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrisee.FirebaseManager
import com.example.nutrisee.R
import com.example.nutrisee.SessionManager
import com.example.nutrisee.databinding.ActivityEditProfilBinding

class EditProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfilBinding
    private var userId: Int = -1
    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) userId = SessionManager.getUserId(this)
        uid = FirebaseManager.auth.currentUser?.uid ?: ""

        loadProfile()
        setupItemClicks()
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
                    binding.txtGender.text       = doc.getString("jenisKelamin") ?: "-"
                    binding.txtTarget.text       = "${doc.getDouble("targetBerat")?.toInt() ?: "-"} kg"
                    binding.txtBerat.text        = "${doc.getDouble("berat")?.toInt() ?: "-"} kg"
                    binding.txtTinggi.text       = "${doc.getDouble("tinggi")?.toInt() ?: "-"} cm"
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

    private fun updateProfileField(field: String, value: Any, onSuccess: () -> Unit) {
        FirebaseManager.db.collection("users")
            .document(uid)
            .collection("profile")
            .document("data")
            .update(field, value)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupItemClicks() {
        binding.btnEditNama.setOnClickListener {
            showEditDialog(
                title     = "Edit Nama",
                subtitle  = "Masukkan nama lengkapmu",
                label     = "Nama",
                hint      = "Contoh: Arya Permana",
                current   = binding.txtNama.text.toString(),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            ) { newVal ->
                updateProfileField("nama", newVal) {
                    binding.txtNama.text = newVal
                    Toast.makeText(this, "Nama berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemTargetBerat.setOnClickListener {
            val current = binding.txtTarget.text.toString().replace("kg", "").trim()
            showEditDialog(
                title     = "Edit Target Berat",
                subtitle  = "Masukkan target berat badanmu",
                label     = "Target Berat (kg)",
                hint      = "Contoh: 65.0",
                current   = current,
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { newVal ->
                val kg = newVal.toFloatOrNull() ?: return@showEditDialog
                updateProfileField("targetBerat", kg) {
                    binding.txtTarget.text = "${kg.toInt()} kg"
                    Toast.makeText(this, "Target berat berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemBeratTerkini.setOnClickListener {
            val current = binding.txtBerat.text.toString().replace("kg", "").trim()
            showEditDialog(
                title     = "Edit Berat Terkini",
                subtitle  = "Masukkan berat badanmu saat ini",
                label     = "Berat Terkini (kg)",
                hint      = "Contoh: 70.5",
                current   = current,
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { newVal ->
                val kg = newVal.toFloatOrNull() ?: return@showEditDialog
                updateProfileField("berat", kg) {
                    binding.txtBerat.text = "${kg.toInt()} kg"
                    Toast.makeText(this, "Berat berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemTinggi.setOnClickListener {
            val current = binding.txtTinggi.text.toString().replace("cm", "").trim()
            showEditDialog(
                title     = "Edit Tinggi",
                subtitle  = "Masukkan tinggi badanmu",
                label     = "Tinggi (cm)",
                hint      = "Contoh: 170",
                current   = current,
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { newVal ->
                val cm = newVal.toFloatOrNull() ?: return@showEditDialog
                updateProfileField("tinggi", cm) {
                    binding.txtTinggi.text = "${cm.toInt()} cm"
                    Toast.makeText(this, "Tinggi berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemGantiPassword.setOnClickListener {
            showGantiPasswordDialog()
        }
    }

    private fun showGantiPasswordDialog() {
        val dp = resources.displayMetrics.density

        fun makeInputBg(strokeColor: String) = GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke((2 * dp).toInt(), Color.parseColor(strokeColor))
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
            text     = "Ganti Password"
            textSize = 20f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerLayout.addView(TextView(this).apply {
            text     = "Masukkan password lama dan password baru"
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

        fun makeLabel(text: String, topPad: Int = 0) = TextView(this).apply {
            this.text = text
            textSize  = 13f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface  = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, (topPad*dp).toInt(), 0, (8*dp).toInt())
        }

        fun makeInput(hint: String) = EditText(this).apply {
            this.hint  = hint
            inputType  = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize   = 16f
            setTextColor(Color.parseColor("#1A1A1A"))
            setHintTextColor(Color.parseColor("#AACABB"))
            setPadding((16*dp).toInt(), (14*dp).toInt(), (16*dp).toInt(), (14*dp).toInt())
            background = makeInputBg("#0D7A45")
        }

        val etPasswordLama = makeInput("Password lama")
        val etPasswordBaru = makeInput("Password baru")
        val etKonfirmasi   = makeInput("Konfirmasi password baru")

        inputLayout.addView(makeLabel("Password Lama"))
        inputLayout.addView(etPasswordLama)
        inputLayout.addView(makeLabel("Password Baru", topPad = 16))
        inputLayout.addView(etPasswordBaru)
        inputLayout.addView(makeLabel("Konfirmasi Password Baru", topPad = 16))
        inputLayout.addView(etKonfirmasi)

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
            val passwordLama = etPasswordLama.text.toString().trim()
            val passwordBaru = etPasswordBaru.text.toString().trim()
            val konfirmasi   = etKonfirmasi.text.toString().trim()

            if (passwordLama.isEmpty()) {
                etPasswordLama.background = makeInputBg("#F44336")
                Toast.makeText(this, "Password lama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (passwordBaru.isEmpty()) {
                etPasswordBaru.background = makeInputBg("#F44336")
                Toast.makeText(this, "Password baru tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (konfirmasi.isEmpty()) {
                etKonfirmasi.background = makeInputBg("#F44336")
                Toast.makeText(this, "Konfirmasi password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (passwordBaru != konfirmasi) {
                etPasswordBaru.background = makeInputBg("#F44336")
                etKonfirmasi.background   = makeInputBg("#F44336")
                Toast.makeText(this, "Password baru tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseManager.auth.currentUser ?: return@setOnClickListener
            val email = user.email ?: return@setOnClickListener

            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, passwordLama)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(passwordBaru)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Password berhasil diubah!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal update password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    etPasswordLama.background = makeInputBg("#F44336")
                    Toast.makeText(this, "Password lama salah", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showEditDialog(
        title: String,
        subtitle: String,
        label: String,
        hint: String,
        current: String,
        inputType: Int,
        onSave: (String) -> Unit
    ) {
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
            text     = title
            textSize = 20f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerLayout.addView(TextView(this).apply {
            text     = subtitle
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
            text     = label
            textSize = 13f
            setTextColor(Color.parseColor("#0D7A45"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, (8*dp).toInt())
        })

        val etInput = EditText(this).apply {
            this.hint      = hint
            this.inputType = inputType
            textSize       = 18f
            setText(current)
            setSelection(current.length)
            setTextColor(Color.parseColor("#1A1A1A"))
            setHintTextColor(Color.parseColor("#AACABB"))
            setPadding((16*dp).toInt(), (14*dp).toInt(), (16*dp).toInt(), (14*dp).toInt())
            background = makeInputBg("#0D7A45")
        }
        inputLayout.addView(etInput)

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
            val value = etInput.text.toString().trim()
            if (value.isEmpty()) {
                etInput.background = makeInputBg("#F44336")
                Toast.makeText(this, "Field tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSave(value)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupNav() {
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

        binding.navProfil.setOnClickListener {
            val intent = Intent(this, ProfilActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }
}