package uk.ac.dmu.koffeecraft.util.orders

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.util.notifications.CustomerNotificationManager

object OrderSimulationManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningOrderIds = mutableSetOf<Long>()

    fun startIfNeeded(context: Context, orderId: Long) {
        synchronized(runningOrderIds) {
            if (runningOrderIds.contains(orderId)) return
            runningOrderIds.add(orderId)
        }

        val appContext = context.applicationContext
        val db = KoffeeCraftDatabase.getInstance(appContext)

        scope.launch {
            try {
                advanceIfCurrent(
                    db = db,
                    context = appContext,
                    orderId = orderId,
                    expectedStatus = "PLACED",
                    nextStatus = "PREPARING",
                    delayMs = 2000
                )

                advanceIfCurrent(
                    db = db,
                    context = appContext,
                    orderId = orderId,
                    expectedStatus = "PREPARING",
                    nextStatus = "READY",
                    delayMs = 3000
                )

                advanceIfCurrent(
                    db = db,
                    context = appContext,
                    orderId = orderId,
                    expectedStatus = "READY",
                    nextStatus = "COLLECTED",
                    delayMs = 10_000
                )
            } finally {
                synchronized(runningOrderIds) {
                    runningOrderIds.remove(orderId)
                }
            }
        }
    }

    private suspend fun advanceIfCurrent(
        db: KoffeeCraftDatabase,
        context: Context,
        orderId: Long,
        expectedStatus: String,
        nextStatus: String,
        delayMs: Long
    ) {
        delay(delayMs)

        val order = db.orderDao().getById(orderId) ?: return
        if (order.status != expectedStatus) return

        db.orderDao().updateStatus(orderId, nextStatus)

        CustomerNotificationManager.createCustomerOrderStatusNotification(
            context = context,
            db = db,
            customerId = order.customerId,
            orderId = order.orderId,
            orderCreatedAt = order.createdAt,
            status = nextStatus
        )
    }
}