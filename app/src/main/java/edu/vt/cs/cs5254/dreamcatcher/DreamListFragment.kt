package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import edu.vt.cs.cs5254.dreamcatcher.database.Dream
import edu.vt.cs.cs5254.dreamcatcher.database.DreamEntry
import edu.vt.cs.cs5254.dreamcatcher.database.DreamEntryKind
import edu.vt.cs.cs5254.dreamcatcher.database.DreamWithEntries
import java.util.*

private const val TAG = "DreamListFragment"

class DreamListFragment : Fragment() {

    /**
     * Required interface for hosting activities
     */
    interface Callbacks {
        fun onDreamSelected(dreamId: UUID)
    }

    private var callbacks: Callbacks? = null
    private var allDreams: List<Dream> = emptyList()
    private lateinit var dreamRecyclerView: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private var adapter: DreamAdapter? = DreamAdapter(emptyList())
    private lateinit var navigationView: NavigationView

    private val dreamListViewModel: DreamListViewModel by lazy {
        ViewModelProvider(this).get(DreamListViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    // New
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_dream_list,
            container,
            false)

        dreamRecyclerView =
            view.findViewById(R.id.dream_recycler_view) as RecyclerView
        dreamRecyclerView.layoutManager = LinearLayoutManager(context)
//        dreamRecyclerView.adapter = adapter

        drawerLayout = view.findViewById(R.id.drawer_layout)
        navigationView = view.findViewById(R.id.nav_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dreamListViewModel.dreamListLiveData.observe(
            viewLifecycleOwner,
            Observer { dreams ->
                dreams?.let {
                    Log.i(TAG, "Got dreams ${dreams.size}")
                    allDreams = dreams
                    updateUI(dreams)
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val dreamItems = navigationView.menu.getItem(0)

        dreamItems.isChecked = true
        dreamItems.setIcon(R.drawable.ic_menu_label_solid)

        navigationView.setNavigationItemSelectedListener { menuItem ->

            navigationView.checkedItem?.setIcon(R.drawable.ic_menu_label_outline)

            menuItem.isChecked = true
            menuItem.setIcon((R.drawable.ic_menu_label_solid))
            val dreamKindFilter = when (menuItem.itemId) {
                R.id.nav_all_dreams -> { _: Dream -> true }

                R.id.nav_active_dreams -> { dream: Dream ->
                    !dream.isDeferred && !dream.isRealized
                }

                R.id.nav_realized_dreams -> { dream: Dream -> dream.isRealized }

                R.id.nav_deferred_dreams -> { dream: Dream -> dream.isDeferred }

                else -> { _: Dream -> false }
            }

            val dreamsSelected: List<Dream> = allDreams.filter { dream -> dreamKindFilter(dream) }
            updateUI(dreamsSelected)
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    // New
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_dream -> {
                val dream = Dream()
                val dreamRevealedEntry = listOf(
                    DreamEntry(
                        comment = "Dream Revealed",
                        kind = DreamEntryKind.REVEALED,
                        dreamId = dream.id
                    )
                )
                val newDreamWithEntry = DreamWithEntries(dream, dreamRevealedEntry)
                dreamListViewModel.addDreamWithEntries(newDreamWithEntry)

//                val dreamWithEntries = DreamWithEntries(dream, emptyList())
//                dreamListViewModel.addDream(dreamWithEntries)
                callbacks?.onDreamSelected(dream.id)
                true
            }

            R.id.delete_dreams -> {
                dreamListViewModel.deleteAllDreams()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(dreams: List<Dream>) {
        adapter = DreamAdapter(dreams)
        dreamRecyclerView.adapter = adapter
    }


    inner class DreamHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var dream: Dream

        private val titleTextView: TextView = itemView.findViewById(R.id.dream_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.dream_date)
        private val dreamImageView: ImageView = itemView.findViewById(R.id.dream_icon)


        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dream: Dream) {
            this.dream = dream
            titleTextView.text = this.dream.description
            dateTextView.text = this.dream.dateRevealed.toString()

            val l = dateTextView.text.split(" ")
            dateTextView.text = listOf(l[1],l[2],l[5]).joinToString(separator = " ")

            when {
                dream.isDeferred -> {
                    dreamImageView.setImageResource(R.drawable.dream_deferred_icon)
                    dreamImageView.tag = R.drawable.dream_deferred_icon
                }
                dream.isRealized -> {
                    dreamImageView.setImageResource(R.drawable.dream_realized_icon)
                    dreamImageView.tag = R.drawable.dream_realized_icon
                }
                else -> {
                    dreamImageView.setImageDrawable(null)
                    dreamImageView.tag = 0
                }
            }
        }

        override fun onClick(v: View) {
            callbacks?.onDreamSelected(dream.id)
        }
    }

    private inner class DreamAdapter(var dreams: List<Dream>)
        : RecyclerView.Adapter<DreamHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : DreamHolder {
            val view = layoutInflater.inflate(R.layout.list_item_dream, parent, false)
            Log.d(TAG, "Create view holder")
            return DreamHolder(view)
        }

        override fun getItemCount() = dreams.size

        override fun onBindViewHolder(holder: DreamHolder, position: Int) {
            val dream = dreams[position]
            Log.d(TAG, "Bind view holder")
            holder.bind(dream)
        }

    }

    companion object {
        fun newInstance(): DreamListFragment {
            return DreamListFragment()
        }
    }
}