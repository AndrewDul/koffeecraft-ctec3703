package uk.ac.dmu.koffeecraft.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import java.util.Locale

class CartAdapter(
    private var items: List<CartItem>,
    private val onPlus: (CartItem) -> Unit,
    private val onMinus: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onPlus, onMinus)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCraftedBadge: TextView = itemView.findViewById(R.id.tvCraftedBadge)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        private val tvAddOns: TextView = itemView.findViewById(R.id.tvAddOns)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvPriceEach: TextView = itemView.findViewById(R.id.tvPriceEach)
        private val tvLineTotal: TextView = itemView.findViewById(R.id.tvLineTotal)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val btnPlus: TextView = itemView.findViewById(R.id.btnPlus)
        private val btnMinus: TextView = itemView.findViewById(R.id.btnMinus)

        fun bind(item: CartItem, onPlus: (CartItem) -> Unit, onMinus: (CartItem) -> Unit) {
            val isCrafted = !item.selectedOptionLabel.isNullOrBlank() ||
                    item.selectedOptionSizeValue != null ||
                    !item.selectedOptionSizeUnit.isNullOrBlank() ||
                    !item.selectedAddOnsSummary.isNullOrBlank() ||
                    item.estimatedCalories != null

            tvName.text = item.product.name
            tvCraftedBadge.visibility = if (isCrafted) View.VISIBLE else View.GONE

            val sizeText = buildSizeText(item)
            if (sizeText.isNullOrBlank()) {
                tvSize.visibility = View.GONE
            } else {
                tvSize.visibility = View.VISIBLE
                tvSize.text = "Size • $sizeText"
            }

            if (item.selectedAddOnsSummary.isNullOrBlank()) {
                tvAddOns.visibility = View.GONE
            } else {
                tvAddOns.visibility = View.VISIBLE
                tvAddOns.text = "Add-ons • ${item.selectedAddOnsSummary}"
            }

            if (item.estimatedCalories == null) {
                tvCalories.visibility = View.GONE
            } else {
                tvCalories.visibility = View.VISIBLE
                tvCalories.text = "Calories • ${item.estimatedCalories} kcal"
            }

            tvPriceEach.text = formatMoney(item.unitPrice)
            tvLineTotal.text = formatMoney(item.unitPrice * item.quantity)
            tvQty.text = item.quantity.toString()

            btnMinus.setOnClickListener { onMinus(item) }

            if (item.isReward) {
                btnPlus.alpha = 0.35f
                btnPlus.isEnabled = false
                btnPlus.setOnClickListener(null)
            } else {
                btnPlus.alpha = 1f
                btnPlus.isEnabled = true
                btnPlus.setOnClickListener { onPlus(item) }
            }
        }

        private fun buildSizeText(item: CartItem): String? {
            val parts = mutableListOf<String>()

            item.selectedOptionLabel
                ?.takeIf { it.isNotBlank() }
                ?.let { parts.add(it) }

            if (item.selectedOptionSizeValue != null && !item.selectedOptionSizeUnit.isNullOrBlank()) {
                parts.add("${item.selectedOptionSizeValue}${item.selectedOptionSizeUnit.lowercase(Locale.UK)}")
            }

            return if (parts.isEmpty()) null else parts.joinToString(" • ")
        }

        private fun formatMoney(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }
    }
}