package uk.ac.dmu.koffeecraft.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.AdminDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

@Database(
    entities = [
        Customer::class,
        Admin::class,
        Product::class,
        Order::class,
        OrderItem::class,
        Payment::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KoffeeCraftDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun adminDao(): AdminDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile private var INSTANCE: KoffeeCraftDatabase? = null

        fun getInstance(context: Context): KoffeeCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoffeeCraftDatabase::class.java,
                    "koffeecraft.db"
                )
                    .addCallback(SeedCallback())
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)

            CoroutineScope(Dispatchers.IO).launch {
                val database = INSTANCE ?: return@launch
                seedAdminIfNeeded(database)
                seedProductsIfNeeded(database)
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
                    email = email,
                    passwordHash = hash,
                    passwordSalt = salt
                )
            )
        }

        private suspend fun seedProductsIfNeeded(database: KoffeeCraftDatabase) {
            val productDao: ProductDao = database.productDao()
            if (productDao.countProducts() > 0) return

            val products = listOf(
                // 3 COFFEES
                Product(name = "Espresso", category = "COFFEE", description = "Strong and bold espresso shot.", price = 2.20),
                Product(name = "Cappuccino", category = "COFFEE", description = "Espresso with steamed milk and foam.", price = 3.40),
                Product(name = "Latte", category = "COFFEE", description = "Smooth espresso with lots of milk.", price = 3.60),

                // 3 CAKES
                Product(name = "Cheesecake", category = "CAKE", description = "Classic creamy cheesecake slice.", price = 4.20),
                Product(name = "Chocolate Brownie", category = "CAKE", description = "Rich chocolate brownie.", price = 3.00),
                Product(name = "Carrot Cake", category = "CAKE", description = "Moist carrot cake with frosting.", price = 3.80)
            )

            productDao.insertAll(products)
        }
    }
}