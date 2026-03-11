package uk.ac.dmu.koffeecraft.ui.menu

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
    private val onAddClicked: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position], onAddClicked)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnAdd: Button = itemView.findViewById(R.id.btnAdd)

        fun bind(product: Product, onAddClicked: (Product) -> Unit) {
            tvName.text = product.name
            tvDesc.text = product.description
            tvPrice.text = "£%.2f".format(product.price)

            val isAvailable = product.isActive
            val contentAlpha = if (isAvailable) 1f else 0.45f

            tvName.alpha = contentAlpha
            tvDesc.alpha = contentAlpha
            tvPrice.alpha = contentAlpha
            btnAdd.alpha = contentAlpha

            btnAdd.isEnabled = isAvailable
            btnAdd.text = if (isAvailable) "Add" else "Unavailable"

            btnAdd.setOnClickListener {
                if (product.isActive) {
                    onAddClicked(product)
                }
            }
        }
    }
}
