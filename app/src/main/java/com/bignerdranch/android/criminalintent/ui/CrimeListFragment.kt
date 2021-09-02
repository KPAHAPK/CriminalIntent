package com.bignerdranch.android.criminalintent.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.R
import com.bignerdranch.android.criminalintent.domain.Crime
import com.bignerdranch.android.criminalintent.viewmodel.CrimeListViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null

    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var buttonAddCrime: Button
    private lateinit var textViewAddCrime: TextView
    private var myAdapter: CrimeAdapter? = CrimeAdapter(emptyList())
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.apply {
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = myAdapter
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonAddCrime = view.findViewById(R.id.button_add_crime)
        textViewAddCrime = view.findViewById(R.id.text_view_add_crime)

        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner, { crimes ->
                crimes?.let {
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "_____________________________onStart")

    }


    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(crimes: List<Crime>) {
        myAdapter = CrimeAdapter(crimes)
        myAdapter?.submitList(crimes)
        crimeRecyclerView.adapter = myAdapter
        showHintForEmptyList(crimes.isEmpty())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                addCrime()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun showHintForEmptyList(isEmptyList: Boolean) {
        if (isEmptyList) {
            buttonAddCrime.apply {
                visibility = View.VISIBLE
                setOnClickListener { this@CrimeListFragment.addCrime() }
            }
            textViewAddCrime.apply {
                visibility = View.VISIBLE
            }
        }else {
            buttonAddCrime.visibility = View.INVISIBLE
            textViewAddCrime.visibility = View.INVISIBLE
        }

    }

    fun addCrime() {
        val crime = Crime(title = "New Crime")
        crimeListViewModel.addCrime(crime)
        callbacks?.onCrimeSelected(crime.id)
    }


    private inner class CrimeAdapter(var crimes: List<Crime>) :
        ListAdapter<Crime, CrimeAdapter.CrimeHolder>(CrimeDiffCallback()) {

        private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
            View.OnClickListener {

            private lateinit var crime: Crime

            private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
            private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
            private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

            fun bind(crime: Crime) {
                this.crime = crime
                titleTextView.text = this.crime.title

                val pattern = "EEEE, MMM dd, yyyy HH:mm"
                val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                dateTextView.text = simpleDateFormat.format(this.crime.date)

                solvedImageView.visibility = if (this.crime.isSolved) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                callbacks?.onCrimeSelected(crime.id)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.apply {
                bind(crime)
            }
        }

        override fun getItemCount(): Int = crimes.size
    }

    class CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean =
            oldItem == newItem

    }
}