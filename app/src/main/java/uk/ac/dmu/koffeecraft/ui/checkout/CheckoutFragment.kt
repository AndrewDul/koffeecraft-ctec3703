package uk.ac.dmu.koffeecraft.ui.checkout

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
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
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager
class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private var selectedPaymentType: String = "CARD"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTotalValue = view.findViewById<TextView>(R.id.tvTotalValue)
        val tvBeans = view.findViewById<TextView>(R.id.tvBeans)
        val tvPaymentCard = view.findViewById<TextView>(R.id.tvPaymentCard)
        val tvPaymentCash = view.findViewById<TextView>(R.id.tvPaymentCash)
        val btnPay = view.findViewById<MaterialButton>(R.id.btnPay)
        val btnBackToCart = view.findViewById<MaterialButton>(R.id.btnBackToCart)

        val initialTotal = CartManager.total()
        val initialBeansToSpend = CartManager.beansToSpend()

        tvTotalValue.text = String.format("£%.2f", initialTotal)
        if (initialBeansToSpend > 0) {
            tvBeans.visibility = View.VISIBLE
            tvBeans.text = "Beans to spend: $initialBeansToSpend"
        } else {
            tvBeans.visibility = View.GONE
        }

        updatePaymentSelectionUi(tvPaymentCard, tvPaymentCash)

        tvPaymentCard.setOnClickListener {
            selectedPaymentType = "CARD"
            updatePaymentSelectionUi(tvPaymentCard, tvPaymentCash)
        }

        tvPaymentCash.setOnClickListener {
            selectedPaymentType = "CASH"
            updatePaymentSelectionUi(tvPaymentCard, tvPaymentCash)
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

                val orderId = repo.placeOrder(
                    customerId = customerId,
                    items = cartItems,
                    paymentType = selectedPaymentType,
                    totalAmount = total
                )

                val updatedBeansBalance = customer.beansBalance - beansToSpend + beansToEarn

                val boosterState = BeansBoosterManager.applyEarnedBeans(
                    currentProgress = customer.beansBoosterProgress,
                    currentPendingBoosters = customer.pendingBeansBoosters,
                    earnedBeans = beansToEarn
                )

                db.customerDao().update(
                    customer.copy(
                        beansBalance = updatedBeansBalance,
                        beansBoosterProgress = boosterState.progress,
                        pendingBeansBoosters = boosterState.pendingBoosters
                    )
                )

                CartManager.clear()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Payment successful.",
                        Toast.LENGTH_SHORT
                    ).show()

                    NotificationHelper.showCustomerOrderNotification(
                        context = requireContext(),
                        title = "Payment confirmed",
                        message = "Your order #$orderId has been placed successfully.",
                        notificationId = (orderId % Int.MAX_VALUE).toInt(),
                        orderId = orderId
                    )

                    val simulate = SimulationSettings.isEnabled(requireContext())

                    findNavController().navigate(
                        R.id.action_checkout_to_status,
                        bundleOf(
                            "orderId" to orderId,
                            "simulate" to simulate,
                            "fromCheckout" to true
                        )
                    )
                }
            }
        }
    }

    private fun updatePaymentSelectionUi(cardView: TextView, cashView: TextView) {
        val selectedBg = R.drawable.bg_orders_filter_chip_selected
        val unselectedBg = R.drawable.bg_orders_filter_chip

        if (selectedPaymentType == "CARD") {
            cardView.setBackgroundResource(selectedBg)
            cashView.setBackgroundResource(unselectedBg)

            cardView.setTextColor(Color.parseColor("#2E2018"))
            cashView.setTextColor(Color.parseColor("#6E5A4D"))
        } else {
            cashView.setBackgroundResource(selectedBg)
            cardView.setBackgroundResource(unselectedBg)

            cashView.setTextColor(Color.parseColor("#2E2018"))
            cardView.setTextColor(Color.parseColor("#6E5A4D"))
        }
    }
}