package uk.ac.dmu.koffeecraft.ui.admin.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerInboxTarget
import uk.ac.dmu.koffeecraft.data.repository.AdminInboxRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminInboxSendResult

data class AdminInboxUiState(
    val currentTargetMode: AdminInboxTargetMode = AdminInboxTargetMode.ORDER_NUMBER,
    val currentMessageType: AdminInboxMessageType = AdminInboxMessageType.CUSTOM,
    val searchHint: String = "Find customer by order number",
    val searchFieldHint: String = "Enter order number",
    val messageHint: String = "Write a direct message for one customer.",
    val selectedTarget: CustomerInboxTarget? = null,
    val selectedTargetVisible: Boolean = false,
    val targetsEmptyVisible: Boolean = false,
    val selectedTargetText: String = "",
    val targetMetaText: String = "",
    val consentStatusText: String = "",
    val consentStatusColorRes: Int = 0,
    val audienceSummaryText: String = "",
    val canSend: Boolean = false
)

class AdminInboxViewModel(
    private val repository: AdminInboxRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class ApplyTemplate(val title: String, val body: String) : UiEffect
        data object ClearSearch : UiEffect
    }

    private val _state = MutableStateFlow(
        AdminInboxUiState(
            consentStatusColorRes = uk.ac.dmu.koffeecraft.R.color.kc_danger
        )
    )
    val state: StateFlow<AdminInboxUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun initialise() {
        applyTargetMode(AdminInboxTargetMode.ORDER_NUMBER)
        applyMessageType(
            type = AdminInboxMessageType.CUSTOM,
            currentTitle = "",
            currentBody = "",
            forceTemplate = true
        )
    }

    fun applyTargetMode(mode: AdminInboxTargetMode) {
        _state.value = _state.value.copy(
            currentTargetMode = mode,
            searchHint = when (mode) {
                AdminInboxTargetMode.ORDER_NUMBER -> "Find customer by order number"
                AdminInboxTargetMode.CUSTOMER_ID -> "Find customer by customer ID"
            },
            searchFieldHint = when (mode) {
                AdminInboxTargetMode.ORDER_NUMBER -> "Enter order number"
                AdminInboxTargetMode.CUSTOMER_ID -> "Enter customer ID"
            },
            selectedTarget = null,
            selectedTargetVisible = false,
            targetsEmptyVisible = false,
            selectedTargetText = "",
            targetMetaText = "",
            consentStatusText = "",
            audienceSummaryText = "",
            canSend = false
        )

        viewModelScope.launch {
            _effects.send(UiEffect.ClearSearch)
        }
    }

    fun applyMessageType(
        type: AdminInboxMessageType,
        currentTitle: String,
        currentBody: String,
        forceTemplate: Boolean = false
    ) {
        _state.value = _state.value.copy(
            currentMessageType = type,
            messageHint = when (type) {
                AdminInboxMessageType.IMPORTANT ->
                    "Use this for direct important notices linked to one customer or one order."
                AdminInboxMessageType.SERVICE ->
                    "Use this for direct service follow-ups, order issues, or operational updates."
                AdminInboxMessageType.CUSTOM ->
                    "Write a custom direct message for one customer."
            }
        )

        val shouldApplyTemplate = forceTemplate || currentTitle.isBlank() || currentBody.isBlank()
        if (shouldApplyTemplate) {
            viewModelScope.launch {
                _effects.send(
                    UiEffect.ApplyTemplate(
                        title = defaultTitle(type),
                        body = defaultBody(type)
                    )
                )
            }
        }
    }

    fun onDraftChanged(
        title: String,
        body: String
    ) {
        _state.update { current ->
            current.copy(
                canSend = current.selectedTarget != null &&
                        title.isNotBlank() &&
                        body.isNotBlank()
            )
        }
    }

    fun performTargetLookup(rawQuery: String) {
        if (rawQuery.isBlank()) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Enter a value first."))
            }
            return
        }

        viewModelScope.launch {
            val result = repository.findTarget(_state.value.currentTargetMode, rawQuery.trim())

            if (result == null) {
                _state.update { current ->
                    current.copy(
                        selectedTarget = null,
                        selectedTargetVisible = false,
                        targetsEmptyVisible = true,
                        selectedTargetText = "",
                        targetMetaText = "",
                        consentStatusText = "",
                        audienceSummaryText = "",
                        canSend = false
                    )
                }
            } else {
                _state.update { current ->
                    current.copy(
                        selectedTarget = result,
                        selectedTargetVisible = true,
                        targetsEmptyVisible = false,
                        selectedTargetText = "${result.firstName} ${result.lastName}".trim()
                            .ifBlank { "Customer #${result.customerId}" },
                        targetMetaText = "Customer #${result.customerId} • ${result.email}",
                        consentStatusText = if (result.marketingInboxConsent) {
                            "Marketing consent: ON • Promotional campaigns are available in Studio"
                        } else {
                            "Marketing consent: OFF • Direct service messages can still be sent here"
                        },
                        consentStatusColorRes = if (result.marketingInboxConsent) {
                            uk.ac.dmu.koffeecraft.R.color.kc_info_text
                        } else {
                            uk.ac.dmu.koffeecraft.R.color.kc_danger
                        },
                        audienceSummaryText = "1 customer will receive this direct message"
                    )
                }
            }
        }
    }

    fun sendDirectMessage(
        title: String,
        body: String
    ) {
        val target = _state.value.selectedTarget
        if (target == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Find a customer first."))
            }
            return
        }

        viewModelScope.launch {
            when (
                val result = repository.sendDirectMessage(
                    target = target,
                    title = title.trim(),
                    body = body.trim(),
                    messageType = _state.value.currentMessageType
                )
            ) {
                is AdminInboxSendResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))

                    _state.update {
                        it.copy(
                            currentMessageType = AdminInboxMessageType.CUSTOM,
                            messageHint = "Write a custom direct message for one customer.",
                            canSend = false
                        )
                    }

                    _effects.send(
                        UiEffect.ApplyTemplate(
                            title = defaultTitle(AdminInboxMessageType.CUSTOM),
                            body = defaultBody(AdminInboxMessageType.CUSTOM)
                        )
                    )
                }

                is AdminInboxSendResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun defaultTitle(type: AdminInboxMessageType): String {
        return when (type) {
            AdminInboxMessageType.IMPORTANT -> "Important KoffeeCraft Notice"
            AdminInboxMessageType.SERVICE -> "KoffeeCraft Service Update"
            AdminInboxMessageType.CUSTOM -> "Message from KoffeeCraft"
        }
    }

    private fun defaultBody(type: AdminInboxMessageType): String {
        return when (type) {
            AdminInboxMessageType.IMPORTANT -> {
                """
Hello,

This is an important message from KoffeeCraft regarding your account or recent order.

Please review the update and contact us if you need any support.

KoffeeCraft
                """.trimIndent()
            }

            AdminInboxMessageType.SERVICE -> {
                """
Hello,

We are contacting you with a service update related to your recent KoffeeCraft experience.

If you need any help, please let us know.

KoffeeCraft
                """.trimIndent()
            }

            AdminInboxMessageType.CUSTOM -> {
                """
Hello,

[WRITE_YOUR_MESSAGE_HERE]

KoffeeCraft
                """.trimIndent()
            }
        }
    }

    class Factory(
        private val repository: AdminInboxRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminInboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminInboxViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}