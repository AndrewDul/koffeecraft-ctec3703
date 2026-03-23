package uk.ac.dmu.koffeecraft.ui.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.querymodel.OrderFeedbackItem
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
        private val tvCraftedBadge: TextView = itemView.findViewById(R.id.tvCraftedBadge)
        private val tvQuantityValue: TextView = itemView.findViewById(R.id.tvQuantityValue)
        private val tvPriceValue: TextView = itemView.findViewById(R.id.tvPriceValue)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvOpenFeedbackAction: TextView = itemView.findViewById(R.id.tvOpenFeedbackAction)

        fun bind(item: OrderFeedbackItem, onOpen: (OrderFeedbackItem) -> Unit) {
            val lineTotal = item.quantity * item.unitPrice

            tvProductName.text = item.productName
            tvCraftedBadge.visibility = if (item.isCrafted) View.VISIBLE else View.GONE
            tvQuantityValue.text = item.quantity.toString()
            tvPriceValue.text = String.format(Locale.UK, "£%.2f", lineTotal)

            val isRated = item.feedbackId != null
            tvStatus.text = if (isRated) {
                val ratingText = item.rating?.let { "$it/5 stars" } ?: "Rated"
                "Rated • $ratingText"
            } else {
                "Not rated yet"
            }

            tvOpenFeedbackAction.text = if (isRated) "Edit feedback" else "Leave feedback"

            itemView.setOnClickListener { onOpen(item) }
            tvOpenFeedbackAction.setOnClickListener { onOpen(item) }
        }
    }
}