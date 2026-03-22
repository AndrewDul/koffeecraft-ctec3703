package uk.ac.dmu.koffeecraft.ui.orderstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrderFeedbackCounters
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderStatusViewModel(
    private val customerOrdersRepository: CustomerOrdersRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrderStatusUiState())
    val state: StateFlow<OrderStatusUiState> = _state

    private var observeJob: Job? = null
    private var startedOrderId: Long? = null

    fun start(orderId: Long, fromCheckout: Boolean) {
        if (startedOrderId == orderId && observeJob != null) return

        startedOrderId = orderId
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            customerOrdersRepository.observeOrder(orderId).collectLatest { order ->
                if (order == null) return@collectLatest

                val snapshot = customerOrdersRepository.loadOrderStatusSnapshot(orderId)
                _state.value = buildState(
                    order = order,
                    snapshot = snapshot,
                    fromCheckout = fromCheckout
                )
            }
        }
    }

    private fun buildState(
        order: Order,
        snapshot: uk.ac.dmu.koffeecraft.data.repository.CustomerOrderStatusSnapshot,
        fromCheckout: Boolean
    ): OrderStatusUiState {
        val status = order.status
        val normalized = status.uppercase(Locale.UK)
        val feedbackEnabled = canOpenFeedback(status) &&
                snapshot.feedbackCounters.eligibleItemCount > 0

        val heroState = buildHeroState(
            status = status,
            fromCheckout = fromCheckout
        )

        return OrderStatusUiState(
            orderId = order.orderId,
            createdAt = order.createdAt,
            statusRaw = status,
            statusLabel = formatStatus(status),
            heroIcon = heroState.icon,
            heroTitle = heroState.title,
            heroSubtitle = heroState.subtitle,
            heroTone = heroState.tone,
            itemsOrderedLabel = snapshot.itemsOrdered.toString(),
            totalPaidLabel = formatCurrency(order.totalAmount),
            paymentTypeLabel = formatPaymentType(snapshot.paymentType),
            etaLabel = buildEtaText(
                status = status,
                createdAt = order.createdAt
            ),
            statusHelper = buildStatusHelperText(
                status = status,
                itemsOrdered = snapshot.itemsOrdered,
                etaText = buildEtaText(status, order.createdAt)
            ),
            feedbackHelper = buildFeedbackHelperText(
                status = status,
                counters = snapshot.feedbackCounters
            ),
            feedbackButtonLabel = buildFeedbackButtonLabel(
                canOpen = canOpenFeedback(status),
                counters = snapshot.feedbackCounters
            ),
            feedbackEnabled = feedbackEnabled,
            stepPlacedActive = normalized in setOf("PLACED", "PREPARING", "READY", "COLLECTED", "COMPLETED"),
            stepPreparingActive = normalized in setOf("PREPARING", "READY", "COLLECTED", "COMPLETED"),
            stepReadyActive = normalized in setOf("READY", "COLLECTED", "COMPLETED"),
            stepCollectedActive = normalized in setOf("COLLECTED", "COMPLETED")
        )
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
        counters: CustomerOrderFeedbackCounters
    ): String {
        if (!canOpen) return "Leave feedback"

        if (counters.eligibleItemCount == 0) {
            return "Leave feedback"
        }

        return when {
            counters.reviewedItemCount <= 0 -> "Leave feedback"
            counters.reviewedItemCount < counters.eligibleItemCount -> "Continue feedback"
            else -> "Edit feedback"
        }
    }

    private fun buildFeedbackHelperText(
        status: String,
        counters: CustomerOrderFeedbackCounters
    ): String {
        if (!canOpenFeedback(status)) {
            return "Feedback becomes available after collection."
        }

        if (counters.eligibleItemCount == 0) {
            return "No paid products from this order are available for feedback."
        }

        return when {
            counters.reviewedItemCount <= 0 ->
                "You can now rate your order and leave product feedback."

            counters.reviewedItemCount < counters.eligibleItemCount ->
                "You can continue rating the remaining products from this order."

            else ->
                "You have already reviewed all paid products from this order. You can still edit them anytime."
        }
    }

    private fun formatStatus(status: String): String {
        return status
            .lowercase(Locale.UK)
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.titlecase(Locale.UK) }
            }
    }
}

private data class HeroState(
    val icon: String,
    val title: String,
    val subtitle: String,
    val tone: HeroTone
)