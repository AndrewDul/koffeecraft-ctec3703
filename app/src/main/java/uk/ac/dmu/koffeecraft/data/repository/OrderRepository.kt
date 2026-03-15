package uk.ac.dmu.koffeecraft.data.repository

import androidx.room.withTransaction
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment

class OrderRepository(private val db: KoffeeCraftDatabase) {

    suspend fun placeOrder(
        customerId: Long,
        items: List<CartItem>,
        paymentType: String,
        totalAmount: Double
    ): Long {
        return db.withTransaction {
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
                    estimatedCalories = cartItem.estimatedCalories
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

            orderId
        }
    }
}