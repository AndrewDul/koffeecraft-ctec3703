package uk.ac.dmu.koffeecraft.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.util.images.ProductImageLoader

data class CustomerHomeCarouselItem(
    val title: String,
    val subtitle: String,
    val metaLine: String,
    val badgeLabel: String? = null,
    val productFamily: String,
    val imageKey: String? = null,
    val customImagePath: String? = null
)

class CustomerHomeCarouselAdapter(
    private var items: List<CustomerHomeCarouselItem>,
    private val onClick: (CustomerHomeCarouselItem) -> Unit
) : RecyclerView.Adapter<CustomerHomeCarouselAdapter.HomeCarouselViewHolder>() {

    fun submitList(newItems: List<CustomerHomeCarouselItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_home_carousel, parent, false)
        return HomeCarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeCarouselViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class HomeCarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivHomeCarouselImage)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvHomeCarouselBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvHomeCarouselTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvHomeCarouselSubtitle)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvHomeCarouselMeta)

        fun bind(item: CustomerHomeCarouselItem, onClick: (CustomerHomeCarouselItem) -> Unit) {
            ProductImageLoader.load(
                imageView = ivImage,
                productFamily = item.productFamily,
                rewardEnabled = item.productFamily.equals("MERCH", ignoreCase = true),
                imageKey = item.imageKey,
                customImagePath = item.customImagePath
            )

            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            tvMeta.text = item.metaLine

            if (item.badgeLabel.isNullOrBlank()) {
                tvBadge.visibility = View.GONE
            } else {
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = item.badgeLabel
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }
}