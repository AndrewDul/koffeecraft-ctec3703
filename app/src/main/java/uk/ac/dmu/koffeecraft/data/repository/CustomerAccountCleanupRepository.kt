package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.cart.RememberedCartStore
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.HiddenOrdersStore

class CustomerAccountCleanupRepository(
    context: Context,
    private val database: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    suspend fun deleteCustomerCompletely(customerId: Long) {
        database.withTransaction {
            val sqlDb = database.openHelper.writableDatabase

            deleteFavouritePresetData(sqlDb, customerId)
            deleteMessagingData(sqlDb, customerId)
            deleteCustomerRecord(sqlDb, customerId)
        }

        clearLocalState(customerId)
    }

    private fun deleteFavouritePresetData(
        sqlDb: SupportSQLiteDatabase,
        customerId: Long
    ) {
        sqlDb.execSQL(
            """
            DELETE FROM customer_favourite_preset_add_on_cross_ref
            WHERE presetId IN (
                SELECT presetId
                FROM customer_favourite_presets
                WHERE customerId = ?
            )
            """.trimIndent(),
            arrayOf(customerId)
        )

        sqlDb.execSQL(
            """
            DELETE FROM customer_favourite_presets
            WHERE customerId = ?
            """.trimIndent(),
            arrayOf(customerId)
        )
    }

    private fun deleteMessagingData(
        sqlDb: SupportSQLiteDatabase,
        customerId: Long
    ) {
        sqlDb.execSQL(
            """
            DELETE FROM inbox_messages
            WHERE recipientCustomerId = ?
            """.trimIndent(),
            arrayOf(customerId)
        )

        sqlDb.execSQL(
            """
            DELETE FROM app_notifications
            WHERE recipientCustomerId = ?
               OR orderId IN (
                   SELECT orderId
                   FROM orders
                   WHERE customerId = ?
               )
            """.trimIndent(),
            arrayOf(customerId, customerId)
        )
    }

    private fun deleteCustomerRecord(
        sqlDb: SupportSQLiteDatabase,
        customerId: Long
    ) {
        sqlDb.execSQL(
            """
            DELETE FROM customers
            WHERE customerId = ?
            """.trimIndent(),
            arrayOf(customerId)
        )
    }

    private fun clearLocalState(customerId: Long) {
        RememberedCartStore.clearCartForCustomer(appContext, customerId)
        HiddenOrdersStore.clearForCustomer(appContext, customerId)

        val rememberedSession = RememberedSessionStore.getSession(appContext)
        if (
            rememberedSession?.role == RememberedSessionStore.Role.CUSTOMER &&
            rememberedSession.userId == customerId
        ) {
            RememberedSessionStore.clear(appContext)
        }

        if (SessionManager.currentCustomerId == customerId) {
            CartManager.clear()
            SessionManager.clear()
        }
    }
}