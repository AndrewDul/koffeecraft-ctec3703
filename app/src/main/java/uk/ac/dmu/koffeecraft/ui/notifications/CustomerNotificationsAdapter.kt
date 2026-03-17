package uk.ac.dmu.koffeecraft.ui.notifications

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
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import java.util.Locale

class CustomerNotificationsAdapter(
    private var items: List<AppNotification>,
    private var detailsByOrderId: Map<Long, List<OrderDisplayItem>>,
    private val onDelete: (AppNotification) -> Unit,
    private val onOpen: (AppNotification) -> Unit
) : RecyclerView.Adapter<CustomerNotificationsAdapter.CustomerNotificationViewHolder>() {

    private val expandedIds = mutableSetOf<Long>()

    fun submitData(
        newItems: List<AppNotification>,
        newDetailsByOrderId: Map<Long, List<OrderDisplayItem>>
    ) {
        items = newItems
        detailsByOrderId = newDetailsByOrderId
        expandedIds.retainAll(newItems.map { it.notificationId }.toSet())
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): AppNotification = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerNotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_notification, parent, false)
        return CustomerNotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerNotificationViewHolder, position: Int) {
        val item = items[position]
        val expanded = expandedIds.contains(item.notificationId)
        val details = item.orderId?.let { detailsByOrderId[it] }.orEmpty()

        holder.bind(
            item = item,
            details = details,
            expanded = expanded,
            onDelete = onDelete,
            onOpen = onOpen,
            onToggle = {
                val id = item.notificationId
                if (!expandedIds.add(id)) {
                    expandedIds.remove(id)
                }

                val index = items.indexOfFirst { it.notificationId == id }
                if (index != -1) notifyItemChanged(index)
            }
        )
    }

    override fun getItemCount(): Int = items.size

    class CustomerNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvStatusSentence: TextView = itemView.findViewById(R.id.tvStatusSentence)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val tvRemove: TextView = itemView.findViewById(R.id.tvRemove)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpandedContent: LinearLayout = itemView.findViewById(R.id.layoutExpandedContent)
        private val layoutNotificationDetails: LinearLayout = itemView.findViewById(R.id.layoutNotificationDetails)

        fun bind(
            item: AppNotification,
            details: List<OrderDisplayItem>,
            expanded: Boolean,
            onDelete: (AppNotification) -> Unit,
            onOpen: (AppNotification) -> Unit,
            onToggle: () -> Unit
        ) {
            val formattedStatus = formatStatus(item.orderStatus)
            val orderIdText = item.orderId?.let { "Order #$it" } ?: item.title
            val statusSentence = buildStatusSentence(item)
            val orderTotal = details.sumOf { it.quantity * it.unitPrice }

            tvStatusChip.text = formattedStatus
            tvOrderId.text = orderIdText
            tvStatusSentence.text = statusSentence

            if (details.isNotEmpty()) {
                tvOrderTotal.visibility = View.VISIBLE
                tvOrderTotal.text = formatMoney(orderTotal)
            } else {
                tvOrderTotal.visibility = View.GONE
            }

            bindStatusChip(item.orderStatus)
            bindDetails(details)

            dividerExpanded.visibility = if (expanded && item.orderId != null) View.VISIBLE else View.GONE
            layoutExpandedContent.visibility = if (expanded && item.orderId != null) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onOpen(item)

                if (item.orderId != null) {
                    onToggle()
                }
            }

            tvRemove.setOnClickListener { onDelete(item) }
        }

        private fun bindDetails(details: List<OrderDisplayItem>) {
            layoutNotificationDetails.removeAllViews()

            if (details.isEmpty()) {
                val emptyView = TextView(itemView.context).apply {
                    text = "No item details available."
                    textSize = 13f
                    setTextColor(Color.parseColor("#7A6558"))
                }
                layoutNotificationDetails.addView(emptyView)
                return
            }

            details.forEach { detail ->
                val detailView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_customer_notification_detail, layoutNotificationDetails, false)

                val tvDetailName = detailView.findViewById<TextView>(R.id.tvDetailName)
                val tvDetailCraftedBadge = detailView.findViewById<TextView>(R.id.tvDetailCraftedBadge)
                val tvDetailQuantity = detailView.findViewById<TextView>(R.id.tvDetailQuantity)
                val tvDetailPriceEach = detailView.findViewById<TextView>(R.id.tvDetailPriceEach)
                val tvDetailLineTotal = detailView.findViewById<TextView>(R.id.tvDetailLineTotal)

                val lineTotal = detail.quantity * detail.unitPrice

                tvDetailName.text = detail.productName
                tvDetailCraftedBadge.visibility = if (detail.isCrafted) View.VISIBLE else View.GONE
                tvDetailQuantity.text = "Qty ${detail.quantity}"
                tvDetailPriceEach.text = "${formatMoney(detail.unitPrice)} each"
                tvDetailLineTotal.text = formatMoney(lineTotal)

                layoutNotificationDetails.addView(detailView)
            }
        }

        private fun bindStatusChip(status: String?) {
            tvStatusChip.text = formatStatus(status)
            val background = tvStatusChip.background.mutate() as GradientDrawable

            when (status?.uppercase(Locale.UK)) {
                "READY" -> {
                    background.setColor(Color.parseColor("#DCE9DA"))
                    tvStatusChip.setTextColor(Color.parseColor("#36533E"))
                }

                "PREPARING" -> {
                    background.setColor(Color.parseColor("#F2E4D3"))
                    tvStatusChip.setTextColor(Color.parseColor("#7A5634"))
                }

                "PLACED" -> {
                    background.setColor(Color.parseColor("#E8DDD4"))
                    tvStatusChip.setTextColor(Color.parseColor("#6A4D3A"))
                }

                "COLLECTED" -> {
                    background.setColor(Color.parseColor("#DFE7D8"))
                    tvStatusChip.setTextColor(Color.parseColor("#3D5640"))
                }

                "COMPLETED" -> {
                    background.setColor(Color.parseColor("#DFE7D8"))
                    tvStatusChip.setTextColor(Color.parseColor("#3D5640"))
                }

                "CANCELLED" -> {
                    background.setColor(Color.parseColor("#F0DCD8"))
                    tvStatusChip.setTextColor(Color.parseColor("#7B4A42"))
                }

                else -> {
                    background.setColor(Color.parseColor("#E9DFD6"))
                    tvStatusChip.setTextColor(Color.parseColor("#5C473A"))
                }
            }
        }

        private fun buildStatusSentence(item: AppNotification): String {
            val orderIdText = item.orderId?.let { "Order #$it" }

            return when (item.orderStatus?.uppercase(Locale.UK)) {
                "PREPARING" -> "${orderIdText ?: "Your order"} is now being prepared."
                "READY" -> "${orderIdText ?: "Your order"} is ready for collection."
                "COLLECTED" -> "${orderIdText ?: "Your order"} has been collected."
                "COMPLETED" -> "${orderIdText ?: "Your order"} has been completed."
                "PLACED" -> "${orderIdText ?: "Your order"} has been placed successfully."
                "CANCELLED" -> "${orderIdText ?: "Your order"} has been cancelled."
                else -> item.message
            }
        }

        private fun formatStatus(status: String?): String {
            if (status.isNullOrBlank()) return "Update"

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