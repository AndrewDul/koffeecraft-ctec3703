package uk.ac.dmu.koffeecraft.ui.admin.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackItem

class AdminFeedbackAdapter(
    private var items: List<AdminFeedbackItem>,
    private val onDelete: (AdminFeedbackItem) -> Unit,
    private val onHideToggle: (AdminFeedbackItem) -> Unit
) : RecyclerView.Adapter<AdminFeedbackAdapter.AdminFeedbackViewHolder>() {

    fun submitList(newItems: List<AdminFeedbackItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminFeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_feedback, parent, false)
        return AdminFeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminFeedbackViewHolder, position: Int) {
        holder.bind(items[position], onDelete, onHideToggle)
    }

    override fun getItemCount(): Int = items.size

    class AdminFeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        private val tvModeration: TextView = itemView.findViewById(R.id.tvModeration)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        private val btnHideToggle: Button = itemView.findViewById(R.id.btnHideToggle)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        private val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK)

        fun bind(
            item: AdminFeedbackItem,
            onDelete: (AdminFeedbackItem) -> Unit,
            onHideToggle: (AdminFeedbackItem) -> Unit
        ) {
            tvProductName.text = item.productName
            tvMeta.text = "Order #${item.orderId} • Customer #${item.customerId} • ${item.productCategory}"
            tvRating.text = "Rating: ${item.rating}/5"

            if (item.isHidden) {
                tvComment.text = "Hidden comment"
                tvModeration.visibility = View.VISIBLE
                tvModeration.text = "Comment hidden by admin"
                btnHideToggle.text = "Unhide comment"
            } else {
                tvComment.text = if (item.comment.isBlank()) "No comment" else item.comment
                tvModeration.visibility = if (item.isModerated) View.VISIBLE else View.GONE
                tvModeration.text = "Moderated by admin"
                btnHideToggle.text = "Hide comment"
            }

            tvUpdatedAt.text = "Updated: ${formatter.format(Date(item.updatedAt))}"

            btnHideToggle.isEnabled = item.comment.isNotBlank()
            btnHideToggle.setOnClickListener { onHideToggle(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
