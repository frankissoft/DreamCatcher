package edu.vt.cs.cs5254.dreamcatcher

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
//import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.vt.cs.cs5254.dreamcatcher.database.DreamEntry
import edu.vt.cs.cs5254.dreamcatcher.database.DreamEntryKind
import edu.vt.cs.cs5254.dreamcatcher.database.DreamWithEntries
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.createCaptureImageIntent
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.getScaledBitmap
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.isCameraAvailable
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.revokeCaptureImagePermissions
import java.io.File
import java.util.*

//private const val TAG = "DreamFragment"
private const val DIALOG_ENTRY = "DialogEntry"
private const val ARG_DREAM_ID = "dream_id"

private const val REVEAL_COLOR = "#861F41"
private const val COMMENT_COLOR = "#E87722"
private const val REALIZED_COLOR = "#642667"
private const val DEFERRED_COLOR = "#75787b"

private const val ADD_ENTRY = 0
private const val REQUEST_PHOTO = 1

private const val DATE_FORMAT = "(MMM dd, yyyy)"

class DreamDetailFragment : Fragment(), DreamEntryEditFragment.Callbacks {

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var dreamEntryRecyclerView: RecyclerView
    private lateinit var titleField: EditText
    private lateinit var realizedCheckBox: CheckBox
    private lateinit var deferredCheckBox: CheckBox
//    private lateinit var dreamRevealedButton: Button
//    private lateinit var dreamDeferredButton: Button
    private lateinit var dreamEntryEditButton: FloatingActionButton
    private lateinit var dreamIconImageView: ImageView
    private lateinit var dreamPhotoImageView: ImageView

    private var adapter: DreamEntryAdapter? = DreamEntryAdapter(emptyList())
//    private lateinit var dreamEntry1Button: Button
//    private lateinit var dreamEntry2Button: Button
//    private lateinit var dreamEntry3Button: Button
//    private val dreamCommentButtons: ArrayList<Button> = ArrayList()

//    private val dreamRepository = DreamRepository.get()

    private val dreamDetailViewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        dream = Dream()
        setHasOptionsMenu(true)
//        dreamWithEntries = DreamWithEntries(Dream(), ArrayList())

        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        dreamDetailViewModel.loadDream(dreamId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dream_detail, container, false)

        titleField = view.findViewById(R.id.dream_title) as EditText
        realizedCheckBox = view.findViewById(R.id.dream_realized) as CheckBox
        deferredCheckBox = view.findViewById(R.id.dream_deferred) as CheckBox
//        dreamRevealedButton = view.findViewById(R.id.dream_entry_0_button) as Button
//        dreamDeferredButton = view.findViewById(R.id.dream_entry_4_button) as Button
//        dreamEntry1Button = view.findViewById(R.id.dream_entry_1_button) as Button
//        dreamEntry2Button = view.findViewById(R.id.dream_entry_2_button) as Button
//        dreamEntry3Button = view.findViewById(R.id.dream_entry_3_button) as Button
        dreamEntryEditButton = view.findViewById(R.id.add_comment_fab)
        dreamIconImageView = view.findViewById(R.id.dream_icon)
        dreamPhotoImageView = view.findViewById(R.id.dream_photo)
        dreamEntryRecyclerView = view.findViewById(R.id.dream_entry_recycler_view)
        dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)
//        dreamCommentButtons.add(dreamEntry1Button)
//        dreamCommentButtons.add(dreamEntry2Button)
//        dreamCommentButtons.add(dreamEntry3Button)

//        if (deferredCheckBox.isChecked) {
//            dreamDeferredButton.text = getString(R.string.Deferred)
//            realizedCheckBox.isEnabled = false
//        } else if (realizedCheckBox.isChecked) {
//            dreamDeferredButton.text = getString(R.string.Realized)
//            deferredCheckBox.isEnabled = false
//        } else {
//            dreamDeferredButton.visibility = View.INVISIBLE
//        }
//
//        dreamCommentButtons.forEach {
//            it.visibility = View.GONE
//        }
        val itemTouchHelper = adapter?.let {
            SwipeToDeleteCallback()
        }?.let {
            ItemTouchHelper(it)
        }
        itemTouchHelper?.attachToRecyclerView(dreamEntryRecyclerView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dreamDetailViewModel.dreamLiveData.observe(
            viewLifecycleOwner,
            Observer { dream ->
                dream?.let {
                    this.dreamWithEntries = dream
                    photoFile = dreamDetailViewModel.getPhotoFile(dreamWithEntries)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "edu.vt.cs.cs5254.dreamcatcher.fileprovider",
                        photoFile
                    )

                    adapter = DreamEntryAdapter(dream.dreamEntries)
                    dreamEntryRecyclerView.adapter = adapter
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

            }
        }

        titleField.addTextChangedListener(titleWatcher)

//        deferredCheckBox.apply {
//            setOnCheckedChangeListener { _, isChecked ->
//                dreamWithEntries.dream.isDeferred = isChecked
//
//                if (isChecked) {
////                    dreamDeferredButton.visibility = View.VISIBLE
////                    dreamDeferredButton.text = getString(R.string.Deferred)
//                    realizedCheckBox.isEnabled = false
//                } else {
//                    realizedCheckBox.isEnabled = true
////                    dreamDeferredButton.visibility = View.INVISIBLE
//                }
//                addOrDeleteDreamEntry(isChecked, DreamEntryKind.DEFERRED)
//            }
//        }
//
//        realizedCheckBox.apply {
//            setOnCheckedChangeListener { _, isChecked ->
//                dreamWithEntries.dream.isRealized = isChecked
//
//                if (isChecked) {
////                    dreamDeferredButton.visibility = View.VISIBLE
////                    dreamDeferredButton.text = getString(R.string.Realized)
//                    deferredCheckBox.isEnabled = false
//                } else {
//                    deferredCheckBox.isEnabled = true
////                    dreamDeferredButton.visibility = View.INVISIBLE
//                }
//                addOrDeleteDreamEntry(isChecked, DreamEntryKind.REALIZED)
//            }
//        }
//
//        dreamEntryEditButton.apply {
//            setOnClickListener {
//                DreamEntryEditFragment.newInstance().apply {
//                    setTargetFragment(this@DreamDetailFragment, ADD_ENTRY)
//                    show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ENTRY)
//                }
//            }
//        }
        realizedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isRealized = isChecked
                updateUI()
                addOrDeleteDreamEntry(isChecked, DreamEntryKind.REALIZED)
            }
        }

        deferredCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                updateUI()
                addOrDeleteDreamEntry(isChecked, DreamEntryKind.DEFERRED)
            }
        }

        dreamEntryEditButton.apply {
            setOnClickListener {
                DreamEntryEditFragment.newInstance().apply {
                    setTargetFragment(this@DreamDetailFragment, ADD_ENTRY)
                    show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ENTRY)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dreamDetailViewModel.saveDream(dreamWithEntries)
    }

    override fun onDetach() {
        super.onDetach()
        revokeCaptureImagePermissions(requireActivity(), photoUri)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)

        val cameraAvailability = isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            isEnabled = cameraAvailability
            isVisible = cameraAvailability
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(Intent.EXTRA_SUBJECT, "My Dream")
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, "My Dream")
                    startActivity(chooserIntent)
                }
                true
            }

            R.id.take_dream_photo -> {
                val capturePhotoIntent = createCaptureImageIntent(requireActivity(), photoUri)
                startActivityForResult(capturePhotoIntent, REQUEST_PHOTO)
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_PHOTO -> {
                revokeCaptureImagePermissions(requireActivity(), photoUri)
                updatePhotoView()
            }
        }
    }

    override fun onDreamEntryEdited(dreamEntry: String) {
        val newDreamEntry = DreamEntry(comment = dreamEntry, dreamId = dreamWithEntries.dream.id)
        dreamDetailViewModel.saveDream(dreamWithEntries)
        dreamDetailViewModel.addDreamEntry(newDreamEntry)
    }

    private fun addOrDeleteDreamEntry(checked: Boolean, kind: DreamEntryKind) {
        dreamDetailViewModel.saveDream(dreamWithEntries)

        val comment = if (kind == DreamEntryKind.REALIZED) "Dream Realized" else "Dream Deferred"

        if (checked) {
            val newDreamEntry = DreamEntry(
                comment = comment,
                kind = kind,
                dreamId = dreamWithEntries.dream.id
            )

            if (dreamWithEntries.dreamEntries.none { it.kind == kind}) {
                dreamDetailViewModel.addDreamEntry(newDreamEntry)
            }
        } else {
            dreamDetailViewModel.deleteDreamEntry(dreamWithEntries.dream.id, kind)
        }
    }



//    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        titleField.setText(dreamWithEntries.dream.description)

        deferredCheckBox.apply {
            isChecked = dreamWithEntries.dream.isDeferred
            jumpDrawablesToCurrentState()
        }
        realizedCheckBox.apply {
            isChecked = dreamWithEntries.dream.isRealized
            jumpDrawablesToCurrentState()
        }

        when {
            dreamWithEntries.dream.isRealized -> {
                dreamIconImageView.setImageResource(R.drawable.dream_realized_icon)
                dreamIconImageView.tag = R.drawable.dream_realized_icon
            }

            dreamWithEntries.dream.isDeferred -> {
                dreamIconImageView.setImageResource(R.drawable.dream_deferred_icon)
                dreamIconImageView.tag = R.drawable.dream_deferred_icon
            }

            else -> {
                dreamIconImageView.setImageDrawable(null)
                dreamIconImageView.tag = 0
            }
        }

        deferredCheckBox.isEnabled = !realizedCheckBox.isChecked
        realizedCheckBox.isEnabled = !deferredCheckBox.isChecked
        dreamEntryEditButton.isEnabled = !realizedCheckBox.isChecked

        updatePhotoView()
//        var buttonIndex = 0
//
//        if (dreamCommentButtons[0] == dreamEntry1Button) {
//            Log.d(TAG, "Good")
//        }
//
//        dreamWithEntries.dreamEntries.forEach {
//            if (it.kind == DreamEntryKind.COMMENT) {
//                dreamCommentButtons[buttonIndex].visibility = View.VISIBLE
//                val dateList = it.dateCreated.toString().split(" ")
//                dreamCommentButtons[buttonIndex].text = it.comment +
//                        listOf(dateList[1],dateList[2],dateList[5])
//                            .joinToString(prefix = " (", postfix = ") ", separator = " ")
//
//                buttonIndex++
//            }
//        }
    }

    private fun setButtonColor(button: Button, bkgdColor: String, textColor: Int = Color.WHITE) {
        button.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(bkgdColor))
        button.setTextColor(textColor)
    }

    private fun updateEntryButton(button: Button, entry: DreamEntry) {
        button.isEnabled = false
        button.text = entry.comment

        when (entry.kind) {
            DreamEntryKind.REVEALED -> {
                setButtonColor(button, REVEAL_COLOR)
            }

            DreamEntryKind.COMMENT -> {
                setButtonColor(button, COMMENT_COLOR)
                val dateFormat = DateFormat.getMediumDateFormat((activity))
                val createdDate = dateFormat.format(dreamWithEntries.dream.dateRevealed)
                button.text = "${entry.comment} ($createdDate)"
            }

            DreamEntryKind.DEFERRED -> {
                setButtonColor(button, DEFERRED_COLOR)
            }

            DreamEntryKind.REALIZED -> {
                setButtonColor(button, REALIZED_COLOR)
            }
        }
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val photoBitMap = getScaledBitmap(photoFile.path, requireActivity())
            dreamPhotoImageView.setImageBitmap(photoBitMap)
        } else {
            dreamPhotoImageView.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }

    private fun getDreamReport(): String {
        var report = "# ${dreamWithEntries.dream.description} \n"
        for (entry in dreamWithEntries.dreamEntries) {
            val date = DateFormat.format(DATE_FORMAT, entry.dateCreated).toString()
            report += "${entry.comment} $date \n"
        }
        return report
    }

    private fun deleteItem(position: Int) {
        val dreamEntry = dreamWithEntries.dreamEntries[position]
        if (dreamEntry.kind == DreamEntryKind.COMMENT) {
            dreamDetailViewModel.deleteDreamEntry(dreamEntry.id)
        }
    }

    inner class DreamEntryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var dreamEntry: DreamEntry
        private val dreamEntryButton: Button = itemView.findViewById(R.id.dream_entry_button)

        fun bind(dreamEntry: DreamEntry) {
            this.dreamEntry = dreamEntry
            updateEntryButton(dreamEntryButton, dreamEntry)
        }
    }

    inner class DreamEntryAdapter(private var dreamEntries: List<DreamEntry>) :
        RecyclerView.Adapter<DreamEntryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
            val view = layoutInflater.inflate(R.layout.list_item_dream_entry, parent, false)
            return DreamEntryHolder(view)
        }

        override fun getItemCount(): Int = dreamEntries.size

        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }
    }

    inner class SwipeToDeleteCallback:
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            //To change body of created functions use File | Settings | File Templates.
            TODO("Not yet implemented")
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            deleteItem(position)
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dreamEntry = dreamWithEntries.dreamEntries[viewHolder.adapterPosition]
            return if (dreamEntry.kind == DreamEntryKind.COMMENT) {
                ItemTouchHelper.LEFT
            } else {
                0
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