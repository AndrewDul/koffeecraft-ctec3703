package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
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

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = items[position].orderId

    fun submitState(
        items: List<AdminOrderRow>,
        expandedOrderId: Long?,
        loadingOrderId: Long?,
        detailByOrderId: Map<Long, AdminOrderDetailsUi>
    ) {
        val oldItems = this.items
        val oldExpanded = this.expandedOrderId
        val oldLoading = this.loadingOrderId
        val oldDetails = this.detailByOrderId

        val diffResult = DiffUtil.calculateDiff(
            OrdersDiffCallback(
                oldItems = oldItems,
                newItems = items,
                oldExpandedOrderId = oldExpanded,
                newExpandedOrderId = expandedOrderId,
                oldLoadingOrderId = oldLoading,
                newLoadingOrderId = loadingOrderId,
                oldDetailByOrderId = oldDetails,
                newDetailByOrderId = detailByOrderId
            )
        )

        this.items = items
        this.expandedOrderId = expandedOrderId
        this.loadingOrderId = loadingOrderId
        this.detailByOrderId = detailByOrderId

        diffResult.dispatchUpdatesTo(this)
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
            if (row.craftedLineCount > 0) summaryParts += "${row.craftedLineCount} crafted"
            if (row.rewardLineCount > 0) summaryParts += "${row.rewardLineCount} reward"
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
                    setTextColor(color(R.color.kc_text_muted))
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
                if (item.isReward) optionParts += "Reward item"
                if (item.isCrafted) optionParts += "Crafted"
                if (!item.selectedOptionLabel.isNullOrBlank()) optionParts += item.selectedOptionLabel
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
                    tvDetailCalories.text = "${item.estimatedCalories} kcal"
                } else {
                    tvDetailCalories.visibility = View.GONE
                }

                layoutItemsContainer.addView(detailView)
            }
        }

        private fun bindTimeline(status: String) {
            bindStep(tvStepPlaced, status in listOf("PLACED", "PREPARING", "READY", "COLLECTED"))
            bindStep(tvStepPreparing, status in listOf("PREPARING", "READY", "COLLECTED"))
            bindStep(tvStepReady, status in listOf("READY", "COLLECTED"))
            bindStep(tvStepCollected, status == "COLLECTED")
        }

        private fun bindStep(view: TextView, reached: Boolean) {
            view.alpha = if (reached) 1f else 0.45f
        }

        private fun bindActions(row: AdminOrderRow, status: String) {
            when (status) {
                "PLACED" -> {
                    enableAction(tvActionPreparing, "Move to Preparing") { onStatusAction(row, "PREPARING") }
                    disableAction(tvActionReady, "Mark Ready", true)
                    disableAction(tvActionCollected, "Mark Collected", true)
                }

                "PREPARING" -> {
                    disableAction(tvActionPreparing, "Preparing", true)
                    enableAction(tvActionReady, "Mark Ready") { onStatusAction(row, "READY") }
                    disableAction(tvActionCollected, "Mark Collected", true)
                }

                "READY" -> {
                    disableAction(tvActionPreparing, "Preparing", true)
                    disableAction(tvActionReady, "Ready", true)
                    enableAction(tvActionCollected, "Mark Collected") { onStatusAction(row, "COLLECTED") }
                }

                "COLLECTED" -> {
                    disableAction(tvActionPreparing, "Preparing", true)
                    disableAction(tvActionReady, "Ready", true)
                    disableAction(tvActionCollected, "Collected", true)
                }

                else -> {
                    disableAction(tvActionPreparing, "Move to Preparing", false)
                    disableAction(tvActionReady, "Mark Ready", false)
                    disableAction(tvActionCollected, "Mark Collected", false)
                }
            }
        }

        private fun enableAction(view: TextView, label: String, onClick: () -> Unit) {
            view.text = label
            view.alpha = 1f
            view.isEnabled = true
            view.setOnClickListener { onClick() }
        }

        private fun disableAction(view: TextView, label: String, dimmed: Boolean) {
            view.text = label
            view.alpha = if (dimmed) 0.5f else 0.75f
            view.isEnabled = false
            view.setOnClickListener(null)
        }

        private fun bindStatusChip(view: TextView, status: String) {
            val (fillColorRes, textColorRes) = when (status.uppercase(Locale.UK)) {
                "PLACED" -> R.color.kc_status_crafted_bg to R.color.kc_warning_text
                "PREPARING" -> R.color.kc_surface_info to R.color.kc_info_text
                "READY" -> R.color.kc_validation_valid_bg to R.color.kc_success_text
                "COLLECTED" -> R.color.kc_status_neutral_bg to R.color.kc_neutral_text
                else -> R.color.kc_surface_secondary to R.color.kc_text_secondary
            }

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(color(fillColorRes))
            }

            view.background = shape
            view.text = status.replaceFirstChar { it.uppercase() }
            view.setTextColor(color(textColorRes))
        }

        private fun bindNoteChip(view: TextView, visible: Boolean) {
            view.visibility = if (visible) View.VISIBLE else View.GONE
        }

        private fun formatPaymentType(type: String): String {
            return type
                .replace('_', ' ')
                .lowercase(Locale.UK)
                .replaceFirstChar { it.uppercase(Locale.UK) }
        }

        private fun formatCurrency(amount: Double): String {
            return "£%.2f".format(Locale.UK, amount)
        }

        private fun color(colorResId: Int): Int {
            return ContextCompat.getColor(itemView.context, colorResId)
        }
    }
}

private class OrdersDiffCallback(
    private val oldItems: List<AdminOrderRow>,
    private val newItems: List<AdminOrderRow>,
    private val oldExpandedOrderId: Long?,
    private val newExpandedOrderId: Long?,
    private val oldLoadingOrderId: Long?,
    private val newLoadingOrderId: Long?,
    private val oldDetailByOrderId: Map<Long, AdminOrderDetailsUi>,
    private val newDetailByOrderId: Map<Long, AdminOrderDetailsUi>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].orderId == newItems[newItemPosition].orderId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldRow = oldItems[oldItemPosition]
        val newRow = newItems[newItemPosition]

        if (oldRow != newRow) return false

        val orderId = oldRow.orderId
        val oldExpanded = oldExpandedOrderId == orderId
        val newExpanded = newExpandedOrderId == orderId
        if (oldExpanded != newExpanded) return false

        val oldLoading = oldLoadingOrderId == orderId
        val newLoading = newLoadingOrderId == orderId
        if (oldLoading != newLoading) return false

        val oldDetails = oldDetailByOrderId[orderId]
        val newDetails = newDetailByOrderId[orderId]
        if (oldDetails != newDetails) return false

        return true
    }
}