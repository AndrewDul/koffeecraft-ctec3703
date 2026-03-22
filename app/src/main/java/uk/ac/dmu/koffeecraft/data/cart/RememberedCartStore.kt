package uk.ac.dmu.koffeecraft.data.cart

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RememberedCartStore {

    private const val PREFS_NAME = "koffeecraft_remembered_carts"

    data class CartItemSnapshot(
        val lineKey: String,
        val productId: Long,
        val quantity: Int,
        val unitPrice: Double,
        val isReward: Boolean,
        val rewardType: String?,
        val beansCostPerUnit: Int,
        val selectedOptionId: Long?,
        val selectedOptionLabel: String?,
        val selectedOptionSizeValue: Int?,
        val selectedOptionSizeUnit: String?,
        val selectedAddOnIds: List<Long>,
        val selectedAddOnsSummary: String?,
        val estimatedCalories: Int?
    )

    fun saveCartForCustomer(
        context: Context,
        customerId: Long,
        items: List<CartItem>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cartKey(customerId)

        if (items.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }

        val jsonArray = JSONArray()

        items.forEach { item ->
            val objectJson = JSONObject().apply {
                put("lineKey", item.lineKey)
                put("productId", item.product.productId)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("isReward", item.isReward)
                put("rewardType", item.rewardType ?: JSONObject.NULL)
                put("beansCostPerUnit", item.beansCostPerUnit)

                if (item.selectedOptionId == null) {
                    put("selectedOptionId", JSONObject.NULL)
                } else {
                    put("selectedOptionId", item.selectedOptionId.toString())
                }

                put("selectedOptionLabel", item.selectedOptionLabel ?: JSONObject.NULL)

                if (item.selectedOptionSizeValue == null) {
                    put("selectedOptionSizeValue", JSONObject.NULL)
                } else {
                    put("selectedOptionSizeValue", item.selectedOptionSizeValue)
                }

                put("selectedOptionSizeUnit", item.selectedOptionSizeUnit ?: JSONObject.NULL)
                put("selectedAddOnsSummary", item.selectedAddOnsSummary ?: JSONObject.NULL)
                put("estimatedCalories", item.estimatedCalories ?: JSONObject.NULL)

                val addOnIdsJson = JSONArray()
                item.selectedAddOnIds.forEach { addOnId ->
                    addOnIdsJson.put(addOnId.toString())
                }
                put("selectedAddOnIds", addOnIdsJson)
            }

            jsonArray.put(objectJson)
        }

        prefs.edit()
            .putString(key, jsonArray.toString())
            .apply()
    }

    fun loadCartForCustomer(
        context: Context,
        customerId: Long
    ): List<CartItemSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(cartKey(customerId), null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    val addOnIdsJson = item.optJSONArray("selectedAddOnIds") ?: JSONArray()
                    val addOnIds = buildList {
                        for (i in 0 until addOnIdsJson.length()) {
                            add(addOnIdsJson.optLong(i))
                        }
                    }.filter { it > 0L }

                    add(
                        CartItemSnapshot(
                            lineKey = item.optString("lineKey"),
                            productId = item.optLong("productId"),
                            quantity = item.optInt("quantity", 1).coerceAtLeast(1),
                            unitPrice = item.optDouble("unitPrice", 0.0),
                            isReward = item.optBoolean("isReward", false),
                            rewardType = item.optNullableString("rewardType"),
                            beansCostPerUnit = item.optInt("beansCostPerUnit", 0).coerceAtLeast(0),
                            selectedOptionId = item.optNullableLong("selectedOptionId"),
                            selectedOptionLabel = item.optNullableString("selectedOptionLabel"),
                            selectedOptionSizeValue = item.optNullableInt("selectedOptionSizeValue"),
                            selectedOptionSizeUnit = item.optNullableString("selectedOptionSizeUnit"),
                            selectedAddOnIds = addOnIds,
                            selectedAddOnsSummary = item.optNullableString("selectedAddOnsSummary"),
                            estimatedCalories = item.optNullableInt("estimatedCalories")
                        )
                    )
                }
            }.filter { it.productId > 0L && it.lineKey.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearCartForCustomer(
        context: Context,
        customerId: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(cartKey(customerId)).apply()
    }

    private fun cartKey(customerId: Long): String = "cart_customer_$customerId"

    private fun JSONObject.optNullableString(key: String): String? {
        return if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (!has(key) || isNull(key)) null else optLong(key).takeIf { it > 0L }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        return if (!has(key) || isNull(key)) null else optInt(key)
    }
}