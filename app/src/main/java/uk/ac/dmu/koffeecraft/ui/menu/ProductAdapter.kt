package uk.ac.dmu.koffeecraft.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product
import java.util.Locale
import android.widget.ImageView
import uk.ac.dmu.koffeecraft.util.images.ProductImageLoader
class ProductAdapter(
    private var items: List<Product>,
    private var favouriteIds: Set<Long>,
    private var cardStates: Map<Long, ProductCardUiState>,
    private var expandedProductId: Long?,
    private val onToggleExpand: (Long) -> Unit,
    private val onFavouriteToggle: (Product, Boolean) -> Unit,
    private val onOptionSelected: (Long, Long) -> Unit,
    private val onAddOnToggled: (Long, Long, Boolean) -> Unit,
    private val onSavePreset: (Product) -> Unit,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun submitData(
        newItems: List<Product>,
        newFavouriteIds: Set<Long>,
        newCardStates: Map<Long, ProductCardUiState>,
        newExpandedProductId: Long?
    ) {
        items = newItems
        favouriteIds = newFavouriteIds
        cardStates = newCardStates
        expandedProductId = newExpandedProductId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = items[position]
        val state = cardStates[product.productId] ?: ProductCardUiState()
        val expanded = expandedProductId == product.productId

        holder.bind(
            product = product,
            isFavourite = favouriteIds.contains(product.productId),
            expanded = expanded,
            state = state,
            onToggleExpand = { onToggleExpand(product.productId) },
            onFavouriteToggle = { shouldFavourite ->
                onFavouriteToggle(product, shouldFavourite)
            },
            onOptionSelected = { optionId ->
                onOptionSelected(product.productId, optionId)
            },
            onAddOnToggled = { addOnId, checked ->
                onAddOnToggled(product.productId, addOnId, checked)
            },
            onSavePreset = {
                onSavePreset(product)
            },
            onAddToCart = {
                onAddToCart(product)
            }
        )
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)

        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val tvStandardSize: TextView = itemView.findViewById(R.id.tvStandardSize)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvUnavailable: TextView = itemView.findViewById(R.id.tvUnavailable)
        private val tvFavourite: TextView = itemView.findViewById(R.id.tvFavourite)

        private val dividerExpanded: View = itemView.findViewById(R.id.dividerExpanded)
        private val layoutExpandedContent: LinearLayout = itemView.findViewById(R.id.layoutExpandedContent)
        private val tvLoadingCustomization: TextView = itemView.findViewById(R.id.tvLoadingCustomization)

        private val layoutOptionsSection: LinearLayout = itemView.findViewById(R.id.layoutOptionsSection)
        private val chipGroupOptions: ChipGroup = itemView.findViewById(R.id.chipGroupOptions)

        private val layoutAddOnsSection: LinearLayout = itemView.findViewById(R.id.layoutAddOnsSection)
        private val chipGroupAddOns: ChipGroup = itemView.findViewById(R.id.chipGroupAddOns)
        private val tvNoExtras: TextView = itemView.findViewById(R.id.tvNoExtras)

        private val layoutSummarySection: LinearLayout = itemView.findViewById(R.id.layoutSummarySection)
        private val tvCaloriesValue: TextView = itemView.findViewById(R.id.tvCaloriesValue)
        private val tvAllergensValue: TextView = itemView.findViewById(R.id.tvAllergensValue)
        private val tvExtrasTotalValue: TextView = itemView.findViewById(R.id.tvExtrasTotalValue)
        private val tvTotalValue: TextView = itemView.findViewById(R.id.tvTotalValue)

        private val tvSavePreset: TextView = itemView.findViewById(R.id.tvSavePreset)
        private val btnAddToCart: MaterialButton = itemView.findViewById(R.id.btnAddToCart)

        fun bind(
            product: Product,
            isFavourite: Boolean,
            expanded: Boolean,
            state: ProductCardUiState,
            onToggleExpand: () -> Unit,
            onFavouriteToggle: (Boolean) -> Unit,
            onOptionSelected: (Long) -> Unit,
            onAddOnToggled: (Long, Boolean) -> Unit,
            onSavePreset: () -> Unit,
            onAddToCart: () -> Unit
        ) {
            ProductImageLoader.load(
                imageView = ivProductImage,
                productFamily = product.productFamily,
                rewardEnabled = product.rewardEnabled,
                imageKey = product.imageKey,
                customImagePath = product.customImagePath
            )

            tvName.text = product.name
            tvDesc.text = product.description
            tvStandardSize.text = "Standard size • ${buildStandardSizeText(state)}"
            tvPrice.text = String.format(Locale.UK, "From £%.2f", product.price)

            val isAvailable = product.isActive
            cardRoot.alpha = if (isAvailable) 1f else 0.72f
            ivProductImage.alpha = if (isAvailable) 1f else 0.72f
            tvUnavailable.visibility = if (isAvailable) View.GONE else View.VISIBLE

            tvFavourite.text = if (isFavourite) "♥" else "♡"
            tvFavourite.setTextColor(
                if (isFavourite) color(R.color.kc_danger) else color(R.color.kc_text_secondary)
            )

            dividerExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
            layoutExpandedContent.visibility = if (expanded) View.VISIBLE else View.GONE

            if (expanded) {
                bindExpandedState(
                    product = product,
                    state = state,
                    isAvailable = isAvailable,
                    onOptionSelected = onOptionSelected,
                    onAddOnToggled = onAddOnToggled,
                    onSavePreset = onSavePreset,
                    onAddToCart = onAddToCart
                )
            }

            cardRoot.setOnClickListener { onToggleExpand() }

            tvFavourite.setOnClickListener {
                animateFavourite()
                onFavouriteToggle(!isFavourite)
            }
        }

        private fun bindExpandedState(
            product: Product,
            state: ProductCardUiState,
            isAvailable: Boolean,
            onOptionSelected: (Long) -> Unit,
            onAddOnToggled: (Long, Boolean) -> Unit,
            onSavePreset: () -> Unit,
            onAddToCart: () -> Unit
        ) {
            val isLoaded = state.isLoaded && !state.isLoading

            tvLoadingCustomization.visibility = if (isLoaded) View.GONE else View.VISIBLE
            layoutOptionsSection.visibility = if (isLoaded) View.VISIBLE else View.GONE
            layoutAddOnsSection.visibility = if (isLoaded) View.VISIBLE else View.GONE
            layoutSummarySection.visibility = if (isLoaded) View.VISIBLE else View.GONE
            tvSavePreset.visibility = if (isLoaded) View.VISIBLE else View.GONE
            btnAddToCart.visibility = if (isLoaded) View.VISIBLE else View.GONE

            if (!isLoaded) return

            bindOptionChips(state, isAvailable, onOptionSelected)
            bindAddOnChips(state, isAvailable, onAddOnToggled)

            tvNoExtras.visibility = if (state.addOns.isEmpty()) View.VISIBLE else View.GONE

            val calories = calculateCalories(state)
            val allergens = buildAllergensText(state)
            val extrasTotal = calculateExtrasTotal(state)
            val total = calculateTotal(product, state)

            tvCaloriesValue.text = "$calories kcal"
            tvAllergensValue.text = allergens
            tvExtrasTotalValue.text = formatMoney(extrasTotal)
            tvTotalValue.text = formatMoney(total)

            tvSavePreset.alpha = if (state.savePresetEnabled) 1f else 0.45f
            tvSavePreset.setTextColor(
                if (state.savePresetEnabled) color(R.color.kc_success) else color(R.color.kc_text_muted)
            )
            tvSavePreset.setOnClickListener {
                if (state.savePresetEnabled) onSavePreset()
            }

            val hasOptions = state.options.isNotEmpty()
            val canAddToCart = isAvailable && hasOptions

            btnAddToCart.isEnabled = canAddToCart
            btnAddToCart.alpha = if (canAddToCart) 1f else 0.55f
            btnAddToCart.text = when {
                !isAvailable -> "Unavailable"
                !hasOptions -> "Missing size"
                else -> "Add to cart"
            }
            btnAddToCart.setOnClickListener {
                if (isAvailable) onAddToCart()
            }
        }

        private fun bindOptionChips(
            state: ProductCardUiState,
            isAvailable: Boolean,
            onOptionSelected: (Long) -> Unit
        ) {
            chipGroupOptions.removeAllViews()

            state.options.forEach { option ->
                val chip = Chip(itemView.context).apply {
                    isCheckable = true
                    isCheckedIconVisible = false
                    text = buildString {
                        append(option.displayLabel)
                        append(" • ")
                        append(option.sizeValue)
                        append(option.sizeUnit.lowercase(Locale.UK))
                        if (option.extraPrice > 0.0) {
                            append(" (+£")
                            append(String.format(Locale.UK, "%.2f", option.extraPrice))
                            append(")")
                        }
                    }
                    isChecked = state.selectedOptionId == option.optionId
                    isEnabled = isAvailable
                    alpha = if (isAvailable) 1f else 0.65f
                    styleChoiceChip(this)

                    setOnCheckedChangeListener { _, checked ->
                        if (checked) onOptionSelected(option.optionId)
                    }
                }

                chipGroupOptions.addView(chip)
            }
        }

        private fun bindAddOnChips(
            state: ProductCardUiState,
            isAvailable: Boolean,
            onAddOnToggled: (Long, Boolean) -> Unit
        ) {
            chipGroupAddOns.removeAllViews()

            state.addOns.forEach { addOn ->
                val chip = Chip(itemView.context).apply {
                    isCheckable = true
                    isCheckedIconVisible = false
                    text = "${addOn.name} +£${String.format(Locale.UK, "%.2f", addOn.price)}"
                    isChecked = state.selectedAddOnIds.contains(addOn.addOnId)
                    isEnabled = isAvailable
                    alpha = if (isAvailable) 1f else 0.65f
                    styleChoiceChip(this)

                    setOnCheckedChangeListener { _, checked ->
                        onAddOnToggled(addOn.addOnId, checked)
                    }
                }

                chipGroupAddOns.addView(chip)
            }
        }

        private fun styleChoiceChip(chip: Chip) {
            val density = itemView.resources.displayMetrics.density

            chip.chipBackgroundColor =
                AppCompatResources.getColorStateList(itemView.context, R.color.kc_chip_bg_selector)
            chip.chipStrokeColor =
                AppCompatResources.getColorStateList(itemView.context, R.color.kc_chip_stroke_selector)
            chip.setTextColor(
                AppCompatResources.getColorStateList(itemView.context, R.color.kc_chip_text_selector)
            )

            chip.chipStrokeWidth = 0f
            chip.chipCornerRadius = 18f * density
            chip.minHeight = (42f * density).toInt()
            chip.textStartPadding = 14f * density
            chip.textEndPadding = 14f * density
            chip.chipStartPadding = 2f * density
            chip.chipEndPadding = 2f * density
            chip.rippleColor =
                AppCompatResources.getColorStateList(itemView.context, R.color.kc_chip_ripple_selector)
            chip.setEnsureMinTouchTargetSize(false)
        }

        private fun animateFavourite() {
            tvFavourite.animate()
                .scaleX(1.22f)
                .scaleY(1.22f)
                .setDuration(110)
                .withEndAction {
                    tvFavourite.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(110)
                        .start()
                }
                .start()
        }

        private fun formatMoney(value: Double): String {
            return String.format(Locale.UK, "£%.2f", value)
        }

        private fun color(colorResId: Int): Int {
            return ContextCompat.getColor(itemView.context, colorResId)
        }
    }

    private fun calculateExtrasTotal(state: ProductCardUiState): Double {
        return state.addOns
            .filter { state.selectedAddOnIds.contains(it.addOnId) }
            .sumOf { it.price }
    }

    private fun calculateTotal(product: Product, state: ProductCardUiState): Double {
        val selectedOption = state.options.firstOrNull { it.optionId == state.selectedOptionId }
        val optionExtra = selectedOption?.extraPrice ?: 0.0
        return product.price + optionExtra + calculateExtrasTotal(state)
    }

    private fun calculateCalories(state: ProductCardUiState): Int {
        val selectedOption = state.options.firstOrNull { it.optionId == state.selectedOptionId }
        val optionCalories = selectedOption?.estimatedCalories ?: 0
        val extrasCalories = state.addOns
            .filter { state.selectedAddOnIds.contains(it.addOnId) }
            .sumOf { it.estimatedCalories }

        return optionCalories + extrasCalories
    }

    private fun buildAllergensText(state: ProductCardUiState): String {
        val allergenNames = (
                state.baseAllergens.map { it.name } +
                        state.addOns.flatMap { addOn ->
                            if (state.selectedAddOnIds.contains(addOn.addOnId)) {
                                state.addOnAllergens[addOn.addOnId].orEmpty().map { it.name }
                            } else {
                                emptyList()
                            }
                        }
                ).distinct().sorted()

        return if (allergenNames.isEmpty()) {
            "No allergens listed"
        } else {
            allergenNames.joinToString(", ")
        }
    }

    private fun buildStandardSizeText(state: ProductCardUiState): String {
        val option = state.options.firstOrNull { it.isDefault } ?: state.options.firstOrNull()

        return if (option == null) {
            "Loading..."
        } else {
            buildString {
                append(option.displayLabel)
                append(" • ")
                append(option.sizeValue)
                append(option.sizeUnit.lowercase(Locale.UK))
            }
        }
    }
}