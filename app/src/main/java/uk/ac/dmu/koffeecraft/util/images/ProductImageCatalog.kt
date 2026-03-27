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
            key = "coffee_signature_house",
            label = "House Signature",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.kc_image_placeholder_coffee
        ),
        ProductImageCatalogEntry(
            key = "coffee_velvet_latte",
            label = "Velvet Latte",
            category = ProductImageLibraryCategory.COFFEE,
            drawableResId = R.drawable.kc_image_placeholder_coffee_alt
        ),
        ProductImageCatalogEntry(
            key = "cake_cream_slice",
            label = "Cream Slice",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.kc_image_placeholder_cake
        ),
        ProductImageCatalogEntry(
            key = "cake_crafted_layer",
            label = "Crafted Layer",
            category = ProductImageLibraryCategory.CAKE,
            drawableResId = R.drawable.kc_image_placeholder_cake_alt
        ),
        ProductImageCatalogEntry(
            key = "reward_mug",
            label = "Reward Mug",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.kc_image_placeholder_reward
        ),
        ProductImageCatalogEntry(
            key = "reward_teddy",
            label = "Reward Teddy",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.kc_image_placeholder_reward_alt
        ),
        ProductImageCatalogEntry(
            key = "reward_beans_1kg",
            label = "Reward Beans",
            category = ProductImageLibraryCategory.REWARD,
            drawableResId = R.drawable.kc_image_placeholder_reward_beans
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