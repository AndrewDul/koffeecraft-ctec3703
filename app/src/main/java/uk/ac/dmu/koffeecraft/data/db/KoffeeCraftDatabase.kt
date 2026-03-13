package uk.ac.dmu.koffeecraft.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.AdminDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerDao
import uk.ac.dmu.koffeecraft.data.dao.FeedbackDao
import uk.ac.dmu.koffeecraft.data.dao.InboxMessageDao
import uk.ac.dmu.koffeecraft.data.dao.NotificationDao
import uk.ac.dmu.koffeecraft.data.dao.OrderDao
import uk.ac.dmu.koffeecraft.data.dao.OrderItemDao
import uk.ac.dmu.koffeecraft.data.dao.PaymentDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
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
        Payment::class,
        Feedback::class,
        AppNotification::class,
        InboxMessage::class
    ],
    version = 8,
    exportSchema = true
)
abstract class KoffeeCraftDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun adminDao(): AdminDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun notificationDao(): NotificationDao
    abstract fun inboxMessageDao(): InboxMessageDao

    companion object {
        @Volatile
        private var INSTANCE: KoffeeCraftDatabase? = null

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDef: String
        ) {
            val cursor = db.query("PRAGMA table_info($tableName)")
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    val existingName = it.getString(nameIndex)
                    if (existingName == columnName) return
                }
            }
            db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS feedback (
                        feedbackId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        customerId INTEGER NOT NULL,
                        rating INTEGER NOT NULL,
                        comment TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feedback_orderId ON feedback(orderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_customerId ON feedback(customerId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // I keep the real DB column name: isAvailable (admin enable/disable).
                addColumnIfMissing(db, "products", "isAvailable", "INTEGER NOT NULL DEFAULT 1")

                // I add a flag for the customer NEW carousel.
                addColumnIfMissing(db, "products", "isNew", "INTEGER NOT NULL DEFAULT 0")

                // I store a drawable key / identifier for future product images.
                addColumnIfMissing(db, "products", "imageKey", "TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // I rebuild feedback because the old schema stored one feedback per order,
                // while the new schema stores one feedback per purchased product (orderItem).
                db.execSQL("DROP INDEX IF EXISTS index_feedback_orderId")
                db.execSQL("DROP INDEX IF EXISTS index_feedback_customerId")
                db.execSQL("DROP TABLE IF EXISTS feedback")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS feedback (
                        feedbackId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderItemId INTEGER NOT NULL,
                        customerId INTEGER NOT NULL,
                        rating INTEGER NOT NULL,
                        comment TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feedback_orderItemId ON feedback(orderItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_customerId ON feedback(customerId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "feedback", "isHidden", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "feedback", "isModerated", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // I add date of birth for future rewards and birthday messaging.
                addColumnIfMissing(db, "customers", "dateOfBirth", "TEXT")

                // I create the stored in-app notifications table for admin and customer notification centers.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_notifications (
                        notificationId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipientRole TEXT NOT NULL,
                        recipientCustomerId INTEGER,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        notificationType TEXT NOT NULL,
                        orderId INTEGER,
                        orderCreatedAt INTEGER,
                        orderStatus TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_recipientRole ON app_notifications(recipientRole)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_recipientCustomerId ON app_notifications(recipientCustomerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_isRead ON app_notifications(isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_createdAt ON app_notifications(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_orderId ON app_notifications(orderId)")

                // I create the stored inbox messages table for customer messages from admin.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS inbox_messages (
                        inboxMessageId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipientCustomerId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        deliveryType TEXT NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_recipientCustomerId ON inbox_messages(recipientCustomerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_isRead ON inbox_messages(isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_createdAt ON inbox_messages(createdAt)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // I add new customer profile and rewards fields for premium registration and future rewards logic.
                addColumnIfMissing(db, "customers", "country", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "customers", "marketingInboxConsent", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "customers", "termsAccepted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "customers", "privacyAccepted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "customers", "beansBalance", "INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "customers", "isActive", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): KoffeeCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoffeeCraftDatabase::class.java,
                    "koffeecraft.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .addCallback(SeedCallback())
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

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
                Product(
                    name = "Espresso",
                    category = "COFFEE",
                    description = "Strong and bold espresso shot.",
                    price = 2.20
                ),
                Product(
                    name = "Cappuccino",
                    category = "COFFEE",
                    description = "Espresso with steamed milk and foam.",
                    price = 3.40
                ),
                Product(
                    name = "Latte",
                    category = "COFFEE",
                    description = "Smooth espresso with lots of milk.",
                    price = 3.60
                ),
                Product(
                    name = "Cheesecake",
                    category = "CAKE",
                    description = "Classic creamy cheesecake slice.",
                    price = 4.20
                ),
                Product(
                    name = "Chocolate Brownie",
                    category = "CAKE",
                    description = "Rich chocolate brownie.",
                    price = 3.00
                ),
                Product(
                    name = "Carrot Cake",
                    category = "CAKE",
                    description = "Moist carrot cake with frosting.",
                    price = 3.80
                )
            )

            productDao.insertAll(products)
        }
    }
}