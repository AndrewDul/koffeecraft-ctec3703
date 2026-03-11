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
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val btnBackToMenu = view.findViewById<Button>(R.id.btnBackToMenu)

        val orderId = requireArguments().getLong("orderId")
        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        // Disable button until READY (optional UX)
        btnBackToMenu.isEnabled = false

        btnBackToMenu.setOnClickListener {
            findNavController().navigate(R.id.menuFragment)
        }

        var lastStatus: String? = null

        // Observe order status from DB
        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeById(orderId).collect { order ->
                val status = order?.status ?: "UNKNOWN"
                tvStatus.text = "Status: $status"
                btnBackToMenu.isEnabled = (status == "READY")

                // Notify only when status changes
                if (status != lastStatus) {
                    when (status) {
                        "PREPARING" -> NotificationHelper.showOrderNotification(
                            context = requireContext(),
                            title = "Order update",
                            message = "Order #$orderId is now being prepared.",
                            notificationId = 100000 + (orderId % 50000).toInt()
                        )
                        "READY" -> NotificationHelper.showOrderNotification(
                            context = requireContext(),
                            title = "Ready for pickup",
                            message = "Order #$orderId is ready. You can collect it now.",
                            notificationId = 200000 + (orderId % 50000).toInt()
                        )
                    }
                    lastStatus = status
                }
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