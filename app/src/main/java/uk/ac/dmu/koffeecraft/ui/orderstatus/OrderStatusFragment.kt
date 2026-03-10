package uk.ac.dmu.koffeecraft.ui.orderstatus

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val btnBackToMenu = view.findViewById<Button>(R.id.btnBackToMenu)

        val orderId = requireArguments().getLong("orderId")
        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        // Optional: disable button until READY
        btnBackToMenu.isEnabled = false

        btnBackToMenu.setOnClickListener {
            // Go to Menu and clear back stack up to Menu (so user doesn't go back to checkout)
            findNavController().navigate(R.id.menuFragment)
        }

        // Observe order status from DB
        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeById(orderId).collect { order ->
                val status = order?.status ?: "UNKNOWN"
                tvStatus.text = "Status: $status"

                // Enable when READY (optional)
                btnBackToMenu.isEnabled = (status == "READY")
            }
        }

        // Solo MVP: simulate status changes automatically
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            db.orderDao().updateStatus(orderId, "PREPARING")
            delay(3000)
            db.orderDao().updateStatus(orderId, "READY")
        }
    }
}