package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class AdminOrdersFragment : Fragment(R.layout.fragment_admin_orders) {

    private lateinit var adapter: AdminOrdersAdapter
    private var currentStatusFilter: String? = "PLACED"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminOrders)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        val chipPlaced = view.findViewById<Chip>(R.id.chipPlaced)
        val chipPreparing = view.findViewById<Chip>(R.id.chipPreparing)
        val chipReady = view.findViewById<Chip>(R.id.chipReady)
        val chipCollected = view.findViewById<Chip>(R.id.chipCollected)

        // I set default filter to PLACED to show new orders first.
        chipPlaced.isChecked = true

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

        fun reload() {
            viewLifecycleOwner.lifecycleScope.launch {
                db.orderDao().observeAdminOrders(currentStatusFilter).collect { rows ->
                    adapter.submitList(rows)
                    tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Initial load
        reload()

        // Filter clicks
        chipPlaced.setOnClickListener { currentStatusFilter = "PLACED"; reload() }
        chipPreparing.setOnClickListener { currentStatusFilter = "PREPARING"; reload() }
        chipReady.setOnClickListener { currentStatusFilter = "READY"; reload() }
        chipCollected.setOnClickListener { currentStatusFilter = "COLLECTED"; reload() }
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