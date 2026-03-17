package uk.ac.dmu.koffeecraft.ui.admin.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminFeedbackAdapter(
    private var items: List<AdminFeedbackItem>,
    private val onDelete: (AdminFeedbackItem) -> Unit,
    private val onHideToggle: (AdminFeedbackItem) -> Unit
) : RecyclerView.Adapter<AdminFeedbackAdapter.AdminFeedbackViewHolder>() {

    private var expandedFeedbackId: Long? = null

    fun submitList(newItems: List<AdminFeedbackItem>) {
        items = newItems
        if (expandedFeedbackId != null && items.none { it.feedbackId == expandedFeedbackId }) {
            expandedFeedbackId = null
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminFeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_feedback, parent, false)
        return AdminFeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminFeedbackViewHolder, position: Int) {
        val item = items[position]
        holder.bind(
            item = item,
            isExpanded = expandedFeedbackId == item.feedbackId,
            onCardClick = {
                expandedFeedbackId = if (expandedFeedbackId == item.feedbackId) null else item.feedbackId
                notifyDataSetChanged()
            },
            onDelete = onDelete,
            onHideToggle = onHideToggle
        )
    }

    override fun getItemCount(): Int = items.size

    class AdminFeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: View = itemView.findViewById(R.id.cardRoot)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvCategoryChip: TextView = itemView.findViewById(R.id.tvCategoryChip)
        private val tvRatingChip: TextView = itemView.findViewById(R.id.tvRatingChip)
        private val tvVisibilityChip: TextView = itemView.findViewById(R.id.tvVisibilityChip)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvCommentPreview: TextView = itemView.findViewById(R.id.tvCommentPreview)
        private val tvExpandIndicator: TextView = itemView.findViewById(R.id.tvExpandIndicator)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layoutExpanded)
        private val tvFullComment: TextView = itemView.findViewById(R.id.tvFullComment)
        private val tvModeration: TextView = itemView.findViewById(R.id.tvModeration)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        private val tvHideToggleAction: TextView = itemView.findViewById(R.id.tvHideToggleAction)
        private val tvDeleteAction: TextView = itemView.findViewById(R.id.tvDeleteAction)

        private val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK)

        fun bind(
            item: AdminFeedbackItem,
            isExpanded: Boolean,
            onCardClick: () -> Unit,
            onDelete: (AdminFeedbackItem) -> Unit,
            onHideToggle: (AdminFeedbackItem) -> Unit
        ) {
            tvProductName.text = item.productName
            tvCategoryChip.text = formatCategory(item.productCategory)
            tvRatingChip.text = "${item.rating} ★"
            tvVisibilityChip.text = if (item.isHidden) "Hidden" else "Visible"

            tvMeta.text = "Customer #${item.customerId} • Order #${item.orderId} • ${formatter.format(Date(item.createdAt))}"

            tvCommentPreview.text = when {
                item.comment.isBlank() -> "Rating only — no written comment"
                item.isHidden -> "Comment hidden by admin."
                else -> item.comment
            }

            tvFullComment.text = when {
                item.comment.isBlank() -> "Rating only — no written comment"
                item.isHidden -> "This comment is currently hidden from customer-facing views."
                else -> item.comment
            }

            tvModeration.visibility = if (item.isModerated || item.isHidden) View.VISIBLE else View.GONE
            tvModeration.text = when {
                item.isHidden -> "Moderation status: hidden by admin"
                item.isModerated -> "Moderation status: moderated"
                else -> ""
            }

            tvUpdatedAt.text = "Updated ${formatter.format(Date(item.updatedAt))}"

            val hasComment = item.comment.isNotBlank()
            tvHideToggleAction.isEnabled = hasComment
            tvHideToggleAction.alpha = if (hasComment) 1f else 0.45f
            tvHideToggleAction.text = if (item.isHidden) "Unhide comment" else "Hide comment"

            tvDeleteAction.setOnClickListener { onDelete(item) }
            tvHideToggleAction.setOnClickListener {
                if (hasComment) {
                    onHideToggle(item)
                }
            }

            dividerExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            tvExpandIndicator.text = if (isExpanded) "▴" else "▾"

            cardRoot.setOnClickListener { onCardClick() }
        }

        private fun formatCategory(category: String): String {
            return when {
                category.equals("COFFEE", ignoreCase = true) -> "Coffee"
                category.equals("CAKE", ignoreCase = true) -> "Cake"
                category.equals("MERCH", ignoreCase = true) -> "Merch"
                else -> category.replaceFirstChar { it.uppercase() }
            }
        }
    }
}