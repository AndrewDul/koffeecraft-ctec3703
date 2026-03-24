package uk.ac.dmu.koffeecraft.data.db

import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

object CatalogDefaults {

    data class AddOnSeed(
        val name: String,
        val category: String,
        val price: Double,
        val estimatedCalories: Int,
        val allergenNames: List<String> = emptyList()
    )

    data class ProductOptionSeed(
        val optionName: String,
        val displayLabel: String,
        val sizeValue: Int,
        val sizeUnit: String,
        val extraPrice: Double,
        val estimatedCalories: Int,
        val isDefault: Boolean
    )

    val defaultAllergens: List<String> = listOf(
        "Milk",
        "Nuts",
        "Gluten",
        "Soy",
        "Coconut"
    )

    val defaultAddOns: List<AddOnSeed> = listOf(
        AddOnSeed("Milk", "COFFEE", 0.30, 25, listOf("Milk")),
        AddOnSeed("Vanilla syrup", "COFFEE", 0.30, 35),
        AddOnSeed("Caramel syrup", "COFFEE", 0.30, 40),
        AddOnSeed("Cinnamon topping", "COFFEE", 0.30, 10),
        AddOnSeed("Chocolate topping", "COFFEE", 0.30, 35),
        AddOnSeed("Hazelnut syrup", "COFFEE", 0.30, 40, listOf("Nuts")),
        AddOnSeed("Coconut topping", "COFFEE", 0.30, 30, listOf("Coconut")),
        AddOnSeed("Nut topping", "COFFEE", 0.30, 45, listOf("Nuts")),
        AddOnSeed("Chocolate sauce", "CAKE", 0.30, 45),
        AddOnSeed("Raspberry sauce", "CAKE", 0.30, 30),
        AddOnSeed("Toffee sauce", "CAKE", 0.30, 50),
        AddOnSeed("Coconut topping", "CAKE", 0.30, 30, listOf("Coconut")),
        AddOnSeed("Nut topping", "CAKE", 0.30, 45, listOf("Nuts")),
        AddOnSeed("Icing sugar", "CAKE", 0.30, 20)
    )

    private val coffeeOptions: List<ProductOptionSeed> = listOf(
        ProductOptionSeed("SMALL", "Small", 250, "ML", 0.0, 120, true),
        ProductOptionSeed("MEDIUM", "Medium", 350, "ML", 0.5, 170, false),
        ProductOptionSeed("LARGE", "Large", 450, "ML", 1.0, 220, false)
    )

    private val cakeOptions: List<ProductOptionSeed> = listOf(
        ProductOptionSeed("STANDARD", "Standard slice", 120, "G", 0.0, 300, true),
        ProductOptionSeed("MEDIUM", "Medium", 220, "G", 0.5, 550, false),
        ProductOptionSeed("LARGE", "Large", 330, "G", 1.0, 820, false)
    )

    fun defaultOptionsForFamily(productFamily: String): List<ProductOptionSeed> {
        return when (productFamily.uppercase()) {
            "COFFEE" -> coffeeOptions
            "CAKE" -> cakeOptions
            else -> emptyList()
        }
    }

    suspend fun seedMissingCatalogData(database: KoffeeCraftDatabase) {
        seedCatalogLibrary(database)

        val products = database.productDao().getAllOnce()
        products.forEach { product ->
            seedProductDefaults(database, product)
        }
    }

    suspend fun seedCatalogLibrary(database: KoffeeCraftDatabase) {
        val allergenDao = database.allergenDao()
        val addOnDao = database.addOnDao()

        defaultAllergens.forEach { allergenName ->
            allergenDao.insert(Allergen(name = allergenName))
        }

        val existingCoffeeAddOns = addOnDao.getAllByCategory("COFFEE")
        val existingCakeAddOns = addOnDao.getAllByCategory("CAKE")
        val existingByKey = (existingCoffeeAddOns + existingCakeAddOns)
            .associateBy { seedKey(it.name, it.category) }

        defaultAddOns.forEach { seed ->
            val key = seedKey(seed.name, seed.category)
            if (key !in existingByKey) {
                addOnDao.insert(
                    AddOn(
                        name = seed.name,
                        category = seed.category,
                        price = seed.price,
                        estimatedCalories = seed.estimatedCalories,
                        isActive = true
                    )
                )
            }
        }

        val allergensByName = database.allergenDao().getAll().associateBy { it.name }
        val addOnsByKey = (addOnDao.getAllByCategory("COFFEE") + addOnDao.getAllByCategory("CAKE"))
            .associateBy { seedKey(it.name, it.category) }

        val addOnRefs = defaultAddOns.flatMap { seed ->
            val addOnId = addOnsByKey[seedKey(seed.name, seed.category)]?.addOnId ?: return@flatMap emptyList()
            seed.allergenNames.mapNotNull { allergenName ->
                val allergenId = allergensByName[allergenName]?.allergenId ?: return@mapNotNull null
                AddOnAllergenCrossRef(addOnId = addOnId, allergenId = allergenId)
            }
        }

        if (addOnRefs.isNotEmpty()) {
            allergenDao.insertAddOnRefs(addOnRefs)
        }
    }

    suspend fun seedProductDefaults(
        database: KoffeeCraftDatabase,
        product: Product
    ) {
        val family = product.productFamily.uppercase()
        if (family !in setOf("COFFEE", "CAKE")) return

        val optionDao = database.productOptionDao()
        val addOnDao = database.addOnDao()

        val existingOptions = optionDao.getForProduct(product.productId)
        val existingOptionNames = existingOptions.map { it.optionName.uppercase() }.toSet()

        defaultOptionsForFamily(family)
            .filter { it.optionName.uppercase() !in existingOptionNames }
            .forEach { option ->
                optionDao.insert(
                    ProductOption(
                        productId = product.productId,
                        optionName = option.optionName,
                        displayLabel = option.displayLabel,
                        sizeValue = option.sizeValue,
                        sizeUnit = option.sizeUnit,
                        extraPrice = option.extraPrice,
                        estimatedCalories = option.estimatedCalories,
                        isDefault = option.isDefault
                    )
                )
            }

        val availableAddOns = addOnDao.getAllByCategory(family)
        val assignedAddOnIds = addOnDao.getAssignedIdsForProduct(product.productId).toSet()
        val refs = availableAddOns
            .filter { it.addOnId !in assignedAddOnIds }
            .map { addOn ->
                ProductAddOnCrossRef(
                    productId = product.productId,
                    addOnId = addOn.addOnId
                )
            }

        if (refs.isNotEmpty()) {
            addOnDao.insertProductRefs(refs)
        }
    }

    private fun seedKey(name: String, category: String): String {
        return "${category.uppercase()}|${name.uppercase()}"
    }
}