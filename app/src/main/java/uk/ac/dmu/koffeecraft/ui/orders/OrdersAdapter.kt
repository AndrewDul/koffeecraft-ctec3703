package uk.ac.dmu.koffeecraft.ui.orders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.entities.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private var items: List<Order>,
    private var detailsByOrderId: Map<Long, List<OrderDisplayItem>>,
    private val onOrderAgain: (Order) -> Unit,
    private val onRemoveOrder: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK)
    private val expandedIds = mutableSetOf<Long>()

    fun submitData(
        newItems: List<Order>,
        newDetailsByOrderId: Map<Long, List<OrderDisplayItem>>
    ) {
        items = newItems
        detailsByOrderId = newDetailsByOrderId
        expandedIds.retainAll(newItems.map { it.orderId }.toSet())
        notifyDataSetChanged()
    }

    fun getOrderAt(position: Int): Order? = items.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return VH(view, onOrderAgain, onRemoveOrder, formatter)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]
        val details = detailsByOrderId[order.orderId].orEmpty()
        val expanded = expandedIds.contains(order.orderId)

        holder.bind(
            order = order,
            details = details,
            expanded = expanded,
            onToggle = {
                val id = order.orderId
                if (!expandedIds.add(id)) {
                    expandedIds.remove(id)
                }

                val index = items.indexOfFirst { it.orderId == id }
                if (index != -1) notifyItemChanged(index)
            }
        )
    }

    class VH(
        itemView: View,
        private val onOrderAgain: (Order) -> Unit,
        private val onRemoveOrder: (Order) -> Unit,
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvCraftedBadge: TextView = itemView.findViewById(R.id.tvCraftedBadge)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvRemove: TextView = itemView.findViewById(R.id.tvRemove)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpandedContent: LinearLayout = itemView.findViewById(R.id.layoutExpandedContent)
        private val layoutOrderDetails: LinearLayout = itemView.findViewById(R.id.layoutOrderDetails)
        private val tvSummaryStatusValue: TextView = itemView.findViewById(R.id.tvSummaryStatusValue)
        private val tvSummaryDateValue: TextView = itemView.findViewById(R.id.tvSummaryDateValue)
        private val tvSummaryTotalValue: TextView = itemView.findViewById(R.id.tvSummaryTotalValue)
        private val tvBuyAgain: TextView = itemView.findViewById(R.id.tvBuyAgain)

        fun bind(
            order: Order,
            details: List<OrderDisplayItem>,
            expanded: Boolean,
            onToggle: () -> Unit
        ) {
            val formattedDate = formatter.format(Date(order.createdAt))
            val isCrafted = details.any { it.isCrafted }

            tvOrderId.text = "Order #${order.orderId}"
            tvDate.text = formattedDate
            tvItemCount.text = buildItemCountText(details)
            tvTotal.text = formatMoney(order.totalAmount)

            tvCraftedBadge.visibility = if (isCrafted) View.VISIBLE else View.GONE

            bindStatusChip(order.status)
            tvSummaryStatusValue.text = formatStatus(order.status)
            tvSummaryDateValue.text = formattedDate
            tvSummaryTotalValue.text = formatMoney(order.totalAmount)

            bindDetails(details)

            layoutExpandedContent.visibility = if (expanded) View.VISIBLE else View.GONE
            dividerExpanded.visibility = if (expanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onToggle() }
            tvBuyAgain.setOnClickListener { onOrderAgain(order) }
            tvRemove.setOnClickListener { onRemoveOrder(order) }
        }

        private fun bindDetails(details: List<OrderDisplayItem>) {
            layoutOrderDetails.removeAllViews()

            if (details.isEmpty()) {
                val emptyView = TextView(itemView.context).apply {
                    text = "No item details available."
                    textSize = 13f
                    setTextColor(Color.parseColor("#7A6558"))
                }
                layoutOrderDetails.addView(emptyView)
                return
            }

            details.forEach { detail ->
                val detailView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_order_detail, layoutOrderDetails, false)

                val tvDetailName = detailView.findViewById<TextView>(R.id.tvDetailName)
                val tvDetailLineTotal = detailView.findViewById<TextView>(R.id.tvDetailLineTotal)
                val tvDetailQuantityPrice = detailView.findViewById<TextView>(R.id.tvDetailQuantityPrice)
                val tvDetailCustomOption = detailView.findViewById<TextView>(R.id.tvDetailCustomOption)
                val tvDetailAddOns = detailView.findViewById<TextView>(R.id.tvDetailAddOns)
                val tvDetailCalories = detailView.findViewById<TextView>(R.id.tvDetailCalories)

                val lineTotal = detail.quantity * detail.unitPrice

                tvDetailName.text = detail.productName
                tvDetailLineTotal.text = formatMoney(lineTotal)
                tvDetailQuantityPrice.text = "Qty ${detail.quantity} • ${formatMoney(detail.unitPrice)} each"

                val optionText = if (detail.isCrafted) buildOptionText(detail) else null
                if (optionText.isNullOrBlank()) {
                    tvDetailCustomOption.visibility = View.GONE
                } else {
                    tvDetailCustomOption.visibility = View.VISIBLE
                    tvDetailCustomOption.text = "Crafted • $optionText"
                }

                if (detail.selectedAddOnsSummary.isNullOrBlank()) {
                    tvDetailAddOns.visibility = View.GONE
                } else {
                    tvDetailAddOns.visibility = View.VISIBLE
                    tvDetailAddOns.text = "Add-ons • ${detail.selectedAddOnsSummary}"
                }

                if (detail.estimatedCalories == null) {
                    tvDetailCalories.visibility = View.GONE
                } else {
                    tvDetailCalories.visibility = View.VISIBLE
                    tvDetailCalories.text = if (detail.isCrafted) {
                        "Approx. ${detail.estimatedCalories} kcal"
                    } else {
                        "${detail.estimatedCalories} kcal"
                    }
                }

                layoutOrderDetails.addView(detailView)
            }
        }

        private fun buildOptionText(detail: OrderDisplayItem): String? {
            val parts = mutableListOf<String>()

            detail.selectedOptionLabel
                ?.takeIf { it.isNotBlank() }
                ?.let { parts.add(it) }

            if (detail.selectedOptionSizeValue != null && !detail.selectedOptionSizeUnit.isNullOrBlank()) {
                parts.add("${detail.selectedOptionSizeValue}${detail.selectedOptionSizeUnit}")
            }

            return if (parts.isEmpty()) null else parts.joinToString(" • ")
        }

        private fun bindStatusChip(status: String) {
            tvStatus.text = formatStatus(status)

            val background = tvStatus.background.mutate() as GradientDrawable

            when (status.uppercase(Locale.UK)) {
                "READY" -> {
                    background.setColor(Color.parseColor("#DCE9DA"))
                    tvStatus.setTextColor(Color.parseColor("#36533E"))
                }

                "PREPARING" -> {
                    background.setColor(Color.parseColor("#F2E4D3"))
                    tvStatus.setTextColor(Color.parseColor("#7A5634"))
                }

                "PLACED" -> {
                    background.setColor(Color.parseColor("#E8DDD4"))
                    tvStatus.setTextColor(Color.parseColor("#6A4D3A"))
                }

                "COMPLETED" -> {
                    background.setColor(Color.parseColor("#DFE7D8"))
                    tvStatus.setTextColor(Color.parseColor("#3D5640"))
                }

                "CANCELLED" -> {
                    background.setColor(Color.parseColor("#F0DCD8"))
                    tvStatus.setTextColor(Color.parseColor("#7B4A42"))
                }

                else -> {
                    background.setColor(Color.parseColor("#E9DFD6"))
                    tvStatus.setTextColor(Color.parseColor("#5C473A"))
                }
            }
        }

        private fun buildItemCountText(details: List<OrderDisplayItem>): String {
            val itemCount = details.sumOf { it.quantity }
            return if (itemCount == 1) "1 item" else "$itemCount items"
        }

        private fun formatStatus(status: String): String {
            return status
                .lowercase(Locale.UK)
                .split("_")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { firstChar ->
                        firstChar.titlecase(Locale.UK)
                    }
                }
        }

        private fun formatMoney(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }
    }
}