package uk.ac.dmu.koffeecraft.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private var items: List<Order>,
    private val onOpen: (Order) -> Unit,
    private val onOrderAgain: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK)

    fun submitList(newItems: List<Order>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return VH(view, onOpen, onOrderAgain, formatter)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(
        itemView: View,
        private val onOpen: (Order) -> Unit,
        private val onOrderAgain: (Order) -> Unit,
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnOrderAgain: Button = itemView.findViewById(R.id.btnOrderAgain)

        fun bind(order: Order) {
            tvOrderId.text = "Order #${order.orderId}"
            tvStatus.text = "Status: ${order.status}"
            tvTotal.text = "£%.2f".format(order.totalAmount)
            tvDate.text = formatter.format(Date(order.createdAt))

            itemView.setOnClickListener { onOpen(order) }
            btnOrderAgain.setOnClickListener { onOrderAgain(order) }
        }
    }
}