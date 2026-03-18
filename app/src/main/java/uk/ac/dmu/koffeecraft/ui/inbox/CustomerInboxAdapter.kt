package uk.ac.dmu.koffeecraft.ui.inbox

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
                cardRoot.setCardBackgroundColor(color(R.color.kc_surface_primary))
                cardRoot.strokeColor = color(R.color.kc_border_soft)
            } else {
                cardRoot.setCardBackgroundColor(color(R.color.kc_surface_secondary))
                cardRoot.strokeColor = color(R.color.kc_border_warm)
            }
        }

        private fun bindReadChip(isRead: Boolean) {
            val background = tvReadStateChip.background.mutate() as GradientDrawable

            if (isRead) {
                tvReadStateChip.text = "Read"
                background.setColor(color(R.color.kc_surface_chip))
                tvReadStateChip.setTextColor(color(R.color.kc_text_muted))
            } else {
                tvReadStateChip.text = "Unread"
                background.setColor(color(R.color.kc_surface_info))
                tvReadStateChip.setTextColor(color(R.color.kc_info_text))
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
                    background.setColor(color(R.color.kc_surface_warning))
                    tvCategoryChip.setTextColor(color(R.color.kc_warning_text))
                }

                "Important" -> {
                    background.setColor(color(R.color.kc_surface_error))
                    tvCategoryChip.setTextColor(color(R.color.kc_danger_text))
                }

                "Service" -> {
                    background.setColor(color(R.color.kc_surface_success))
                    tvCategoryChip.setTextColor(color(R.color.kc_success_text))
                }
            }
        }

        private fun color(colorResId: Int): Int {
            return ContextCompat.getColor(itemView.context, colorResId)
        }
    }
}