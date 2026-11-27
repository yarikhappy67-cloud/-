package com.example.notes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter
    private val notes = mutableListOf<String>()
    private val prefs by lazy { getSharedPreferences("notes_prefs", Context.MODE_PRIVATE) }

    private var exportPosition: Int = -1
    private val exportFileRequest = 1001
    private val pickBgRequest = 1002

    private lateinit var bgImage: ImageView
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bgImage = findViewById(R.id.bgImage)
        tvTitle = findViewById(R.id.tvTitle)

        applyTheme()
        loadBackground()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotesAdapter(
            notes,
            onDelete = { position -> deleteNote(position) },
            onExport = { position -> startExport(position) }
        )
        recyclerView.adapter = adapter

        loadNotes()

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddDialog() }

        findViewById<TextView>(R.id.btnTheme).setOnClickListener {
            val isDark = prefs.getBoolean("is_dark", true)
            prefs.edit().putBoolean("is_dark", !isDark).apply()
            applyTheme()
            recreate()
        }

        findViewById<TextView>(R.id.btnBg).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, pickBgRequest)
        }

        findViewById<TextView>(R.id.btnLock).setOnClickListener {
            startActivity(Intent(this, DrawingActivity::class.java))
        }
    }

    private fun applyTheme() {
        val isDark = prefs.getBoolean("is_dark", true)
        if (isDark) {
            tvTitle.setTextColor(0xFFFFFFFF.toInt())
            window.setBackgroundDrawableResource(android.R.color.black)
            findViewById<View>(android.R.id.content).setBackgroundColor(0xFF000000.toInt())
        } else {
            tvTitle.setTextColor(0xFF000000.toInt())
            findViewById<View>(android.R.id.content).setBackgroundColor(0xFFFFFFFF.toInt())
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadBackground() {
        val uriStr = prefs.getString("bg_uri", null)
        if (uriStr != null) {
            try {
                bgImage.setImageURI(Uri.parse(uriStr))
            } catch (_: Exception) { }
        }
    }

    private fun loadNotes() {
        val saved = prefs.getString("notes_list", "") ?: ""
        notes.clear()
        if (saved.isNotEmpty()) {
            notes.addAll(saved.split("|||"))
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveNotes() {
        prefs.edit().putString("notes_list", notes.joinToString("|||")).apply()
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Текст заметки"
            setPadding(24, 24, 24, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Новая заметка")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    notes.add(text)
                    saveNotes()
                    adapter.notifyItemInserted(notes.size - 1)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteNote(position: Int) {
        notes.removeAt(position)
        saveNotes()
        adapter.notifyItemRemoved(position)
    }

    private fun startExport(position: Int) {
        exportPosition = position
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "note_${position + 1}.txt")
        }
        startActivityForResult(intent, exportFileRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            exportFileRequest -> {
                data?.data?.let { uri ->
                    try {
                        val content = notes.getOrNull(exportPosition) ?: return
                        contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                        Toast.makeText(this, "Файл сохранён", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            pickBgRequest -> {
                data?.data?.let { uri ->
                    prefs.edit().putString("bg_uri", uri.toString()).apply()
                    bgImage.setImageURI(uri)
                }
            }
        }
    }
}

class NotesAdapter(
    private val items: List<String>,
    private val onDelete: (Int) -> Unit,
    private val onExport: (Int) -> Unit
) : RecyclerView.Adapter<NotesAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNote: TextView = view.findViewById(R.id.tvNote)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
        val btnExport: TextView = view.findViewById(R.id.btnExport)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tvNote.text = items[position]
        holder.btnDelete.setOnClickListener { onDelete(position) }
        holder.btnExport.setOnClickListener { onExport(position) }
    }

    override fun getItemCount() = items.size
}
