package uk.ac.dmu.koffeecraft.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator

class CustomerPaymentMethodsRepository(
    private val db: KoffeeCraftDatabase
) {

    fun observeCards(customerId: Long): Flow<List<CustomerPaymentCard>> {
        return db.customerPaymentCardDao().observeForCustomer(customerId)
    }

    suspend fun addCard(
        customerId: Long,
        nickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String,
        setAsDefault: Boolean
    ): SettingsActionResult {
        val brand = PaymentCardValidator.detectBrand(cardNumber)
        val numberDigits = PaymentCardValidator.extractDigits(cardNumber)
        val expiry = PaymentCardValidator.parseExpiry(expiryText)
            ?: return SettingsActionResult.Error(
                "The card expiry could not be read. Please check the expiry date and try again."
            )

        val last4 = numberDigits.takeLast(4)
        val finalNickname = if (nickname.isBlank()) {
            PaymentCardValidator.defaultNickname(brand, last4)
        } else {
            nickname.trim()
        }

        db.withTransaction {
            val currentDefault = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
            val shouldBeDefault = setAsDefault || currentDefault == null

            if (shouldBeDefault) {
                db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
            }

            db.customerPaymentCardDao().insert(
                CustomerPaymentCard(
                    customerId = customerId,
                    nickname = finalNickname,
                    cardholderName = cardholderName.trim(),
                    brand = brand.displayName,
                    maskedCardNumber = PaymentCardValidator.buildMaskedNumber(cardNumber),
                    last4 = last4,
                    expiryMonth = expiry.first,
                    expiryYear = expiry.second,
                    isDefault = shouldBeDefault
                )
            )
        }

        return SettingsActionResult.Success("Card saved for faster checkout.")
    }

    suspend fun setDefaultCard(
        customerId: Long,
        card: CustomerPaymentCard
    ): SettingsActionResult {
        db.withTransaction {
            db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
            db.customerPaymentCardDao().setDefault(card.cardId, customerId)
        }

        return SettingsActionResult.Success("\"${card.nickname}\" is now your default card.")
    }

    suspend fun deleteCard(
        customerId: Long,
        card: CustomerPaymentCard
    ): SettingsActionResult {
        db.withTransaction {
            db.customerPaymentCardDao().deleteByIdAndCustomer(card.cardId, customerId)

            if (card.isDefault) {
                val replacement = db.customerPaymentCardDao().getMostRecentForCustomer(customerId)
                if (replacement != null) {
                    db.customerPaymentCardDao().clearDefaultForCustomer(customerId)
                    db.customerPaymentCardDao().setDefault(replacement.cardId, customerId)
                }
            }
        }

        return SettingsActionResult.Success("Saved card removed.")
    }
}