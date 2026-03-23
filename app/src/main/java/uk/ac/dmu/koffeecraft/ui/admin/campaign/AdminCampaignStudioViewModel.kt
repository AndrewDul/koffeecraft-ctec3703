package uk.ac.dmu.koffeecraft.ui.admin.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AdminCampaignPreview
import uk.ac.dmu.koffeecraft.data.repository.AdminCampaignRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminCampaignSendResult
import uk.ac.dmu.koffeecraft.data.repository.CampaignAudienceMode
import uk.ac.dmu.koffeecraft.data.repository.CampaignType

class AdminCampaignStudioViewModel(
    private val adminCampaignRepository: AdminCampaignRepository
) : ViewModel() {

    data class UiState(
        val selectedAudience: CampaignAudienceMode = CampaignAudienceMode.ALL_OPTED_IN,
        val selectedCampaignType: CampaignType = CampaignType.PROMOTIONAL_OFFER,
        val audienceRuleInput: String = "",
        val titleInput: String = "",
        val messageInput: String = "",
        val beansInput: String = "",
        val preview: AdminCampaignPreview = AdminCampaignPreview.empty(),
        val isSending: Boolean = false
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var previewJob: Job? = null

    init {
        refreshPreview(_state.value)
    }

    fun selectAudience(mode: CampaignAudienceMode) {
        val current = _state.value
        if (current.selectedAudience == mode) return

        val nextState = current.copy(
            selectedAudience = mode,
            audienceRuleInput = if (requiresAudienceRule(mode)) {
                current.audienceRuleInput
            } else {
                ""
            }
        )

        _state.value = nextState
        refreshPreview(nextState)
    }

    fun selectCampaignType(type: CampaignType, forceTemplate: Boolean = false) {
        val current = _state.value
        if (!forceTemplate && current.selectedCampaignType == type) return

        val shouldApplyTemplate = forceTemplate ||
                current.titleInput.isBlank() ||
                current.messageInput.isBlank()

        val nextState = current.copy(
            selectedCampaignType = type,
            titleInput = if (shouldApplyTemplate) {
                AdminCampaignTemplates.defaultTitle(type)
            } else {
                current.titleInput
            },
            messageInput = if (shouldApplyTemplate) {
                AdminCampaignTemplates.defaultBody(type)
            } else {
                current.messageInput
            },
            beansInput = if (type.includesBeans) current.beansInput else ""
        )

        _state.value = nextState
        refreshPreview(nextState)
    }

    fun updateAudienceRule(value: String) {
        updateState { current ->
            current.copy(audienceRuleInput = value)
        }
    }

    fun updateTitle(value: String) {
        updateState { current ->
            current.copy(titleInput = value)
        }
    }

    fun updateMessage(value: String) {
        updateState { current ->
            current.copy(messageInput = value)
        }
    }

    fun updateBeans(value: String) {
        updateState { current ->
            current.copy(beansInput = value)
        }
    }

    fun sendCampaign() {
        val current = _state.value
        if (current.isSending) return

        _state.value = current.copy(isSending = true)

        viewModelScope.launch {
            when (
                val result = adminCampaignRepository.sendCampaign(
                    selectedAudience = current.selectedAudience,
                    selectedCampaignType = current.selectedCampaignType,
                    audienceRuleInput = current.audienceRuleInput,
                    titleInput = current.titleInput,
                    messageInput = current.messageInput,
                    beansInput = current.beansInput
                )
            ) {
                is AdminCampaignSendResult.Success -> {
                    val resetState = _state.value.copy(
                        audienceRuleInput = "",
                        titleInput = AdminCampaignTemplates.defaultTitle(_state.value.selectedCampaignType),
                        messageInput = AdminCampaignTemplates.defaultBody(_state.value.selectedCampaignType),
                        beansInput = "",
                        isSending = false
                    )

                    _state.value = resetState
                    refreshPreview(resetState)

                    _effects.send(
                        UiEffect.ShowMessage(
                            "Campaign sent to ${result.recipientsCount} recipient(s)."
                        )
                    )
                }

                is AdminCampaignSendResult.Error -> {
                    _state.value = _state.value.copy(isSending = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun updateState(transform: (UiState) -> UiState) {
        val nextState = transform(_state.value)
        _state.value = nextState
        refreshPreview(nextState)
    }

    private fun refreshPreview(snapshot: UiState) {
        previewJob?.cancel()

        previewJob = viewModelScope.launch {
            val preview = adminCampaignRepository.buildPreview(
                selectedAudience = snapshot.selectedAudience,
                selectedCampaignType = snapshot.selectedCampaignType,
                audienceRuleInput = snapshot.audienceRuleInput,
                titleInput = snapshot.titleInput,
                messageInput = snapshot.messageInput,
                beansInput = snapshot.beansInput
            )

            _state.value = _state.value.copy(preview = preview)
        }
    }

    private fun buildInitialState(): UiState {
        val initialType = CampaignType.PROMOTIONAL_OFFER
        return UiState(
            selectedAudience = CampaignAudienceMode.ALL_OPTED_IN,
            selectedCampaignType = initialType,
            titleInput = AdminCampaignTemplates.defaultTitle(initialType),
            messageInput = AdminCampaignTemplates.defaultBody(initialType)
        )
    }

    private fun requiresAudienceRule(mode: CampaignAudienceMode): Boolean {
        return when (mode) {
            CampaignAudienceMode.LOYAL_CUSTOMERS,
            CampaignAudienceMode.INACTIVE_USERS,
            CampaignAudienceMode.CLOSE_TO_REWARD,
            CampaignAudienceMode.DIRECT_ORDER,
            CampaignAudienceMode.DIRECT_CUSTOMER -> true

            CampaignAudienceMode.ALL_OPTED_IN,
            CampaignAudienceMode.BIRTHDAY_TODAY,
            CampaignAudienceMode.NO_ORDERS -> false
        }
    }

    class Factory(
        private val adminCampaignRepository: AdminCampaignRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminCampaignStudioViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminCampaignStudioViewModel(adminCampaignRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}