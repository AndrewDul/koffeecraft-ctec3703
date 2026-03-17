package uk.ac.dmu.koffeecraft.ui.admin.notifications

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        private val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val dividerActions: View = itemView.findViewById(R.id.dividerActions)
        private val layoutActionRow: LinearLayout = itemView.findViewById(R.id.layoutActionRow)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val btnNext: MaterialButton = itemView.findViewById(R.id.btnNext)

        fun bind(
            item: AppNotification,
            onDelete: (AppNotification) -> Unit,
            onNext: (AppNotification) -> Unit
        ) {
            val orderIdText = item.orderId?.let { "#$it" } ?: "notification"
            val statusLabel = formatStatus(item.orderStatus)

            tvStatusChip.text = statusLabel
            bindStatusChip(item.orderStatus)

            tvTitle.text = when {
                item.orderId != null -> "Order $orderIdText needs attention"
                else -> item.title
            }

            tvMessage.text = item.message
            tvMeta.text = buildMeta(item)

            val nextLabel = nextActionLabel(item.orderStatus)
            val showNext = nextLabel != null
            val canDelete = item.orderStatus == "COLLECTED"

            btnNext.visibility = if (showNext) View.VISIBLE else View.GONE
            btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

            btnNext.text = nextLabel ?: ""
            btnDelete.text = "Remove notification"

            val showActionArea = showNext || canDelete
            dividerActions.visibility = if (showActionArea) View.VISIBLE else View.GONE
            layoutActionRow.visibility = if (showActionArea) View.VISIBLE else View.GONE

            btnDelete.setOnClickListener { onDelete(item) }
            btnNext.setOnClickListener { onNext(item) }
        }

        private fun buildMeta(item: AppNotification): String {
            val parts = mutableListOf<String>()

            if (item.createdAt > 0L) {
                val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK)
                parts += "Created ${formatter.format(Date(item.createdAt))}"
            }

            if (item.orderId != null) {
                parts += "Order #${item.orderId}"
            }

            parts += if (item.notificationType == "ADMIN_ORDER_ACTION") {
                "Admin action queue"
            } else {
                "Admin notification"
            }

            return parts.joinToString(" • ")
        }

        private fun nextActionLabel(status: String?): String? {
            return when (status?.uppercase(Locale.UK)) {
                "PLACED" -> "Move to Preparing"
                "PREPARING" -> "Move to Ready"
                "READY" -> "Mark as Collected"
                else -> null
            }
        }

        private fun bindStatusChip(status: String?) {
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
    }
}