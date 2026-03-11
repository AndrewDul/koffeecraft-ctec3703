package uk.ac.dmu.koffeecraft.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private lateinit var adapter: OrdersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<Button>(R.id.btnBackToMenu)
        val rv = view.findViewById<RecyclerView>(R.id.rvOrders)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        btnBack.setOnClickListener { findNavController().navigateUp() }

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "You are not logged in as a customer."
            return
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = OrdersAdapter(emptyList()) { order ->
            // Open OrderStatus for selected order
            findNavController().navigate(
                R.id.orderStatusFragment,
                bundleOf("orderId" to order.orderId)
            )
        }
        rv.adapter = adapter

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeByCustomer(customerId).collect { orders ->
                adapter.submitList(orders)
                tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}