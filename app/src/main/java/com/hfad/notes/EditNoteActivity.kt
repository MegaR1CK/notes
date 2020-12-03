package com.hfad.notes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_edit_note.*
import java.text.SimpleDateFormat
import java.util.*

class EditNoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "noteTitle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) // Получить дату
        sdf.timeZone = TimeZone.getTimeZone("Europe/Moscow")
        val currentDate = sdf.format(Date())

        var currentNoteId = -1
        val helper = DatabaseHelper(this)
        intent.extras?.let { // Получить данные выбранной заметки (если есть)
            currentNoteId = it.getInt(EXTRA_NOTE_ID)
            val currentNote = helper.getNote(currentNoteId)
            title_edit.setText(currentNote.title)
            body_edit.setText(currentNote.body)
        }
        done_btn.setOnClickListener {
            val title = if (title_edit.text.toString().isNotBlank()) title_edit.text.toString() else resources.getString(R.string.no_title)
            val body = body_edit.text.toString()
            if (intent.extras != null) // Создать или изменить заметку
                helper.updateNote(currentNoteId, title, body, currentDate)
            else helper.addNote(title, body, currentDate)
            finish()
        }
    }
}