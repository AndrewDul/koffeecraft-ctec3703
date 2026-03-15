package uk.ac.dmu.koffeecraft.ui.inbox

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerInboxAdapter(
    private var items: List<InboxMessage>,
    private val onDelete: (InboxMessage) -> Unit,
    private val onOpen: (InboxMessage) -> Unit
) : RecyclerView.Adapter<CustomerInboxAdapter.CustomerInboxViewHolder>() {

    private val expandedIds = mutableSetOf<Long>()
    private val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK)

    fun submitList(newItems: List<InboxMessage>) {
        items = newItems
        expandedIds.retainAll(newItems.map { it.inboxMessageId }.toSet())
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): InboxMessage = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerInboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_inbox, parent, false)
        return CustomerInboxViewHolder(view, formatter)
    }

    override fun onBindViewHolder(holder: CustomerInboxViewHolder, position: Int) {
        val item = items[position]
        val expanded = expandedIds.contains(item.inboxMessageId)

        holder.bind(
            item = item,
            expanded = expanded,
            onDelete = onDelete,
            onOpen = onOpen,
            onToggle = {
                val id = item.inboxMessageId
                if (!expandedIds.add(id)) {
                    expandedIds.remove(id)
                }

                val index = items.indexOfFirst { it.inboxMessageId == id }
                if (index != -1) notifyItemChanged(index)
            }
        )
    }

    override fun getItemCount(): Int = items.size

    class CustomerInboxViewHolder(
        itemView: View,
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvReadStateChip: TextView = itemView.findViewById(R.id.tvReadStateChip)
        private val tvCategoryChip: TextView = itemView.findViewById(R.id.tvCategoryChip)
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvToggle: TextView = itemView.findViewById(R.id.tvToggle)
        private val tvRemove: TextView = itemView.findViewById(R.id.tvRemove)

        fun bind(
            item: InboxMessage,
            expanded: Boolean,
            onDelete: (InboxMessage) -> Unit,
            onOpen: (InboxMessage) -> Unit,
            onToggle: () -> Unit
        ) {
            tvTitle.text = item.title
            tvTimestamp.text = formatter.format(Date(item.createdAt))
            tvBody.text = item.body

            bindReadChip(item.isRead)
            bindCategoryChip(item.deliveryType)
            bindCardState(item.isRead)

            val longMessage = item.body.length > 140

            if (expanded || !longMessage) {
                tvBody.maxLines = Int.MAX_VALUE
                tvToggle.visibility = if (longMessage) View.VISIBLE else View.GONE
                tvToggle.text = "Read less"
            } else {
                tvBody.maxLines = 3
                tvToggle.visibility = View.VISIBLE
                tvToggle.text = "Read more"
            }

            val openAction = {
                onOpen(item)
                onToggle()
            }

            cardRoot.setOnClickListener { openAction() }
            tvToggle.setOnClickListener { openAction() }
            tvRemove.setOnClickListener { onDelete(item) }
        }

        private fun bindCardState(isRead: Boolean) {
            if (isRead) {
                cardRoot.setCardBackgroundColor(Color.parseColor("#FFF8F2"))
                cardRoot.strokeColor = Color.parseColor("#E3D4C6")
            } else {
                cardRoot.setCardBackgroundColor(Color.parseColor("#FFF6EE"))
                cardRoot.strokeColor = Color.parseColor("#D8BBA4")
            }
        }

        private fun bindReadChip(isRead: Boolean) {
            val background = tvReadStateChip.background.mutate() as GradientDrawable

            if (isRead) {
                tvReadStateChip.text = "Read"
                background.setColor(Color.parseColor("#EEE4DA"))
                tvReadStateChip.setTextColor(Color.parseColor("#7A6558"))
            } else {
                tvReadStateChip.text = "Unread"
                background.setColor(Color.parseColor("#E8DDD4"))
                tvReadStateChip.setTextColor(Color.parseColor("#6A4D3A"))
            }
        }

        private fun bindCategoryChip(deliveryType: String) {
            val label = when {
                deliveryType.startsWith("PROMO") -> "Promo"
                deliveryType.startsWith("IMPORTANT") -> "Important"
                deliveryType.startsWith("SERVICE") -> "Service"
                else -> null
            }

            if (label == null) {
                tvCategoryChip.visibility = View.GONE
                return
            }

            tvCategoryChip.visibility = View.VISIBLE
            tvCategoryChip.text = label

            val background = tvCategoryChip.background.mutate() as GradientDrawable

            when (label) {
                "Promo" -> {
                    background.setColor(Color.parseColor("#F2E4D3"))
                    tvCategoryChip.setTextColor(Color.parseColor("#7A5634"))
                }

                "Important" -> {
                    background.setColor(Color.parseColor("#F0DCD8"))
                    tvCategoryChip.setTextColor(Color.parseColor("#7B4A42"))
                }

                "Service" -> {
                    background.setColor(Color.parseColor("#DFE7D8"))
                    tvCategoryChip.setTextColor(Color.parseColor("#3D5640"))
                }
            }
        }
    }
}