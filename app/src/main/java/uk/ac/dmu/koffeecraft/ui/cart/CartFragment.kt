package uk.ac.dmu.koffeecraft.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var adapter: CartAdapter
    private lateinit var tvTotalValue: TextView
    private lateinit var tvBeans: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCheckout: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvCart)
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvBeans = view.findViewById(R.id.tvBeans)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        val btnBackToMenu = view.findViewById<MaterialButton>(R.id.btnBackToMenu)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)

        adapter = CartAdapter(
            items = CartManager.getItems(),
            onPlus = {
                CartManager.addExisting(it)
                refresh()
            },
            onMinus = {
                CartManager.removeOne(it.lineKey)
                refresh()
            }
        )
        rv.adapter = adapter

        refresh()

        btnCheckout.setOnClickListener {
            if (CartManager.getItems().isEmpty()) return@setOnClickListener
            findNavController().navigate(R.id.action_cart_to_checkout)
        }

        btnBackToMenu.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun refresh() {
        val items = CartManager.getItems()
        adapter.submitList(items)

        val total = CartManager.total()
        val beansToSpend = CartManager.beansToSpend()

        tvTotalValue.text = String.format("£%.2f", total)

        if (beansToSpend > 0) {
            tvBeans.visibility = View.VISIBLE
            tvBeans.text = "Beans to spend: $beansToSpend"
        } else {
            tvBeans.visibility = View.GONE
        }

        val isEmpty = items.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

        btnCheckout.isEnabled = !isEmpty
        btnCheckout.alpha = if (isEmpty) 0.55f else 1f
    }
}