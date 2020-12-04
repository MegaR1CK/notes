package com.hfad.notes

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_notes.*
import kotlinx.android.synthetic.main.fragment_notes.view.*
import kotlinx.android.synthetic.main.note_element.view.*

// TODO: Корзина заметок

class NotesFragment : Fragment() {

    private lateinit var helper: DatabaseHelper
    private lateinit var snackbar: Snackbar
    private lateinit var notesAdapter: RecyclerNoteAdapter
    private val deletedNotes = mutableListOf<Note>()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layout = inflater.inflate(R.layout.fragment_notes, container, false) // Настройка RecyclerView
        val notesRecycler = layout.recycler_notes
        helper = DatabaseHelper(notesRecycler.context)
        val standardAdapter = RecyclerNoteAdapter(helper.getNoteList()) // Настройка адаптера для RecyclerView
        notesRecycler.adapter = standardAdapter
        notesAdapter = notesRecycler.adapter as RecyclerNoteAdapter

        val listener = object : RecyclerNoteAdapter.Listener {
            override fun onClick(position: Int) {
                val intent = Intent(activity, EditNoteActivity :: class.java) // Перейти в EditNoteActivity
                intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID,
                        notesAdapter.cardsAndNotes[notesRecycler[position]]) // Отправка позиции заметки в массиве в EditNoteActivity
                activity?.startActivity(intent)
            }

            override fun onLongClick(position: Int) { // Показать интерфейс удаления заметок
                notesRecycler.forEach {
                    it.delete_cb.visibility = View.VISIBLE
                }
                notesRecycler[position].delete_cb.isChecked = true
                delete_options.visibility = View.VISIBLE
                add_note.visibility = View.INVISIBLE
                standardAdapter.listener = object : RecyclerNoteAdapter.Listener { // Изменение поведения элементов меню при клике при переходе в режим удаления
                    override fun onClick(position: Int) {
                        notesRecycler[position].delete_cb.isChecked =
                                !notesRecycler[position].delete_cb.isChecked
                    }
                    override fun onLongClick(position: Int) {}
                }
            }
        }

        notesAdapter.listener = listener
        val layoutManager = LinearLayoutManager(activity)
        notesRecycler.layoutManager = layoutManager


        val delOptionsListener = View.OnClickListener { btn -> // Поведение кнопок удаления
            if (btn.id == R.id.delete_btn) {
                val helper = DatabaseHelper(notesRecycler.context)
                notesRecycler.forEach {
                    if (it.delete_cb.isChecked) {
                        val noteForDeleteId = notesAdapter.cardsAndNotes[it] ?: 0 // Получение заметки и ее удаление
                        val noteForDelete = helper.getNote(noteForDeleteId)
                        deletedNotes.add(noteForDelete)
                        notesAdapter.notesParam.remove(noteForDelete)
                    }
                }
                notesAdapter.notifyDataSetChanged() // Обновление RecyclerVIew

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    deletedNotes.forEach {
                        helper.delNote(it.id)
                    }
                    deletedNotes.clear()
                 }, 3500)

                if (deletedNotes.isNotEmpty()) {
                    snackbar = Snackbar.make(coordinator, resources.getText(R.string.notes_deleted), Snackbar.LENGTH_LONG)
                    snackbar.setAction(resources.getText(R.string.undo)) {
                        notesAdapter.syncWithDatabase(notesRecycler.context)
                        deletedNotes.clear()
                        no_notes_title.visibility = View.INVISIBLE
                    }
                    snackbar.show()
                }

                if (notesAdapter.notesParam.isEmpty())
                    no_notes_title.visibility = View.VISIBLE
            }
            notesRecycler.forEach { // Скрыть интерфейс удаления заметок
                it.delete_cb.visibility = View.INVISIBLE
                it.delete_cb.isChecked = false
            }
            delete_options.visibility = View.INVISIBLE
            add_note.visibility = View.VISIBLE
            standardAdapter.listener = listener
        }
        layout.delete_btn?.setOnClickListener(delOptionsListener)
        layout.back_btn?.setOnClickListener(delOptionsListener)
        layout.add_note.setOnClickListener {
            val intent = Intent(activity, EditNoteActivity :: class.java) // Обработка нажатия кнопки добавления
            activity?.startActivity(intent)
        }
        return layout
    }


    fun searchNotes(text: String) {
        notesAdapter.notesParam = helper.getNoteList(text)
        notesAdapter.notifyDataSetChanged()
        if (notesAdapter.notesParam.isEmpty()) no_notes_title.visibility = View.VISIBLE
    }

    fun clearSearch() {
        notesAdapter.notesParam = helper.getNoteList()
        notesAdapter.notifyDataSetChanged()
        if (notesAdapter.notesParam.isNotEmpty()) no_notes_title.visibility = View.INVISIBLE
        else no_notes_title.visibility = View.VISIBLE
    }

    fun sortNotes() {
        val dialog = SortDialog()
        val ft = fragmentManager?.beginTransaction()
        if (ft != null) {
            dialog.show(ft, "dialog")
            dialog.dialogListener = object : SortDialog.DialogListener {
                override fun changeSort(type: SortDialog.SortType?) {
                    when (type) {
                        SortDialog.SortType.AlphabetAscend ->
                            notesAdapter.notesParam.sortBy { it.title }
                        SortDialog.SortType.AlphabetDescend ->
                            notesAdapter.notesParam.sortByDescending { it.title }
                        SortDialog.SortType.DateAscend ->
                            notesAdapter.notesParam.sortBy { it.lastEditDate }
                        SortDialog.SortType.DateDescend ->
                            notesAdapter.notesParam.sortByDescending { it.lastEditDate }
                    }
                    notesAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (deletedNotes.isNotEmpty()) {
            deletedNotes.forEach {
                helper.delNote(it.id)
            }
            deletedNotes.clear()
            snackbar.dismiss()
        }
        notesAdapter.syncWithDatabase(recycler_notes.context) // Обновление RecyclerView
        if (notesAdapter.notesParam.isEmpty())
            no_notes_title.visibility = View.VISIBLE
        else no_notes_title.visibility = View.INVISIBLE
    }
}
