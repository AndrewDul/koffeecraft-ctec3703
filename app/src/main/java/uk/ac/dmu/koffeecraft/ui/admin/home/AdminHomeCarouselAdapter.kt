package uk.ac.dmu.koffeecraft.ui.admin.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R

data class AdminHomeCarouselItem(
    val rankLabel: String,
    val productName: String,
    val primaryText: String,
    val secondaryText: String
)

class AdminHomeCarouselAdapter(
    private var items: List<AdminHomeCarouselItem>
) : RecyclerView.Adapter<AdminHomeCarouselAdapter.CarouselViewHolder>() {

    fun submitList(newItems: List<AdminHomeCarouselItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getRealItemCount(): Int = items.size

    fun getInitialPosition(): Int {
        if (items.size <= 1) return 0
        val middle = Int.MAX_VALUE / 2
        return middle - (middle % items.size)
    }

    fun getIndicatorPosition(position: Int): Int {
        if (items.isEmpty()) return 0
        if (items.size == 1) return 0
        return position % items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_home_carousel_page, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        if (items.isEmpty()) return
        val item = if (items.size == 1) items[0] else items[position % items.size]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return when {
            items.isEmpty() -> 0
            items.size == 1 -> 1
            else -> Int.MAX_VALUE
        }
    }

    class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrimary: TextView = itemView.findViewById(R.id.tvPrimary)
        private val tvSecondary: TextView = itemView.findViewById(R.id.tvSecondary)

        fun bind(item: AdminHomeCarouselItem) {
            tvRank.text = item.rankLabel
            tvProductName.text = item.productName
            tvPrimary.text = item.primaryText
            tvSecondary.text = item.secondaryText
        }
    }
}
