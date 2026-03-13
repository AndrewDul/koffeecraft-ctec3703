package uk.ac.dmu.koffeecraft.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private val onOrderAgain: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return VH(view, onOrderAgain, formatter)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]
        val expanded = expandedIds.contains(order.orderId)
        val details = detailsByOrderId[order.orderId].orEmpty()

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
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
        private val btnOrderAgain: Button = itemView.findViewById(R.id.btnOrderAgain)

        fun bind(
            order: Order,
            details: List<OrderDisplayItem>,
            expanded: Boolean,
            onToggle: () -> Unit
        ) {
            tvOrderId.text = "Order #${order.orderId}"
            tvStatus.text = "Status: ${order.status}"
            tvTotal.text = "£%.2f".format(order.totalAmount)
            tvDate.text = formatter.format(Date(order.createdAt))

            if (expanded) {
                tvDetails.visibility = View.VISIBLE
                tvDetails.text = buildDetailsText(details)
            } else {
                tvDetails.visibility = View.GONE
                tvDetails.text = ""
            }

            itemView.setOnClickListener { onToggle() }
            btnOrderAgain.setOnClickListener { onOrderAgain(order) }
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