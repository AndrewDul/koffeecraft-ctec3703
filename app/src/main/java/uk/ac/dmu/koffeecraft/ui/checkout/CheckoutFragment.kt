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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator
import java.util.Locale

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private lateinit var vm: CheckoutViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Not logged in as customer.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        vm = ViewModelProvider(
            this,
            CheckoutViewModelFactory(
                checkoutRepository = appContainer.checkoutRepository,
                cartRepository = appContainer.cartRepository
            )
        )[CheckoutViewModel::class.java]

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

                vm.clearCardValidation()
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

                vm.clearCardValidation()
                updatePreview()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        etNickname.addTextChangedListener(simpleWatcher {
            vm.clearCardValidation()
            updatePreview()
        })
        etHolder.addTextChangedListener(simpleWatcher {
            vm.clearCardValidation()
            updatePreview()
        })
        etCvv.addTextChangedListener(simpleWatcher {
            vm.clearCardValidation()
        })

        updatePreview()

        tvPaymentCard.setOnClickListener {
            vm.selectPaymentType("CARD")
        }

        tvPaymentApplePay.setOnClickListener {
            vm.selectPaymentType("APPLE_PAY")
        }

        tvPaymentCash.setOnClickListener {
            vm.selectPaymentType("CASH")
        }

        btnBackToCart.setOnClickListener {
            findNavController().navigateUp()
        }

        btnPay.setOnClickListener {
            vm.submitOrder(
                customerId = customerId,
                cardNickname = etNickname.text?.toString()?.trim().orEmpty(),
                cardholderName = etHolder.text?.toString()?.trim().orEmpty(),
                cardNumber = etNumber.text?.toString()?.trim().orEmpty(),
                expiryText = etExpiry.text?.toString()?.trim().orEmpty(),
                cvv = etCvv.text?.toString()?.trim().orEmpty(),
                saveNewCardForFuture = switchSaveCard.isChecked
            )
        }

        vm.start(customerId)

        collectState(
            tvTotalValue = tvTotalValue,
            tvBeans = tvBeans,
            tilHolder = tilHolder,
            tilNumber = tilNumber,
            tilExpiry = tilExpiry,
            tilCvv = tilCvv,
            containerSavedCards = containerSavedCards,
            tvSavedCardsEmpty = tvSavedCardsEmpty,
            tvPaymentCard = tvPaymentCard,
            tvPaymentApplePay = tvPaymentApplePay,
            tvPaymentCash = tvPaymentCash,
            cardSection = cardSection,
            applePaySection = applePaySection,
            cashSection = cashSection,
            btnPay = btnPay
        )

        collectEffects()
    }

    private fun collectState(
        tvTotalValue: TextView,
        tvBeans: TextView,
        tilHolder: TextInputLayout,
        tilNumber: TextInputLayout,
        tilExpiry: TextInputLayout,
        tilCvv: TextInputLayout,
        containerSavedCards: LinearLayout,
        tvSavedCardsEmpty: TextView,
        tvPaymentCard: TextView,
        tvPaymentApplePay: TextView,
        tvPaymentCash: TextView,
        cardSection: View,
        applePaySection: View,
        cashSection: View,
        btnPay: MaterialButton
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { state ->
                    tvTotalValue.text = String.format(Locale.UK, "£%.2f", state.total)

                    if (state.beansToSpend > 0) {
                        tvBeans.visibility = View.VISIBLE
                        tvBeans.text = "Beans to spend: ${state.beansToSpend}"
                    } else {
                        tvBeans.visibility = View.GONE
                    }

                    applyValidationErrors(
                        validation = state.cardValidation,
                        tilHolder = tilHolder,
                        tilNumber = tilNumber,
                        tilExpiry = tilExpiry,
                        tilCvv = tilCvv
                    )

                    renderSavedCards(
                        savedCards = state.savedCards,
                        selectedSavedCardId = state.selectedSavedCardId,
                        containerSavedCards = containerSavedCards,
                        tvSavedCardsEmpty = tvSavedCardsEmpty,
                        onCardSelected = { cardId ->
                            vm.selectSavedCard(cardId)
                        }
                    )

                    updatePaymentSelectionUi(
                        selectedPaymentType = state.paymentType,
                        tvPaymentCard = tvPaymentCard,
                        tvPaymentApplePay = tvPaymentApplePay,
                        tvPaymentCash = tvPaymentCash,
                        cardSection = cardSection,
                        applePaySection = applePaySection,
                        cashSection = cashSection
                    )

                    val canPay = !state.isSubmitting && !state.isCartEmpty
                    btnPay.isEnabled = canPay
                    btnPay.alpha = if (canPay) 1f else 0.7f
                    btnPay.text = if (state.isSubmitting) "Processing..." else "Pay now"
                }
            }
        }
    }

    private fun collectEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.effects.collect { effect ->
                    when (effect) {
                        is CheckoutUiEffect.ShowMessage -> {
                            Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                        }

                        is CheckoutUiEffect.CheckoutCompleted -> {
                            if (!isAdded) return@collect

                            Toast.makeText(
                                requireContext(),
                                effect.paymentMessage,
                                Toast.LENGTH_SHORT
                            ).show()

                            NotificationHelper.showCustomerOrderNotification(
                                context = requireContext(),
                                title = "Payment confirmed",
                                message = "Your order #${effect.orderId} has been placed successfully.",
                                notificationId = (effect.orderId % Int.MAX_VALUE).toInt(),
                                orderId = effect.orderId
                            )

                            val simulate = SimulationSettings.isEnabled(requireContext())

                            findNavController().navigate(
                                R.id.action_checkout_to_status,
                                bundleOf(
                                    "orderId" to effect.orderId,
                                    "simulate" to simulate,
                                    "fromCheckout" to true
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun renderSavedCards(
        savedCards: List<CustomerPaymentCard>,
        selectedSavedCardId: Long?,
        containerSavedCards: LinearLayout,
        tvSavedCardsEmpty: TextView,
        onCardSelected: (Long) -> Unit
    ) {
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
                onCardSelected(card.cardId)
            }

            containerSavedCards.addView(itemView)
        }
    }

    private fun applyValidationErrors(
        validation: uk.ac.dmu.koffeecraft.util.validation.CheckoutCardValidationResult,
        tilHolder: TextInputLayout,
        tilNumber: TextInputLayout,
        tilExpiry: TextInputLayout,
        tilCvv: TextInputLayout
    ) {
        tilHolder.error = validation.holderError
        tilNumber.error = validation.numberError
        tilExpiry.error = validation.expiryError
        tilCvv.error = validation.cvvError
    }

    private fun updatePaymentSelectionUi(
        selectedPaymentType: String,
        tvPaymentCard: TextView,
        tvPaymentApplePay: TextView,
        tvPaymentCash: TextView,
        cardSection: View,
        applePaySection: View,
        cashSection: View
    ) {
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