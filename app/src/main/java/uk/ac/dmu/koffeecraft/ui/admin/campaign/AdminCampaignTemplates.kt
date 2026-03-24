package uk.ac.dmu.koffeecraft.ui.admin.campaign

import uk.ac.dmu.koffeecraft.data.repository.CampaignType

object AdminCampaignTemplates {

    fun defaultTitle(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> "A special KoffeeCraft offer for you"
            CampaignType.BONUS_BEANS -> "A beans reward from KoffeeCraft"
            CampaignType.OFFER_PLUS_BEANS -> "A special offer + bonus beans from KoffeeCraft"
        }
    }

    fun defaultBody(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> {
                """
Hello,

We have prepared something special for you at KoffeeCraft.

Offer details:
[ADD_YOUR_OFFER_HERE]

See you soon,
KoffeeCraft
                """.trimIndent()
            }

            CampaignType.BONUS_BEANS -> {
                """
Hello,

We have added [BEANS_AMOUNT] bonus beans to your KoffeeCraft account.

Enjoy your reward and keep crafting your next favourite order.

KoffeeCraft
                """.trimIndent()
            }

            CampaignType.OFFER_PLUS_BEANS -> {
                """
Hello,

We have prepared a special KoffeeCraft offer for you and added [BEANS_AMOUNT] bonus beans to your account.

Offer details:
[ADD_YOUR_OFFER_HERE]

Enjoy,
KoffeeCraft
                """.trimIndent()
            }
        }
    }

    fun builderHint(type: CampaignType): String {
        return when (type) {
            CampaignType.PROMOTIONAL_OFFER -> {
                "Promotional consent will be applied automatically for this campaign type."
            }

            CampaignType.BONUS_BEANS -> {
                "Bonus beans will be added automatically when the campaign is sent. Use [BEANS_AMOUNT] if you want to mention the reward."
            }

            CampaignType.OFFER_PLUS_BEANS -> {
                "This campaign sends a promo message and awards beans at the same time. Promotional consent will be applied automatically."
            }
        }
    }
}