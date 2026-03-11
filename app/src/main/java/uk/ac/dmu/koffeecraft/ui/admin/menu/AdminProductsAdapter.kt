package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminProductsAdapter(
    private var items: List<Product>,
    private val onToggle: (Product) -> Unit,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductsAdapter.ProductViewHolder>() {

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvBadges: TextView = itemView.findViewById(R.id.tvBadges)
        private val btnToggle: ImageButton = itemView.findViewById(R.id.btnToggle)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(product: Product) {
            ivImage.setImageResource(android.R.drawable.ic_menu_gallery)

            tvName.text = product.name
            tvPrice.text = String.format(Locale.UK, "£%.2f", product.price)

            val badges = mutableListOf<String>()
            if (product.isNew) badges += "NEW"
            if (!product.isActive) badges += "DISABLED"

            if (badges.isEmpty()) {
                tvBadges.visibility = View.GONE
            } else {
                tvBadges.visibility = View.VISIBLE
                tvBadges.text = badges.joinToString(" • ")
            }

            val alpha = if (product.isActive) 1f else 0.55f
            ivImage.alpha = alpha
            tvName.alpha = alpha
            tvPrice.alpha = alpha
            tvBadges.alpha = alpha

            btnToggle.setImageResource(
                if (product.isActive) {
                    android.R.drawable.ic_menu_close_clear_cancel
                } else {
                    android.R.drawable.ic_menu_view
                }
            )

            btnToggle.contentDescription =
                if (product.isActive) "Disable product" else "Enable product"

            btnToggle.setOnClickListener { onToggle(product) }
            btnEdit.setOnClickListener { onEdit(product) }
            btnDelete.setOnClickListener { onDelete(product) }
        }
    }
}
