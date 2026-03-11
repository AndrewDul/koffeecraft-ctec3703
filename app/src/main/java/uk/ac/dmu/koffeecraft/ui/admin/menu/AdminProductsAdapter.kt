package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminProductsAdapter(
    private var items: List<Product>,
    private val onToggle: (Product) -> Unit,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductsAdapter.VH>() {

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_product, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onToggle, onEdit, onDelete)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvBadges: TextView = itemView.findViewById(R.id.tvBadges)

        private val btnToggle: ImageButton = itemView.findViewById(R.id.btnToggle)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            p: Product,
            onToggle: (Product) -> Unit,
            onEdit: (Product) -> Unit,
            onDelete: (Product) -> Unit
        ) {
            tvName.text = p.name
            tvPrice.text = "£%.2f".format(p.price)

            // I show "NEW" badge only when product is flagged as new.
            tvBadges.visibility = if (p.isNew) View.VISIBLE else View.GONE

            // I visually grey out disabled products (admin view).
            val alpha = if (p.isActive) 1.0f else 0.45f
            itemView.alpha = alpha

            // TODO: I will load real images later (imageKey -> drawable). For now I keep a placeholder.
            ivImage.setImageResource(android.R.drawable.ic_menu_gallery)

            btnToggle.setOnClickListener { onToggle(p) }
            btnEdit.setOnClickListener { onEdit(p) }
            btnDelete.setOnClickListener { onDelete(p) }
        }
    }
}