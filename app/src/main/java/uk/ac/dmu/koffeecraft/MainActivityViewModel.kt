package uk.ac.dmu.koffeecraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.MainActivityRepository
import uk.ac.dmu.koffeecraft.data.repository.MainBootstrapResult

data class MainActivityUiState(
    val showCartBadge: Boolean = false,
    val cartBadgeText: String = "",
    val showInboxBadge: Boolean = false,
    val inboxBadgeText: String = "",
    val showNotificationBadge: Boolean = false,
    val notificationBadgeText: String = ""
)

class MainActivityViewModel(
    private val repository: MainActivityRepository
) : ViewModel() {

    sealed interface UiEffect {
        data object LaunchAdminActivity : UiEffect
        data class LaunchCustomerSession(
            val customerId: Long,
            val onboardingPending: Boolean
        ) : UiEffect

        data class NavigateOrderStatus(
            val orderId: Long
        ) : UiEffect

        data class NavigateCustomerInbox(
            val inboxMessageId: Long
        ) : UiEffect
    }

    private val _state = MutableStateFlow(MainActivityUiState())
    val state: StateFlow<MainActivityUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observedCustomerId: Long? = null
    private var badgeJob: Job? = null

    fun bootstrapRememberedSession() {
        viewModelScope.launch {
            when (val result = repository.bootstrapRememberedSession()) {
                MainBootstrapResult.None -> Unit
                MainBootstrapResult.LaunchAdmin -> {
                    _effects.send(UiEffect.LaunchAdminActivity)
                }

                is MainBootstrapResult.LaunchCustomer -> {
                    _effects.send(
                        UiEffect.LaunchCustomerSession(
                            customerId = result.customerId,
                            onboardingPending = result.onboardingPending
                        )
                    )
                }
            }
        }
    }

    fun bindActiveCustomerBadges() {
        bindCustomerBadges(repository.currentCustomerId())
    }

    fun currentCustomerId(): Long? = repository.currentCustomerId()

    fun isAdminSession(): Boolean = repository.isAdminSession()

    fun bindCustomerBadges(customerId: Long?) {
        if (customerId == null) {
            observedCustomerId = null
            badgeJob?.cancel()
            _state.value = MainActivityUiState()
            return
        }

        if (observedCustomerId == customerId) return
        observedCustomerId = customerId

        badgeJob?.cancel()
        badgeJob = viewModelScope.launch {
            repository.observeCustomerBadges(customerId).collect { data ->
                repository.deliverPromoNotifications(data.unreadPromoMessages)

                _state.value = MainActivityUiState(
                    showCartBadge = data.cartCount > 0,
                    cartBadgeText = badgeText(data.cartCount),
                    showInboxBadge = data.inboxUnreadCount > 0,
                    inboxBadgeText = badgeText(data.inboxUnreadCount),
                    showNotificationBadge = data.notificationUnreadCount > 0,
                    notificationBadgeText = badgeText(data.notificationUnreadCount)
                )
            }
        }
    }

    fun openOrderStatusFromNotification(
        customerId: Long,
        orderId: Long
    ) {
        viewModelScope.launch {
            repository.markCustomerOrderNotificationsAsRead(customerId, orderId)
            _effects.send(UiEffect.NavigateOrderStatus(orderId))
        }
    }

    fun openCustomerInboxFromNotification(inboxMessageId: Long) {
        viewModelScope.launch {
            _effects.send(UiEffect.NavigateCustomerInbox(inboxMessageId))
        }
    }

    private fun badgeText(count: Int): String {
        return if (count > 99) "99+" else count.toString()
    }

    class Factory(
        private val repository: MainActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}