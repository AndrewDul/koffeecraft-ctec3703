package uk.ac.dmu.koffeecraft.ui.admin.inbox

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerInboxTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

class AdminInboxFragment : Fragment(R.layout.fragment_admin_inbox) {

    private lateinit var tvModeOrderNumber: TextView
    private lateinit var tvModeCustomerId: TextView
    private lateinit var tvSearchHint: TextView
    private lateinit var tilSearch: TextInputLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var tvTargetsEmpty: TextView

    private lateinit var cardSelectedTarget: MaterialCardView
    private lateinit var tvSelectedTarget: TextView
    private lateinit var tvTargetMeta: TextView
    private lateinit var tvConsentStatus: TextView
    private lateinit var tvAudienceSummary: TextView

    private lateinit var btnTemplateImportant: MaterialButton
    private lateinit var btnTemplateService: MaterialButton
    private lateinit var btnTemplateCustom: MaterialButton

    private lateinit var tvMessageHint: TextView
    private lateinit var etMessageTitle: TextInputEditText
    private lateinit var etMessageBody: TextInputEditText
    private lateinit var btnSendMessage: MaterialButton

    private var selectedTarget: CustomerInboxTarget? = null
    private var currentTargetMode: TargetMode = TargetMode.ORDER_NUMBER
    private var currentMessageType: MessageType = MessageType.CUSTOM

    private enum class TargetMode {
        ORDER_NUMBER,
        CUSTOMER_ID
    }

    private enum class MessageType {
        CUSTOM,
        IMPORTANT,
        SERVICE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupModeClicks()
        setupTypeClicks()
        setupTextWatchers()

        applyTargetMode(TargetMode.ORDER_NUMBER)
        applyMessageType(MessageType.CUSTOM, forceTemplate = true)

        btnSearch.setOnClickListener {
            performTargetLookup()
        }

        btnSendMessage.setOnClickListener {
            sendDirectMessage()
        }
    }

    private fun bindViews(view: View) {
        tvModeOrderNumber = view.findViewById(R.id.tvModeOrderNumber)
        tvModeCustomerId = view.findViewById(R.id.tvModeCustomerId)
        tvSearchHint = view.findViewById(R.id.tvSearchHint)
        tilSearch = view.findViewById(R.id.tilSearch)
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        tvTargetsEmpty = view.findViewById(R.id.tvTargetsEmpty)

        cardSelectedTarget = view.findViewById(R.id.cardSelectedTarget)
        tvSelectedTarget = view.findViewById(R.id.tvSelectedTarget)
        tvTargetMeta = view.findViewById(R.id.tvTargetMeta)
        tvConsentStatus = view.findViewById(R.id.tvConsentStatus)
        tvAudienceSummary = view.findViewById(R.id.tvAudienceSummary)

        btnTemplateImportant = view.findViewById(R.id.btnTemplateImportant)
        btnTemplateService = view.findViewById(R.id.btnTemplateService)
        btnTemplateCustom = view.findViewById(R.id.btnTemplateCustom)

        tvMessageHint = view.findViewById(R.id.tvMessageHint)
        etMessageTitle = view.findViewById(R.id.etMessageTitle)
        etMessageBody = view.findViewById(R.id.etMessageBody)
        btnSendMessage = view.findViewById(R.id.btnSendMessage)
    }

    private fun setupModeClicks() {
        tvModeOrderNumber.setOnClickListener {
            applyTargetMode(TargetMode.ORDER_NUMBER)
        }

        tvModeCustomerId.setOnClickListener {
            applyTargetMode(TargetMode.CUSTOMER_ID)
        }
    }

    private fun setupTypeClicks() {
        btnTemplateImportant.setOnClickListener {
            applyMessageType(MessageType.IMPORTANT)
        }

        btnTemplateService.setOnClickListener {
            applyMessageType(MessageType.SERVICE)
        }

        btnTemplateCustom.setOnClickListener {
            applyMessageType(MessageType.CUSTOM)
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSendState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        etMessageTitle.addTextChangedListener(watcher)
        etMessageBody.addTextChangedListener(watcher)
    }

    private fun applyTargetMode(mode: TargetMode) {
        currentTargetMode = mode

        styleModeChip(tvModeOrderNumber, mode == TargetMode.ORDER_NUMBER)
        styleModeChip(tvModeCustomerId, mode == TargetMode.CUSTOMER_ID)

        when (mode) {
            TargetMode.ORDER_NUMBER -> {
                tvSearchHint.text = "Find customer by order number"
                tilSearch.hint = "Enter order number"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            TargetMode.CUSTOMER_ID -> {
                tvSearchHint.text = "Find customer by customer ID"
                tilSearch.hint = "Enter customer ID"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        }

        etSearch.setText("")
        selectedTarget = null
        cardSelectedTarget.visibility = View.GONE
        tvTargetsEmpty.visibility = View.GONE
        updateSendState()
    }

    private fun styleModeChip(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_orders_filter_chip_selected
            else R.drawable.bg_orders_filter_chip
        )
        view.setTextColor(
            color(
                if (selected) R.color.kc_text_primary else R.color.kc_text_secondary
            )
        )
        view.alpha = if (selected) 1f else 0.95f
    }

    private fun applyMessageType(type: MessageType, forceTemplate: Boolean = false) {
        currentMessageType = type

        styleTypeButton(btnTemplateImportant, type == MessageType.IMPORTANT)
        styleTypeButton(btnTemplateService, type == MessageType.SERVICE)
        styleTypeButton(btnTemplateCustom, type == MessageType.CUSTOM)

        tvMessageHint.text = when (type) {
            MessageType.IMPORTANT -> {
                "Use this for direct important notices linked to one customer or one order."
            }

            MessageType.SERVICE -> {
                "Use this for direct service follow-ups, order issues, or operational updates."
            }

            MessageType.CUSTOM -> {
                "Use [FIRST_NAME] and [LAST_NAME] if you want light personalisation in direct messages."
            }
        }

        val shouldApplyTemplate = forceTemplate ||
                etMessageTitle.text.isNullOrBlank() ||
                etMessageBody.text.isNullOrBlank()

        if (shouldApplyTemplate) {
            etMessageTitle.setText(defaultTitle(type))
            etMessageBody.setText(defaultBody(type))
        }

        updateSendState()
    }

    private fun styleTypeButton(button: MaterialButton, selected: Boolean) {
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_brand_strong))
            button.setTextColor(color(R.color.kc_text_inverse))
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_surface_panel))
            button.setTextColor(color(R.color.kc_icon_primary))
            button.strokeWidth = 1
            button.strokeColor = ColorStateList.valueOf(color(R.color.kc_border_warm))
        }
    }

    private fun defaultTitle(type: MessageType): String {
        return when (type) {
            MessageType.IMPORTANT -> "Important KoffeeCraft Notice"
            MessageType.SERVICE -> "KoffeeCraft Service Update"
            MessageType.CUSTOM -> "Message from KoffeeCraft"
        }
    }

    private fun defaultBody(type: MessageType): String {
        return when (type) {
            MessageType.IMPORTANT -> {
                """
Hello [FIRST_NAME],

This is an important message from KoffeeCraft regarding your account or recent order.

Please review the update and contact us if you need any support.

KoffeeCraft
                """.trimIndent()
            }

            MessageType.SERVICE -> {
                """
Hello [FIRST_NAME],

We are contacting you with a service update related to your recent KoffeeCraft experience.

If you need any help, please let us know.

KoffeeCraft
                """.trimIndent()
            }

            MessageType.CUSTOM -> {
                """
Hello [FIRST_NAME],

[WRITE_YOUR_MESSAGE_HERE]

KoffeeCraft
                """.trimIndent()
            }
        }
    }

    private fun performTargetLookup() {
        tvTargetsEmpty.visibility = View.GONE

        val rawQuery = etSearch.text?.toString()?.trim().orEmpty()
        if (rawQuery.isBlank()) {
            Toast.makeText(requireContext(), "Enter a value first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = when (currentTargetMode) {
                TargetMode.ORDER_NUMBER -> {
                    val orderId = rawQuery.toLongOrNull()
                    if (orderId == null) null else db.customerDao().getInboxTargetByOrderId(orderId)
                }

                TargetMode.CUSTOMER_ID -> {
                    val customerId = rawQuery.toLongOrNull()
                    if (customerId == null) null else db.customerDao().getInboxTargetByCustomerId(customerId)
                }
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (result == null) {
                    selectedTarget = null
                    cardSelectedTarget.visibility = View.GONE
                    tvTargetsEmpty.visibility = View.VISIBLE
                } else {
                    selectedTarget = result
                    tvTargetsEmpty.visibility = View.GONE
                    renderSelectedTarget(result)
                }

                updateSendState()
            }
        }
    }

    private fun renderSelectedTarget(target: CustomerInboxTarget) {
        cardSelectedTarget.visibility = View.VISIBLE

        val displayName = "${target.firstName} ${target.lastName}".trim().ifBlank {
            "Customer #${target.customerId}"
        }

        tvSelectedTarget.text = displayName
        tvTargetMeta.text = "Customer #${target.customerId} • ${target.email}"
        tvConsentStatus.text = if (target.marketingInboxConsent) {
            "Marketing consent: ON • Promotional campaigns are available in Studio"
        } else {
            "Marketing consent: OFF • Direct service messages can still be sent here"
        }
        tvConsentStatus.setTextColor(
            color(
                if (target.marketingInboxConsent) R.color.kc_info_text else R.color.kc_danger
            )
        )
        tvAudienceSummary.text = "1 customer will receive this direct message"
    }

    private fun updateSendState() {
        val canSend = selectedTarget != null &&
                !etMessageTitle.text.isNullOrBlank() &&
                !etMessageBody.text.isNullOrBlank()

        btnSendMessage.isEnabled = canSend
        btnSendMessage.alpha = if (canSend) 1f else 0.5f
    }

    private fun sendDirectMessage() {
        val target = selectedTarget
        if (target == null) {
            Toast.makeText(requireContext(), "Find a customer first.", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etMessageTitle.text?.toString()?.trim().orEmpty()
        val body = etMessageBody.text?.toString()?.trim().orEmpty()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Enter a message title.", Toast.LENGTH_SHORT).show()
            return
        }

        if (body.isBlank()) {
            Toast.makeText(requireContext(), "Write a message first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val resolvedBody = body
            .replace("[FIRST_NAME]", target.firstName.ifBlank { "Customer" })
            .replace("[LAST_NAME]", target.lastName.ifBlank { "" })

        val deliveryType = when (currentMessageType) {
            MessageType.IMPORTANT -> "IMPORTANT_DIRECT"
            MessageType.SERVICE -> "SERVICE_DIRECT"
            MessageType.CUSTOM -> "CUSTOM_DIRECT"
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.inboxMessageDao().insertAll(
                listOf(
                    InboxMessage(
                        recipientCustomerId = target.customerId,
                        title = title,
                        body = resolvedBody,
                        deliveryType = deliveryType
                    )
                )
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                Toast.makeText(
                    requireContext(),
                    "Direct message sent successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                applyMessageType(MessageType.CUSTOM, forceTemplate = true)
            }
        }
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}