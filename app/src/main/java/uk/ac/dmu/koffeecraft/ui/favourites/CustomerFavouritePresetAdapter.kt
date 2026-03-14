package uk.ac.dmu.koffeecraft.ui.favourites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetCard

class CustomerFavouritePresetAdapter(
    private var items: List<CustomerFavouritePresetCard>,
    private val onOpen: (CustomerFavouritePresetCard) -> Unit,
    private val onBuyAgain: (CustomerFavouritePresetCard) -> Unit
) : RecyclerView.Adapter<CustomerFavouritePresetAdapter.PresetViewHolder>() {

    fun submitList(newItems: List<CustomerFavouritePresetCard>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_favourite_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(items[position], onOpen, onBuyAgain)
    }

    override fun getItemCount(): Int = items.size

    class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPresetName)
        private val tvConfig: TextView = itemView.findViewById(R.id.tvPresetConfig)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvPresetMeta)
        private val btnBuyAgain: Button = itemView.findViewById(R.id.btnPresetBuyAgain)

        fun bind(
            item: CustomerFavouritePresetCard,
            onOpen: (CustomerFavouritePresetCard) -> Unit,
            onBuyAgain: (CustomerFavouritePresetCard) -> Unit
        ) {
            tvName.text = item.productName

            tvConfig.text = buildString {
                append(item.optionLabel)
                append(" • ")
                append(item.optionSizeValue)
                append(item.optionSizeUnit.lowercase())

                if (!item.addOnSummary.isNullOrBlank()) {
                    append("\n")
                    append(item.addOnSummary)
                }
            }

            tvMeta.text = "£%.2f • %d kcal".format(item.totalPrice, item.totalCalories)

            itemView.setOnClickListener { onOpen(item) }
            btnBuyAgain.setOnClickListener { onBuyAgain(item) }
        }
    }
}