package com.hfad.notes

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_sort_dialog.*

class SortDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity?.layoutInflater
        val view = inflater?.inflate(R.layout.fragment_sort_dialog, null)
        (view as ListView).onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val sortType = when (position) {
                0 -> SortType.AlphabetAscend
                1 -> SortType.AlphabetDescend
                2 -> SortType.DateAscend
                3 -> SortType.DateDescend
                else -> null
            }
            dialogListener.changeSort(sortType)
            dialog?.cancel()
        }
        builder.setTitle(resources.getString(R.string.sort_title)).setView(view)
        return builder.create()
    }

    lateinit var dialogListener: DialogListener

    interface DialogListener {
        fun changeSort(type: SortType?)
    }

    enum class SortType {
        AlphabetAscend, AlphabetDescend, DateAscend, DateDescend
    }
}