package uk.ac.dmu.koffeecraft.ui.favourites

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.ui.menu.ProductCustomizationBottomSheet

class CustomerFavouritesFragment : Fragment(R.layout.fragment_customer_favourites) {

    private lateinit var viewModel: CustomerFavouritesViewModel

    private lateinit var presetAdapter: CustomerFavouritePresetAdapter
    private lateinit var standardAdapter: StandardFavouriteAdapter

    private lateinit var tvEmpty: TextView
    private lateinit var tvPresetSection: TextView
    private lateinit var tvStandardSection: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            CustomerFavouritesViewModel.Factory(
                repository = appContainer.customerFavouritesRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[CustomerFavouritesViewModel::class.java]

        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvPresetSection = view.findViewById(R.id.tvPresetSection)
        tvStandardSection = view.findViewById(R.id.tvStandardSection)

        val rvPresets = view.findViewById<RecyclerView>(R.id.rvFavouritePresets)
        val rvProducts = view.findViewById<RecyclerView>(R.id.rvFavouriteProducts)

        rvPresets.layoutManager = LinearLayoutManager(requireContext())
        rvProducts.layoutManager = LinearLayoutManager(requireContext())
        rvPresets.isNestedScrollingEnabled = false
        rvProducts.isNestedScrollingEnabled = false
        rvPresets.setHasFixedSize(false)
        rvProducts.setHasFixedSize(false)

        presetAdapter = CustomerFavouritePresetAdapter(
            items = emptyList(),
            onRemove = { preset ->
                viewModel.removePreset(preset.presetId)
            },
            onBuyAgain = { preset ->
                viewModel.buyPresetAgain(preset.presetId)
            }
        )

        standardAdapter = StandardFavouriteAdapter(
            items = emptyList(),
            onRemove = { card ->
                viewModel.removeStandardFavourite(card.productId)
            },
            onCustomize = { card ->
                ProductCustomizationBottomSheet.newInstance(card.productId)
                    .show(parentFragmentManager, "product_customize")
            },
            onBuyAgain = { card ->
                viewModel.buyStandardFavouriteAgain(card.productId)
            }
        )

        rvPresets.adapter = presetAdapter
        rvProducts.adapter = standardAdapter

        observeState()
        viewModel.start()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                presetAdapter.submitList(state.presets)
                standardAdapter.submitList(state.standardProducts)

                tvPresetSection.visibility = if (state.showPresetSection) View.VISIBLE else View.GONE
                tvStandardSection.visibility = if (state.showStandardSection) View.VISIBLE else View.GONE
                tvEmpty.visibility = if (state.showEmpty) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is CustomerFavouritesViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}