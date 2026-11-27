package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class PinActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("notes_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val tvTitle = findViewById<TextView>(R.id.tvPinTitle)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnOk = findViewById<Button>(R.id.btnPinOk)

        val isSetup = !prefs.contains("pin_hash")
        if (isSetup) tvTitle.text = "Установите PIN (4 цифры)"

        btnOk.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin.length != 4) {
                Toast.makeText(this, "Введите 4 цифры", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isSetup) {
                prefs.edit().putString("pin_hash", hashPin(pin)).apply()
                Toast.makeText(this, "PIN установлен", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val stored = prefs.getString("pin_hash", "") ?: ""
                if (hashPin(pin) == stored) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Неверный PIN", Toast.LENGTH_SHORT).show()
                    etPin.text.clear()
                }
            }
        }
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(("notes_salt_v1" + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
