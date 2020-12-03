package com.hfad.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        const val DB_NAME = "notes"
        const val DB_VERSION = 1
    }

    fun addNote(title: String, body: String, date: String) {
        val noteValues = ContentValues()
        noteValues.put("TITLE", title)
        noteValues.put("BODY", body)
        noteValues.put("DATE", date)
        writableDatabase.insert("NOTES", null, noteValues)
        writableDatabase.close()
    }

    fun addNote(note: Note) {
        val noteValues = ContentValues()
        noteValues.put("TITLE", note.title)
        noteValues.put("BODY", note.body)
        noteValues.put("DATE", note.lastEditDate)
        writableDatabase.insert("NOTES", null, noteValues)
        writableDatabase.close()
    }

    fun updateNote(id: Int, title: String, body: String, date: String) {
        val noteValues = ContentValues()
        noteValues.put("TITLE", title)
        noteValues.put("BODY", body)
        noteValues.put("DATE", date)
        writableDatabase.update("NOTES", noteValues, "_id = ?", arrayOf(id.toString()))
        writableDatabase.close()
    }

    fun delNote(id: Int) {
        writableDatabase.delete("NOTES", "_id = ?", arrayOf(id.toString()))
        writableDatabase.close()
    }

    fun getNote(pos: Int) : Note {
        val cursor = readableDatabase.query("NOTES", arrayOf("TITLE", "BODY", "DATE"),
                "_id = ?", arrayOf(pos.toString()), null, null, null)
        cursor.moveToFirst()
        val note = Note(cursor.getString(0), cursor.getString(1), cursor.getString(2), pos)
        cursor.close()
        readableDatabase.close()
        return note
    }

    fun getNoteList(searchText: String? = null) : MutableList<Note> {
        val cursor = readableDatabase.query("NOTES", arrayOf("_id", "TITLE", "BODY", "DATE"),
                null, null, null, null, null)
        cursor.moveToFirst()
        var noteList = mutableListOf<Note>()
        do {
            noteList.add(Note(cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getInt(0)))
        } while (cursor.moveToNext())
        cursor.close()
        readableDatabase.close()
        if (searchText != null) {
            noteList = noteList.filter { it.title.contains(searchText) || it.body.contains(searchText) }.toMutableList()
        }
        return noteList
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE NOTES (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TITLE TEXT, BODY TEXT, DATE TEXT);")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}