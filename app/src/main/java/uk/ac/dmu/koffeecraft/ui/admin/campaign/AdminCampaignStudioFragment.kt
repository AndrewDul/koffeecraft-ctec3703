package uk.ac.dmu.koffeecraft.ui.admin.campaign

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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerCampaignTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminCampaignStudioFragment : Fragment(R.layout.fragment_admin_campaign_studio) {

    private lateinit var db: KoffeeCraftDatabase

    private lateinit var tvAudienceAllOptedIn: TextView
    private lateinit var tvAudienceBirthdayToday: TextView
    private lateinit var tvAudienceNoOrders: TextView
    private lateinit var tvAudienceLoyal: TextView
    private lateinit var tvAudienceInactive: TextView
    private lateinit var tvAudienceCloseReward: TextView
    private lateinit var tvAudienceDirectOrder: TextView
    private lateinit var tvAudienceDirectCustomer: TextView

    private lateinit var tvAudienceRuleLabel: TextView
    private lateinit var tilAudienceRule: TextInputLayout
    private lateinit var etAudienceRule: TextInputEditText

    private lateinit var btnTypePromo: MaterialButton
    private lateinit var btnTypeBeans: MaterialButton
    private lateinit var btnTypeOfferBeans: MaterialButton

    private lateinit var tvBuilderHint: TextView
    private lateinit var etCampaignTitle: TextInputEditText
    private lateinit var etCampaignMessage: TextInputEditText
    private lateinit var tilBeansAmount: TextInputLayout
    private lateinit var etBeansAmount: TextInputEditText

    private lateinit var tvPreviewAudienceValue: TextView
    private lateinit var tvPreviewRecipientsValue: TextView
    private lateinit var tvPreviewExcludedValue: TextView
    private lateinit var tvPreviewBeansTotalValue: TextView
    private lateinit var tvPreviewModeNote: TextView
    private lateinit var tvPreviewSummary: TextView
    private lateinit var btnSendCampaign: MaterialButton

    private var selectedAudience = AudienceMode.ALL_OPTED_IN
    private var selectedCampaignType = CampaignType.PROMOTIONAL_OFFER

    private var previewJob: Job? = null
    private var currentPreview = CampaignPreview.empty()

    private enum class AudienceMode {
        ALL_OPTED_IN,
        BIRTHDAY_TODAY,
        NO_ORDERS,
        LOYAL_CUSTOMERS,
        INACTIVE_USERS,
        CLOSE_TO_REWARD,
        DIRECT_ORDER,
        DIRECT_CUSTOMER
    }

    private enum class CampaignType(
        val includesBeans: Boolean,
        val requiresMarketingConsent: Boolean
    ) {
        PROMOTIONAL_OFFER(
            includesBeans = false,
            requiresMarketingConsent = true
        ),
        BONUS_BEANS(
            includesBeans = true,
            requiresMarketingConsent = false
        ),
        OFFER_PLUS_BEANS(
            includesBeans = true,
            requiresMarketingConsent = true
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        bindViews(view)
        setupAudienceClicks()
        setupCampaignTypeClicks()
        setupTextWatchers()

        applyAudienceSelection(AudienceMode.ALL_OPTED_IN)
        applyCampaignTypeSelection(CampaignType.PROMOTIONAL_OFFER, forceTemplate = true)

        btnSendCampaign.setOnClickListener {
            sendCampaign()
        }
    }

    private fun bindViews(view: View) {
        tvAudienceAllOptedIn = view.findViewById(R.id.tvAudienceAllOptedIn)
        tvAudienceBirthdayToday = view.findViewById(R.id.tvAudienceBirthdayToday)
        tvAudienceNoOrders = view.findViewById(R.id.tvAudienceNoOrders)
        tvAudienceLoyal = view.findViewById(R.id.tvAudienceLoyal)
        tvAudienceInactive = view.findViewById(R.id.tvAudienceInactive)
        tvAudienceCloseReward = view.findViewById(R.id.tvAudienceCloseReward)
        tvAudienceDirectOrder = view.findViewById(R.id.tvAudienceDirectOrder)
        tvAudienceDirectCustomer = view.findViewById(R.id.tvAudienceDirectCustomer)

        tvAudienceRuleLabel = view.findViewById(R.id.tvAudienceRuleLabel)
        tilAudienceRule = view.findViewById(R.id.tilAudienceRule)
        etAudienceRule = view.findViewById(R.id.etAudienceRule)

        btnTypePromo = view.findViewById(R.id.btnTypePromo)
        btnTypeBeans = view.findViewById(R.id.btnTypeBeans)
        btnTypeOfferBeans = view.findViewById(R.id.btnTypeOfferBeans)

        tvBuilderHint = view.findViewById(R.id.tvBuilderHint)
        etCampaignTitle = view.findViewById(R.id.etCampaignTitle)
        etCampaignMessage = view.findViewById(R.id.etCampaignMessage)
        tilBeansAmount = view.findViewById(R.id.tilBeansAmount)
        etBeansAmount = view.findViewById(R.id.etBeansAmount)

        tvPreviewAudienceValue = view.findViewById(R.id.tvPreviewAudienceValue)
        tvPreviewRecipientsValue = view.findViewById(R.id.tvPreviewRecipientsValue)
        tvPreviewExcludedValue = view.findViewById(R.id.tvPreviewExcludedValue)
        tvPreviewBeansTotalValue = view.findViewById(R.id.tvPreviewBeansTotalValue)
        tvPreviewModeNote = view.findViewById(R.id.tvPreviewModeNote)
        tvPreviewSummary = view.findViewById(R.id.tvPreviewSummary)
        btnSendCampaign = view.findViewById(R.id.btnSendCampaign)
    }

    private fun setupAudienceClicks() {
        tvAudienceAllOptedIn.setOnClickListener { applyAudienceSelection(AudienceMode.ALL_OPTED_IN) }
        tvAudienceBirthdayToday.setOnClickListener { applyAudienceSelection(AudienceMode.BIRTHDAY_TODAY) }
        tvAudienceNoOrders.setOnClickListener { applyAudienceSelection(AudienceMode.NO_ORDERS) }
        tvAudienceLoyal.setOnClickListener { applyAudienceSelection(AudienceMode.LOYAL_CUSTOMERS) }
        tvAudienceInactive.setOnClickListener { applyAudienceSelection(AudienceMode.INACTIVE_USERS) }
        tvAudienceCloseReward.setOnClickListener { applyAudienceSelection(AudienceMode.CLOSE_TO_REWARD) }
        tvAudienceDirectOrder.setOnClickListener { applyAudienceSelection(AudienceMode.DIRECT_ORDER) }
        tvAudienceDirectCustomer.setOnClickListener { applyAudienceSelection(AudienceMode.DIRECT_CUSTOMER) }
    }

    private fun setupCampaignTypeClicks() {
        btnTypePromo.setOnClickListener {
            applyCampaignTypeSelection(CampaignType.PROMOTIONAL_OFFER)
        }

        btnTypeBeans.setOnClickListener {
            applyCampaignTypeSelection(CampaignType.BONUS_BEANS)
        }

        btnTypeOfferBeans.setOnClickListener {
            applyCampaignTypeSelection(CampaignType.OFFER_PLUS_BEANS)
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                refreshPreview()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        etAudienceRule.addTextChangedListener(watcher)
        etCampaignTitle.addTextChangedListener(watcher)
        etCampaignMessage.addTextChangedListener(watcher)
        etBeansAmount.addTextChangedListener(watcher)
    }

    private fun applyAudienceSelection(mode: AudienceMode) {
        selectedAudience = mode
        updateAudienceChipStyles()
        updateAudienceRuleUi()
        refreshPreview()
    }

    private fun applyCampaignTypeSelection(
        type: CampaignType,
        forceTemplate: Boolean = false
    ) {
        selectedCampaignType = type
        updateCampaignTypeStyles()
        updateCampaignBuilderUi()
        applyTemplateForCampaignType(type, forceTemplate)
        refreshPreview()
    }

    private fun updateAudienceChipStyles() {
        styleAudienceChip(tvAudienceAllOptedIn, selectedAudience == AudienceMode.ALL_OPTED_IN)
        styleAudienceChip(tvAudienceBirthdayToday, selectedAudience == AudienceMode.BIRTHDAY_TODAY)
        styleAudienceChip(tvAudienceNoOrders, selectedAudience == AudienceMode.NO_ORDERS)
        styleAudienceChip(tvAudienceLoyal, selectedAudience == AudienceMode.LOYAL_CUSTOMERS)
        styleAudienceChip(tvAudienceInactive, selectedAudience == AudienceMode.INACTIVE_USERS)
        styleAudienceChip(tvAudienceCloseReward, selectedAudience == AudienceMode.CLOSE_TO_REWARD)
        styleAudienceChip(tvAudienceDirectOrder, selectedAudience == AudienceMode.DIRECT_ORDER)
        styleAudienceChip(tvAudienceDirectCustomer, selectedAudience == AudienceMode.DIRECT_CUSTOMER)
    }

    private fun styleAudienceChip(view: TextView, selected: Boolean) {
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

    private fun updateCampaignTypeStyles() {
        styleTypeButton(btnTypePromo, selectedCampaignType == CampaignType.PROMOTIONAL_OFFER)
        styleTypeButton(btnTypeBeans, selectedCampaignType == CampaignType.BONUS_BEANS)
        styleTypeButton(btnTypeOfferBeans, selectedCampaignType == CampaignType.OFFER_PLUS_BEANS)
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

    private fun updateAudienceRuleUi() {
        when (selectedAudience) {
            AudienceMode.LOYAL_CUSTOMERS -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Minimum orders"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter minimum orders"
                etAudienceRule.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AudienceMode.INACTIVE_USERS -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Inactive for at least X days"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter days"
                etAudienceRule.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AudienceMode.CLOSE_TO_REWARD -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Within X beans"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter beans gap"
                etAudienceRule.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AudienceMode.DIRECT_ORDER -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Order number"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter order number"
                etAudienceRule.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AudienceMode.DIRECT_CUSTOMER -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Customer ID"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter customer ID"
                etAudienceRule.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            else -> {
                tvAudienceRuleLabel.visibility = View.GONE
                tilAudienceRule.visibility = View.GONE
                etAudienceRule.setText("")
            }
        }
    }

    private fun updateCampaignBuilderUi() {
        tilBeansAmount.visibility =
            if (selectedCampaignType.includesBeans) View.VISIBLE else View.GONE

        if (!selectedCampaignType.includesBeans) {
            etBeansAmount.setText("")
        }

        tvBuilderHint.text = when (selectedCampaignType) {
            CampaignType.PROMOTIONAL_OFFER -> {
                "Promotional consent will be applied. Use [FIRST_NAME] and [LAST_NAME] if you want light personalisation."
            }

            CampaignType.BONUS_BEANS -> {
                "Bonus beans will be added automatically when the campaign is sent. Use [BEANS_AMOUNT] in the message if you want to mention the reward."
            }

            CampaignType.OFFER_PLUS_BEANS -> {
                "This campaign sends a promo message and awards beans at the same time. Promotional consent will be applied."
            }
        }
    }

    private fun applyTemplateForCampaignType(
        type: CampaignType,
        forceTemplate: Boolean
    ) {
        val shouldApply = forceTemplate ||
                etCampaignTitle.text.isNullOrBlank() ||
                etCampaignMessage.text.isNullOrBlank()

        if (!shouldApply) return

        etCampaignTitle.setText(defaultTitle(type))
        etCampaignMessage.setText(defaultBody(type))
    }

    private fun defaultTitle(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> "A special KoffeeCraft offer for you"
            CampaignType.BONUS_BEANS -> "A beans reward from KoffeeCraft"
            CampaignType.OFFER_PLUS_BEANS -> "A special offer + bonus beans from KoffeeCraft"
        }
    }

    private fun defaultBody(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> {
                """
Hello [FIRST_NAME],

We have prepared something special for you at KoffeeCraft.

Offer details:
[ADD_YOUR_OFFER_HERE]

See you soon,
KoffeeCraft
                """.trimIndent()
            }

            CampaignType.BONUS_BEANS -> {
                """
Hello [FIRST_NAME],

We have added [BEANS_AMOUNT] bonus beans to your KoffeeCraft account.

Enjoy your reward and keep crafting your next favourite order.

KoffeeCraft
                """.trimIndent()
            }

            CampaignType.OFFER_PLUS_BEANS -> {
                """
Hello [FIRST_NAME],

We have prepared a special KoffeeCraft offer for you and added [BEANS_AMOUNT] bonus beans to your account.

Offer details:
[ADD_YOUR_OFFER_HERE]

Enjoy,
KoffeeCraft
                """.trimIndent()
            }
        }
    }

    private fun refreshPreview() {
        previewJob?.cancel()

        val selectedAudienceSnapshot = selectedAudience
        val selectedCampaignTypeSnapshot = selectedCampaignType
        val audienceRuleInput = etAudienceRule.text?.toString()?.trim().orEmpty()
        val titleInput = etCampaignTitle.text?.toString()?.trim().orEmpty()
        val messageInput = etCampaignMessage.text?.toString()?.trim().orEmpty()
        val beansInput = etBeansAmount.text?.toString()?.trim().orEmpty()

        previewJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val preview = buildPreviewSnapshot(
                selectedAudience = selectedAudienceSnapshot,
                selectedCampaignType = selectedCampaignTypeSnapshot,
                audienceRuleInput = audienceRuleInput,
                titleInput = titleInput,
                messageInput = messageInput,
                beansInput = beansInput
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                currentPreview = preview
                renderPreview(preview)
            }
        }
    }

    private suspend fun buildPreviewSnapshot(
        selectedAudience: AudienceMode,
        selectedCampaignType: CampaignType,
        audienceRuleInput: String,
        titleInput: String,
        messageInput: String,
        beansInput: String
    ): CampaignPreview {
        val allTargets = when (selectedAudience) {
            AudienceMode.DIRECT_ORDER,
            AudienceMode.DIRECT_CUSTOMER -> emptyList()
            else -> db.customerDao().getAllCampaignTargets()
        }

        val baseTargets = when (selectedAudience) {
            AudienceMode.ALL_OPTED_IN -> {
                allTargets
            }

            AudienceMode.BIRTHDAY_TODAY -> {
                val monthDay = SimpleDateFormat("MM-dd", Locale.UK).format(Date())
                allTargets.filter { target ->
                    target.dateOfBirth?.let { dob ->
                        dob.length >= 10 && dob.substring(5, 10) == monthDay
                    } == true
                }
            }

            AudienceMode.NO_ORDERS -> {
                allTargets.filter { it.orderCount == 0 }
            }

            AudienceMode.LOYAL_CUSTOMERS -> {
                val minOrders = audienceRuleInput.toIntOrNull()
                if (minOrders == null || minOrders <= 0) {
                    emptyList()
                } else {
                    allTargets.filter { it.orderCount >= minOrders }
                }
            }

            AudienceMode.INACTIVE_USERS -> {
                val inactiveDays = audienceRuleInput.toIntOrNull()
                if (inactiveDays == null || inactiveDays <= 0) {
                    emptyList()
                } else {
                    val now = System.currentTimeMillis()
                    allTargets.filter { target ->
                        val lastOrderAt = target.lastOrderAt
                        target.orderCount > 0 &&
                                lastOrderAt != null &&
                                ((now - lastOrderAt) / MILLIS_PER_DAY) >= inactiveDays
                    }
                }
            }

            AudienceMode.CLOSE_TO_REWARD -> {
                val withinBeans = audienceRuleInput.toIntOrNull()
                if (withinBeans == null || withinBeans <= 0) {
                    emptyList()
                } else {
                    allTargets.filter { target ->
                        val gap = nearestRewardGap(target.beansBalance)
                        gap != null && gap in 1..withinBeans
                    }
                }
            }

            AudienceMode.DIRECT_ORDER -> {
                val orderId = audienceRuleInput.toLongOrNull()
                if (orderId == null) {
                    emptyList()
                } else {
                    listOfNotNull(db.customerDao().getCampaignTargetByOrderId(orderId))
                }
            }

            AudienceMode.DIRECT_CUSTOMER -> {
                val customerId = audienceRuleInput.toLongOrNull()
                if (customerId == null) {
                    emptyList()
                } else {
                    listOfNotNull(db.customerDao().getCampaignTargetByCustomerId(customerId))
                }
            }
        }

        val beansAmount = if (selectedCampaignType.includesBeans) {
            beansInput.toIntOrNull()?.takeIf { it > 0 } ?: 0
        } else {
            0
        }

        val eligibleTargets = if (selectedCampaignType.requiresMarketingConsent) {
            baseTargets.filter { it.marketingInboxConsent }
        } else {
            baseTargets
        }

        val excludedByConsent = if (selectedCampaignType.requiresMarketingConsent) {
            baseTargets.size - eligibleTargets.size
        } else {
            0
        }

        val totalBeans = eligibleTargets.size * beansAmount

        val canSend = titleInput.isNotBlank() &&
                messageInput.isNotBlank() &&
                eligibleTargets.isNotEmpty() &&
                (!selectedCampaignType.includesBeans || beansAmount > 0)

        return CampaignPreview(
            audienceLabel = buildAudienceLabel(selectedAudience, audienceRuleInput),
            recipientsCount = eligibleTargets.size,
            excludedByConsent = excludedByConsent,
            beansAmountPerCustomer = beansAmount,
            totalBeansToGrant = totalBeans,
            previewNote = buildPreviewNote(selectedCampaignType),
            summaryText = buildSummaryText(eligibleTargets.size),
            canSend = canSend,
            eligibleTargets = eligibleTargets,
            deliveryType = buildDeliveryType(selectedCampaignType)
        )
    }

    private fun renderPreview(preview: CampaignPreview) {
        tvPreviewAudienceValue.text = "Audience • ${preview.audienceLabel}"
        tvPreviewRecipientsValue.text = "Recipients • ${preview.recipientsCount}"
        tvPreviewExcludedValue.text = "Excluded by consent • ${preview.excludedByConsent}"
        tvPreviewBeansTotalValue.text = "Total bonus beans • ${preview.totalBeansToGrant}"
        tvPreviewModeNote.text = preview.previewNote
        tvPreviewSummary.text = preview.summaryText

        btnSendCampaign.isEnabled = preview.canSend
        btnSendCampaign.alpha = if (preview.canSend) 1f else 0.5f
    }

    private fun sendCampaign() {
        val titleInput = etCampaignTitle.text?.toString()?.trim().orEmpty()
        val messageInput = etCampaignMessage.text?.toString()?.trim().orEmpty()
        val audienceRuleInput = etAudienceRule.text?.toString()?.trim().orEmpty()
        val beansInput = etBeansAmount.text?.toString()?.trim().orEmpty()
        val selectedAudienceSnapshot = selectedAudience
        val selectedCampaignTypeSnapshot = selectedCampaignType

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val preview = buildPreviewSnapshot(
                selectedAudience = selectedAudienceSnapshot,
                selectedCampaignType = selectedCampaignTypeSnapshot,
                audienceRuleInput = audienceRuleInput,
                titleInput = titleInput,
                messageInput = messageInput,
                beansInput = beansInput
            )

            if (!preview.canSend) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Complete the campaign details and make sure there is at least one eligible recipient.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            val uniqueTargets = preview.eligibleTargets.distinctBy { it.customerId }

            val messages = uniqueTargets.map { target ->
                InboxMessage(
                    recipientCustomerId = target.customerId,
                    title = resolveTemplate(titleInput, target, preview.beansAmountPerCustomer),
                    body = resolveTemplate(messageInput, target, preview.beansAmountPerCustomer),
                    deliveryType = preview.deliveryType
                )
            }

            db.inboxMessageDao().insertAll(messages)

            if (preview.beansAmountPerCustomer > 0) {
                uniqueTargets.forEach { target ->
                    db.customerDao().addBeansToCustomer(
                        customerId = target.customerId,
                        beansAmount = preview.beansAmountPerCustomer
                    )
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Campaign sent to ${messages.size} recipient(s).",
                    Toast.LENGTH_SHORT
                ).show()

                applyCampaignTypeSelection(selectedCampaignTypeSnapshot, forceTemplate = true)
                etAudienceRule.setText("")
                etBeansAmount.setText("")
                refreshPreview()
            }
        }
    }

    private fun resolveTemplate(
        raw: String,
        target: CustomerCampaignTarget,
        beansAmount: Int
    ): String {
        return raw
            .replace("[FIRST_NAME]", target.firstName.ifBlank { "Customer" })
            .replace("[LAST_NAME]", target.lastName.ifBlank { "" })
            .replace("[BEANS_AMOUNT]", beansAmount.toString())
    }

    private fun buildAudienceLabel(
        mode: AudienceMode,
        rule: String
    ): String {
        return when (mode) {
            AudienceMode.ALL_OPTED_IN -> "All opted-in customers"
            AudienceMode.BIRTHDAY_TODAY -> "Birthday today"
            AudienceMode.NO_ORDERS -> "Customers with 0 orders"
            AudienceMode.LOYAL_CUSTOMERS -> {
                if (rule.isBlank()) "Loyal customers"
                else "Loyal customers • min $rule orders"
            }

            AudienceMode.INACTIVE_USERS -> {
                if (rule.isBlank()) "Inactive users"
                else "Inactive users • $rule days"
            }

            AudienceMode.CLOSE_TO_REWARD -> {
                if (rule.isBlank()) "Close to reward"
                else "Close to reward • within $rule beans"
            }

            AudienceMode.DIRECT_ORDER -> {
                if (rule.isBlank()) "Direct by order"
                else "Direct by order • #$rule"
            }

            AudienceMode.DIRECT_CUSTOMER -> {
                if (rule.isBlank()) "Direct by customer"
                else "Direct by customer • #$rule"
            }
        }
    }

    private fun buildPreviewNote(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> {
                "Promotional consent applies. Customers who opted out will be excluded automatically."
            }

            CampaignType.BONUS_BEANS -> {
                "Beans rewards will be added automatically when this campaign is sent."
            }

            CampaignType.OFFER_PLUS_BEANS -> {
                "Promotional consent applies and bonus beans will also be granted automatically."
            }
        }
    }

    private fun buildSummaryText(recipientsCount: Int): String {
        return when {
            recipientsCount <= 0 -> "No customers will receive this message"
            recipientsCount == 1 -> "1 customer will receive this message"
            else -> "$recipientsCount customers will receive this message"
        }
    }

    private fun buildDeliveryType(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> "PROMO_STUDIO_OFFER"
            CampaignType.BONUS_BEANS -> "PROMO_STUDIO_BEANS"
            CampaignType.OFFER_PLUS_BEANS -> "PROMO_STUDIO_OFFER_BEANS"
        }
    }

    private fun nearestRewardGap(beansBalance: Int): Int? {
        val nextThreshold = REWARD_THRESHOLDS.firstOrNull { threshold ->
            beansBalance < threshold
        } ?: return null

        return nextThreshold - beansBalance
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private val REWARD_THRESHOLDS = listOf(15, 18, 125, 250, 370)
    }
}

private data class CampaignPreview(
    val audienceLabel: String,
    val recipientsCount: Int,
    val excludedByConsent: Int,
    val beansAmountPerCustomer: Int,
    val totalBeansToGrant: Int,
    val previewNote: String,
    val summaryText: String,
    val canSend: Boolean,
    val eligibleTargets: List<CustomerCampaignTarget>,
    val deliveryType: String
) {
    companion object {
        fun empty(): CampaignPreview {
            return CampaignPreview(
                audienceLabel = "All opted-in customers",
                recipientsCount = 0,
                excludedByConsent = 0,
                beansAmountPerCustomer = 0,
                totalBeansToGrant = 0,
                previewNote = "Promotional consent applies. Customers who opted out will be excluded automatically.",
                summaryText = "No customers will receive this message",
                canSend = false,
                eligibleTargets = emptyList(),
                deliveryType = "PROMO_STUDIO_OFFER"
            )
        }
    }
}