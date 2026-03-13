package uk.ac.dmu.koffeecraft.ui.menu

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product

class ProductAdapter(
    private var items: List<Product>,
    private var favouriteIds: Set<Long>,
    private val onCustomizeClicked: (Product) -> Unit,
    private val onFavouriteToggle: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateFavouriteIds(newFavouriteIds: Set<Long>) {
        favouriteIds = newFavouriteIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position], favouriteIds.contains(items[position].productId), onCustomizeClicked, onFavouriteToggle)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnCustomize: Button = itemView.findViewById(R.id.btnCustomize)
        private val tvFavourite: TextView = itemView.findViewById(R.id.tvFavourite)

        fun bind(
            product: Product,
            isFavourite: Boolean,
            onCustomizeClicked: (Product) -> Unit,
            onFavouriteToggle: (Product, Boolean) -> Unit
        ) {
            tvName.text = product.name
            tvDesc.text = product.description
            tvPrice.text = "From £%.2f".format(product.price)

            tvMeta.text = when (product.category) {
                "COFFEE" -> "Customisable • 250–450 ml"
                "CAKE" -> "Customisable • 120–330 g"
                else -> "Customisable"
            }

            tvFavourite.text = if (isFavourite) "♥" else "♡"
            tvFavourite.setTextColor(if (isFavourite) Color.parseColor("#A12727") else Color.parseColor("#6E5A4D"))

            val isAvailable = product.isActive
            val contentAlpha = if (isAvailable) 1f else 0.45f

            tvName.alpha = contentAlpha
            tvDesc.alpha = contentAlpha
            tvMeta.alpha = contentAlpha
            tvPrice.alpha = contentAlpha
            tvFavourite.alpha = contentAlpha
            btnCustomize.alpha = contentAlpha

            btnCustomize.isEnabled = isAvailable
            btnCustomize.text = if (isAvailable) "Customize" else "Unavailable"

            btnCustomize.setOnClickListener {
                if (product.isActive) {
                    onCustomizeClicked(product)
                }
            }

            tvFavourite.setOnClickListener {
                if (product.isActive) {
                    onFavouriteToggle(product, !isFavourite)
                }
            }
        }
    }
}