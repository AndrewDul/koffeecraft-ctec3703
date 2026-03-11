package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminOrdersAdapter(
    private var items: List<AdminOrderRow>,
    private val onNextStatus: (AdminOrderRow) -> Unit
) : RecyclerView.Adapter<AdminOrdersAdapter.VH>() {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)

    fun submitList(newItems: List<AdminOrderRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false)
        return VH(view, onNextStatus, formatter)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(
        itemView: View,
        private val onNextStatus: (AdminOrderRow) -> Unit,
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val btnNext: Button = itemView.findViewById(R.id.btnNext)

        fun bind(row: AdminOrderRow) {
            tvOrderId.text = "Order #${row.orderId}"
            tvEmail.text = row.customerEmail
            tvMeta.text = "${row.status} • £%.2f • %s".format(
                row.totalAmount,
                formatter.format(Date(row.createdAt))
            )

            // I hide the action button when the order is already completed.
            if (row.status == "COLLECTED") {
                btnNext.visibility = View.GONE
                btnNext.setOnClickListener(null)
            } else {
                btnNext.visibility = View.VISIBLE
                btnNext.setOnClickListener { onNextStatus(row) }
            }
        }
    }
}