package uk.ac.dmu.koffeecraft.ui.checkout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.repository.OrderRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private var selectedPaymentType: String = "CARD"
    private var selectedSavedCardId: Long? = null
    private var savedCards: List<CustomerPaymentCard> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Not logged in as customer.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val repo = OrderRepository(db)

        val tvTotalValue = view.findViewById<TextView>(R.id.tvTotalValue)
        val tvBeans = view.findViewById<TextView>(R.id.tvBeans)

        val tvPaymentCard = view.findViewById<TextView>(R.id.tvPaymentCard)
        val tvPaymentApplePay = view.findViewById<TextView>(R.id.tvPaymentApplePay)
        val tvPaymentCash = view.findViewById<TextView>(R.id.tvPaymentCash)

        val cardSection = view.findViewById<View>(R.id.cardSavedCardsSection)
        val applePaySection = view.findViewById<View>(R.id.cardApplePaySection)
        val cashSection = view.findViewById<View>(R.id.cardCashSection)

        val tvSavedCardsEmpty = view.findViewById<TextView>(R.id.tvSavedCardsEmpty)
        val containerSavedCards = view.findViewById<LinearLayout>(R.id.containerSavedCards)

        val tilNickname = view.findViewById<TextInputLayout>(R.id.tilCheckoutCardNickname)
        val tilHolder = view.findViewById<TextInputLayout>(R.id.tilCheckoutCardHolder)
        val tilNumber = view.findViewById<TextInputLayout>(R.id.tilCheckoutCardNumber)
        val tilExpiry = view.findViewById<TextInputLayout>(R.id.tilCheckoutCardExpiry)
        val tilCvv = view.findViewById<TextInputLayout>(R.id.tilCheckoutCardCvv)

        val etNickname = view.findViewById<TextInputEditText>(R.id.etCheckoutCardNickname)
        val etHolder = view.findViewById<TextInputEditText>(R.id.etCheckoutCardHolder)
        val etNumber = view.findViewById<TextInputEditText>(R.id.etCheckoutCardNumber)
        val etExpiry = view.findViewById<TextInputEditText>(R.id.etCheckoutCardExpiry)
        val etCvv = view.findViewById<TextInputEditText>(R.id.etCheckoutCardCvv)

        val switchSaveCard = view.findViewById<SwitchMaterial>(R.id.switchSaveCheckoutCard)

        val tvPreviewNickname = view.findViewById<TextView>(R.id.tvCheckoutPreviewCardNickname)
        val tvPreviewBrand = view.findViewById<TextView>(R.id.tvCheckoutPreviewCardBrand)
        val tvPreviewNumber = view.findViewById<TextView>(R.id.tvCheckoutPreviewCardNumber)
        val tvPreviewHolder = view.findViewById<TextView>(R.id.tvCheckoutPreviewCardHolder)
        val tvPreviewExpiry = view.findViewById<TextView>(R.id.tvCheckoutPreviewCardExpiry)

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

        tilNumber.helperText = PaymentCardValidator.demoCardExamplesText()

        fun updatePreview() {
            val brand = PaymentCardValidator.detectBrand(etNumber.text?.toString().orEmpty())
            val rawNumber = etNumber.text?.toString().orEmpty()
            val rawHolder = etHolder.text?.toString()?.trim().orEmpty()
            val rawNickname = etNickname.text?.toString()?.trim().orEmpty()
            val rawExpiry = etExpiry.text?.toString()?.trim().orEmpty()

            tvPreviewBrand.text = brand.displayName
            tvPreviewNumber.text = PaymentCardValidator.buildPreviewNumber(rawNumber)
            tvPreviewHolder.text = if (rawHolder.isBlank()) "CARDHOLDER NAME" else rawHolder.uppercase()
            tvPreviewExpiry.text = if (rawExpiry.isBlank()) "MM/YY" else rawExpiry
            tvPreviewNickname.text = if (rawNickname.isBlank()) "New checkout card" else rawNickname
        }

        var formattingNumber = false
        var formattingExpiry = false

        etNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (formattingNumber) return

                val formatted = PaymentCardValidator.formatCardNumberInput(s?.toString().orEmpty())
                if (formatted != s?.toString().orEmpty()) {
                    formattingNumber = true
                    etNumber.setText(formatted)
                    etNumber.setSelection(formatted.length)
                    formattingNumber = false
                }

                updatePreview()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        etExpiry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (formattingExpiry) return

                val formatted = PaymentCardValidator.formatExpiryInput(s?.toString().orEmpty())
                if (formatted != s?.toString().orEmpty()) {
                    formattingExpiry = true
                    etExpiry.setText(formatted)
                    etExpiry.setSelection(formatted.length)
                    formattingExpiry = false
                }

                updatePreview()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        etNickname.addTextChangedListener(simpleWatcher { updatePreview() })
        etHolder.addTextChangedListener(simpleWatcher { updatePreview() })

        updatePreview()

        fun renderSavedCards() {
            containerSavedCards.removeAllViews()
            tvSavedCardsEmpty.visibility = if (savedCards.isEmpty()) View.VISIBLE else View.GONE

            savedCards.forEach { card ->
                val itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_checkout_saved_card, containerSavedCards, false)

                val rootCard = itemView.findViewById<MaterialCardView>(R.id.cardCheckoutSavedPayment)
                val tvNickname = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardNickname)
                val tvBrand = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardBrand)
                val tvNumber = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardNumber)
                val tvHolder = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardHolder)
                val tvExpiry = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardExpiry)
                val tvDefault = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardDefault)
                val tvSelected = itemView.findViewById<TextView>(R.id.tvCheckoutSavedCardSelected)

                val isSelected = selectedSavedCardId == card.cardId

                tvNickname.text = card.nickname
                tvBrand.text = card.brand
                tvNumber.text = card.maskedCardNumber
                tvHolder.text = card.cardholderName
                tvExpiry.text = "Exp ${card.expiryLabel}"
                tvDefault.visibility = if (card.isDefault) View.VISIBLE else View.GONE
                tvSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

                rootCard.strokeWidth = if (isSelected) 2 else 1
                rootCard.strokeColor = color(
                    if (isSelected) R.color.kc_brand_strong else R.color.kc_border_soft
                )

                itemView.setOnClickListener {
                    selectedSavedCardId = card.cardId
                    renderSavedCards()
                }

                containerSavedCards.addView(itemView)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.customerPaymentCardDao().observeForCustomer(customerId).collect { cards ->
                savedCards = cards

                val currentSelectionStillExists = cards.any { it.cardId == selectedSavedCardId }

                if (!currentSelectionStillExists) {
                    selectedSavedCardId = cards.firstOrNull { it.isDefault }?.cardId
                        ?: cards.firstOrNull()?.cardId
                }

                renderSavedCards()
            }
        }

        fun updatePaymentSelectionUi() {
            val selectedBg = R.drawable.bg_orders_filter_chip_selected
            val unselectedBg = R.drawable.bg_orders_filter_chip

            tvPaymentCard.setBackgroundResource(if (selectedPaymentType == "CARD") selectedBg else unselectedBg)
            tvPaymentApplePay.setBackgroundResource(if (selectedPaymentType == "APPLE_PAY") selectedBg else unselectedBg)
            tvPaymentCash.setBackgroundResource(if (selectedPaymentType == "CASH") selectedBg else unselectedBg)

            tvPaymentCard.setTextColor(
                color(if (selectedPaymentType == "CARD") R.color.kc_text_primary else R.color.kc_text_secondary)
            )
            tvPaymentApplePay.setTextColor(
                color(if (selectedPaymentType == "APPLE_PAY") R.color.kc_text_primary else R.color.kc_text_secondary)
            )
            tvPaymentCash.setTextColor(
                color(if (selectedPaymentType == "CASH") R.color.kc_text_primary else R.color.kc_text_secondary)
            )

            cardSection.visibility = if (selectedPaymentType == "CARD") View.VISIBLE else View.GONE
            applePaySection.visibility = if (selectedPaymentType == "APPLE_PAY") View.VISIBLE else View.GONE
            cashSection.visibility = if (selectedPaymentType == "CASH") View.VISIBLE else View.GONE
        }

        tvPaymentCard.setOnClickListener {
            selectedPaymentType = "CARD"
            updatePaymentSelectionUi()
        }

        tvPaymentApplePay.setOnClickListener {
            selectedPaymentType = "APPLE_PAY"
            updatePaymentSelectionUi()
        }

        tvPaymentCash.setOnClickListener {
            selectedPaymentType = "CASH"
            updatePaymentSelectionUi()
        }

        updatePaymentSelectionUi()

        btnBackToCart.setOnClickListener {
            findNavController().navigateUp()
        }

        btnPay.setOnClickListener {
            val total = CartManager.total()
            val beansToSpend = CartManager.beansToSpend()
            val beansToEarn = CartManager.purchasedProductCountForBeans()
            val cartItems = CartManager.getItems()

            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tilNickname.error = null
            tilHolder.error = null
            tilNumber.error = null
            tilExpiry.error = null
            tilCvv.error = null

            var saveNewCardForFuture = false

            if (selectedPaymentType == "CARD") {
                val nicknameInput = etNickname.text?.toString()?.trim().orEmpty()
                val holderInput = etHolder.text?.toString()?.trim().orEmpty()
                val numberInput = etNumber.text?.toString()?.trim().orEmpty()
                val expiryInput = etExpiry.text?.toString()?.trim().orEmpty()
                val cvvInput = etCvv.text?.toString()?.trim().orEmpty()

                val hasStartedTypingNewCard = listOf(
                    nicknameInput,
                    holderInput,
                    numberInput,
                    expiryInput,
                    cvvInput
                ).any { it.isNotBlank() }

                if (hasStartedTypingNewCard) {
                    val brand = PaymentCardValidator.detectBrand(numberInput)

                    var hasError = false

                    if (!PaymentCardValidator.isValidCardholderName(holderInput)) {
                        tilHolder.error = "Enter the cardholder first and last name as shown on the card"
                        hasError = true
                    }

                    val numberError = PaymentCardValidator.explainInvalidCardNumber(numberInput)
                    if (numberError != null) {
                        tilNumber.error = numberError
                        hasError = true
                    }

                    val expiry = PaymentCardValidator.parseExpiry(expiryInput)
                    if (expiry == null || !PaymentCardValidator.isExpiryValid(expiry.first, expiry.second)) {
                        tilExpiry.error = "Enter a valid future expiry date"
                        hasError = true
                    }

                    if (!PaymentCardValidator.isValidCvv(cvvInput, brand)) {
                        tilCvv.error = if (brand == PaymentCardValidator.CardBrand.AMEX) {
                            "AmEx uses a 4-digit security code"
                        } else {
                            "Enter a valid 3-digit security code"
                        }
                        hasError = true
                    }

                    if (hasError) return@setOnClickListener
                    saveNewCardForFuture = switchSaveCard.isChecked
                } else if (selectedSavedCardId == null) {
                    Toast.makeText(
                        requireContext(),
                        "Select a saved card or enter a new card.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

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

                if (selectedPaymentType == "CARD") {
                    val nicknameInput = etNickname.text?.toString()?.trim().orEmpty()
                    val holderInput = etHolder.text?.toString()?.trim().orEmpty()
                    val numberInput = etNumber.text?.toString()?.trim().orEmpty()
                    val expiryInput = etExpiry.text?.toString()?.trim().orEmpty()

                    val hasStartedTypingNewCard = listOf(
                        nicknameInput,
                        holderInput,
                        numberInput,
                        expiryInput,
                        etCvv.text?.toString()?.trim().orEmpty()
                    ).any { it.isNotBlank() }

                    if (hasStartedTypingNewCard && saveNewCardForFuture) {
                        val brand = PaymentCardValidator.detectBrand(numberInput)
                        val numberDigits = PaymentCardValidator.extractDigits(numberInput)
                        val expiry = PaymentCardValidator.parseExpiry(expiryInput)

                        if (expiry == null) {
                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(
                                    requireContext(),
                                    "The card expiry could not be read. Please check the expiry date and try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        val last4 = numberDigits.takeLast(4)

                        val finalNickname = if (nicknameInput.isBlank()) {
                            PaymentCardValidator.defaultNickname(brand, last4)
                        } else {
                            nicknameInput
                        }

                        val currentDefault = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
                        val shouldBeDefault = currentDefault == null

                        if (shouldBeDefault) {
                            db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
                        }

                        db.customerPaymentCardDao().insert(
                            CustomerPaymentCard(
                                customerId = customerId,
                                nickname = finalNickname,
                                cardholderName = holderInput,
                                brand = brand.displayName,
                                maskedCardNumber = PaymentCardValidator.buildMaskedNumber(numberInput),
                                last4 = last4,
                                expiryMonth = expiry.first,
                                expiryYear = expiry.second,
                                isDefault = shouldBeDefault
                            )
                        )
                    }
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
                    if (!isAdded) return@withContext

                    val paymentMessage = when (selectedPaymentType) {
                        "APPLE_PAY" -> "Apple Pay payment successful."
                        "CASH" -> "Cash payment selected for collection."
                        else -> "Card payment successful."
                    }

                    Toast.makeText(
                        requireContext(),
                        paymentMessage,
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

    private fun simpleWatcher(onAfterChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = onAfterChanged()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}