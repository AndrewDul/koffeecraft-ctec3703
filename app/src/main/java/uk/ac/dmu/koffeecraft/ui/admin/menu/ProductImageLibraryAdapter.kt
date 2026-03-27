package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.util.images.ProductImageCatalogEntry

class ProductImageLibraryAdapter(
    private var items: List<ProductImageCatalogEntry>,
    private val onSelected: (ProductImageCatalogEntry) -> Unit
) : RecyclerView.Adapter<ProductImageLibraryAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image_library, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onSelected)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivLibraryImage)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLibraryLabel)

        fun bind(item: ProductImageCatalogEntry, onSelected: (ProductImageCatalogEntry) -> Unit) {
            ivImage.setImageResource(item.drawableResId)
            tvLabel.text = item.label
            itemView.setOnClickListener { onSelected(item) }
        }
    }
}