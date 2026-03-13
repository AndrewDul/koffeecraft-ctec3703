package uk.ac.dmu.koffeecraft.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

class CustomerNotificationsAdapter(
    private var items: List<AppNotification>,
    private var detailsByOrderId: Map<Long, List<OrderDisplayItem>>,
    private val onDelete: (AppNotification) -> Unit
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
            onToggle = {
                if (item.orderId == null) return@bind

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
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            item: AppNotification,
            details: List<OrderDisplayItem>,
            expanded: Boolean,
            onDelete: (AppNotification) -> Unit,
            onToggle: () -> Unit
        ) {
            tvTitle.text = item.title
            tvMessage.text = item.message

            if (expanded && item.orderId != null) {
                tvDetails.visibility = View.VISIBLE
                tvDetails.text = buildDetailsText(details)
            } else {
                tvDetails.visibility = View.GONE
                tvDetails.text = ""
            }

            itemView.setOnClickListener {
                if (item.orderId != null) onToggle()
            }

            btnDelete.setOnClickListener { onDelete(item) }
        }

        private fun buildDetailsText(details: List<OrderDisplayItem>): String {
            if (details.isEmpty()) return "No item details available."

            return buildString {
                details.forEachIndexed { index, detail ->
                    val lineTotal = detail.quantity * detail.unitPrice
                    append("${detail.productName} x${detail.quantity} • £${"%.2f".format(detail.unitPrice)} each • £${"%.2f".format(lineTotal)}")
                    if (index != details.lastIndex) append("\n")
                }
            }
        }
    }
}