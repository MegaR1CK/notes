package com.hfad.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_notes.*
import kotlinx.android.synthetic.main.fragment_notes.view.*
import kotlinx.android.synthetic.main.note_element.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Корзина заметок

class NotesFragment : Fragment() {

    lateinit var helper: DatabaseHelper
    var savedSearchText: String? = null
    private val deletedNotes = mutableListOf<Note>()
    lateinit var snackbar: Snackbar

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layout = inflater.inflate(R.layout.fragment_notes, container, false) // Настройка RecyclerView
        val notesRecycler = layout.recycler_notes
        helper = DatabaseHelper(notesRecycler.context)
        val standardAdapter = RecyclerNoteAdapter(helper.getNoteList()) // Настройка адаптера для RecyclerView
        val listener = object : RecyclerNoteAdapter.Listener {
            override fun onClick(position: Int) {
                val intent = Intent(activity, EditNoteActivity :: class.java) // Перейти в EditNoteActivity
                intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID,
                        (notesRecycler.adapter as RecyclerNoteAdapter).cardsAndNotes[notesRecycler[position]]) // Отправка позиции заметки в массиве в EditNoteActivity
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
                        notesRecycler[position].delete_cb.isChecked = !notesRecycler[position].delete_cb.isChecked
                    }
                    override fun onLongClick(position: Int) {}
                }
            }
        }
        standardAdapter.listener = listener
        notesRecycler.adapter = standardAdapter
        val layoutManager = LinearLayoutManager(activity)
        notesRecycler.layoutManager = layoutManager

        val delOptionsListener = View.OnClickListener { btn -> // Поведение кнопок удаления
            if (btn.id == R.id.delete_btn) {
                val helper = DatabaseHelper(notesRecycler.context)
                notesRecycler.forEach {
                    if (it.delete_cb.isChecked) {
                        val noteForDeleteId = (notesRecycler.adapter as RecyclerNoteAdapter).cardsAndNotes[it] ?: 0 // Получение заметки и ее удаление
                        val noteForDelete = helper.getNote(noteForDeleteId)
                        deletedNotes.add(noteForDelete)
                        (notesRecycler.adapter as RecyclerNoteAdapter).notesParam.remove(noteForDelete)
                    }
                }
                (notesRecycler.adapter as RecyclerNoteAdapter).notifyDataSetChanged() // Обновление RecyclerVIew

                GlobalScope.launch {
                    delay(3500)
                    deletedNotes.forEach {
                        helper.delNote(it.id)
                    }
                    deletedNotes.clear()
                }

                snackbar = Snackbar.make(coordinator, resources.getText(R.string.notes_deleted), Snackbar.LENGTH_LONG)
                snackbar.setAction(resources.getText(R.string.undo)) {
                    (notesRecycler.adapter as RecyclerNoteAdapter).syncWithDatabase(notesRecycler.context)
                    deletedNotes.clear()
                    no_notes_title.visibility = View.INVISIBLE
                }
                snackbar.show()
                if ((notesRecycler.adapter as RecyclerNoteAdapter).notesParam.isEmpty())
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

        val otherButtonsListener = View.OnClickListener { v -> // Назначение действий для кнопок добавления и поиска
            when (v?.id) {
                R.id.add_note -> {
                    val intent = Intent(activity, EditNoteActivity :: class.java) // Обработка нажатия кнопки добавления
                    activity?.startActivity(intent)
                }
                R.id.search_btn -> {
                    val searchAdapter = RecyclerNoteAdapter(helper.getNoteList(search_field.text.toString().trim())) // Создание и присваивание адаптера с найденными заметками
                    searchAdapter.listener = listener
                    notesRecycler.adapter = searchAdapter
                }
                R.id.sort_btn -> {
                    val dialog = SortDialog()
                    val ft = fragmentManager?.beginTransaction()
                    if (ft != null) {
                        dialog.show(ft, "dialog")
                        dialog.dialogListener = object : SortDialog.DialogListener {
                            override fun changeSort(type: SortDialog.SortType?) {
                                when (type) {
                                    SortDialog.SortType.AlphabetAscend ->
                                        (notesRecycler.adapter as RecyclerNoteAdapter).notesParam.sortBy { it.title }
                                    SortDialog.SortType.AlphabetDescend ->
                                        (notesRecycler.adapter as RecyclerNoteAdapter).notesParam.sortByDescending { it.title }
                                    SortDialog.SortType.DateAscend ->
                                        (notesRecycler.adapter as RecyclerNoteAdapter).notesParam.sortBy { it.lastEditDate }
                                    SortDialog.SortType.DateDescend ->
                                        (notesRecycler.adapter as RecyclerNoteAdapter).notesParam.sortByDescending { it.lastEditDate }
                                }
                                (notesRecycler.adapter as RecyclerNoteAdapter).notifyDataSetChanged()
                            }
                        }
                    }
                }
                R.id.clear_search_btn -> {
                    notesRecycler.adapter = standardAdapter // Сброс поиска
                    search_field.text.clear()
                }
            }
        }
        notesRecycler.setOnTouchListener { v, event -> // При касании снять фокус с search_field и спрятать клавиатуру
            if (event.action == MotionEvent.ACTION_UP) {
                layout.search_field.clearFocus()
                (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(layout.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                v.performClick()
            }
            false
        }
        layout.add_note.setOnClickListener(otherButtonsListener)
        layout.search_btn.setOnClickListener(otherButtonsListener)
        layout.clear_search_btn.setOnClickListener(otherButtonsListener)
        layout.sort_btn.setOnClickListener(otherButtonsListener)
        return layout
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
        (view?.recycler_notes?.adapter as RecyclerNoteAdapter).syncWithDatabase(recycler_notes.context) // Обновление RecyclerView
        if ((view?.recycler_notes?.adapter as RecyclerNoteAdapter).notesParam.isEmpty())
            no_notes_title.visibility = View.VISIBLE
        else no_notes_title.visibility = View.INVISIBLE
    }

    override fun onStop() {
        super.onStop()
        if (search_field.text.isNotBlank()) savedSearchText = search_field.text.toString()
    }
}
