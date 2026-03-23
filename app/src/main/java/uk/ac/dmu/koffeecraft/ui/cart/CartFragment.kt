package uk.ac.dmu.koffeecraft.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import java.util.Locale

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var vm: CartViewModel
    private lateinit var adapter: CartAdapter
    private lateinit var tvTotalValue: TextView
    private lateinit var tvBeans: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCheckout: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CartViewModelFactory(appContainer.cartRepository)
        )[CartViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvCart)
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvBeans = view.findViewById(R.id.tvBeans)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        val btnBackToMenu = view.findViewById<MaterialButton>(R.id.btnBackToMenu)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)

        adapter = CartAdapter(
            items = emptyList(),
            onPlus = { item ->
                vm.addOne(item)
            },
            onMinus = { item ->
                vm.removeOne(item.lineKey)
            }
        )
        rv.adapter = adapter

        btnCheckout.setOnClickListener {
            if (vm.state.value.isEmpty) return@setOnClickListener
            findNavController().navigate(R.id.action_cart_to_checkout)
        }

        btnBackToMenu.setOnClickListener {
            findNavController().navigateUp()
        }

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: CartUiState) {
        adapter.submitList(state.items)
        tvTotalValue.text = String.format(Locale.UK, "£%.2f", state.total)

        if (state.beansToSpend > 0) {
            tvBeans.visibility = View.VISIBLE
            tvBeans.text = "Beans to spend: ${state.beansToSpend}"
        } else {
            tvBeans.visibility = View.GONE
        }

        tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE

        btnCheckout.isEnabled = !state.isEmpty
        btnCheckout.alpha = if (state.isEmpty) 0.55f else 1f
    }
}