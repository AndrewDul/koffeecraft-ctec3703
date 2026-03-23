package uk.ac.dmu.koffeecraft.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var vm: MenuViewModel
    private lateinit var adapter: ProductAdapter

    private lateinit var tvFilterCoffee: TextView
    private lateinit var tvFilterCake: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuRepository = appContainer.menuRepository
        val sessionRepository = appContainer.sessionRepository

        vm = ViewModelProvider(
            this,
            MenuViewModelFactory(menuRepository, sessionRepository)
        )[MenuViewModel::class.java]

        tvFilterCoffee = view.findViewById(R.id.tvFilterCoffee)
        tvFilterCake = view.findViewById(R.id.tvFilterCake)

        val rv = view.findViewById<RecyclerView>(R.id.rvProducts)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = ProductAdapter(
            items = emptyList(),
            favouriteIds = emptySet(),
            cardStates = emptyMap(),
            expandedProductId = null,
            onToggleExpand = { productId ->
                vm.toggleExpand(productId)
            },
            onFavouriteToggle = { product, shouldFavourite ->
                vm.onFavouriteToggle(product, shouldFavourite)
            },
            onOptionSelected = { productId, optionId ->
                vm.onOptionSelected(productId, optionId)
            },
            onAddOnToggled = { productId, addOnId, checked ->
                vm.onAddOnToggled(productId, addOnId, checked)
            },
            onSavePreset = { product ->
                vm.onSavePreset(product)
            },
            onAddToCart = { product ->
                vm.onAddToCart(product)
            }
        )

        rv.adapter = adapter

        tvFilterCoffee.setOnClickListener { vm.setCategory("COFFEE") }
        tvFilterCake.setOnClickListener { vm.setCategory("CAKE") }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collectLatest { state ->
                adapter.submitData(
                    newItems = state.products,
                    newFavouriteIds = state.favouriteIds,
                    newCardStates = state.cardStates,
                    newExpandedProductId = state.expandedProductId
                )
                renderCategoryChips(state.category)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collectLatest { effect ->
                when (effect) {
                    is MenuViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        vm.start()
    }

    private fun renderCategoryChips(currentCategory: String) {
        styleFilterChip(tvFilterCoffee, currentCategory == "COFFEE")
        styleFilterChip(tvFilterCake, currentCategory == "CAKE")
    }

    private fun styleFilterChip(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip_selected)
            view.setTextColor(color(R.color.kc_text_primary))
        } else {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip)
            view.setTextColor(color(R.color.kc_text_secondary))
        }
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}