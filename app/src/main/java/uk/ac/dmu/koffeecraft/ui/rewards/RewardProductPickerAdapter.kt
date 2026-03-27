package uk.ac.dmu.koffeecraft.ui.rewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.util.images.ProductImageLoader

class RewardProductPickerAdapter(
    private var items: List<Product>,
    private val onSelect: (Product) -> Unit
) : RecyclerView.Adapter<RewardProductPickerAdapter.VH>() {

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward_product_picker, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onSelect)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivPickerProductImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvPickerProductName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvPickerProductDescription)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvPickerProductMeta)

        fun bind(item: Product, onSelect: (Product) -> Unit) {
            ProductImageLoader.load(
                imageView = ivImage,
                productFamily = item.productFamily,
                rewardEnabled = item.rewardEnabled,
                imageKey = item.imageKey,
                customImagePath = item.customImagePath
            )

            tvName.text = item.name
            tvDescription.text = item.description
            tvMeta.text = "Base reward included"

            itemView.setOnClickListener { onSelect(item) }
        }
    }
}