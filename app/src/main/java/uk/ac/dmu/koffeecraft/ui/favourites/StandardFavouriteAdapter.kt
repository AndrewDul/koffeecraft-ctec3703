package uk.ac.dmu.koffeecraft.ui.favourites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.StandardFavouriteCard
import java.util.Locale

class StandardFavouriteAdapter(
    private var items: List<StandardFavouriteCard>,
    private val onRemove: (StandardFavouriteCard) -> Unit,
    private val onCustomize: (StandardFavouriteCard) -> Unit,
    private val onBuyAgain: (StandardFavouriteCard) -> Unit
) : RecyclerView.Adapter<StandardFavouriteAdapter.StandardFavouriteViewHolder>() {

    private val expandedIds = mutableSetOf<Long>()

    fun submitList(newItems: List<StandardFavouriteCard>) {
        items = newItems
        expandedIds.retainAll(newItems.map { it.productId }.toSet())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandardFavouriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_standard_favourite, parent, false)
        return StandardFavouriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandardFavouriteViewHolder, position: Int) {
        val item = items[position]
        val expanded = expandedIds.contains(item.productId)

        holder.bind(
            item = item,
            expanded = expanded,
            onToggle = {
                val id = item.productId
                if (!expandedIds.add(id)) {
                    expandedIds.remove(id)
                }

                val index = items.indexOfFirst { it.productId == id }
                if (index != -1) notifyItemChanged(index)
            },
            onRemove = onRemove,
            onCustomize = onCustomize,
            onBuyAgain = onBuyAgain
        )
    }

    override fun getItemCount(): Int = items.size

    class StandardFavouriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvFavouriteName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvFavouriteDescription)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvFavouriteMeta)
        private val tvPriceValue: TextView = itemView.findViewById(R.id.tvFavouritePriceValue)
        private val tvCaloriesValue: TextView = itemView.findViewById(R.id.tvFavouriteCaloriesValue)
        private val tvCustomizeCollapsed: TextView = itemView.findViewById(R.id.tvFavouriteCustomizeCollapsed)
        private val tvBuyAgainCollapsed: TextView = itemView.findViewById(R.id.tvFavouriteBuyAgainCollapsed)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpandedContent: LinearLayout = itemView.findViewById(R.id.layoutExpandedContent)

        private val tvExpandedStandardSizeValue: TextView = itemView.findViewById(R.id.tvExpandedStandardSizeValue)
        private val tvExpandedCaloriesValue: TextView = itemView.findViewById(R.id.tvExpandedCaloriesValue)
        private val tvExpandedPriceValue: TextView = itemView.findViewById(R.id.tvExpandedPriceValue)

        private val tvRemove: TextView = itemView.findViewById(R.id.tvFavouriteRemove)

        fun bind(
            item: StandardFavouriteCard,
            expanded: Boolean,
            onToggle: () -> Unit,
            onRemove: (StandardFavouriteCard) -> Unit,
            onCustomize: (StandardFavouriteCard) -> Unit,
            onBuyAgain: (StandardFavouriteCard) -> Unit
        ) {
            tvName.text = item.name
            tvDescription.text = item.description
            tvMeta.text = "${item.familyLabel} • Standard favourite"
            tvPriceValue.text = formatMoney(item.price)
            tvCaloriesValue.text = item.standardCaloriesText

            tvExpandedStandardSizeValue.text = item.standardSizeText
            tvExpandedCaloriesValue.text = item.standardCaloriesText
            tvExpandedPriceValue.text = formatMoney(item.price)

            dividerExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
            layoutExpandedContent.visibility = if (expanded) View.VISIBLE else View.GONE

            itemView.alpha = if (item.isActive) 1f else 0.72f

            itemView.setOnClickListener { onToggle() }

            tvCustomizeCollapsed.setOnClickListener { onCustomize(item) }
            tvBuyAgainCollapsed.setOnClickListener { onBuyAgain(item) }
            tvRemove.setOnClickListener { onRemove(item) }
        }

        private fun formatMoney(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }
    }
}