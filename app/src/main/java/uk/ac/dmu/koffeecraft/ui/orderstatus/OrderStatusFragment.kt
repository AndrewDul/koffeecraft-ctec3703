package uk.ac.dmu.koffeecraft.ui.orderstatus

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.orders.OrderSimulationManager

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val btnBackToMenu = view.findViewById<Button>(R.id.btnBackToMenu)
        val btnFeedback = view.findViewById<Button>(R.id.btnFeedback)

        val orderId = requireArguments().getLong("orderId")
        val simulate = requireArguments().getBoolean("simulate", false)

        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        btnBackToMenu.isEnabled = true
        btnFeedback.visibility = View.GONE

        btnBackToMenu.setOnClickListener { findNavController().navigate(R.id.menuFragment) }
        btnFeedback.setOnClickListener {
            findNavController().navigate(
                R.id.feedbackFragment,
                bundleOf("orderId" to orderId)
            )
        }

        if (simulate) {
            OrderSimulationManager.startIfNeeded(requireContext().applicationContext, orderId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeById(orderId).collect { order ->
                val status = order?.status ?: "UNKNOWN"
                tvStatus.text = "Status: $status"

                if (!simulate && order != null) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        AdminNotificationManager.syncAdminOrderActionNotification(
                            context = requireContext(),
                            db = db,
                            orderId = order.orderId,
                            orderCreatedAt = order.createdAt,
                            orderStatus = status
                        )
                    }
                }

                btnFeedback.visibility = if (status == "COLLECTED") View.VISIBLE else View.GONE
            }
        }
    }
}