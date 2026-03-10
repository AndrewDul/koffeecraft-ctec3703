package uk.ac.dmu.koffeecraft.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var adapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvCart)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)
        val btnCheckout = view.findViewById<Button>(R.id.btnCheckout)
        val btnBackToMenu = view.findViewById<Button>(R.id.btnBackToMenu)

        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = CartAdapter(
            items = CartManager.getItems(),
            onPlus = { CartManager.add(it.product); refresh(tvTotal) },
            onMinus = { CartManager.removeOne(it.product.productId); refresh(tvTotal) }
        )
        rv.adapter = adapter

        refresh(tvTotal)

        btnCheckout.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_checkout)
        }

        btnBackToMenu.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun refresh(tvTotal: TextView) {
        adapter.submitList(CartManager.getItems())
        tvTotal.text = "Total: £%.2f".format(CartManager.total())
    }
}