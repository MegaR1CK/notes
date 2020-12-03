package com.hfad.notes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_element.view.*

class RecyclerNoteAdapter(var notesParam: MutableList<Note>) : RecyclerView.Adapter<RecyclerNoteAdapter.ViewHolder>() {

    val cardsAndNotes = mutableMapOf<CardView, Int?>() // Массив карточных представлений и связанных с ними заметок

    override fun getItemCount() = notesParam.size

    inner class ViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cv = LayoutInflater.from(parent.context).inflate(R.layout.note_element, parent, false) as CardView
        return ViewHolder(cv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) { // Назначение данных карточному представлению
        val currentNote = notesParam[position]
        val view = holder.cardView
        cardsAndNotes[view] = currentNote.id
        view.title.text = currentNote.title
        view.body.text = if (currentNote.body.contains('\n'))
                             currentNote.body.substring(0, currentNote.body.indexOf('\n'))
                         else currentNote.body
        view.date.text = currentNote.lastEditDate
        view.setOnClickListener {
            listener?.onClick(position)
        }
        view.setOnLongClickListener {
            listener?.onLongClick(position)
            true
        }
    }

    fun syncWithDatabase(context: Context) {
        val helper = DatabaseHelper(context)
        notesParam = helper.getNoteList()
        notifyDataSetChanged()
    }

    var listener: Listener? = null

    interface Listener { // Интерфейс для переноса исполняемого кода во фрагмент
        fun onClick(position: Int)
        fun onLongClick(position: Int)
    }
}