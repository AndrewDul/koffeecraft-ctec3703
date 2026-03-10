package uk.ac.dmu.koffeecraft.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartItem

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
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val btnPlus: Button = itemView.findViewById(R.id.btnPlus)
        private val btnMinus: Button = itemView.findViewById(R.id.btnMinus)

        fun bind(item: CartItem, onPlus: (CartItem) -> Unit, onMinus: (CartItem) -> Unit) {
            tvName.text = item.product.name
            tvQty.text = item.quantity.toString()
            btnPlus.setOnClickListener { onPlus(item) }
            btnMinus.setOnClickListener { onMinus(item) }
        }
    }
}