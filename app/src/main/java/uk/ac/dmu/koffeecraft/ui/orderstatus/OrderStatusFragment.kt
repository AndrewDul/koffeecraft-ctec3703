package uk.ac.dmu.koffeecraft.ui.orderstatus

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.orders.OrderSimulationManager
import java.util.Locale

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvStatusChip = view.findViewById<TextView>(R.id.tvStatusChip)
        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val btnBackToMenu = view.findViewById<MaterialButton>(R.id.btnBackToMenu)
        val btnFeedback = view.findViewById<MaterialButton>(R.id.btnFeedback)

        val orderId = requireArguments().getLong("orderId")
        val simulate = requireArguments().getBoolean("simulate", false)

        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        btnBackToMenu.setOnClickListener {
            findNavController().navigate(R.id.menuFragment)
        }

        btnFeedback.setOnClickListener {
            if (!btnFeedback.isEnabled) return@setOnClickListener

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
                val formattedStatus = formatStatus(status)

                tvStatusChip.text = formattedStatus
                tvStatus.text = formattedStatus
                bindStatusChip(tvStatusChip, status)

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

                val feedbackEnabled = status == "COLLECTED"
                btnFeedback.visibility = View.VISIBLE
                btnFeedback.isEnabled = feedbackEnabled
                btnFeedback.alpha = if (feedbackEnabled) 1f else 0.45f
            }
        }
    }

    private fun bindStatusChip(view: TextView, status: String) {
        val background = view.background.mutate() as GradientDrawable

        when (status.uppercase(Locale.UK)) {
            "READY" -> {
                background.setColor(Color.parseColor("#DCE9DA"))
                view.setTextColor(Color.parseColor("#36533E"))
            }

            "PREPARING" -> {
                background.setColor(Color.parseColor("#F2E4D3"))
                view.setTextColor(Color.parseColor("#7A5634"))
            }

            "PLACED" -> {
                background.setColor(Color.parseColor("#E8DDD4"))
                view.setTextColor(Color.parseColor("#6A4D3A"))
            }

            "COLLECTED" -> {
                background.setColor(Color.parseColor("#DFE7D8"))
                view.setTextColor(Color.parseColor("#3D5640"))
            }

            "COMPLETED" -> {
                background.setColor(Color.parseColor("#DFE7D8"))
                view.setTextColor(Color.parseColor("#3D5640"))
            }

            "CANCELLED" -> {
                background.setColor(Color.parseColor("#F0DCD8"))
                view.setTextColor(Color.parseColor("#7B4A42"))
            }

            else -> {
                background.setColor(Color.parseColor("#E9DFD6"))
                view.setTextColor(Color.parseColor("#5C473A"))
            }
        }
    }

    private fun formatStatus(status: String): String {
        return status
            .lowercase(Locale.UK)
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { firstChar -> firstChar.titlecase(Locale.UK) }
            }
    }
}