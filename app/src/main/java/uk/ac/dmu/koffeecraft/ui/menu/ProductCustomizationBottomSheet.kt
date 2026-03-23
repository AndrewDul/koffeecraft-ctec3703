package uk.ac.dmu.koffeecraft.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class ProductCustomizationBottomSheet : BottomSheetDialogFragment(R.layout.sheet_product_customize) {

    private var productId: Long = 0L
    private var rewardMode: Boolean = false
    private var rewardType: String? = null
    private var rewardBeansCost: Int = 0

    private lateinit var viewModel: ProductCustomizationViewModel

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var chipGroupOptions: ChipGroup
    private lateinit var chipGroupAddOns: ChipGroup
    private lateinit var tvAllergens: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnSavePreset: MaterialButton
    private lateinit var btnAddToCart: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = requireArguments().getLong(ARG_PRODUCT_ID)
        rewardMode = requireArguments().getBoolean(ARG_REWARD_MODE, false)
        rewardType = requireArguments().getString(ARG_REWARD_TYPE)
        rewardBeansCost = requireArguments().getInt(ARG_REWARD_BEANS_COST, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = ViewModelProvider(
            this,
            ProductCustomizationViewModel.Factory(
                appContainer.productCustomizationRepository,
                appContainer.sessionRepository
            )
        )[ProductCustomizationViewModel::class.java]

        tvTitle = view.findViewById(R.id.tvSheetTitle)
        tvDescription = view.findViewById(R.id.tvSheetDescription)
        chipGroupOptions = view.findViewById(R.id.chipGroupOptions)
        chipGroupAddOns = view.findViewById(R.id.chipGroupAddOns)
        tvAllergens = view.findViewById(R.id.tvAllergens)
        tvCalories = view.findViewById(R.id.tvCalories)
        tvTotal = view.findViewById(R.id.tvTotal)
        btnSavePreset = view.findViewById(R.id.btnSavePreset)
        btnAddToCart = view.findViewById(R.id.btnAddToCart)

        btnSavePreset.setOnClickListener {
            if (btnSavePreset.isEnabled) {
                viewModel.savePreset()
            }
        }

        btnAddToCart.setOnClickListener {
            viewModel.addToCart()
        }

        observeState()

        viewModel.start(
            productId = productId,
            rewardMode = rewardMode,
            rewardType = rewardType,
            rewardBeansCost = rewardBeansCost
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvTitle.text = state.title
                tvDescription.text = state.description
                tvAllergens.text = state.allergensText
                tvCalories.text = state.caloriesText
                tvTotal.text = state.totalText

                btnSavePreset.visibility = if (state.savePresetVisible) View.VISIBLE else View.GONE
                btnSavePreset.text = "Save as favourite combo"
                btnSavePreset.isEnabled = state.savePresetEnabled
                btnSavePreset.alpha = if (state.savePresetEnabled) 1f else 0.45f

                btnAddToCart.text = state.addToCartText

                bindOptions(state.optionItems)
                bindAddOns(state.addOnItems)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is ProductCustomizationViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    ProductCustomizationViewModel.UiEffect.Dismiss -> {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun bindOptions(items: List<ProductCustomizationOptionUi>) {
        chipGroupOptions.removeAllViews()

        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                isCheckable = true
                isCheckedIconVisible = false
                text = item.label
                isChecked = item.selected
                styleChoiceChip(this)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        viewModel.selectOption(item.optionId)
                    }
                }
            }
            chipGroupOptions.addView(chip)
        }
    }

    private fun bindAddOns(items: List<ProductCustomizationAddOnUi>) {
        chipGroupAddOns.removeAllViews()

        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                isCheckable = true
                isCheckedIconVisible = false
                text = item.label
                isChecked = item.selected
                styleChoiceChip(this)

                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.toggleAddOn(item.addOnId, isChecked)
                }
            }
            chipGroupAddOns.addView(chip)
        }
    }

    private fun styleChoiceChip(chip: Chip) {
        val density = resources.displayMetrics.density

        chip.chipBackgroundColor =
            AppCompatResources.getColorStateList(requireContext(), R.color.kc_chip_bg_selector)
        chip.chipStrokeColor =
            AppCompatResources.getColorStateList(requireContext(), R.color.kc_chip_stroke_selector)
        chip.setTextColor(
            AppCompatResources.getColorStateList(requireContext(), R.color.kc_chip_text_selector)
        )

        chip.chipStrokeWidth = 1.2f * density
        chip.chipCornerRadius = 16f * density
        chip.minHeight = (42f * density).toInt()
        chip.textStartPadding = 14f * density
        chip.textEndPadding = 14f * density
        chip.chipStartPadding = 2f * density
        chip.chipEndPadding = 2f * density
        chip.rippleColor =
            AppCompatResources.getColorStateList(requireContext(), R.color.kc_chip_ripple_selector)
        chip.setEnsureMinTouchTargetSize(false)
    }

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"
        private const val ARG_REWARD_MODE = "reward_mode"
        private const val ARG_REWARD_TYPE = "reward_type"
        private const val ARG_REWARD_BEANS_COST = "reward_beans_cost"

        fun newInstance(productId: Long): ProductCustomizationBottomSheet {
            return ProductCustomizationBottomSheet().apply {
                arguments = bundleOf(ARG_PRODUCT_ID to productId)
            }
        }

        fun newRewardInstance(
            productId: Long,
            rewardType: String,
            beansCost: Int
        ): ProductCustomizationBottomSheet {
            return ProductCustomizationBottomSheet().apply {
                arguments = bundleOf(
                    ARG_PRODUCT_ID to productId,
                    ARG_REWARD_MODE to true,
                    ARG_REWARD_TYPE to rewardType,
                    ARG_REWARD_BEANS_COST to beansCost
                )
            }
        }
    }
}