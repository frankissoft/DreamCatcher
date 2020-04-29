package edu.vt.cs.cs5254.dreamcatcher

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import edu.vt.cs.cs5254.dreamcatcher.util.KeyboardUtil
import kotlinx.android.synthetic.main.dialog_add_entry.view.*

class DreamEntryEditFragment : DialogFragment() {

    interface Callbacks {
        fun onDreamEntryEdited(dreamEntry: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_add_entry, null)

            builder
                .setView(view)
                .setMessage("Add Comment")
                .setPositiveButton("OK") { _, _ ->
                    val text = view.comment_text.text.toString()
                    targetFragment?.let { fragment ->
                        (fragment as Callbacks).onDreamEntryEdited(text)
                    }

                        // FIRE ZE MISSILES!
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                        KeyboardUtil.hideSoftKeyboard(requireActivity(), view)
                        // User cancelled the dialog
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        fun newInstance(): DreamEntryEditFragment {
            return DreamEntryEditFragment()
        }
    }
}