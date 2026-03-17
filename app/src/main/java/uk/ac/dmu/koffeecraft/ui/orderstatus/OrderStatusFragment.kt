package uk.ac.dmu.koffeecraft.ui.orderstatus

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.orders.OrderSimulationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvHeroIcon = view.findViewById<TextView>(R.id.tvHeroIcon)
        val tvHeroTitle = view.findViewById<TextView>(R.id.tvHeroTitle)
        val tvHeroSubtitle = view.findViewById<TextView>(R.id.tvHeroSubtitle)

        val tvStatusChip = view.findViewById<TextView>(R.id.tvStatusChip)
        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)

        val tvItemsOrderedValue = view.findViewById<TextView>(R.id.tvItemsOrderedValue)
        val tvTotalPaidValue = view.findViewById<TextView>(R.id.tvTotalPaidValue)
        val tvPaymentTypeValue = view.findViewById<TextView>(R.id.tvPaymentTypeValue)
        val tvEtaValue = view.findViewById<TextView>(R.id.tvEtaValue)

        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvStatusHelper = view.findViewById<TextView>(R.id.tvStatusHelper)
        val tvFeedbackHelper = view.findViewById<TextView>(R.id.tvFeedbackHelper)

        val tvStepPlaced = view.findViewById<TextView>(R.id.tvStepPlaced)
        val tvStepPreparing = view.findViewById<TextView>(R.id.tvStepPreparing)
        val tvStepReady = view.findViewById<TextView>(R.id.tvStepReady)
        val tvStepCollected = view.findViewById<TextView>(R.id.tvStepCollected)

        val btnBackToHome = view.findViewById<MaterialButton>(R.id.btnBackToHome)
        val btnViewOrders = view.findViewById<MaterialButton>(R.id.btnViewOrders)
        val btnFeedback = view.findViewById<MaterialButton>(R.id.btnFeedback)

        val orderId = requireArguments().getLong("orderId")
        val simulate = requireArguments().getBoolean("simulate", false)
        val fromCheckout = requireArguments().getBoolean("fromCheckout", false)

        tvOrderId.text = "Order #$orderId"

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        btnBackToHome.setOnClickListener {
            findNavController().navigate(R.id.customerHomeFragment)
        }

        btnViewOrders.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment)
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
                if (order == null) return@collect

                val status = order.status
                val formattedStatus = formatStatus(status)

                val orderUiData = withContext(Dispatchers.IO) {
                    val payment = db.paymentDao().getLatestForOrder(orderId)
                    val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)
                    val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)
                    val reviewedCount = feedbackItems.count { it.feedbackId != null }

                    OrderStatusUiData(
                        paymentType = payment?.paymentType ?: "UNKNOWN",
                        itemsOrdered = displayItems.sumOf { it.quantity },
                        displayItems = displayItems,
                        feedbackState = FeedbackEntryState(
                            eligibleItemCount = feedbackItems.size,
                            reviewedItemCount = reviewedCount
                        )
                    )
                }

                tvStatusChip.text = formattedStatus
                tvStatus.text = formattedStatus
                bindStatusChip(tvStatusChip, status)

                tvItemsOrderedValue.text = orderUiData.itemsOrdered.toString()
                tvTotalPaidValue.text = formatCurrency(order.totalAmount)
                tvPaymentTypeValue.text = formatPaymentType(orderUiData.paymentType)
                tvEtaValue.text = buildEtaText(
                    status = status,
                    createdAt = order.createdAt
                )

                val heroState = buildHeroState(
                    status = status,
                    fromCheckout = fromCheckout
                )
                bindHero(
                    iconView = tvHeroIcon,
                    titleView = tvHeroTitle,
                    subtitleView = tvHeroSubtitle,
                    heroState = heroState
                )

                tvStatusHelper.text = buildStatusHelperText(
                    status = status,
                    itemsOrdered = orderUiData.itemsOrdered,
                    etaText = tvEtaValue.text.toString()
                )

                bindJourney(
                    status = status,
                    tvStepPlaced = tvStepPlaced,
                    tvStepPreparing = tvStepPreparing,
                    tvStepReady = tvStepReady,
                    tvStepCollected = tvStepCollected
                )

                if (!simulate) {
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

                val feedbackEnabled = canOpenFeedback(status) &&
                        orderUiData.feedbackState.eligibleItemCount > 0

                btnFeedback.visibility = View.VISIBLE
                btnFeedback.isEnabled = feedbackEnabled
                btnFeedback.alpha = if (feedbackEnabled) 1f else 0.45f
                btnFeedback.text = buildFeedbackButtonLabel(
                    canOpen = canOpenFeedback(status),
                    summary = orderUiData.feedbackState
                )

                tvFeedbackHelper.text = buildFeedbackHelperText(
                    status = status,
                    summary = orderUiData.feedbackState
                )
            }
        }
    }

    private fun bindHero(
        iconView: TextView,
        titleView: TextView,
        subtitleView: TextView,
        heroState: HeroState
    ) {
        iconView.text = heroState.icon
        titleView.text = heroState.title
        subtitleView.text = heroState.subtitle

        when (heroState.tone) {
            HeroTone.SUCCESS -> {
                iconView.setTextColor(Color.parseColor("#36533E"))
            }
            HeroTone.WARM -> {
                iconView.setTextColor(Color.parseColor("#7A5634"))
            }
            HeroTone.NEUTRAL -> {
                iconView.setTextColor(Color.parseColor("#6A4D3A"))
            }
            HeroTone.DANGER -> {
                iconView.setTextColor(Color.parseColor("#7B4A42"))
            }
        }
    }

    private fun buildHeroState(
        status: String,
        fromCheckout: Boolean
    ): HeroState {
        return when (status.uppercase(Locale.UK)) {
            "PLACED" -> HeroState(
                icon = "✓",
                title = if (fromCheckout) "Order confirmed" else "Order received",
                subtitle = if (fromCheckout) {
                    "Your payment was successful and your order is now waiting to be prepared."
                } else {
                    "Your order has been placed successfully and is waiting for preparation."
                },
                tone = HeroTone.SUCCESS
            )

            "PREPARING" -> HeroState(
                icon = "☕",
                title = "Order in progress",
                subtitle = "Your drinks and treats are being prepared now.",
                tone = HeroTone.WARM
            )

            "READY" -> HeroState(
                icon = "★",
                title = "Ready for collection",
                subtitle = "Your order is ready. You can collect it now.",
                tone = HeroTone.SUCCESS
            )

            "COLLECTED" -> HeroState(
                icon = "✓",
                title = "Order collected",
                subtitle = "Your order has been collected successfully. Enjoy your KoffeeCraft moment.",
                tone = HeroTone.SUCCESS
            )

            "COMPLETED" -> HeroState(
                icon = "✓",
                title = "Order completed",
                subtitle = "This order has been completed successfully.",
                tone = HeroTone.SUCCESS
            )

            "CANCELLED" -> HeroState(
                icon = "!",
                title = "Order cancelled",
                subtitle = "This order was cancelled and is no longer being processed.",
                tone = HeroTone.DANGER
            )

            else -> HeroState(
                icon = "•",
                title = "Order update",
                subtitle = "Your order details are available below.",
                tone = HeroTone.NEUTRAL
            )
        }
    }

    private fun bindJourney(
        status: String,
        tvStepPlaced: TextView,
        tvStepPreparing: TextView,
        tvStepReady: TextView,
        tvStepCollected: TextView
    ) {
        val normalized = status.uppercase(Locale.UK)

        val placedActive = normalized in setOf("PLACED", "PREPARING", "READY", "COLLECTED", "COMPLETED")
        val preparingActive = normalized in setOf("PREPARING", "READY", "COLLECTED", "COMPLETED")
        val readyActive = normalized in setOf("READY", "COLLECTED", "COMPLETED")
        val collectedActive = normalized in setOf("COLLECTED", "COMPLETED")

        setJourneyStep(tvStepPlaced, placedActive)
        setJourneyStep(tvStepPreparing, preparingActive)
        setJourneyStep(tvStepReady, readyActive)
        setJourneyStep(tvStepCollected, collectedActive)

        if (normalized == "CANCELLED") {
            setJourneyStep(tvStepPlaced, false)
            setJourneyStep(tvStepPreparing, false)
            setJourneyStep(tvStepReady, false)
            setJourneyStep(tvStepCollected, false)
        }
    }

    private fun setJourneyStep(view: TextView, isActive: Boolean) {
        view.setBackgroundResource(
            if (isActive) R.drawable.bg_orders_filter_chip_selected
            else R.drawable.bg_orders_filter_chip
        )
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isActive) android.R.color.black else android.R.color.darker_gray
            )
        )
        view.alpha = if (isActive) 1f else 0.9f
    }

    private fun buildStatusHelperText(
        status: String,
        itemsOrdered: Int,
        etaText: String
    ): String {
        return when (status.uppercase(Locale.UK)) {
            "PLACED" ->
                "We received your order for $itemsOrdered item${if (itemsOrdered == 1) "" else "s"}. Estimated ready time: $etaText."

            "PREPARING" ->
                "Your order is now being prepared. Estimated ready time: $etaText."

            "READY" ->
                "Your order is ready for collection."

            "COLLECTED" ->
                "Your order has been collected. Thank you for ordering with KoffeeCraft."

            "COMPLETED" ->
                "Your order is complete. You can still review your purchased products below."

            "CANCELLED" ->
                "This order has been cancelled. If this looks unexpected, please contact KoffeeCraft support."

            else ->
                "Track the latest progress of your order below."
        }
    }

    private fun buildEtaText(
        status: String,
        createdAt: Long
    ): String {
        return when (status.uppercase(Locale.UK)) {
            "PLACED", "PREPARING" -> {
                val formatter = SimpleDateFormat("HH:mm", Locale.UK)
                formatter.format(Date(createdAt + (15 * 60 * 1000L)))
            }

            "READY" -> "Ready now"
            "COLLECTED" -> "Collected"
            "COMPLETED" -> "Completed"
            "CANCELLED" -> "Unavailable"
            else -> "Updating"
        }
    }

    private fun formatPaymentType(paymentType: String): String {
        return when (paymentType.uppercase(Locale.UK)) {
            "CARD" -> "Card"
            "CASH" -> "Cash"
            else -> paymentType.lowercase(Locale.UK)
                .replaceFirstChar { it.titlecase(Locale.UK) }
        }
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.UK, "£%.2f", value)
    }

    private fun canOpenFeedback(status: String): Boolean {
        return when (status.uppercase(Locale.UK)) {
            "COLLECTED", "COMPLETED" -> true
            else -> false
        }
    }

    private fun buildFeedbackButtonLabel(
        canOpen: Boolean,
        summary: FeedbackEntryState
    ): String {
        if (!canOpen) return "Leave feedback"

        if (summary.eligibleItemCount == 0) {
            return "Leave feedback"
        }

        return when {
            summary.reviewedItemCount <= 0 -> "Leave feedback"
            summary.reviewedItemCount < summary.eligibleItemCount -> "Continue feedback"
            else -> "Edit feedback"
        }
    }

    private fun buildFeedbackHelperText(
        status: String,
        summary: FeedbackEntryState
    ): String {
        if (!canOpenFeedback(status)) {
            return "Feedback becomes available after collection."
        }

        if (summary.eligibleItemCount == 0) {
            return "No paid products from this order are available for feedback."
        }

        return when {
            summary.reviewedItemCount <= 0 ->
                "You can now rate your order and leave product feedback."

            summary.reviewedItemCount < summary.eligibleItemCount ->
                "You can continue rating the remaining products from this order."

            else ->
                "You have already reviewed all paid products from this order. You can still edit them anytime."
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

private data class FeedbackEntryState(
    val eligibleItemCount: Int,
    val reviewedItemCount: Int
)

private data class OrderStatusUiData(
    val paymentType: String,
    val itemsOrdered: Int,
    val displayItems: List<OrderDisplayItem>,
    val feedbackState: FeedbackEntryState
)

private data class HeroState(
    val icon: String,
    val title: String,
    val subtitle: String,
    val tone: HeroTone
)

private enum class HeroTone {
    SUCCESS,
    WARM,
    NEUTRAL,
    DANGER
}