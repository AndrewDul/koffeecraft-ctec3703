package uk.ac.dmu.koffeecraft.data.repository

import androidx.room.withTransaction
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment

class OrderRepository(private val db: KoffeeCraftDatabase) {

    suspend fun placeOrder(
        customerId: Long,
        items: List<Pair<Long, Pair<Int, Double>>>, // productId -> (qty, unitPrice)
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

            val orderItems = items.map { (productId, qtyPrice) ->
                val qty = qtyPrice.first
                val unitPrice = qtyPrice.second
                OrderItem(
                    orderId = orderId,
                    productId = productId,
                    quantity = qty,
                    unitPrice = unitPrice
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