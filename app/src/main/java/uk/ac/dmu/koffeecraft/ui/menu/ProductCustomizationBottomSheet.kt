package uk.ac.dmu.koffeecraft.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

class ProductCustomizationBottomSheet : BottomSheetDialogFragment(R.layout.sheet_product_customize) {

    private var productId: Long = -1L

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var chipGroupOptions: ChipGroup
    private lateinit var chipGroupAddOns: ChipGroup
    private lateinit var tvAllergens: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnAddToCart: MaterialButton

    private lateinit var product: Product
    private var options: List<ProductOption> = emptyList()
    private var addOns: List<AddOn> = emptyList()
    private var baseAllergens: List<Allergen> = emptyList()
    private var addOnAllergens: Map<Long, List<Allergen>> = emptyMap()

    private var selectedOption: ProductOption? = null
    private val selectedAddOnIds = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = requireArguments().getLong(ARG_PRODUCT_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvSheetTitle)
        tvDescription = view.findViewById(R.id.tvSheetDescription)
        chipGroupOptions = view.findViewById(R.id.chipGroupOptions)
        chipGroupAddOns = view.findViewById(R.id.chipGroupAddOns)
        tvAllergens = view.findViewById(R.id.tvAllergens)
        tvCalories = view.findViewById(R.id.tvCalories)
        tvTotal = view.findViewById(R.id.tvTotal)
        btnAddToCart = view.findViewById(R.id.btnAddToCart)

        loadData()
    }

    private fun loadData() {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val loadedProduct = db.productDao().getById(productId)
            val loadedOptions = db.productOptionDao().getForProduct(productId)
            val loadedAddOns = db.addOnDao().getActiveForProduct(productId)
            val loadedBaseAllergens = db.allergenDao().getForProduct(productId)
            val loadedAddOnAllergens = loadedAddOns.associate { addOn ->
                addOn.addOnId to db.allergenDao().getForAddOn(addOn.addOnId)
            }

            withContext(Dispatchers.Main) {
                if (!isAdded || loadedProduct == null) return@withContext

                product = loadedProduct
                options = loadedOptions
                addOns = loadedAddOns
                baseAllergens = loadedBaseAllergens
                addOnAllergens = loadedAddOnAllergens

                bindUi()
            }
        }
    }

    private fun bindUi() {
        tvTitle.text = product.name
        tvDescription.text = product.description

        chipGroupOptions.removeAllViews()
        chipGroupAddOns.removeAllViews()

        selectedOption = options.firstOrNull { it.isDefault } ?: options.firstOrNull()

        options.forEach { option ->
            val chip = Chip(requireContext()).apply {
                isCheckable = true
                isCheckedIconVisible = false
                text = buildString {
                    append(option.displayLabel)
                    append(" • ")
                    append(option.sizeValue)
                    append(option.sizeUnit.lowercase())
                    if (option.extraPrice > 0.0) {
                        append(" (+£")
                        append(String.format("%.2f", option.extraPrice))
                        append(")")
                    }
                }
                isChecked = selectedOption?.optionId == option.optionId
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedOption = option
                        recalculate()
                    }
                }
            }
            chipGroupOptions.addView(chip)
        }

        addOns.forEach { addOn ->
            val chip = Chip(requireContext()).apply {
                isCheckable = true
                isCheckedIconVisible = false
                text = "${addOn.name} +£%.2f".format(addOn.price)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedAddOnIds.add(addOn.addOnId)
                    } else {
                        selectedAddOnIds.remove(addOn.addOnId)
                    }
                    recalculate()
                }
            }
            chipGroupAddOns.addView(chip)
        }

        btnAddToCart.setOnClickListener {
            val option = selectedOption
            if (option == null) {
                Toast.makeText(requireContext(), "Please choose a size first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedAddOns = addOns.filter { selectedAddOnIds.contains(it.addOnId) }

            CartManager.addCustomisedProduct(
                product = product,
                option = option,
                addOns = selectedAddOns
            )

            Toast.makeText(requireContext(), "Product added to cart.", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        recalculate()
    }

    private fun recalculate() {
        val option = selectedOption ?: return
        val selectedAddOns = addOns.filter { selectedAddOnIds.contains(it.addOnId) }

        val totalPrice = product.price + option.extraPrice + selectedAddOns.sumOf { it.price }
        val totalCalories = option.estimatedCalories + selectedAddOns.sumOf { it.estimatedCalories }

        val allergenNames = (
                baseAllergens.map { it.name } +
                        selectedAddOns.flatMap { addOn ->
                            addOnAllergens[addOn.addOnId].orEmpty().map { it.name }
                        }
                ).distinct().sorted()

        tvTotal.text = "Total: £%.2f".format(totalPrice)
        tvCalories.text = "Estimated calories: $totalCalories kcal"
        tvAllergens.text = if (allergenNames.isEmpty()) {
            "Allergens: No allergens listed"
        } else {
            "Allergens: ${allergenNames.joinToString(", ")}"
        }
    }

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"

        fun newInstance(productId: Long): ProductCustomizationBottomSheet {
            return ProductCustomizationBottomSheet().apply {
                arguments = bundleOf(ARG_PRODUCT_ID to productId)
            }
        }
    }
}