package uk.ac.dmu.koffeecraft.ui.admin.inbox

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerInboxTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import android.widget.ArrayAdapter

class AdminInboxFragment : Fragment(R.layout.fragment_admin_inbox) {

    private lateinit var spinnerAudience: Spinner
    private lateinit var tvSearchHint: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var tvSelectedTarget: TextView
    private lateinit var tvConsentStatus: TextView
    private lateinit var tvAudienceSummary: TextView
    private lateinit var tvTargetsEmpty: TextView
    private lateinit var etMessageBody: EditText
    private lateinit var btnSendMessage: MaterialButton

    private lateinit var btnTemplatePromotional: MaterialButton
    private lateinit var btnTemplateImportant: MaterialButton
    private lateinit var btnTemplateService: MaterialButton

    private var selectedTarget: CustomerInboxTarget? = null
    private var currentAudienceMode: AudienceMode = AudienceMode.ALL_USERS
    private var currentTemplateType: TemplateType = TemplateType.NONE

    private enum class AudienceMode {
        ALL_USERS,
        BIRTHDAY_TODAY,
        ORDER_NUMBER,
        CUSTOMER_ID
    }

    private enum class TemplateType {
        NONE,
        PROMOTIONAL,
        IMPORTANT,
        SERVICE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerAudience = view.findViewById(R.id.spinnerAudience)
        tvSearchHint = view.findViewById(R.id.tvSearchHint)
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        tvSelectedTarget = view.findViewById(R.id.tvSelectedTarget)
        tvConsentStatus = view.findViewById(R.id.tvConsentStatus)
        tvAudienceSummary = view.findViewById(R.id.tvAudienceSummary)
        tvTargetsEmpty = view.findViewById(R.id.tvTargetsEmpty)
        etMessageBody = view.findViewById(R.id.etMessageBody)
        btnSendMessage = view.findViewById(R.id.btnSendMessage)

        btnTemplatePromotional = view.findViewById(R.id.btnTemplatePromotional)
        btnTemplateImportant = view.findViewById(R.id.btnTemplateImportant)
        btnTemplateService = view.findViewById(R.id.btnTemplateService)

        setupAudienceSpinner()
        setupTemplateButtons()
        updateAudienceSummary()
        updateTemplateButtons()

        btnSearch.setOnClickListener {
            performTargetLookup()
        }

        btnSendMessage.setOnClickListener {
            sendMessages()
        }
    }

    private fun setupAudienceSpinner() {
        val options = listOf(
            "All users",
            "Birthday today",
            "Find by order number",
            "Find by customer ID"
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerAudience.adapter = spinnerAdapter

        spinnerAudience.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentAudienceMode = when (position) {
                    1 -> AudienceMode.BIRTHDAY_TODAY
                    2 -> AudienceMode.ORDER_NUMBER
                    3 -> AudienceMode.CUSTOMER_ID
                    else -> AudienceMode.ALL_USERS
                }

                resetSelectionUi()

                when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> {
                        tvSearchHint.visibility = View.GONE
                        etSearch.visibility = View.GONE
                        btnSearch.visibility = View.GONE
                    }

                    AudienceMode.BIRTHDAY_TODAY -> {
                        tvSearchHint.visibility = View.GONE
                        etSearch.visibility = View.GONE
                        btnSearch.visibility = View.GONE
                    }

                    AudienceMode.ORDER_NUMBER -> {
                        tvSearchHint.visibility = View.VISIBLE
                        tvSearchHint.text = "Find customer by order number"
                        etSearch.visibility = View.VISIBLE
                        etSearch.hint = "Enter order number"
                        etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        btnSearch.visibility = View.VISIBLE
                    }

                    AudienceMode.CUSTOMER_ID -> {
                        tvSearchHint.visibility = View.VISIBLE
                        tvSearchHint.text = "Find customer by customer ID"
                        etSearch.visibility = View.VISIBLE
                        etSearch.hint = "Enter customer ID"
                        etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        btnSearch.visibility = View.VISIBLE
                    }
                }

                updateAudienceSummary()
                updateTemplateButtons()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupTemplateButtons() {
        btnTemplatePromotional.setOnClickListener {
            if (!btnTemplatePromotional.isEnabled) return@setOnClickListener
            currentTemplateType = TemplateType.PROMOTIONAL
            etMessageBody.setText(buildTemplateBody(TemplateType.PROMOTIONAL))
            updateTemplateButtons()
        }

        btnTemplateImportant.setOnClickListener {
            currentTemplateType = TemplateType.IMPORTANT
            etMessageBody.setText(buildTemplateBody(TemplateType.IMPORTANT))
            updateTemplateButtons()
        }

        btnTemplateService.setOnClickListener {
            currentTemplateType = TemplateType.SERVICE
            etMessageBody.setText(buildTemplateBody(TemplateType.SERVICE))
            updateTemplateButtons()
        }
    }

    private fun buildTemplateBody(type: TemplateType): String {
        return when (type) {
            TemplateType.PROMOTIONAL -> """
KoffeeCraft has a special offer for you.

Offer details: [PROMO_DETAILS]
Bonus beans / reward: [BEANS_AMOUNT]
Valid until: [DATE]

Enjoy,
KoffeeCraft Team
            """.trimIndent()

            TemplateType.IMPORTANT -> """
This is an important message from KoffeeCraft regarding account activity.

Reason: [REASON]
Action required: [ACTION_REQUIRED]

Please review this information carefully.

KoffeeCraft Team
            """.trimIndent()

            TemplateType.SERVICE -> """
KoffeeCraft will be performing planned service work.

Date: [DATE]
Time: [TIME]
Details: [SERVICE_DETAILS]

Some app features may be temporarily unavailable during this period.

Thank you,
KoffeeCraft Team
            """.trimIndent()

            TemplateType.NONE -> ""
        }
    }

    private fun updateTemplateButtons() {
        val promoAllowedForSingleTarget =
            selectedTarget?.marketingInboxConsent != false ||
                    !(currentAudienceMode == AudienceMode.ORDER_NUMBER || currentAudienceMode == AudienceMode.CUSTOMER_ID)

        btnTemplatePromotional.isEnabled = promoAllowedForSingleTarget
        btnTemplatePromotional.alpha = if (promoAllowedForSingleTarget) 1f else 0.45f

        styleTemplateButton(btnTemplatePromotional, currentTemplateType == TemplateType.PROMOTIONAL)
        styleTemplateButton(btnTemplateImportant, currentTemplateType == TemplateType.IMPORTANT)
        styleTemplateButton(btnTemplateService, currentTemplateType == TemplateType.SERVICE)
    }

    private fun styleTemplateButton(button: MaterialButton, selected: Boolean) {
        button.strokeWidth = if (selected) 4 else 0
        button.strokeColor =
            if (selected) android.content.res.ColorStateList.valueOf(Color.parseColor("#F5EBDD")) else null
    }

    private fun resetSelectionUi() {
        selectedTarget = null
        tvSelectedTarget.visibility = View.GONE
        tvConsentStatus.visibility = View.GONE
        tvTargetsEmpty.visibility = View.GONE
        etSearch.setText("")
    }

    private fun performTargetLookup() {
        val query = etSearch.text.toString().trim()
        if (query.isBlank()) {
            Toast.makeText(requireContext(), "Enter a value first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = when (currentAudienceMode) {
                AudienceMode.ORDER_NUMBER -> {
                    val orderId = query.toLongOrNull()
                    if (orderId == null) null else db.customerDao().getInboxTargetByOrderId(orderId)
                }

                AudienceMode.CUSTOMER_ID -> {
                    val customerId = query.toLongOrNull()
                    if (customerId == null) null else db.customerDao().getInboxTargetByCustomerId(customerId)
                }

                else -> null
            }

            withContext(Dispatchers.Main) {
                if (result == null) {
                    selectedTarget = null
                    tvSelectedTarget.visibility = View.GONE
                    tvConsentStatus.visibility = View.GONE
                    tvTargetsEmpty.visibility = View.VISIBLE
                } else {
                    selectedTarget = result
                    tvTargetsEmpty.visibility = View.GONE
                    tvSelectedTarget.visibility = View.VISIBLE
                    tvSelectedTarget.text = "Selected: Customer #${result.customerId}"

                    updateSingleTargetConsentStatus()
                }

                updateTemplateButtons()
            }
        }
    }

    private fun updateSingleTargetConsentStatus() {
        val target = selectedTarget
        if (target == null) {
            tvConsentStatus.visibility = View.GONE
            return
        }

        tvConsentStatus.visibility = View.VISIBLE

        if (target.marketingInboxConsent) {
            tvConsentStatus.text = "Promo messages: ON"
            tvConsentStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvConsentStatus.text = "Promo messages: OFF"
            tvConsentStatus.setTextColor(Color.parseColor("#B71C1C"))
        }
    }

    private fun updateAudienceSummary() {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        if (currentAudienceMode == AudienceMode.ORDER_NUMBER || currentAudienceMode == AudienceMode.CUSTOMER_ID) {
            tvAudienceSummary.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val targets = when (currentAudienceMode) {
                AudienceMode.ALL_USERS -> db.customerDao().getAllInboxTargets()
                AudienceMode.BIRTHDAY_TODAY -> {
                    val monthDay = SimpleDateFormat("MM-dd", Locale.UK).format(Date())
                    db.customerDao().getBirthdayInboxTargets(monthDay)
                }
                else -> emptyList()
            }

            val total = targets.size
            val promoEligible = targets.count { it.marketingInboxConsent }
            val optedOut = total - promoEligible

            withContext(Dispatchers.Main) {
                tvAudienceSummary.visibility = View.VISIBLE
                tvAudienceSummary.text =
                    "Recipients: $total • Promo ON: $promoEligible • Promo OFF: $optedOut"
            }
        }
    }

    private fun sendMessages() {
        val body = etMessageBody.text.toString().trim()

        if (body.isBlank()) {
            Toast.makeText(requireContext(), "Write a message first.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentAudienceMode == AudienceMode.ORDER_NUMBER || currentAudienceMode == AudienceMode.CUSTOMER_ID) {
            val target = selectedTarget
            if (target == null) {
                Toast.makeText(requireContext(), "Select a customer first.", Toast.LENGTH_SHORT).show()
                return
            }

            if (currentTemplateType == TemplateType.PROMOTIONAL && !target.marketingInboxConsent) {
                Toast.makeText(
                    requireContext(),
                    "This customer has opted out of promotional messages.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val rawTargets = when (currentAudienceMode) {
                AudienceMode.ALL_USERS -> db.customerDao().getAllInboxTargets()

                AudienceMode.BIRTHDAY_TODAY -> {
                    val monthDay = SimpleDateFormat("MM-dd", Locale.UK).format(Date())
                    db.customerDao().getBirthdayInboxTargets(monthDay)
                }

                AudienceMode.ORDER_NUMBER,
                AudienceMode.CUSTOMER_ID -> selectedTarget?.let { listOf(it) } ?: emptyList()
            }

            if (rawTargets.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No target customers found.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val filteredTargets = if (currentTemplateType == TemplateType.PROMOTIONAL) {
                rawTargets.filter { it.marketingInboxConsent }
            } else {
                rawTargets
            }

            if (filteredTargets.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "No eligible recipients for this promotional message.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            val deliveryType = when (currentTemplateType) {
                TemplateType.PROMOTIONAL -> when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> "PROMO_BROADCAST"
                    AudienceMode.BIRTHDAY_TODAY -> "PROMO_BIRTHDAY"
                    AudienceMode.ORDER_NUMBER,
                    AudienceMode.CUSTOMER_ID -> "PROMO_DIRECT"
                }

                TemplateType.IMPORTANT -> when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> "IMPORTANT_BROADCAST"
                    AudienceMode.BIRTHDAY_TODAY -> "IMPORTANT_BIRTHDAY"
                    AudienceMode.ORDER_NUMBER,
                    AudienceMode.CUSTOMER_ID -> "IMPORTANT_DIRECT"
                }

                TemplateType.SERVICE -> when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> "SERVICE_BROADCAST"
                    AudienceMode.BIRTHDAY_TODAY -> "SERVICE_BIRTHDAY"
                    AudienceMode.ORDER_NUMBER,
                    AudienceMode.CUSTOMER_ID -> "SERVICE_DIRECT"
                }

                TemplateType.NONE -> when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> "CUSTOM_BROADCAST"
                    AudienceMode.BIRTHDAY_TODAY -> "CUSTOM_BIRTHDAY"
                    AudienceMode.ORDER_NUMBER,
                    AudienceMode.CUSTOMER_ID -> "CUSTOM_DIRECT"
                }
            }

            val title = when (currentTemplateType) {
                TemplateType.PROMOTIONAL -> "Special KoffeeCraft Offer"
                TemplateType.IMPORTANT -> "Important KoffeeCraft Notice"
                TemplateType.SERVICE -> "KoffeeCraft Service Update"
                TemplateType.NONE -> "Message from KoffeeCraft"
            }

            val messages = filteredTargets
                .distinctBy { it.customerId }
                .map { target ->
                    InboxMessage(
                        recipientCustomerId = target.customerId,
                        title = title,
                        body = body,
                        deliveryType = deliveryType
                    )
                }

            db.inboxMessageDao().insertAll(messages)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Message sent to ${messages.size} recipient(s).",
                    Toast.LENGTH_SHORT
                ).show()

                etMessageBody.setText("")
                currentTemplateType = TemplateType.NONE
                resetSelectionUi()
                updateAudienceSummary()
                updateTemplateButtons()
            }
        }
    }
}