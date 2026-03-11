package uk.ac.dmu.koffeecraft.ui.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderFeedbackItem

class FeedbackProductsAdapter(
    private var items: List<OrderFeedbackItem>,
    private val onOpen: (OrderFeedbackItem) -> Unit
) : RecyclerView.Adapter<FeedbackProductsAdapter.FeedbackProductViewHolder>() {

    fun submitList(newItems: List<OrderFeedbackItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback_product, parent, false)
        return FeedbackProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackProductViewHolder, position: Int) {
        holder.bind(items[position], onOpen)
    }

    override fun getItemCount(): Int = items.size

    class FeedbackProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnOpenFeedback: Button = itemView.findViewById(R.id.btnOpenFeedback)

        fun bind(item: OrderFeedbackItem, onOpen: (OrderFeedbackItem) -> Unit) {
            tvProductName.text = item.productName
            tvMeta.text = "Qty: ${item.quantity} • ${String.format(Locale.UK, "£%.2f", item.unitPrice)}"

            val isRated = item.feedbackId != null
            tvStatus.text = if (isRated) {
                val ratingText = item.rating?.let { "$it/5 stars" } ?: "Rated"
                "Rated • $ratingText"
            } else {
                "Not rated yet"
            }

            btnOpenFeedback.text = if (isRated) "Edit feedback" else "Leave feedback"

            itemView.setOnClickListener { onOpen(item) }
            btnOpenFeedback.setOnClickListener { onOpen(item) }
        }
    }
}
