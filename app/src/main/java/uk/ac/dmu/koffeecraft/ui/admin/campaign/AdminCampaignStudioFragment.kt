package uk.ac.dmu.koffeecraft.ui.admin.campaign

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.repository.CampaignAudienceMode
import uk.ac.dmu.koffeecraft.data.repository.CampaignType

class AdminCampaignStudioFragment : Fragment(R.layout.fragment_admin_campaign_studio) {

    private lateinit var vm: AdminCampaignStudioViewModel

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

    private var selectedAudience = CampaignAudienceMode.ALL_OPTED_IN
    private var selectedCampaignType = CampaignType.PROMOTIONAL_OFFER

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            AdminCampaignStudioViewModel.Factory(appContainer.adminCampaignRepository)
        )[AdminCampaignStudioViewModel::class.java]

        bindViews(view)
        setupAudienceClicks()
        setupCampaignTypeClicks()
        setupTextWatchers()

        applyAudienceSelection(CampaignAudienceMode.ALL_OPTED_IN)
        applyCampaignTypeSelection(CampaignType.PROMOTIONAL_OFFER, forceTemplate = true)

        btnSendCampaign.setOnClickListener {
            vm.sendCampaign(
                selectedAudience = selectedAudience,
                selectedCampaignType = selectedCampaignType,
                audienceRuleInput = etAudienceRule.text?.toString()?.trim().orEmpty(),
                titleInput = etCampaignTitle.text?.toString()?.trim().orEmpty(),
                messageInput = etCampaignMessage.text?.toString()?.trim().orEmpty(),
                beansInput = etBeansAmount.text?.toString()?.trim().orEmpty()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                renderPreview(state.preview)

                btnSendCampaign.isEnabled = state.preview.canSend && !state.isSending
                btnSendCampaign.alpha = if (state.preview.canSend && !state.isSending) 1f else 0.5f
                btnSendCampaign.text = if (state.isSending) "Sending..." else "Send campaign"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is AdminCampaignStudioViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_LONG).show()
                    }

                    is AdminCampaignStudioViewModel.UiEffect.CampaignSent -> {
                        applyCampaignTypeSelection(selectedCampaignType, forceTemplate = true)
                        etAudienceRule.setText("")
                        etBeansAmount.setText("")
                        refreshPreview()
                    }
                }
            }
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
        tvAudienceAllOptedIn.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.ALL_OPTED_IN) }
        tvAudienceBirthdayToday.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.BIRTHDAY_TODAY) }
        tvAudienceNoOrders.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.NO_ORDERS) }
        tvAudienceLoyal.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.LOYAL_CUSTOMERS) }
        tvAudienceInactive.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.INACTIVE_USERS) }
        tvAudienceCloseReward.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.CLOSE_TO_REWARD) }
        tvAudienceDirectOrder.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.DIRECT_ORDER) }
        tvAudienceDirectCustomer.setOnClickListener { applyAudienceSelection(CampaignAudienceMode.DIRECT_CUSTOMER) }
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

    private fun applyAudienceSelection(mode: CampaignAudienceMode) {
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
        styleAudienceChip(tvAudienceAllOptedIn, selectedAudience == CampaignAudienceMode.ALL_OPTED_IN)
        styleAudienceChip(tvAudienceBirthdayToday, selectedAudience == CampaignAudienceMode.BIRTHDAY_TODAY)
        styleAudienceChip(tvAudienceNoOrders, selectedAudience == CampaignAudienceMode.NO_ORDERS)
        styleAudienceChip(tvAudienceLoyal, selectedAudience == CampaignAudienceMode.LOYAL_CUSTOMERS)
        styleAudienceChip(tvAudienceInactive, selectedAudience == CampaignAudienceMode.INACTIVE_USERS)
        styleAudienceChip(tvAudienceCloseReward, selectedAudience == CampaignAudienceMode.CLOSE_TO_REWARD)
        styleAudienceChip(tvAudienceDirectOrder, selectedAudience == CampaignAudienceMode.DIRECT_ORDER)
        styleAudienceChip(tvAudienceDirectCustomer, selectedAudience == CampaignAudienceMode.DIRECT_CUSTOMER)
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
            CampaignAudienceMode.LOYAL_CUSTOMERS -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Minimum orders"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter minimum orders"
                etAudienceRule.inputType = InputType.TYPE_CLASS_NUMBER
            }

            CampaignAudienceMode.INACTIVE_USERS -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Inactive for at least X days"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter days"
                etAudienceRule.inputType = InputType.TYPE_CLASS_NUMBER
            }

            CampaignAudienceMode.CLOSE_TO_REWARD -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Within X beans"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter beans gap"
                etAudienceRule.inputType = InputType.TYPE_CLASS_NUMBER
            }

            CampaignAudienceMode.DIRECT_ORDER -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Order number"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter order number"
                etAudienceRule.inputType = InputType.TYPE_CLASS_NUMBER
            }

            CampaignAudienceMode.DIRECT_CUSTOMER -> {
                tvAudienceRuleLabel.visibility = View.VISIBLE
                tvAudienceRuleLabel.text = "Customer ID"
                tilAudienceRule.visibility = View.VISIBLE
                tilAudienceRule.hint = "Enter customer ID"
                etAudienceRule.inputType = InputType.TYPE_CLASS_NUMBER
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

        tvBuilderHint.text = AdminCampaignTemplates.builderHint(selectedCampaignType)
    }

    private fun applyTemplateForCampaignType(
        type: CampaignType,
        forceTemplate: Boolean
    ) {
        val shouldApply = forceTemplate ||
                etCampaignTitle.text.isNullOrBlank() ||
                etCampaignMessage.text.isNullOrBlank()

        if (!shouldApply) return

        etCampaignTitle.setText(AdminCampaignTemplates.defaultTitle(type))
        etCampaignMessage.setText(AdminCampaignTemplates.defaultBody(type))
    }

    private fun refreshPreview() {
        vm.refreshPreview(
            selectedAudience = selectedAudience,
            selectedCampaignType = selectedCampaignType,
            audienceRuleInput = etAudienceRule.text?.toString()?.trim().orEmpty(),
            titleInput = etCampaignTitle.text?.toString()?.trim().orEmpty(),
            messageInput = etCampaignMessage.text?.toString()?.trim().orEmpty(),
            beansInput = etBeansAmount.text?.toString()?.trim().orEmpty()
        )
    }

    private fun renderPreview(preview: uk.ac.dmu.koffeecraft.data.repository.AdminCampaignPreview) {
        tvPreviewAudienceValue.text = "Audience • ${preview.audienceLabel}"
        tvPreviewRecipientsValue.text = "Recipients • ${preview.recipientsCount}"
        tvPreviewExcludedValue.text = "Excluded by consent • ${preview.excludedByConsent}"
        tvPreviewBeansTotalValue.text = "Total bonus beans • ${preview.totalBeansToGrant}"
        tvPreviewModeNote.text = preview.previewNote
        tvPreviewSummary.text = preview.summaryText
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}