package uk.ac.dmu.koffeecraft.ui.rewards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.ui.menu.ProductCustomizationBottomSheet

class RewardProductPickerBottomSheet : BottomSheetDialogFragment(R.layout.sheet_reward_product_picker) {

    private var category: String = ""
    private var rewardType: String = ""
    private var beansCost: Int = 0

    private lateinit var tvPickerTitle: TextView
    private lateinit var tvPickerSubtitle: TextView
    private lateinit var rvPicker: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = requireArguments().getString(ARG_CATEGORY).orEmpty()
        rewardType = requireArguments().getString(ARG_REWARD_TYPE).orEmpty()
        beansCost = requireArguments().getInt(ARG_BEANS_COST)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvPickerTitle = view.findViewById(R.id.tvPickerTitle)
        tvPickerSubtitle = view.findViewById(R.id.tvPickerSubtitle)
        rvPicker = view.findViewById(R.id.rvRewardProductPicker)

        tvPickerTitle.text = if (category.equals("COFFEE", ignoreCase = true)) {
            "Choose your free coffee"
        } else {
            "Choose your free cake"
        }

        tvPickerSubtitle.text =
            "Pick one reward item and customise it. The base item is free — you only pay for size upgrades and extras."

        rvPicker.layoutManager = LinearLayoutManager(requireContext())

        loadProducts()
    }

    private fun loadProducts() {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val products = db.productDao().getAvailableByCategory(category)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (products.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No products are available in this category right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                    return@withContext
                }

                rvPicker.adapter = RewardProductPickerAdapter(products) { selected ->
                    dismiss()
                    ProductCustomizationBottomSheet.newRewardInstance(
                        productId = selected.productId,
                        rewardType = rewardType,
                        beansCost = beansCost
                    ).show(parentFragmentManager, "reward_customize")
                }
            }
        }
    }

    companion object {
        private const val ARG_CATEGORY = "category"
        private const val ARG_REWARD_TYPE = "reward_type"
        private const val ARG_BEANS_COST = "beans_cost"

        fun newInstance(
            category: String,
            rewardType: String,
            beansCost: Int
        ): RewardProductPickerBottomSheet {
            return RewardProductPickerBottomSheet().apply {
                arguments = bundleOf(
                    ARG_CATEGORY to category,
                    ARG_REWARD_TYPE to rewardType,
                    ARG_BEANS_COST to beansCost
                )
            }
        }
    }
}