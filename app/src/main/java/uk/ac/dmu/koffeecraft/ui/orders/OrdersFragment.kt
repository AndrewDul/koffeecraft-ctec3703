package uk.ac.dmu.koffeecraft.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.cart.CartManager
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

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrdersAdapter(
            items = emptyList(),
            detailsByOrderId = emptyMap(),
            onOrderAgain = { order ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val reorderItems = db.orderItemDao().getReorderItems(order.orderId)

                    CartManager.clear()
                    reorderItems.forEach { CartManager.add(it.product, it.quantity) }

                    withContext(Dispatchers.Main) {
                        findNavController().navigate(R.id.cartFragment)
                    }
                }
            }
        )

        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeByCustomer(customerId).collect { orders ->
                val detailsByOrderId: Map<Long, List<OrderDisplayItem>> = withContext(Dispatchers.IO) {
                    orders.associate { order ->
                        order.orderId to db.orderItemDao().getDisplayItemsForOrder(order.orderId)
                    }
                }

                adapter.submitData(orders, detailsByOrderId)
                tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}