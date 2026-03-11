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
        val btnFeedback = view.findViewById<Button>(R.id.btnFeedback)

        val orderId = requireArguments().getLong("orderId")
        val simulate = requireArguments().getBoolean("simulate", false)

        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        btnBackToMenu.isEnabled = false
        btnFeedback.visibility = View.GONE

        btnBackToMenu.setOnClickListener { findNavController().navigate(R.id.menuFragment) }
        btnFeedback.setOnClickListener {
            findNavController().navigate(
                R.id.feedbackFragment,
                bundleOf("orderId" to orderId)
            )
        }

        var lastStatus: String? = null
        var firstEmission = true
        var simulationStarted = false

        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeById(orderId).collect { order ->
                val status = order?.status ?: "UNKNOWN"
                tvStatus.text = "Status: $status"

                val canFinish = (status == "READY" || status == "COLLECTED")
                val canLeaveFeedback = (status == "COLLECTED")

                btnBackToMenu.isEnabled = canFinish
                btnFeedback.visibility = if (canLeaveFeedback) View.VISIBLE else View.GONE


                // Start simulation ONLY for brand new order flow (from checkout)
                if (simulate && !simulationStarted && status == "PLACED") {
                    simulationStarted = true
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        delay(2000)
                        db.orderDao().updateStatus(orderId, "PREPARING")
                        delay(3000)
                        db.orderDao().updateStatus(orderId, "READY")
                        delay(10_000)
                        db.orderDao().updateStatus(orderId, "COLLECTED")
                    }
                }

                // Do NOT notify on first emission (opening old order should not trigger notifications)
                if (firstEmission) {
                    firstEmission = false
                    lastStatus = status
                    return@collect
                }

                // Notify only when status actually changes after first emission
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
                        "COLLECTED" -> NotificationHelper.showOrderNotification(
                            context = requireContext(),
                            title = "Collected",
                            message = "Order #$orderId has been collected. Enjoy!",
                            notificationId = 250000 + (orderId % 50000).toInt()
                        )
                    }
                    lastStatus = status
                }
            }
        }
    }
}