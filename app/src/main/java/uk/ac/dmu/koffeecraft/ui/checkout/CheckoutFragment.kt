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
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)
        val rbCard = view.findViewById<RadioButton>(R.id.rbCard)
        val btnPay = view.findViewById<Button>(R.id.btnPay)
        val btnBackToCart = view.findViewById<Button>(R.id.btnBackToCart)

        val initialTotal = CartManager.total()
        val initialBeansToSpend = CartManager.beansToSpend()

        tvTotal.text = if (initialBeansToSpend > 0) {
            "Total: £%.2f\nBeans to spend: %d".format(initialTotal, initialBeansToSpend)
        } else {
            "Total: £%.2f".format(initialTotal)
        }

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
            val beansToSpend = CartManager.beansToSpend()
            val beansToEarn = CartManager.purchasedProductCountForBeans()
            val cartItems = CartManager.getItems()

            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
            val repo = OrderRepository(db)

            lifecycleScope.launch(Dispatchers.IO) {
                val customer = db.customerDao().getById(customerId)
                if (customer == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Customer not found.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (customer.beansBalance < beansToSpend) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "You do not have enough beans for the selected rewards.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val items = cartItems.map {
                    it.product.productId to (it.quantity to it.product.price)
                }

                val orderId = repo.placeOrder(
                    customerId = customerId,
                    items = items,
                    paymentType = paymentType,
                    totalAmount = total
                )

                val updatedBeansBalance = customer.beansBalance - beansToSpend + beansToEarn
                db.customerDao().update(
                    customer.copy(beansBalance = updatedBeansBalance)
                )

                CartManager.clear()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Payment successful (simulated).",
                        Toast.LENGTH_SHORT
                    ).show()

                    NotificationHelper.showOrderNotification(
                        context = requireContext(),
                        title = "Payment confirmed",
                        message = "Your order #$orderId has been placed successfully.",
                        notificationId = (orderId % Int.MAX_VALUE).toInt()
                    )

                    val simulate = SimulationSettings.isEnabled(requireContext())

                    findNavController().navigate(
                        R.id.action_checkout_to_status,
                        bundleOf("orderId" to orderId, "simulate" to simulate)
                    )
                }
            }
        }
    }
}