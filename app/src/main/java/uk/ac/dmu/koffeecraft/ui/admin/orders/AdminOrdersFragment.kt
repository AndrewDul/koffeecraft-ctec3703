package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class AdminOrdersFragment : Fragment(R.layout.fragment_admin_orders) {

    private lateinit var adapter: AdminOrdersAdapter

    private var currentStatusFilter: String? = "PLACED"
    private var currentQuery: String = ""
    private var sortDir: String = "DESC" // I default to Newest first.

    private var collectJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminOrders)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        val etSearch = view.findViewById<TextInputEditText>(R.id.etSearch)

        val chipPlaced = view.findViewById<Chip>(R.id.chipPlaced)
        val chipPreparing = view.findViewById<Chip>(R.id.chipPreparing)
        val chipReady = view.findViewById<Chip>(R.id.chipReady)
        val chipCollected = view.findViewById<Chip>(R.id.chipCollected)

        val chipNewest = view.findViewById<Chip>(R.id.chipNewest)
        val chipOldest = view.findViewById<Chip>(R.id.chipOldest)

        // I set defaults: show new orders first and sort by newest.
        chipPlaced.isChecked = true
        chipNewest.isChecked = true

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminOrdersAdapter(emptyList()) { row ->
            // I advance the status in a single click to simulate staff workflow.
            val next = nextStatus(row.status)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                db.orderDao().updateStatus(row.orderId, next)
            }
        }
        rv.adapter = adapter

        fun startCollecting() {
            // I cancel the previous collector to avoid multiple collectors running at once.
            collectJob?.cancel()

            collectJob = viewLifecycleOwner.lifecycleScope.launch {
                db.orderDao()
                    .observeAdminOrdersFiltered(currentStatusFilter, currentQuery, sortDir)
                    .collect { rows ->
                        adapter.submitList(rows)
                        tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    }
            }
        }

        // Start initial collection.
        startCollecting()

        // Status filters.
        chipPlaced.setOnClickListener { currentStatusFilter = "PLACED"; startCollecting() }
        chipPreparing.setOnClickListener { currentStatusFilter = "PREPARING"; startCollecting() }
        chipReady.setOnClickListener { currentStatusFilter = "READY"; startCollecting() }
        chipCollected.setOnClickListener { currentStatusFilter = "COLLECTED"; startCollecting() }

        // Sorting.
        chipNewest.setOnClickListener { sortDir = "DESC"; startCollecting() }
        chipOldest.setOnClickListener { sortDir = "ASC"; startCollecting() }

        // Search (Order # or email).
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim().orEmpty()
                startCollecting()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun nextStatus(current: String): String {
        return when (current) {
            "PLACED" -> "PREPARING"
            "PREPARING" -> "READY"
            "READY" -> "COLLECTED"
            else -> current
        }
    }
}