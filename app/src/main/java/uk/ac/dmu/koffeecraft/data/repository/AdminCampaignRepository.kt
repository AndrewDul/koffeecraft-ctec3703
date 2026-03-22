package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.dao.CustomerCampaignTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CampaignAudienceMode {
    ALL_OPTED_IN,
    BIRTHDAY_TODAY,
    NO_ORDERS,
    LOYAL_CUSTOMERS,
    INACTIVE_USERS,
    CLOSE_TO_REWARD,
    DIRECT_ORDER,
    DIRECT_CUSTOMER
}

enum class CampaignType(
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

data class AdminCampaignPreview(
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
        fun empty(): AdminCampaignPreview {
            return AdminCampaignPreview(
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

sealed interface AdminCampaignSendResult {
    data class Success(val recipientsCount: Int) : AdminCampaignSendResult
    data class Error(val message: String) : AdminCampaignSendResult
}

class AdminCampaignRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun buildPreview(
        selectedAudience: CampaignAudienceMode,
        selectedCampaignType: CampaignType,
        audienceRuleInput: String,
        titleInput: String,
        messageInput: String,
        beansInput: String
    ): AdminCampaignPreview {
        val allTargets = when (selectedAudience) {
            CampaignAudienceMode.DIRECT_ORDER,
            CampaignAudienceMode.DIRECT_CUSTOMER -> emptyList()
            else -> db.customerDao().getAllCampaignTargets()
        }

        val baseTargets = when (selectedAudience) {
            CampaignAudienceMode.ALL_OPTED_IN -> {
                allTargets
            }

            CampaignAudienceMode.BIRTHDAY_TODAY -> {
                val monthDay = SimpleDateFormat("MM-dd", Locale.UK).format(Date())
                allTargets.filter { target ->
                    target.dateOfBirth?.let { dob ->
                        dob.length >= 10 && dob.substring(5, 10) == monthDay
                    } == true
                }
            }

            CampaignAudienceMode.NO_ORDERS -> {
                allTargets.filter { it.orderCount == 0 }
            }

            CampaignAudienceMode.LOYAL_CUSTOMERS -> {
                val minOrders = audienceRuleInput.toIntOrNull()
                if (minOrders == null || minOrders <= 0) {
                    emptyList()
                } else {
                    allTargets.filter { it.orderCount >= minOrders }
                }
            }

            CampaignAudienceMode.INACTIVE_USERS -> {
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

            CampaignAudienceMode.CLOSE_TO_REWARD -> {
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

            CampaignAudienceMode.DIRECT_ORDER -> {
                val orderId = audienceRuleInput.toLongOrNull()
                if (orderId == null) {
                    emptyList()
                } else {
                    listOfNotNull(db.customerDao().getCampaignTargetByOrderId(orderId))
                }
            }

            CampaignAudienceMode.DIRECT_CUSTOMER -> {
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

        return AdminCampaignPreview(
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

    suspend fun sendCampaign(
        selectedAudience: CampaignAudienceMode,
        selectedCampaignType: CampaignType,
        audienceRuleInput: String,
        titleInput: String,
        messageInput: String,
        beansInput: String
    ): AdminCampaignSendResult {
        val preview = buildPreview(
            selectedAudience = selectedAudience,
            selectedCampaignType = selectedCampaignType,
            audienceRuleInput = audienceRuleInput,
            titleInput = titleInput,
            messageInput = messageInput,
            beansInput = beansInput
        )

        if (!preview.canSend) {
            return AdminCampaignSendResult.Error(
                "Complete the campaign details and make sure there is at least one eligible recipient."
            )
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

        return AdminCampaignSendResult.Success(messages.size)
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
        mode: CampaignAudienceMode,
        rule: String
    ): String {
        return when (mode) {
            CampaignAudienceMode.ALL_OPTED_IN -> "All opted-in customers"
            CampaignAudienceMode.BIRTHDAY_TODAY -> "Birthday today"
            CampaignAudienceMode.NO_ORDERS -> "Customers with 0 orders"
            CampaignAudienceMode.LOYAL_CUSTOMERS -> {
                if (rule.isBlank()) "Loyal customers"
                else "Loyal customers • min $rule orders"
            }

            CampaignAudienceMode.INACTIVE_USERS -> {
                if (rule.isBlank()) "Inactive users"
                else "Inactive users • $rule days"
            }

            CampaignAudienceMode.CLOSE_TO_REWARD -> {
                if (rule.isBlank()) "Close to reward"
                else "Close to reward • within $rule beans"
            }

            CampaignAudienceMode.DIRECT_ORDER -> {
                if (rule.isBlank()) "Direct by order"
                else "Direct by order • #$rule"
            }

            CampaignAudienceMode.DIRECT_CUSTOMER -> {
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

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private val REWARD_THRESHOLDS = listOf(15, 18, 125, 250, 370)
    }
}