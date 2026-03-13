package uk.ac.dmu.koffeecraft.ui.inbox

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

class CustomerInboxAdapter(
    private var items: List<InboxMessage>,
    private val onDelete: (InboxMessage) -> Unit
) : RecyclerView.Adapter<CustomerInboxAdapter.CustomerInboxViewHolder>() {

    private val expandedIds = mutableSetOf<Long>()

    fun submitList(newItems: List<InboxMessage>) {
        items = newItems
        expandedIds.retainAll(newItems.map { it.inboxMessageId }.toSet())
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): InboxMessage = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerInboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_inbox, parent, false)
        return CustomerInboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerInboxViewHolder, position: Int) {
        val item = items[position]
        val expanded = expandedIds.contains(item.inboxMessageId)

        holder.bind(
            item = item,
            expanded = expanded,
            onDelete = onDelete,
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

    class CustomerInboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvToggle: TextView = itemView.findViewById(R.id.tvToggle)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            item: InboxMessage,
            expanded: Boolean,
            onDelete: (InboxMessage) -> Unit,
            onToggle: () -> Unit
        ) {
            tvTitle.text = item.title
            tvBody.text = item.body

            val badge = mapBadge(item.deliveryType)
            tvBadge.text = badge.label
            tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(badge.backgroundColor))
            tvBadge.setTextColor(Color.parseColor(badge.textColor))

            cardRoot.setCardBackgroundColor(Color.parseColor("#E7D8C7"))

            val longMessage = item.body.length > 140

            if (expanded || !longMessage) {
                tvBody.maxLines = Int.MAX_VALUE
                tvToggle.isVisible = longMessage
                tvToggle.text = "Read less"
            } else {
                tvBody.maxLines = 3
                tvToggle.isVisible = true
                tvToggle.text = "Read more"
            }

            tvToggle.setOnClickListener { onToggle() }
            btnDelete.setOnClickListener { onDelete(item) }
        }

        private fun mapBadge(deliveryType: String): BadgeStyle {
            return when {
                deliveryType.startsWith("PROMO") -> BadgeStyle(
                    label = "Promo",
                    backgroundColor = "#8B6B4A",
                    textColor = "#FFF8F0"
                )

                deliveryType.startsWith("IMPORTANT") -> BadgeStyle(
                    label = "Important",
                    backgroundColor = "#7A4C2A",
                    textColor = "#FFF8F0"
                )

                deliveryType.startsWith("SERVICE") -> BadgeStyle(
                    label = "Service",
                    backgroundColor = "#5F7F5B",
                    textColor = "#FFF8F0"
                )

                else -> BadgeStyle(
                    label = "Custom",
                    backgroundColor = "#B08968",
                    textColor = "#FFF8F0"
                )
            }
        }

        data class BadgeStyle(
            val label: String,
            val backgroundColor: String,
            val textColor: String
        )
    }
}