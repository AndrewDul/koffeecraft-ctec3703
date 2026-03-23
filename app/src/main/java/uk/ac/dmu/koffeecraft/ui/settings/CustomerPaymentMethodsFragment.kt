package uk.ac.dmu.koffeecraft.ui.settings

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator
import uk.ac.dmu.koffeecraft.util.validation.SavedPaymentCardFormValidator

class CustomerPaymentMethodsFragment : Fragment(R.layout.fragment_customer_payment_methods) {

    private lateinit var vm: CustomerPaymentMethodsViewModel
    private lateinit var tvEmpty: TextView
    private lateinit var containerCards: LinearLayout
    private var activeAddCardDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CustomerPaymentMethodsViewModel.Factory(
                customerPaymentMethodsRepository = appContainer.customerPaymentMethodsRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[CustomerPaymentMethodsViewModel::class.java]

        tvEmpty = view.findViewById(R.id.tvPaymentCardsEmpty)
        containerCards = view.findViewById(R.id.containerPaymentCards)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val btnAddCard = view.findViewById<MaterialButton>(R.id.btnAddPaymentCard)
        btnAddCard.setOnClickListener {
            showAddCardDialog()
        }

        vm.start()
        collectViewModel()
    }

    override fun onDestroyView() {
        activeAddCardDialog?.dismiss()
        activeAddCardDialog = null
        super.onDestroyView()
    }

    private fun collectViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.state.collect { state ->
                        renderCards(state.cards)
                    }
                }

                launch {
                    vm.effects.collect { effect ->
                        when (effect) {
                            is CustomerPaymentMethodsViewModel.UiEffect.ShowMessage -> {
                                Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                            }

                            CustomerPaymentMethodsViewModel.UiEffect.DismissAddCardDialog -> {
                                activeAddCardDialog?.dismiss()
                                activeAddCardDialog = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderCards(cards: List<CustomerPaymentCard>) {
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
                vm.setDefaultCard(card)
            }

            btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove saved card?")
                    .setMessage("This will remove \"${card.nickname}\" from your saved payment methods.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ ->
                        vm.deleteCard(card)
                    }
                    .show()
            }

            containerCards.addView(itemView)
        }
    }

    private fun showAddCardDialog() {
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

        activeAddCardDialog = dialog

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

                val validation = SavedPaymentCardFormValidator.validate(
                    holder = holderInput,
                    number = numberInput,
                    expiry = expiryInput,
                    cvv = cvvInput
                )

                tilHolder.error = validation.holderError
                tilNumber.error = validation.numberError
                tilExpiry.error = validation.expiryError
                tilCvv.error = validation.cvvError

                if (!validation.isValid) return@setOnClickListener

                vm.addCard(
                    nickname = nicknameInput,
                    cardholderName = holderInput,
                    cardNumber = numberInput,
                    expiryText = expiryInput,
                    setAsDefault = switchDefault.isChecked
                )
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