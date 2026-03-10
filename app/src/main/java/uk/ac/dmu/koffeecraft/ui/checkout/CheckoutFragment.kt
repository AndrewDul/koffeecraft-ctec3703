package uk.ac.dmu.koffeecraft.ui.checkout

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.OrderRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)
        val rbCard = view.findViewById<RadioButton>(R.id.rbCard)
        val btnPay = view.findViewById<Button>(R.id.btnPay)
        val btnBackToCart = view.findViewById<Button>(R.id.btnBackToCart)

        tvTotal.text = "Total: £%.2f".format(CartManager.total())

        btnBackToCart.setOnClickListener {
            findNavController().navigateUp()
        }

        btnPay.setOnClickListener {
            val customerId = SessionManager.currentCustomerId
            if (customerId == null) {
                Toast.makeText(requireContext(), "Not logged in as customer.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentType = if (rbCard.isChecked) "CARD" else "CASH"
            val total = CartManager.total()
            val cartItems = CartManager.getItems()

            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
            val repo = OrderRepository(db)

            lifecycleScope.launch(Dispatchers.IO) {
                val items = cartItems.map {
                    it.product.productId to (it.quantity to it.product.price)
                }

                val orderId = repo.placeOrder(
                    customerId = customerId,
                    items = items,
                    paymentType = paymentType,
                    totalAmount = total
                )

                CartManager.clear()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Payment successful (simulated).", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_checkout_to_status,
                        bundleOf("orderId" to orderId)
                    )
                }
            }
        }
    }
}