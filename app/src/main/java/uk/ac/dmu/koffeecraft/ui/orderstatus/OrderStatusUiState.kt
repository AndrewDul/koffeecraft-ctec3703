package uk.ac.dmu.koffeecraft.ui.orderstatus

data class OrderStatusUiState(
    val orderId: Long = 0L,
    val createdAt: Long = 0L,
    val statusRaw: String = "",
    val statusLabel: String = "",
    val heroIcon: String = "",
    val heroTitle: String = "",
    val heroSubtitle: String = "",
    val heroTone: HeroTone = HeroTone.NEUTRAL,
    val itemsOrderedLabel: String = "0",
    val totalPaidLabel: String = "£0.00",
    val paymentTypeLabel: String = "Unknown",
    val etaLabel: String = "Updating",
    val statusHelper: String = "",
    val feedbackHelper: String = "Feedback becomes available after collection.",
    val feedbackButtonLabel: String = "Leave feedback",
    val feedbackEnabled: Boolean = false,
    val stepPlacedActive: Boolean = false,
    val stepPreparingActive: Boolean = false,
    val stepReadyActive: Boolean = false,
    val stepCollectedActive: Boolean = false
)

enum class HeroTone {
    SUCCESS,
    WARM,
    NEUTRAL,
    DANGER
}