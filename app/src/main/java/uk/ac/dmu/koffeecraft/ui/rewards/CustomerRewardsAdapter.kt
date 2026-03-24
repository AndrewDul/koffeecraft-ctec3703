package uk.ac.dmu.koffeecraft.ui.rewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uk.ac.dmu.koffeecraft.R

data class RewardUiModel(
    val id: String,
    val title: String,
    val description: String,
    val beansLabel: String,
    val actionLabel: String,
    val enabled: Boolean
)

class CustomerRewardsAdapter(
    private var items: List<RewardUiModel>,
    private val onAction: (RewardUiModel) -> Unit
) : RecyclerView.Adapter<CustomerRewardsAdapter.RewardViewHolder>() {

    fun submitList(newItems: List<RewardUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(items[position], onAction)
    }

    override fun getItemCount(): Int = items.size

    class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivReward: ImageView = itemView.findViewById(R.id.ivReward)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvRewardTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvRewardDescription)
        private val tvBeansLabel: TextView = itemView.findViewById(R.id.tvRewardBeans)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btnRewardAction)

        fun bind(item: RewardUiModel, onAction: (RewardUiModel) -> Unit) {
            ivReward.setImageResource(android.R.drawable.ic_menu_gallery)
            tvTitle.text = item.title
            tvDescription.text = item.description
            tvBeansLabel.text = item.beansLabel
            btnAction.text = item.actionLabel

            if (item.enabled) {
                btnAction.alpha = 1f
            } else {
                btnAction.alpha = 0.55f
            }

            btnAction.isEnabled = true
            btnAction.setOnClickListener {
                onAction(item)
            }

            itemView.isClickable = true
            itemView.isFocusable = true
            itemView.setOnClickListener {
                onAction(item)
            }
        }
    }
}