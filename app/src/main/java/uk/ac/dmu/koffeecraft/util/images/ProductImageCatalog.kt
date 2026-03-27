package uk.ac.dmu.koffeecraft.util.images

import androidx.annotation.DrawableRes
import uk.ac.dmu.koffeecraft.R

private const val FAMILY_COFFEE = "COFFEE"
private const val FAMILY_CAKE = "CAKE"
private const val FAMILY_MERCH = "MERCH"

enum class ProductImageLibraryCategory {
    COFFEE,
    CAKE,
    REWARD
}

data class ProductImageCatalogEntry(
    val key: String,
    val label: String,
    val category: ProductImageLibraryCategory,
    @DrawableRes val drawableResId: Int
)

object ProductImageCatalog {

    private val entries: List<ProductImageCatalogEntry> = listOf(
        ProductImageCatalogEntry(
            key = "coffee_espresso",
            label = "Espresso",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_espresso
        ),
        ProductImageCatalogEntry(
            key = "coffee_cappuccino",
            label = "Cappuccino",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_cappuccino
        ),
        ProductImageCatalogEntry(
            key = "coffee_latte",
            label = "Latte",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_latte
        ),
        ProductImageCatalogEntry(
            key = "coffee_flat_white",
            label = "Flat White",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_flat_white
        ),
        ProductImageCatalogEntry(
            key = "coffee_pistachio_latte",
            label = "Pistachio Latte",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_pistachio_latte
        ),
        ProductImageCatalogEntry(
            key = "coffee_salted_caramel_mocha",
            label = "Salted Caramel Mocha",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.coffee_salted_caramel_mocha
        ),

        ProductImageCatalogEntry(
            key = "cake_cheesecake",
            label = "Cheesecake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_cheesecake
        ),
        ProductImageCatalogEntry(
            key = "cake_chocolate_brownie",
            label = "Chocolate Brownie",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_chocolate_brownie
        ),
        ProductImageCatalogEntry(
            key = "cake_carrot_cake",
            label = "Carrot Cake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_carrot_cake
        ),
        ProductImageCatalogEntry(
            key = "cake_tiramisu",
            label = "Tiramisu",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_tiramisu
        ),
        ProductImageCatalogEntry(
            key = "cake_victoria_sponge_cake",
            label = "Victoria Sponge Cake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_victoria_sponge_cake
        ),
        ProductImageCatalogEntry(
            key = "cake_lemon_drizzle_cake",
            label = "Lemon Drizzle Cake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_lemon_drizzle_cake
        ),
        ProductImageCatalogEntry(
            key = "cake_lotus_biscoff_cheesecake",
            label = "Lotus Biscoff Cheesecake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_lotus_biscoff_cheesecake
        ),
        ProductImageCatalogEntry(
            key = "cake_red_velvet_slice",
            label = "Red Velvet Slice",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_red_velvet_slice
        ),
        ProductImageCatalogEntry(
            key = "cake_basque_cheesecake",
            label = "Basque Cheesecake",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.cake_basque_cheesecake
        ),

        ProductImageCatalogEntry(
            key = "reward_bean_booster",
            label = "5 Bean Booster",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_bean_booster
        ),
        ProductImageCatalogEntry(
            key = "reward_free_coffee",
            label = "Free Coffee Reward",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_free_coffee
        ),
        ProductImageCatalogEntry(
            key = "reward_free_cake",
            label = "Free Cake Reward",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_free_cake
        ),
        ProductImageCatalogEntry(
            key = "reward_mug",
            label = "KoffeeCraft Mug",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_mug
        ),
        ProductImageCatalogEntry(
            key = "reward_teddy",
            label = "KoffeeCraft Teddy Bear",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_teddy
        ),
        ProductImageCatalogEntry(
            key = "reward_beans_1kg",
            label = "1kg Crafted Coffee Beans",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.reward_beans_1kg
        )
    )

    fun allEntries(): List<ProductImageCatalogEntry> = entries

    fun entryForKey(key: String?): ProductImageCatalogEntry? {
        if (key.isNullOrBlank()) return null
        return entries.firstOrNull { it.key == key }
    }

    @DrawableRes
    fun drawableForKey(key: String?): Int? = entryForKey(key)?.drawableResId

    fun entriesForProduct(productFamily: String, rewardEnabled: Boolean): List<ProductImageCatalogEntry> {
        val category = when {
            productFamily.equals(FAMILY_COFFEE, ignoreCase = true) -> ProductImageLibraryCategory.COFFEE
            productFamily.equals(FAMILY_CAKE, ignoreCase = true) -> ProductImageLibraryCategory.CAKE
            productFamily.equals(FAMILY_MERCH, ignoreCase = true) -> ProductImageLibraryCategory.REWARD
            rewardEnabled && productFamily.equals(FAMILY_COFFEE, ignoreCase = true) -> ProductImageLibraryCategory.COFFEE
            rewardEnabled && productFamily.equals(FAMILY_CAKE, ignoreCase = true) -> ProductImageLibraryCategory.CAKE
            else -> ProductImageLibraryCategory.COFFEE
        }
        return entries.filter { it.category == category }
    }

    @DrawableRes
    fun fallbackDrawable(productFamily: String, rewardEnabled: Boolean): Int {
        return when {
            productFamily.equals(FAMILY_COFFEE, ignoreCase = true) -> R.drawable.kc_image_placeholder_coffee
            productFamily.equals(FAMILY_CAKE, ignoreCase = true) -> R.drawable.kc_image_placeholder_cake
            productFamily.equals(FAMILY_MERCH, ignoreCase = true) -> R.drawable.kc_image_placeholder_reward
            rewardEnabled && productFamily.equals(FAMILY_COFFEE, ignoreCase = true) -> R.drawable.kc_image_placeholder_coffee
            rewardEnabled && productFamily.equals(FAMILY_CAKE, ignoreCase = true) -> R.drawable.kc_image_placeholder_cake
            else -> R.drawable.kc_image_placeholder_generic
        }
    }
}