package com.example.notes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DrawingActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private val saveImageRequest = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)

        drawingView = findViewById(R.id.drawingView)

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            drawingView.clear()
        }

        findViewById<Button>(R.id.btnSaveDrawing).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/png"
                putExtra(Intent.EXTRA_TITLE, "drawing.png")
            }
            startActivityForResult(intent, saveImageRequest)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == saveImageRequest && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        drawingView.saveToBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
