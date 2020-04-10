package edu.vt.cs.cs5254.dreamcatcher

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import edu.vt.cs.cs5254.dreamcatcher.database.Dream
import edu.vt.cs.cs5254.dreamcatcher.database.DreamEntryKind
import edu.vt.cs.cs5254.dreamcatcher.database.DreamWithEntries
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "DreamFragment"
private const val ARG_DREAM_ID = "dream_id"

class DreamDetailFragment : Fragment() {

    private lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var titleField: EditText
    private lateinit var realizedCheckBox: CheckBox
    private lateinit var deferredCheckBox: CheckBox
    private lateinit var dreamRevealedButton: Button
    private lateinit var dreamDeferredButton: Button
    private lateinit var dreamEntry1Button: Button
    private lateinit var dreamEntry2Button: Button
    private lateinit var dreamEntry3Button: Button
    private val dreamCommentButtons: ArrayList<Button> = ArrayList()

    private val dreamRepository = DreamRepository.get()

    private val dreamDetailViewModel: DreamDetailViewModel by lazy {
        ViewModelProviders.of(this).get(DreamDetailViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        dream = Dream()
        dreamWithEntries = DreamWithEntries(Dream(), ArrayList())

        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        dreamDetailViewModel.loadDream(dreamId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dream, container, false)

        titleField = view.findViewById(R.id.dream_title) as EditText
        realizedCheckBox = view.findViewById(R.id.dream_realized) as CheckBox
        deferredCheckBox = view.findViewById(R.id.dream_deferred) as CheckBox
        dreamRevealedButton = view.findViewById(R.id.dream_entry_0_button) as Button
        dreamDeferredButton = view.findViewById(R.id.dream_entry_4_button) as Button
        dreamEntry1Button = view.findViewById(R.id.dream_entry_1_button) as Button
        dreamEntry2Button = view.findViewById(R.id.dream_entry_2_button) as Button
        dreamEntry3Button = view.findViewById(R.id.dream_entry_3_button) as Button

        dreamCommentButtons.add(dreamEntry1Button)
        dreamCommentButtons.add(dreamEntry2Button)
        dreamCommentButtons.add(dreamEntry3Button)

        if (deferredCheckBox.isChecked) {
            dreamDeferredButton.text = getString(R.string.Deferred)
            realizedCheckBox.isEnabled = false
        } else if (realizedCheckBox.isChecked) {
            dreamDeferredButton.text = getString(R.string.Realized)
            deferredCheckBox.isEnabled = false
        } else {
            dreamDeferredButton.visibility = View.INVISIBLE
        }

        dreamCommentButtons.forEach {
            it.visibility = View.GONE
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dreamDetailViewModel.dreamLiveData.observe(
            viewLifecycleOwner,
            Observer { dream ->
                dream?.let {
                    this.dreamWithEntries = dream
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                dreamWithEntries.dream.description = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        deferredCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked

                if (isChecked) {
                    dreamDeferredButton.visibility = View.VISIBLE
                    dreamDeferredButton.text = getString(R.string.Deferred)
                    realizedCheckBox.isEnabled = false
                } else {
                    realizedCheckBox.isEnabled = true
                    dreamDeferredButton.visibility = View.INVISIBLE
                }
            }
        }

        realizedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isRealized = isChecked

                if (isChecked) {
                    dreamDeferredButton.visibility = View.VISIBLE
                    dreamDeferredButton.text = getString(R.string.Realized)
                    deferredCheckBox.isEnabled = false
                } else {
                    deferredCheckBox.isEnabled = true
                    dreamDeferredButton.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dreamDetailViewModel.saveDream(dreamWithEntries)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        titleField.setText(dreamWithEntries.dream.description)

        deferredCheckBox.apply {
            isChecked = dreamWithEntries.dream.isDeferred
        }
        realizedCheckBox.apply {
            isChecked = dreamWithEntries.dream.isRealized
        }

        var buttonIndex = 0

        if (dreamCommentButtons[0] == dreamEntry1Button) {
            Log.d(TAG, "Good")
        }

        dreamWithEntries.dreamEntries.forEach {
            if (it.kind == DreamEntryKind.COMMENT) {
                dreamCommentButtons[buttonIndex].visibility = View.VISIBLE
                val dateList = it.dateCreated.toString().split(" ")
                dreamCommentButtons[buttonIndex].text = it.comment +
                        listOf(dateList[1],dateList[2],dateList[5])
                            .joinToString(prefix = " (", postfix = ") ", separator = " ")

                buttonIndex++
            }
        }
    }

    companion object {

        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }

}