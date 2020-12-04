package com.hfad.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private lateinit var helper: DatabaseHelper
    private lateinit var snackbar: Snackbar
    private val deletedNotes = mutableListOf<Note>()
    private var savedSearchText: String? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layout = inflater.inflate(R.layout.fragment_notes, container, false) // Настройка RecyclerView
        val notesRecycler = layout.recycler_notes
        helper = DatabaseHelper(notesRecycler.context)
        val standardAdapter = RecyclerNoteAdapter(helper.getNoteList()) // Настройка адаптера для RecyclerView
        notesRecycler.adapter = standardAdapter
        val notesAdapter = notesRecycler.adapter as RecyclerNoteAdapter

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
                search_field.isEnabled = false
                clear_search_btn.isEnabled = false
                sort_btn.isEnabled = false
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

                clear_search_btn.isClickable = false
                search_btn.isClickable = false
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    search_btn.isClickable = true
                    clear_search_btn.isClickable = true
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
            search_field.isEnabled = true
            clear_search_btn.isEnabled = true
            sort_btn.isEnabled = true
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
                    notesAdapter.notesParam = helper.getNoteList(search_field.text.toString().trim()) // Создание и присваивание адаптера с найденными заметками
                    notesAdapter.notifyDataSetChanged()
                    if (notesAdapter.notesParam.isEmpty()) no_notes_title.visibility = View.VISIBLE
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
                R.id.clear_search_btn -> {
                    notesAdapter.notesParam = helper.getNoteList()
                    notesAdapter.notifyDataSetChanged()
                    if (notesAdapter.notesParam.isNotEmpty())
                        no_notes_title.visibility = View.INVISIBLE
                    else no_notes_title.visibility = View.VISIBLE
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
        val notesAdapter = view?.recycler_notes?.adapter as RecyclerNoteAdapter
        notesAdapter.syncWithDatabase(recycler_notes.context) // Обновление RecyclerView
        if (notesAdapter.notesParam.isEmpty())
            no_notes_title.visibility = View.VISIBLE
        else no_notes_title.visibility = View.INVISIBLE
    }


    override fun onStop() {
        super.onStop()
        if (search_field.text.isNotBlank()) savedSearchText = search_field.text.toString()
    }
}
