package uk.ac.dmu.koffeecraft.ui.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
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

            val longMessage = item.body.length > 140
            tvBody.text = item.body

            if (expanded || !longMessage) {
                tvBody.maxLines = Int.MAX_VALUE
                tvToggle.visibility = if (longMessage) View.VISIBLE else View.GONE
                tvToggle.text = "Read less"
            } else {
                tvBody.maxLines = 3
                tvToggle.visibility = View.VISIBLE
                tvToggle.text = "Read more"
            }

            tvToggle.setOnClickListener { onToggle() }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}