package uk.ac.dmu.koffeecraft.data.settings

import android.content.Context

object HiddenOrdersStore {

    private const val PREFS_NAME = "koffeecraft_hidden_orders"

    private fun keyForCustomer(customerId: Long): String = "hidden_orders_customer_$customerId"

    fun getHiddenOrderIds(context: Context, customerId: Long): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawSet = prefs.getStringSet(keyForCustomer(customerId), emptySet()).orEmpty()
        return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun hideOrder(context: Context, customerId: Long, orderId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(keyForCustomer(customerId), emptySet()).orEmpty().toMutableSet()
        current.add(orderId.toString())
        prefs.edit().putStringSet(keyForCustomer(customerId), current).apply()
    }

    fun clearForCustomer(context: Context, customerId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(keyForCustomer(customerId)).apply()
    }
}