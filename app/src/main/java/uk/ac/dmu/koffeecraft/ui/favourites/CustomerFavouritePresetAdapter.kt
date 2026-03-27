package uk.ac.dmu.koffeecraft.ui.favourites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerFavouritePresetCard
import uk.ac.dmu.koffeecraft.util.images.ProductImageLoader

class CustomerFavouritePresetAdapter(
    private var items: List<CustomerFavouritePresetCard>,
    private val onRemove: (CustomerFavouritePresetCard) -> Unit,
    private val onBuyAgain: (CustomerFavouritePresetCard) -> Unit
) : RecyclerView.Adapter<CustomerFavouritePresetAdapter.PresetViewHolder>() {

    private val expandedIds = mutableSetOf<Long>()

    fun submitList(newItems: List<CustomerFavouritePresetCard>) {
        items = newItems
        expandedIds.retainAll(newItems.map { it.presetId }.toSet())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_favourite_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val item = items[position]
        val expanded = expandedIds.contains(item.presetId)

        holder.bind(
            item = item,
            expanded = expanded,
            onToggle = {
                val id = item.presetId
                if (!expandedIds.add(id)) {
                    expandedIds.remove(id)
                }

                val index = items.indexOfFirst { it.presetId == id }
                if (index != -1) notifyItemChanged(index)
            },
            onRemove = onRemove,
            onBuyAgain = onBuyAgain
        )
    }

    override fun getItemCount(): Int = items.size

    class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivImage: ImageView = itemView.findViewById(R.id.ivPresetImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvPresetName)
        private val tvSize: TextView = itemView.findViewById(R.id.tvPresetSize)
        private val tvAddOns: TextView = itemView.findViewById(R.id.tvPresetAddOns)
        private val tvPriceValue: TextView = itemView.findViewById(R.id.tvPresetPriceValue)
        private val tvCaloriesValue: TextView = itemView.findViewById(R.id.tvPresetCaloriesValue)
        private val tvBuyAgainCollapsed: TextView = itemView.findViewById(R.id.tvPresetBuyAgainCollapsed)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpandedContent: LinearLayout = itemView.findViewById(R.id.layoutExpandedContent)

        private val tvExpandedSizeValue: TextView = itemView.findViewById(R.id.tvExpandedSizeValue)
        private val tvExpandedAddOnsValue: TextView = itemView.findViewById(R.id.tvExpandedAddOnsValue)
        private val tvExpandedCaloriesValue: TextView = itemView.findViewById(R.id.tvExpandedCaloriesValue)
        private val tvExpandedPriceValue: TextView = itemView.findViewById(R.id.tvExpandedPriceValue)

        private val tvRemove: TextView = itemView.findViewById(R.id.tvPresetRemove)
        private val tvBuyAgainExpanded: TextView = itemView.findViewById(R.id.tvPresetBuyAgainExpanded)

        fun bind(
            item: CustomerFavouritePresetCard,
            expanded: Boolean,
            onToggle: () -> Unit,
            onRemove: (CustomerFavouritePresetCard) -> Unit,
            onBuyAgain: (CustomerFavouritePresetCard) -> Unit
        ) {
            ProductImageLoader.load(
                imageView = ivImage,
                productFamily = item.productFamily,
                rewardEnabled = item.productFamily.equals("MERCH", ignoreCase = true),
                imageKey = item.imageKey,
                customImagePath = item.customImagePath
            )

            val sizeText = buildSizeText(item)
            val addOnsText = item.addOnSummary?.takeIf { it.isNotBlank() } ?: "None"

            tvName.text = item.productName
            tvSize.text = "Size • $sizeText"
            tvAddOns.text = "Add-ons • $addOnsText"
            tvPriceValue.text = formatMoney(item.totalPrice)
            tvCaloriesValue.text = "${item.totalCalories} kcal"

            tvExpandedSizeValue.text = sizeText
            tvExpandedAddOnsValue.text = addOnsText
            tvExpandedCaloriesValue.text = "${item.totalCalories} kcal"
            tvExpandedPriceValue.text = formatMoney(item.totalPrice)

            dividerExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
            layoutExpandedContent.visibility = if (expanded) View.VISIBLE else View.GONE
            tvBuyAgainCollapsed.visibility = if (expanded) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onToggle() }
            tvRemove.setOnClickListener { onRemove(item) }
            tvBuyAgainCollapsed.setOnClickListener { onBuyAgain(item) }
            tvBuyAgainExpanded.setOnClickListener { onBuyAgain(item) }
        }

        private fun buildSizeText(item: CustomerFavouritePresetCard): String {
            return buildString {
                append(item.optionLabel)
                append(" • ")
                append(item.optionSizeValue)
                append(item.optionSizeUnit.lowercase(Locale.UK))
            }
        }

        private fun formatMoney(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }
    }
}