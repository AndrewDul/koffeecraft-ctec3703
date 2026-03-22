package uk.ac.dmu.koffeecraft.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment
import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager

class CheckoutRepository(
    private val db: KoffeeCraftDatabase
) {

    sealed interface CheckoutSubmissionResult {
        data class Success(val orderId: Long) : CheckoutSubmissionResult
        data class Error(val message: String) : CheckoutSubmissionResult
    }

    fun observeSavedCards(customerId: Long): Flow<List<CustomerPaymentCard>> {
        return db.customerPaymentCardDao().observeForCustomer(customerId)
    }

    suspend fun submitOrder(
        customerId: Long,
        items: List<CartItem>,
        paymentType: String,
        totalAmount: Double,
        beansToSpend: Int,
        beansToEarn: Int,
        saveNewCardForFuture: Boolean,
        cardNickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String
    ): CheckoutSubmissionResult {
        return db.withTransaction {
            val customer = db.customerDao().getById(customerId)
                ?: return@withTransaction CheckoutSubmissionResult.Error("Customer not found.")

            if (customer.beansBalance < beansToSpend) {
                return@withTransaction CheckoutSubmissionResult.Error(
                    "You do not have enough beans for the selected rewards."
                )
            }

            if (paymentType == "CARD" && saveNewCardForFuture && hasStartedTypingNewCard(
                    nickname = cardNickname,
                    holder = cardholderName,
                    number = cardNumber,
                    expiry = expiryText
                )
            ) {
                val saveCardResult = saveCheckoutCard(
                    customerId = customerId,
                    nickname = cardNickname,
                    cardholderName = cardholderName,
                    cardNumber = cardNumber,
                    expiryText = expiryText
                )

                if (saveCardResult is CheckoutSubmissionResult.Error) {
                    return@withTransaction saveCardResult
                }
            }

            val orderId = db.orderDao().insert(
                Order(
                    customerId = customerId,
                    status = "PLACED",
                    totalAmount = totalAmount
                )
            )

            val orderItems = items.map { cartItem ->
                OrderItem(
                    orderId = orderId,
                    productId = cartItem.product.productId,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice,
                    selectedOptionLabel = cartItem.selectedOptionLabel,
                    selectedOptionSizeValue = cartItem.selectedOptionSizeValue,
                    selectedOptionSizeUnit = cartItem.selectedOptionSizeUnit,
                    selectedAddOnsSummary = cartItem.selectedAddOnsSummary,
                    estimatedCalories = cartItem.estimatedCalories,
                    productNameSnapshot = cartItem.product.name,
                    productDescriptionSnapshot = cartItem.product.description
                )
            }

            db.orderItemDao().insertAll(orderItems)

            db.paymentDao().insert(
                Payment(
                    orderId = orderId,
                    paymentType = paymentType,
                    amount = totalAmount
                )
            )

            val updatedBeansBalance = customer.beansBalance - beansToSpend + beansToEarn

            val boosterState = BeansBoosterManager.applyEarnedBeans(
                currentProgress = customer.beansBoosterProgress,
                currentPendingBoosters = customer.pendingBeansBoosters,
                earnedBeans = beansToEarn
            )

            db.customerDao().update(
                customer.copy(
                    beansBalance = updatedBeansBalance,
                    beansBoosterProgress = boosterState.progress,
                    pendingBeansBoosters = boosterState.pendingBoosters
                )
            )

            CheckoutSubmissionResult.Success(orderId)
        }
    }

    private suspend fun saveCheckoutCard(
        customerId: Long,
        nickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String
    ): CheckoutSubmissionResult {
        val brand = PaymentCardValidator.detectBrand(cardNumber)
        val numberDigits = PaymentCardValidator.extractDigits(cardNumber)
        val expiry = PaymentCardValidator.parseExpiry(expiryText)
            ?: return CheckoutSubmissionResult.Error(
                "The card expiry could not be read. Please check the expiry date and try again."
            )

        val last4 = numberDigits.takeLast(4)

        val finalNickname = if (nickname.isBlank()) {
            PaymentCardValidator.defaultNickname(brand, last4)
        } else {
            nickname.trim()
        }

        val currentDefault = db.customerPaymentCardDao().getDefaultForCustomer(customerId)
        val shouldBeDefault = currentDefault == null

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

        return CheckoutSubmissionResult.Success(orderId = -1L)
    }

    private fun hasStartedTypingNewCard(
        nickname: String,
        holder: String,
        number: String,
        expiry: String
    ): Boolean {
        return listOf(nickname, holder, number, expiry).any { it.isNotBlank() }
    }
}