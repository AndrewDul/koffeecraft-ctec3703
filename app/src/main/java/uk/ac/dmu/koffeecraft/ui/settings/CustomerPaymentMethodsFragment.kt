package uk.ac.dmu.koffeecraft.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator

class CustomerPaymentMethodsFragment : Fragment(R.layout.fragment_customer_payment_methods) {

    private lateinit var tvEmpty: TextView
    private lateinit var containerCards: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvPaymentCardsEmpty)
        containerCards = view.findViewById(R.id.containerPaymentCards)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val btnAddCard = view.findViewById<MaterialButton>(R.id.btnAddPaymentCard)
        val customerId = SessionManager.currentCustomerId

        if (customerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        btnAddCard.setOnClickListener {
            showAddCardDialog(db, customerId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.customerPaymentCardDao().observeForCustomer(customerId).collect { cards ->
                renderCards(cards, db, customerId)
            }
        }
    }

    private fun renderCards(
        cards: List<CustomerPaymentCard>,
        db: KoffeeCraftDatabase,
        customerId: Long
    ) {
        if (!isAdded) return

        containerCards.removeAllViews()
        tvEmpty.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE

        cards.forEach { card ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_customer_payment_card, containerCards, false)

            val tvNickname = itemView.findViewById<TextView>(R.id.tvSavedCardNickname)
            val tvBrand = itemView.findViewById<TextView>(R.id.tvSavedCardBrand)
            val tvNumber = itemView.findViewById<TextView>(R.id.tvSavedCardNumber)
            val tvHolder = itemView.findViewById<TextView>(R.id.tvSavedCardHolder)
            val tvExpiry = itemView.findViewById<TextView>(R.id.tvSavedCardExpiry)
            val tvDefault = itemView.findViewById<TextView>(R.id.tvSavedCardDefault)
            val btnDefault = itemView.findViewById<MaterialButton>(R.id.btnSetDefaultCard)
            val btnDelete = itemView.findViewById<MaterialButton>(R.id.btnDeleteCard)

            tvNickname.text = card.nickname
            tvBrand.text = card.brand
            tvNumber.text = card.maskedCardNumber
            tvHolder.text = card.cardholderName
            tvExpiry.text = "Exp ${card.expiryLabel}"
            tvDefault.visibility = if (card.isDefault) View.VISIBLE else View.GONE

            btnDefault.isEnabled = !card.isDefault
            btnDefault.alpha = if (card.isDefault) 0.55f else 1f
            btnDefault.text = if (card.isDefault) "Default card" else "Set as default"

            btnDefault.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
                    db.customerPaymentCardDao().setDefault(card.cardId, customerId)

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
                            "\"${card.nickname}\" is now your default card.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove saved card?")
                    .setMessage("This will remove \"${card.nickname}\" from your saved payment methods.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            db.customerPaymentCardDao().deleteByIdAndCustomer(card.cardId, customerId)

                            if (card.isDefault) {
                                val replacement = db.customerPaymentCardDao().getMostRecentForCustomer(customerId)
                                if (replacement != null) {
                                    db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
                                    db.customerPaymentCardDao().setDefault(replacement.cardId, customerId)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(
                                    requireContext(),
                                    "Saved card removed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .show()
            }

            containerCards.addView(itemView)
        }
    }

    private fun showAddCardDialog(
        db: KoffeeCraftDatabase,
        customerId: Long
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_customer_payment_card_form, null)

        val tvPreviewNickname = dialogView.findViewById<TextView>(R.id.tvPreviewCardNickname)
        val tvPreviewNumber = dialogView.findViewById<TextView>(R.id.tvPreviewCardNumber)
        val tvPreviewHolder = dialogView.findViewById<TextView>(R.id.tvPreviewCardHolder)
        val tvPreviewExpiry = dialogView.findViewById<TextView>(R.id.tvPreviewCardExpiry)
        val tvPreviewBrand = dialogView.findViewById<TextView>(R.id.tvPreviewCardBrand)

        val tilNickname = dialogView.findViewById<TextInputLayout>(R.id.tilPaymentCardNickname)
        val tilHolder = dialogView.findViewById<TextInputLayout>(R.id.tilPaymentCardHolder)
        val tilNumber = dialogView.findViewById<TextInputLayout>(R.id.tilPaymentCardNumber)
        val tilExpiry = dialogView.findViewById<TextInputLayout>(R.id.tilPaymentCardExpiry)
        val tilCvv = dialogView.findViewById<TextInputLayout>(R.id.tilPaymentCardCvv)

        val etNickname = dialogView.findViewById<TextInputEditText>(R.id.etPaymentCardNickname)
        val etHolder = dialogView.findViewById<TextInputEditText>(R.id.etPaymentCardHolder)
        val etNumber = dialogView.findViewById<TextInputEditText>(R.id.etPaymentCardNumber)
        val etExpiry = dialogView.findViewById<TextInputEditText>(R.id.etPaymentCardExpiry)
        val etCvv = dialogView.findViewById<TextInputEditText>(R.id.etPaymentCardCvv)
        val switchDefault = dialogView.findViewById<SwitchMaterial>(R.id.switchPaymentCardDefault)

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
            tvPreviewNickname.text = if (rawNickname.isBlank()) "Saved card" else rawNickname
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save card", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilNickname.error = null
                tilHolder.error = null
                tilNumber.error = null
                tilExpiry.error = null
                tilCvv.error = null

                val nicknameInput = etNickname.text?.toString()?.trim().orEmpty()
                val holderInput = etHolder.text?.toString()?.trim().orEmpty()
                val numberInput = etNumber.text?.toString()?.trim().orEmpty()
                val expiryInput = etExpiry.text?.toString()?.trim().orEmpty()
                val cvvInput = etCvv.text?.toString()?.trim().orEmpty()

                val brand = PaymentCardValidator.detectBrand(numberInput)
                val numberDigits = PaymentCardValidator.extractDigits(numberInput)

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

                val safeExpiry = expiry
                if (safeExpiry == null) {
                    Toast.makeText(
                        requireContext(),
                        "The card expiry could not be read. Please check the expiry date and try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val last4 = numberDigits.takeLast(4)
                val finalNickname = if (nicknameInput.isBlank()) {
                    PaymentCardValidator.defaultNickname(brand, last4)
                } else {
                    nicknameInput
                }

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val currentDefault = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
                    val shouldBeDefault = switchDefault.isChecked || currentDefault == null

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
                            expiryMonth = safeExpiry.first,
                            expiryYear = safeExpiry.second,
                            isDefault = shouldBeDefault
                        )
                    )

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
                            "Card saved for faster checkout.",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun simpleWatcher(onAfterChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = onAfterChanged()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }
    }
}