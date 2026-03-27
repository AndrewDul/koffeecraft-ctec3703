package uk.ac.dmu.koffeecraft.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.AdminDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.util.images.ProductImageAssignments
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

object DatabaseSeeder {

    fun create(): RoomDatabase.Callback = SeedCallback()

    private class SeedCallback : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            CoroutineScope(Dispatchers.IO).launch {
                val database = KoffeeCraftDatabase.getInstanceHolder() ?: return@launch
                seedAdminIfNeeded(database)
                seedProductsIfNeeded(database)
                syncProductImageKeysByName(database)
                CatalogDefaults.seedMissingCatalogData(database)
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)

            CoroutineScope(Dispatchers.IO).launch {
                val database = KoffeeCraftDatabase.getInstanceHolder() ?: return@launch
                syncProductImageKeysByName(database)
            }
        }

        private suspend fun seedAdminIfNeeded(database: KoffeeCraftDatabase) {
            val adminDao: AdminDao = database.adminDao()
            if (adminDao.countAdmins() > 0) return

            val email = "admin@koffeecraft.local"
            val tempPassword = "KoffeeCraft@123"

            val salt = PasswordHasher.generateSaltBase64()
            val hash = PasswordHasher.hashPasswordBase64(tempPassword.toCharArray(), salt)

            adminDao.insert(
                Admin(
                    fullName = "KoffeeCraft Admin",
                    email = email,
                    phone = "Not set",
                    username = "admin",
                    passwordHash = hash,
                    passwordSalt = salt,
                    isActive = true
                )
            )
        }

        private suspend fun seedProductsIfNeeded(database: KoffeeCraftDatabase) {
            val productDao: ProductDao = database.productDao()
            if (productDao.countProducts() > 0) return

            val products = listOf(
                Product(
                    name = "Espresso",
                    productFamily = "COFFEE",
                    description = "Strong and bold espresso shot.",
                    price = 2.20,
                    imageKey = "coffee_espresso",
                    rewardEnabled = true
                ),
                Product(
                    name = "Cappuccino",
                    productFamily = "COFFEE",
                    description = "Espresso with steamed milk and foam.",
                    price = 3.40,
                    imageKey = "coffee_cappuccino",
                    rewardEnabled = true
                ),
                Product(
                    name = "Latte",
                    productFamily = "COFFEE",
                    description = "Smooth espresso with lots of milk.",
                    price = 3.60,
                    imageKey = "coffee_latte",
                    rewardEnabled = true
                ),
                Product(
                    name = "Cheesecake",
                    productFamily = "CAKE",
                    description = "Classic creamy cheesecake slice.",
                    price = 4.20,
                    imageKey = "cake_cheesecake",
                    rewardEnabled = true
                ),
                Product(
                    name = "Chocolate Brownie",
                    productFamily = "CAKE",
                    description = "Rich chocolate brownie.",
                    price = 3.00,
                    imageKey = "cake_chocolate_brownie",
                    rewardEnabled = true
                ),
                Product(
                    name = "Carrot Cake",
                    productFamily = "CAKE",
                    description = "Moist carrot cake with frosting.",
                    price = 3.80,
                    imageKey = "cake_carrot_cake",
                    rewardEnabled = true
                ),
                Product(
                    name = "KoffeeCraft Mug",
                    productFamily = "MERCH",
                    description = "Crafted mug with the KoffeeCraft logo.",
                    price = 0.0,
                    isActive = true,
                    imageKey = "reward_mug",
                    rewardEnabled = true
                ),
                Product(
                    name = "KoffeeCraft Teddy Bear",
                    productFamily = "MERCH",
                    description = "Soft teddy bear with KoffeeCraft branding.",
                    price = 0.0,
                    isActive = true,
                    imageKey = "reward_teddy",
                    rewardEnabled = true
                ),
                Product(
                    name = "1kg Crafted Coffee Beans",
                    productFamily = "MERCH",
                    description = "One kilogram of crafted KoffeeCraft coffee beans.",
                    price = 0.0,
                    isActive = true,
                    imageKey = "reward_beans_1kg",
                    rewardEnabled = true
                )
            )

            productDao.insertAll(products)
        }

        private suspend fun syncProductImageKeysByName(database: KoffeeCraftDatabase) {
            val productDao: ProductDao = database.productDao()
            val allProducts = productDao.getAllOnce()

            allProducts.forEach { product ->
                val expectedImageKey = ProductImageAssignments.imageKeyForProductName(product.name) ?: return@forEach

                if (product.imageKey != expectedImageKey) {
                    productDao.update(
                        product.copy(imageKey = expectedImageKey)
                    )
                }
            }
        }
    }
}