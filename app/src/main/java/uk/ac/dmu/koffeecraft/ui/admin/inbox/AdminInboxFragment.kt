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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class AdminInboxFragment : Fragment(R.layout.fragment_admin_inbox) {

    private lateinit var viewModel: AdminInboxViewModel

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminInboxViewModel.Factory(appContainer.adminInboxRepository)
        )[AdminInboxViewModel::class.java]

        bindViews(view)
        setupModeClicks()
        setupTypeClicks()
        setupTextWatchers()
        observeState()

        btnSearch.setOnClickListener {
            viewModel.performTargetLookup(etSearch.text?.toString().orEmpty())
        }

        btnSendMessage.setOnClickListener {
            viewModel.sendDirectMessage(
                title = etMessageTitle.text?.toString().orEmpty(),
                body = etMessageBody.text?.toString().orEmpty()
            )
        }

        viewModel.initialise()
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
            viewModel.applyTargetMode(AdminInboxTargetMode.ORDER_NUMBER)
        }

        tvModeCustomerId.setOnClickListener {
            viewModel.applyTargetMode(AdminInboxTargetMode.CUSTOMER_ID)
        }
    }

    private fun setupTypeClicks() {
        btnTemplateImportant.setOnClickListener {
            viewModel.applyMessageType(
                type = AdminInboxMessageType.IMPORTANT,
                currentTitle = etMessageTitle.text?.toString().orEmpty(),
                currentBody = etMessageBody.text?.toString().orEmpty()
            )
        }

        btnTemplateService.setOnClickListener {
            viewModel.applyMessageType(
                type = AdminInboxMessageType.SERVICE,
                currentTitle = etMessageTitle.text?.toString().orEmpty(),
                currentBody = etMessageBody.text?.toString().orEmpty()
            )
        }

        btnTemplateCustom.setOnClickListener {
            viewModel.applyMessageType(
                type = AdminInboxMessageType.CUSTOM,
                currentTitle = etMessageTitle.text?.toString().orEmpty(),
                currentBody = etMessageBody.text?.toString().orEmpty()
            )
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onDraftChanged(
                    title = etMessageTitle.text?.toString().orEmpty(),
                    body = etMessageBody.text?.toString().orEmpty()
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        etMessageTitle.addTextChangedListener(watcher)
        etMessageBody.addTextChangedListener(watcher)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                renderState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is AdminInboxViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    is AdminInboxViewModel.UiEffect.ApplyTemplate -> {
                        etMessageTitle.setText(effect.title)
                        etMessageBody.setText(effect.body)
                    }

                    AdminInboxViewModel.UiEffect.ClearSearch -> {
                        etSearch.setText("")
                    }
                }
            }
        }
    }

    private fun renderState(state: AdminInboxUiState) {
        styleModeChip(tvModeOrderNumber, state.currentTargetMode == AdminInboxTargetMode.ORDER_NUMBER)
        styleModeChip(tvModeCustomerId, state.currentTargetMode == AdminInboxTargetMode.CUSTOMER_ID)

        tilSearch.hint = state.searchFieldHint
        tvSearchHint.text = state.searchHint
        etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        styleTypeButton(btnTemplateImportant, state.currentMessageType == AdminInboxMessageType.IMPORTANT)
        styleTypeButton(btnTemplateService, state.currentMessageType == AdminInboxMessageType.SERVICE)
        styleTypeButton(btnTemplateCustom, state.currentMessageType == AdminInboxMessageType.CUSTOM)

        tvMessageHint.text = state.messageHint

        tvTargetsEmpty.visibility = if (state.targetsEmptyVisible) View.VISIBLE else View.GONE
        cardSelectedTarget.visibility = if (state.selectedTargetVisible) View.VISIBLE else View.GONE

        tvSelectedTarget.text = state.selectedTargetText
        tvTargetMeta.text = state.targetMetaText
        tvConsentStatus.text = state.consentStatusText
        tvConsentStatus.setTextColor(color(state.consentStatusColorRes))
        tvAudienceSummary.text = state.audienceSummaryText

        btnSendMessage.isEnabled = state.canSend
        btnSendMessage.alpha = if (state.canSend) 1f else 0.5f
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

    private fun styleTypeButton(button: MaterialButton, selected: Boolean) {
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_brand_strong))
            button.setTextColor(color(R.color.kc_text_inverse))
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_surface_chip))
            button.setTextColor(color(R.color.kc_text_primary))
            button.strokeWidth = 1
            button.strokeColor = ColorStateList.valueOf(color(R.color.kc_border_warm))
        }
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}