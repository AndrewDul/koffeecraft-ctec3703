package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminProductsAdapter(
    private var items: List<Product>,
    private val onToggle: (Product) -> Unit,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onDetails: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductsAdapter.ProductViewHolder>() {

    private var expandedProductId: Long? = null

    fun submitList(newItems: List<Product>) {
        items = newItems

        val expandedStillExists = newItems.any { it.productId == expandedProductId }
        if (!expandedStillExists) {
            expandedProductId = null
        }

        notifyDataSetChanged()
    }

    fun collapseAll() {
        val previousExpandedId = expandedProductId ?: return
        expandedProductId = null

        val previousIndex = items.indexOfFirst { it.productId == previousExpandedId }
        if (previousIndex != -1) {
            notifyItemChanged(previousIndex)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = items[position]
        val isExpanded = expandedProductId == product.productId
        holder.bind(product, isExpanded)
    }

    override fun getItemCount(): Int = items.size

    private fun toggleExpanded(product: Product) {
        val previousExpandedId = expandedProductId

        expandedProductId = if (expandedProductId == product.productId) {
            null
        } else {
            product.productId
        }

        val previousIndex = items.indexOfFirst { it.productId == previousExpandedId }
        val newIndex = items.indexOfFirst { it.productId == expandedProductId }

        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1 && newIndex != previousIndex) notifyItemChanged(newIndex)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: View = itemView.findViewById(R.id.cardRoot)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvBadges: TextView = itemView.findViewById(R.id.tvBadges)
        private val tvTapHint: TextView = itemView.findViewById(R.id.tvTapHint)
        private val tvExpandIndicator: TextView = itemView.findViewById(R.id.tvExpandIndicator)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layoutExpanded)

        private val tvExpandedDescription: TextView = itemView.findViewById(R.id.tvExpandedDescription)
        private val tvMetaAvailability: TextView = itemView.findViewById(R.id.tvMetaAvailability)
        private val tvMetaReward: TextView = itemView.findViewById(R.id.tvMetaReward)
        private val tvMetaFamily: TextView = itemView.findViewById(R.id.tvMetaFamily)
        private val tvMetaListing: TextView = itemView.findViewById(R.id.tvMetaListing)

        private val btnManageDetails: MaterialButton = itemView.findViewById(R.id.btnManageDetails)
        private val btnToggle: ImageButton = itemView.findViewById(R.id.btnToggle)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(product: Product, isExpanded: Boolean) {
            bindCollapsedContent(product)
            bindExpandedContent(product, isExpanded)

            cardRoot.setOnClickListener {
                toggleExpanded(product)
            }

            btnManageDetails.setOnClickListener {
                onDetails(product)
            }

            btnToggle.setOnClickListener {
                onToggle(product)
            }

            btnEdit.setOnClickListener {
                onEdit(product)
            }

            btnDelete.setOnClickListener {
                onDelete(product)
            }
        }

        private fun bindCollapsedContent(product: Product) {
            ivImage.setImageResource(
                when {
                    product.isCoffee -> R.drawable.coffee_bean
                    else -> android.R.drawable.ic_menu_gallery
                }
            )

            tvName.text = product.name
            tvCategory.text = buildCollapsedCategoryLine(product)
            tvPrice.text = buildCollapsedPriceLine(product)

            val badges = mutableListOf<String>()
            if (product.isNew) badges += "NEW"
            if (product.rewardEnabled) badges += "REWARD"
            if (!product.isActive) badges += "DISABLED"

            if (badges.isEmpty()) {
                tvBadges.visibility = View.GONE
            } else {
                tvBadges.visibility = View.VISIBLE
                tvBadges.text = badges.joinToString(" • ")
            }

            tvTapHint.text = if (product.isMerch) {
                "Tap to open actions, visibility, and reward setup"
            } else {
                "Tap to open full product details, configuration, and admin actions"
            }

            val alpha = if (product.isActive) 1f else 0.58f
            ivImage.alpha = alpha
            tvName.alpha = alpha
            tvCategory.alpha = alpha
            tvPrice.alpha = alpha
            tvBadges.alpha = alpha
            tvTapHint.alpha = alpha
        }

        private fun bindExpandedContent(product: Product, isExpanded: Boolean) {
            dividerExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            tvExpandIndicator.text = if (isExpanded) "▴" else "▾"

            if (!isExpanded) return

            tvExpandedDescription.text = product.description.ifBlank {
                "No product description has been added yet."
            }

            tvMetaAvailability.text = "Availability • ${if (product.isActive) "Active" else "Disabled"}"
            tvMetaReward.text = "Reward Eligible • ${if (product.rewardEnabled) "Yes" else "No"}"
            tvMetaFamily.text = "Product Family • ${product.familyLabel}"
            tvMetaListing.text = "Listing Type • ${product.listingLabel}"

            btnToggle.setImageResource(
                if (product.isActive) {
                    android.R.drawable.ic_menu_close_clear_cancel
                } else {
                    android.R.drawable.ic_menu_view
                }
            )

            btnToggle.contentDescription =
                if (product.isActive) "Deactivate product" else "Activate product"

            btnEdit.contentDescription = "Edit product"
            btnDelete.contentDescription = "Delete product permanently"
        }

        private fun buildCollapsedCategoryLine(product: Product): String {
            val statusLabel = if (product.isActive) "Active" else "Disabled"
            return "${product.familyLabel} • $statusLabel"
        }

        private fun buildCollapsedPriceLine(product: Product): String {
            return if (product.isMerch) {
                "Reward / merch setup"
            } else {
                String.format(Locale.UK, "From £%.2f", product.price)
            }
        }
    }
}