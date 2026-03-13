package uk.ac.dmu.koffeecraft.ui.admin.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

class AdminNotificationsAdapter(
    private var items: List<AppNotification>,
    private val onDelete: (AppNotification) -> Unit,
    private val onNext: (AppNotification) -> Unit
) : RecyclerView.Adapter<AdminNotificationsAdapter.AdminNotificationViewHolder>() {

    fun submitList(newItems: List<AppNotification>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): AppNotification = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminNotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_notification, parent, false)
        return AdminNotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminNotificationViewHolder, position: Int) {
        holder.bind(items[position], onDelete, onNext)
    }

    override fun getItemCount(): Int = items.size

    class AdminNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnNext: Button = itemView.findViewById(R.id.btnNext)

        fun bind(
            item: AppNotification,
            onDelete: (AppNotification) -> Unit,
            onNext: (AppNotification) -> Unit
        ) {
            tvTitle.text = item.title
            tvMessage.text = item.message
            tvStatus.text = "Current status: ${item.orderStatus ?: "UNKNOWN"}"

            val showNext = item.notificationType == "ADMIN_ORDER_ACTION" &&
                    (item.orderStatus == "PLACED" || item.orderStatus == "PREPARING" || item.orderStatus == "READY")

            val canDelete = item.orderStatus == "COLLECTED"

            btnNext.visibility = if (showNext) View.VISIBLE else View.GONE
            btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

            btnDelete.setOnClickListener { onDelete(item) }
            btnNext.setOnClickListener { onNext(item) }
        }
    }
}