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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import java.util.Locale

class ProductAdapter(
    private val scope: CoroutineScope,
    private val db: KoffeeCraftDatabase,
    private val appContext: android.content.Context,
    private var items: List<Product>,
    private var favouriteIds: Set<Long>,
    private val onFavouriteToggle: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    data class ProductCardState(
        val options: List<ProductOption> = emptyList(),
        val addOns: List<AddOn> = emptyList(),
        val baseAllergens: List<Allergen> = emptyList(),
        val addOnAllergens: Map<Long, List<Allergen>> = emptyMap(),
        val selectedOptionId: Long? = null,
        val selectedAddOnIds: Set<Long> = emptySet(),
        val isLoaded: Boolean = false,
        val isLoading: Boolean = false
    )

    private val stateByProductId = mutableMapOf<Long, ProductCardState>()
    private var expandedProductId: Long? = null

    fun submitList(newItems: List<Product>) {
        items = newItems
        if (expandedProductId != null && items.none { it.productId == expandedProductId }) {
            expandedProductId = null
        }
        newItems.forEach { ensureLoaded(it.productId) }
        notifyDataSetChanged()
    }

    fun updateFavouriteIds(newFavouriteIds: Set<Long>) {
        favouriteIds = newFavouriteIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = items[position]
        val state = stateByProductId[product.productId] ?: ProductCardState()
        val expanded = expandedProductId == product.productId

        holder.bind(
            product = product,
            isFavourite = favouriteIds.contains(product.productId),
            expanded = expanded,
            state = state,
            onToggleExpand = { toggleExpand(product.productId) },
            onFavouriteToggle = { shouldFavourite ->
                onFavouriteToggle(product, shouldFavourite)
            },
            onOptionSelected = { optionId ->
                updateSelectedOption(product.productId, optionId)
            },
            onAddOnToggled = { addOnId, checked ->
                updateSelectedAddOn(product.productId, addOnId, checked)
            },
            onSavePreset = {
                saveCurrentConfiguration(product)
            },
            onAddToCart = {
                addCurrentConfigurationToCart(product)
            }
        )
    }

    private fun toggleExpand(productId: Long) {
        val previousExpanded = expandedProductId
        expandedProductId = if (expandedProductId == productId) null else productId

        ensureLoaded(productId)

        val previousIndex = items.indexOfFirst { it.productId == previousExpanded }
        if (previousIndex != -1) notifyItemChanged(previousIndex)

        val newIndex = items.indexOfFirst { it.productId == expandedProductId }
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    private fun updateSelectedOption(productId: Long, optionId: Long) {
        val state = stateByProductId[productId] ?: return
        stateByProductId[productId] = state.copy(selectedOptionId = optionId)
        notifyProductChanged(productId)
    }

    private fun updateSelectedAddOn(productId: Long, addOnId: Long, checked: Boolean) {
        val state = stateByProductId[productId] ?: return
        val newIds = state.selectedAddOnIds.toMutableSet()

        if (checked) {
            newIds.add(addOnId)
        } else {
            newIds.remove(addOnId)
        }

        stateByProductId[productId] = state.copy(selectedAddOnIds = newIds)
        notifyProductChanged(productId)
    }

    private fun ensureLoaded(productId: Long) {
        val currentState = stateByProductId[productId]
        if (currentState?.isLoaded == true || currentState?.isLoading == true) return

        stateByProductId[productId] = (currentState ?: ProductCardState()).copy(isLoading = true)

        scope.launch(Dispatchers.IO) {
            val options = db.productOptionDao().getForProduct(productId)
            val addOns = db.addOnDao().getActiveForProduct(productId)
            val baseAllergens = db.allergenDao().getForProduct(productId)
            val addOnAllergens = addOns.associate { addOn ->
                addOn.addOnId to db.allergenDao().getForAddOn(addOn.addOnId)
            }

            val previousState = stateByProductId[productId] ?: ProductCardState()
            val defaultOptionId = options.firstOrNull { it.isDefault }?.optionId ?: options.firstOrNull()?.optionId

            val resolvedSelectedOptionId = when {
                previousState.selectedOptionId != null && options.any { it.optionId == previousState.selectedOptionId } ->
                    previousState.selectedOptionId

                else -> defaultOptionId
            }

            val resolvedAddOnIds = previousState.selectedAddOnIds.intersect(addOns.map { it.addOnId }.toSet())

            val newState = ProductCardState(
                options = options,
                addOns = addOns,
                baseAllergens = baseAllergens,
                addOnAllergens = addOnAllergens,
                selectedOptionId = resolvedSelectedOptionId,
                selectedAddOnIds = resolvedAddOnIds,
                isLoaded = true,
                isLoading = false
            )

            withContext(Dispatchers.Main) {
                stateByProductId[productId] = newState
                notifyProductChanged(productId)
            }
        }
    }

    private fun saveCurrentConfiguration(product: Product) {
        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            android.widget.Toast.makeText(
                appContext,
                "Please sign in first.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val state = stateByProductId[product.productId] ?: return
        val option = state.options.firstOrNull { it.optionId == state.selectedOptionId } ?: return
        if (!shouldEnableSavePresetButton(product, state)) return

        val selectedAddOns = state.addOns.filter { state.selectedAddOnIds.contains(it.addOnId) }
        val totalPrice = calculateTotal(product, state)
        val totalCalories = calculateCalories(state)

        scope.launch(Dispatchers.IO) {
            val presetId = db.customerFavouritePresetDao().insertPreset(
                CustomerFavouritePreset(
                    customerId = customerId,
                    productId = product.productId,
                    optionId = option.optionId,
                    totalPriceSnapshot = totalPrice,
                    totalCaloriesSnapshot = totalCalories
                )
            )

            if (selectedAddOns.isNotEmpty()) {
                db.customerFavouritePresetDao().insertAddOnRefs(
                    selectedAddOns.map { addOn ->
                        CustomerFavouritePresetAddOnCrossRef(
                            presetId = presetId,
                            addOnId = addOn.addOnId
                        )
                    }
                )
            }

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    appContext,
                    "Favourite combo saved.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addCurrentConfigurationToCart(product: Product) {
        val state = stateByProductId[product.productId] ?: return
        val option = state.options.firstOrNull { it.optionId == state.selectedOptionId } ?: return
        val selectedAddOns = state.addOns.filter { state.selectedAddOnIds.contains(it.addOnId) }

        CartManager.addCustomisedProduct(
            product = product,
            option = option,
            addOns = selectedAddOns
        )

        android.widget.Toast.makeText(
            appContext,
            "Product added to cart.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun notifyProductChanged(productId: Long) {
        val index = items.indexOfFirst { it.productId == productId }
        if (index != -1) notifyItemChanged(index)
    }

    private fun defaultOptionId(state: ProductCardState): Long? {
        return state.options.firstOrNull { it.isDefault }?.optionId ?: state.options.firstOrNull()?.optionId
    }

    private fun shouldEnableSavePresetButton(product: Product, state: ProductCardState): Boolean {
        if (!product.isActive) return false
        if (SessionManager.currentCustomerId == null) return false

        val defaultOptionId = defaultOptionId(state)
        val sizeChanged = state.selectedOptionId != null && state.selectedOptionId != defaultOptionId
        val hasAddOns = state.selectedAddOnIds.isNotEmpty()

        return sizeChanged || hasAddOns
    }

    private fun calculateExtrasTotal(state: ProductCardState): Double {
        return state.addOns
            .filter { state.selectedAddOnIds.contains(it.addOnId) }
            .sumOf { it.price }
    }

    private fun calculateTotal(product: Product, state: ProductCardState): Double {
        val selectedOption = state.options.firstOrNull { it.optionId == state.selectedOptionId }
        val optionExtra = selectedOption?.extraPrice ?: 0.0
        return product.price + optionExtra + calculateExtrasTotal(state)
    }

    private fun calculateCalories(state: ProductCardState): Int {
        val selectedOption = state.options.firstOrNull { it.optionId == state.selectedOptionId }
        val optionCalories = selectedOption?.estimatedCalories ?: 0
        val extrasCalories = state.addOns
            .filter { state.selectedAddOnIds.contains(it.addOnId) }
            .sumOf { it.estimatedCalories }

        return optionCalories + extrasCalories
    }

    private fun buildAllergensText(state: ProductCardState): String {
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

    private fun buildStandardSizeText(state: ProductCardState): String {
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

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)
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
            state: ProductCardState,
            onToggleExpand: () -> Unit,
            onFavouriteToggle: (Boolean) -> Unit,
            onOptionSelected: (Long) -> Unit,
            onAddOnToggled: (Long, Boolean) -> Unit,
            onSavePreset: () -> Unit,
            onAddToCart: () -> Unit
        ) {
            tvName.text = product.name
            tvDesc.text = product.description
            tvStandardSize.text = "Standard size • ${buildStandardSizeText(state)}"
            tvPrice.text = String.format(Locale.UK, "From £%.2f", product.price)

            val isAvailable = product.isActive
            cardRoot.alpha = if (isAvailable) 1f else 0.72f
            tvUnavailable.visibility = if (isAvailable) View.GONE else View.VISIBLE

            tvFavourite.text = if (isFavourite) "♥" else "♡"
            tvFavourite.setTextColor(
                if (isFavourite) color(R.color.kc_danger) else color(R.color.kc_text_secondary)
            )

            dividerExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
            layoutExpandedContent.visibility = if (expanded) View.VISIBLE else View.GONE

            if (expanded) {
                bindExpandedState(product, state, isAvailable, onOptionSelected, onAddOnToggled, onSavePreset, onAddToCart)
            }

            cardRoot.setOnClickListener { onToggleExpand() }

            tvFavourite.setOnClickListener {
                animateFavourite()
                onFavouriteToggle(!isFavourite)
            }
        }

        private fun bindExpandedState(
            product: Product,
            state: ProductCardState,
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

            val saveEnabled = shouldEnableSavePresetButton(product, state)
            tvSavePreset.alpha = if (saveEnabled) 1f else 0.45f
            tvSavePreset.setTextColor(
                if (saveEnabled) color(R.color.kc_success) else color(R.color.kc_text_muted)
            )
            tvSavePreset.setOnClickListener {
                if (saveEnabled) onSavePreset()
            }

            btnAddToCart.isEnabled = isAvailable
            btnAddToCart.alpha = if (isAvailable) 1f else 0.55f
            btnAddToCart.text = if (isAvailable) "Add to cart" else "Unavailable"
            btnAddToCart.setOnClickListener {
                if (isAvailable) onAddToCart()
            }
        }

        private fun bindOptionChips(
            state: ProductCardState,
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
            state: ProductCardState,
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

            chip.chipStrokeWidth = 1.2f * density
            chip.chipCornerRadius = 16f * density
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
}