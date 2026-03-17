package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminOrdersAdapter(
    private var items: List<AdminOrderRow>,
    private var expandedOrderId: Long?,
    private var loadingOrderId: Long?,
    private var detailByOrderId: Map<Long, AdminOrderDetailsUi>,
    private val onToggleExpand: (AdminOrderRow) -> Unit,
    private val onStatusAction: (AdminOrderRow, String) -> Unit
) : RecyclerView.Adapter<AdminOrdersAdapter.VH>() {

    private val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK)

    fun submitState(
        items: List<AdminOrderRow>,
        expandedOrderId: Long?,
        loadingOrderId: Long?,
        detailByOrderId: Map<Long, AdminOrderDetailsUi>
    ) {
        this.items = items
        this.expandedOrderId = expandedOrderId
        this.loadingOrderId = loadingOrderId
        this.detailByOrderId = detailByOrderId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false)
        return VH(view, formatter, onToggleExpand, onStatusAction)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.bind(
            row = row,
            isExpanded = expandedOrderId == row.orderId,
            isLoading = loadingOrderId == row.orderId,
            details = detailByOrderId[row.orderId]
        )
    }

    class VH(
        itemView: View,
        private val formatter: SimpleDateFormat,
        private val onToggleExpand: (AdminOrderRow) -> Unit,
        private val onStatusAction: (AdminOrderRow, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: View = itemView.findViewById(R.id.cardRoot)
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        private val tvExpandIndicator: TextView = itemView.findViewById(R.id.tvExpandIndicator)
        private val tvCustomerLine: TextView = itemView.findViewById(R.id.tvCustomerLine)
        private val tvMetaLine: TextView = itemView.findViewById(R.id.tvMetaLine)
        private val tvSummaryLine: TextView = itemView.findViewById(R.id.tvSummaryLine)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layoutExpanded)
        private val tvLoadingDetails: TextView = itemView.findViewById(R.id.tvLoadingDetails)

        private val tvExpandedCustomerName: TextView = itemView.findViewById(R.id.tvExpandedCustomerName)
        private val tvExpandedCustomerId: TextView = itemView.findViewById(R.id.tvExpandedCustomerId)
        private val tvExpandedCustomerEmail: TextView = itemView.findViewById(R.id.tvExpandedCustomerEmail)
        private val tvExpandedOrderTime: TextView = itemView.findViewById(R.id.tvExpandedOrderTime)
        private val tvExpandedPaymentType: TextView = itemView.findViewById(R.id.tvExpandedPaymentType)
        private val tvExpandedTotal: TextView = itemView.findViewById(R.id.tvExpandedTotal)

        private val tvPromoEligibleChip: TextView = itemView.findViewById(R.id.tvPromoEligibleChip)
        private val tvFeedbackWrittenChip: TextView = itemView.findViewById(R.id.tvFeedbackWrittenChip)
        private val tvCraftedItemsChip: TextView = itemView.findViewById(R.id.tvCraftedItemsChip)
        private val tvRewardIncludedChip: TextView = itemView.findViewById(R.id.tvRewardIncludedChip)

        private val layoutItemsContainer: LinearLayout = itemView.findViewById(R.id.layoutItemsContainer)

        private val tvStepPlaced: TextView = itemView.findViewById(R.id.tvStepPlaced)
        private val tvStepPreparing: TextView = itemView.findViewById(R.id.tvStepPreparing)
        private val tvStepReady: TextView = itemView.findViewById(R.id.tvStepReady)
        private val tvStepCollected: TextView = itemView.findViewById(R.id.tvStepCollected)

        private val tvActionPreparing: TextView = itemView.findViewById(R.id.tvActionPreparing)
        private val tvActionReady: TextView = itemView.findViewById(R.id.tvActionReady)
        private val tvActionCollected: TextView = itemView.findViewById(R.id.tvActionCollected)

        fun bind(
            row: AdminOrderRow,
            isExpanded: Boolean,
            isLoading: Boolean,
            details: AdminOrderDetailsUi?
        ) {
            tvOrderId.text = "Order #${row.orderId}"
            tvCustomerLine.text = buildCustomerLine(row)
            tvMetaLine.text = "Customer #${row.customerId} • ${formatter.format(Date(row.createdAt))} • ${formatCurrency(row.totalAmount)}"
            tvSummaryLine.text = buildSummaryLine(row)
            tvExpandIndicator.text = if (isExpanded) "▴" else "▾"

            bindStatusChip(tvStatusChip, row.status)

            dividerExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE

            tvLoadingDetails.visibility = if (isExpanded && isLoading) View.VISIBLE else View.GONE

            if (isExpanded && details != null && !isLoading) {
                bindExpandedContent(row, details)
            } else if (isExpanded && !isLoading && details == null) {
                clearExpandedContent()
                tvLoadingDetails.visibility = View.VISIBLE
            } else {
                clearExpandedContent()
            }

            cardRoot.setOnClickListener {
                onToggleExpand(row)
            }
        }

        private fun buildCustomerLine(row: AdminOrderRow): String {
            return if (row.customerDisplayName.isBlank()) {
                row.customerEmail
            } else {
                "${row.customerDisplayName} • ${row.customerEmail}"
            }
        }

        private fun buildSummaryLine(row: AdminOrderRow): String {
            val summaryParts = mutableListOf<String>()
            summaryParts += "${row.itemCount} item${if (row.itemCount == 1) "" else "s"}"

            if (row.craftedLineCount > 0) {
                summaryParts += "${row.craftedLineCount} crafted"
            }

            if (row.rewardLineCount > 0) {
                summaryParts += "${row.rewardLineCount} reward"
            }

            return summaryParts.joinToString(" • ")
        }

        private fun bindExpandedContent(
            row: AdminOrderRow,
            details: AdminOrderDetailsUi
        ) {
            tvExpandedCustomerName.text = details.customerName.ifBlank { "Customer name unavailable" }
            tvExpandedCustomerId.text = "Customer #${details.customerId}"
            tvExpandedCustomerEmail.text = details.customerEmail
            tvExpandedOrderTime.text = formatter.format(Date(details.createdAt))
            tvExpandedPaymentType.text = formatPaymentType(details.paymentType)
            tvExpandedTotal.text = formatCurrency(details.totalAmount)

            bindNoteChip(tvPromoEligibleChip, details.promoEligible)
            bindNoteChip(tvFeedbackWrittenChip, details.feedbackWritten)
            bindNoteChip(tvCraftedItemsChip, details.hasCraftedItems)
            bindNoteChip(tvRewardIncludedChip, details.hasRewardItems)

            bindItems(details.items)
            bindTimeline(details.status)
            bindActions(row, details.status)
        }

        private fun clearExpandedContent() {
            tvExpandedCustomerName.text = ""
            tvExpandedCustomerId.text = ""
            tvExpandedCustomerEmail.text = ""
            tvExpandedOrderTime.text = ""
            tvExpandedPaymentType.text = ""
            tvExpandedTotal.text = ""

            layoutItemsContainer.removeAllViews()

            tvPromoEligibleChip.visibility = View.GONE
            tvFeedbackWrittenChip.visibility = View.GONE
            tvCraftedItemsChip.visibility = View.GONE
            tvRewardIncludedChip.visibility = View.GONE

            bindTimeline("")
            disableAction(tvActionPreparing, "Move to Preparing", false)
            disableAction(tvActionReady, "Mark Ready", false)
            disableAction(tvActionCollected, "Mark Collected", false)
        }

        private fun bindItems(items: List<AdminOrderLineUi>) {
            layoutItemsContainer.removeAllViews()

            if (items.isEmpty()) {
                val emptyView = TextView(itemView.context).apply {
                    text = "No order items available."
                    textSize = 13f
                    setTextColor(Color.parseColor("#7A6558"))
                }
                layoutItemsContainer.addView(emptyView)
                return
            }

            items.forEach { item ->
                val detailView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_order_detail, layoutItemsContainer, false)

                val tvDetailName = detailView.findViewById<TextView>(R.id.tvDetailName)
                val tvDetailLineTotal = detailView.findViewById<TextView>(R.id.tvDetailLineTotal)
                val tvDetailQuantityPrice = detailView.findViewById<TextView>(R.id.tvDetailQuantityPrice)
                val tvDetailCustomOption = detailView.findViewById<TextView>(R.id.tvDetailCustomOption)
                val tvDetailAddOns = detailView.findViewById<TextView>(R.id.tvDetailAddOns)
                val tvDetailCalories = detailView.findViewById<TextView>(R.id.tvDetailCalories)

                val lineTotal = item.unitPrice * item.quantity.toDouble()

                tvDetailName.text = item.productName
                tvDetailLineTotal.text = formatCurrency(lineTotal)
                tvDetailQuantityPrice.text = "Qty ${item.quantity} • ${formatCurrency(item.unitPrice)} each"

                val optionParts = mutableListOf<String>()

                if (item.isReward) {
                    optionParts += "Reward item"
                }

                if (item.isCrafted) {
                    optionParts += "Crafted"
                }

                if (!item.selectedOptionLabel.isNullOrBlank()) {
                    optionParts += item.selectedOptionLabel
                }

                if (item.selectedOptionSizeValue != null && !item.selectedOptionSizeUnit.isNullOrBlank()) {
                    optionParts += "${item.selectedOptionSizeValue}${item.selectedOptionSizeUnit}"
                }

                if (optionParts.isNotEmpty()) {
                    tvDetailCustomOption.visibility = View.VISIBLE
                    tvDetailCustomOption.text = optionParts.joinToString(" • ")
                } else {
                    tvDetailCustomOption.visibility = View.GONE
                }

                if (!item.selectedAddOnsSummary.isNullOrBlank()) {
                    tvDetailAddOns.visibility = View.VISIBLE
                    tvDetailAddOns.text = "Add-ons • ${item.selectedAddOnsSummary}"
                } else {
                    tvDetailAddOns.visibility = View.GONE
                }

                if (item.estimatedCalories != null) {
                    tvDetailCalories.visibility = View.VISIBLE
                    tvDetailCalories.text = "Approx. ${item.estimatedCalories} kcal"
                } else {
                    tvDetailCalories.visibility = View.GONE
                }

                layoutItemsContainer.addView(detailView)
            }
        }

        private fun bindTimeline(status: String) {
            val normalized = status.uppercase(Locale.UK)

            val placedActive = normalized in setOf("PLACED", "PREPARING", "READY", "COLLECTED")
            val preparingActive = normalized in setOf("PREPARING", "READY", "COLLECTED")
            val readyActive = normalized in setOf("READY", "COLLECTED")
            val collectedActive = normalized == "COLLECTED"

            setJourneyStep(tvStepPlaced, placedActive)
            setJourneyStep(tvStepPreparing, preparingActive)
            setJourneyStep(tvStepReady, readyActive)
            setJourneyStep(tvStepCollected, collectedActive)
        }

        private fun bindActions(
            row: AdminOrderRow,
            status: String
        ) {
            val normalized = status.uppercase(Locale.UK)

            enableAction(
                view = tvActionPreparing,
                label = "Move to Preparing",
                enabled = normalized == "PLACED"
            ) {
                onStatusAction(row, "PREPARING")
            }

            enableAction(
                view = tvActionReady,
                label = "Mark Ready",
                enabled = normalized == "PREPARING"
            ) {
                onStatusAction(row, "READY")
            }

            enableAction(
                view = tvActionCollected,
                label = "Mark Collected",
                enabled = normalized == "READY"
            ) {
                onStatusAction(row, "COLLECTED")
            }
        }

        private fun bindNoteChip(view: TextView, isVisible: Boolean) {
            view.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        private fun enableAction(
            view: TextView,
            label: String,
            enabled: Boolean,
            onClick: () -> Unit
        ) {
            view.text = label

            if (enabled) {
                view.isEnabled = true
                view.alpha = 1f
                view.setBackgroundResource(R.drawable.bg_secondary_pill_button)
                view.setTextColor(Color.parseColor("#5A4638"))
                view.setOnClickListener { onClick() }
            } else {
                disableAction(view, label, true)
            }
        }

        private fun disableAction(
            view: TextView,
            label: String,
            keepVisible: Boolean
        ) {
            view.text = label
            view.isEnabled = false
            view.alpha = if (keepVisible) 0.45f else 0f
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip)
            view.setTextColor(Color.parseColor("#7A6558"))
            view.setOnClickListener(null)
        }

        private fun setJourneyStep(view: TextView, isActive: Boolean) {
            view.setBackgroundResource(
                if (isActive) R.drawable.bg_orders_filter_chip_selected
                else R.drawable.bg_orders_filter_chip
            )
            view.setTextColor(
                if (isActive) Color.parseColor("#2E2018")
                else Color.parseColor("#7A6558")
            )
            view.alpha = if (isActive) 1f else 0.92f
        }

        private fun bindStatusChip(view: TextView, status: String) {
            view.text = formatStatus(status)
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
                .joinToString(" ") { part ->
                    part.replaceFirstChar { it.titlecase(Locale.UK) }
                }
        }

        private fun formatCurrency(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }

        private fun formatPaymentType(paymentType: String): String {
            return when (paymentType.uppercase(Locale.UK)) {
                "CARD" -> "Card"
                "CASH" -> "Cash"
                else -> paymentType.lowercase(Locale.UK).replaceFirstChar { it.titlecase(Locale.UK) }
            }
        }
    }
}