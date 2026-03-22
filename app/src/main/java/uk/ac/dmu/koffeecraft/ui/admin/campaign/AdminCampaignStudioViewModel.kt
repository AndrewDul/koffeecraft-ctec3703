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
        val preview: AdminCampaignPreview = AdminCampaignPreview.empty(),
        val isSending: Boolean = false
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class CampaignSent(val recipientsCount: Int) : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var previewJob: Job? = null

    fun refreshPreview(
        selectedAudience: CampaignAudienceMode,
        selectedCampaignType: CampaignType,
        audienceRuleInput: String,
        titleInput: String,
        messageInput: String,
        beansInput: String
    ) {
        previewJob?.cancel()

        previewJob = viewModelScope.launch {
            val preview = adminCampaignRepository.buildPreview(
                selectedAudience = selectedAudience,
                selectedCampaignType = selectedCampaignType,
                audienceRuleInput = audienceRuleInput,
                titleInput = titleInput,
                messageInput = messageInput,
                beansInput = beansInput
            )

            _state.value = _state.value.copy(preview = preview)
        }
    }

    fun sendCampaign(
        selectedAudience: CampaignAudienceMode,
        selectedCampaignType: CampaignType,
        audienceRuleInput: String,
        titleInput: String,
        messageInput: String,
        beansInput: String
    ) {
        _state.value = _state.value.copy(isSending = true)

        viewModelScope.launch {
            when (
                val result = adminCampaignRepository.sendCampaign(
                    selectedAudience = selectedAudience,
                    selectedCampaignType = selectedCampaignType,
                    audienceRuleInput = audienceRuleInput,
                    titleInput = titleInput,
                    messageInput = messageInput,
                    beansInput = beansInput
                )
            ) {
                is AdminCampaignSendResult.Success -> {
                    _state.value = _state.value.copy(isSending = false)
                    _effects.send(UiEffect.ShowMessage("Campaign sent to ${result.recipientsCount} recipient(s)."))
                    _effects.send(UiEffect.CampaignSent(result.recipientsCount))
                }

                is AdminCampaignSendResult.Error -> {
                    _state.value = _state.value.copy(isSending = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
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