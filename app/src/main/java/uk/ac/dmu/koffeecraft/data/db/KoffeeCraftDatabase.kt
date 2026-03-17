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
import uk.ac.dmu.koffeecraft.data.dao.AddOnDao
import uk.ac.dmu.koffeecraft.data.dao.AllergenDao
import uk.ac.dmu.koffeecraft.data.dao.FavouriteDao
import uk.ac.dmu.koffeecraft.data.dao.ProductOptionDao

import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetDao
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef


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
        InboxMessage::class,
        AddOn::class,
        ProductOption::class,
        Allergen::class,
        ProductAddOnCrossRef::class,
        ProductAllergenCrossRef::class,
        AddOnAllergenCrossRef::class,
        Favourite::class,
        CustomerFavouritePreset::class,
        CustomerFavouritePresetAddOnCrossRef::class
    ],
    version = 15,
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
    abstract fun addOnDao(): AddOnDao
    abstract fun productOptionDao(): ProductOptionDao
    abstract fun allergenDao(): AllergenDao
    abstract fun favouriteDao(): FavouriteDao

    abstract fun customerFavouritePresetDao(): CustomerFavouritePresetDao

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
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "customers", "nextBeansBonusThreshold", "INTEGER NOT NULL DEFAULT 10")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
            SELECT 'KoffeeCraft Mug', 'REWARD', 'Crafted mug with the KoffeeCraft logo.', 0.0, 1, 0, 'reward_mug'
            WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'KoffeeCraft Mug')
        """.trimIndent())

                db.execSQL("""
            INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
            SELECT 'KoffeeCraft Teddy Bear', 'REWARD', 'Soft teddy bear with KoffeeCraft branding.', 0.0, 1, 0, 'reward_teddy'
            WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'KoffeeCraft Teddy Bear')
        """.trimIndent())

                db.execSQL("""
            INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
            SELECT '1kg Crafted Coffee Beans', 'REWARD', 'One kilogram of crafted KoffeeCraft coffee beans.', 0.0, 1, 0, 'reward_beans_1kg'
            WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = '1kg Crafted Coffee Beans')
        """.trimIndent())
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS product_options (
                optionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                optionName TEXT NOT NULL,
                displayLabel TEXT NOT NULL,
                sizeValue INTEGER NOT NULL,
                sizeUnit TEXT NOT NULL,
                extraPrice REAL NOT NULL,
                estimatedCalories INTEGER NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_options_productId ON product_options(productId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_options_productId_optionName ON product_options(productId, optionName)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS add_ons (
                addOnId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                estimatedCalories INTEGER NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_add_ons_category ON add_ons(category)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_add_ons_name_category ON add_ons(name, category)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS allergens (
                allergenId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
        """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_allergens_name ON allergens(name)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS product_add_on_cross_ref (
                productId INTEGER NOT NULL,
                addOnId INTEGER NOT NULL,
                PRIMARY KEY(productId, addOnId)
            )
        """.trimIndent())

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS product_allergen_cross_ref (
                productId INTEGER NOT NULL,
                allergenId INTEGER NOT NULL,
                PRIMARY KEY(productId, allergenId)
            )
        """.trimIndent())

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS add_on_allergen_cross_ref (
                addOnId INTEGER NOT NULL,
                allergenId INTEGER NOT NULL,
                PRIMARY KEY(addOnId, allergenId)
            )
        """.trimIndent())

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS favourites (
                customerId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(customerId, productId)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_customerId ON favourites(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_productId ON favourites(productId)")

                addColumnIfMissing(db, "order_items", "selectedOptionLabel", "TEXT")
                addColumnIfMissing(db, "order_items", "selectedOptionSizeValue", "INTEGER")
                addColumnIfMissing(db, "order_items", "selectedOptionSizeUnit", "TEXT")
                addColumnIfMissing(db, "order_items", "selectedAddOnsSummary", "TEXT")
                addColumnIfMissing(db, "order_items", "estimatedCalories", "INTEGER")

                db.execSQL("""INSERT OR IGNORE INTO allergens(name) VALUES ('Milk')""")
                db.execSQL("""INSERT OR IGNORE INTO allergens(name) VALUES ('Nuts')""")
                db.execSQL("""INSERT OR IGNORE INTO allergens(name) VALUES ('Gluten')""")
                db.execSQL("""INSERT OR IGNORE INTO allergens(name) VALUES ('Soy')""")
                db.execSQL("""INSERT OR IGNORE INTO allergens(name) VALUES ('Coconut')""")

                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Milk', 'COFFEE', 0.30, 25, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Vanilla syrup', 'COFFEE', 0.30, 35, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Caramel syrup', 'COFFEE', 0.30, 40, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Cinnamon topping', 'COFFEE', 0.30, 10, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Chocolate topping', 'COFFEE', 0.30, 35, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Hazelnut syrup', 'COFFEE', 0.30, 40, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Coconut topping', 'COFFEE', 0.30, 30, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Nut topping', 'COFFEE', 0.30, 45, 1)""")

                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Chocolate sauce', 'CAKE', 0.30, 45, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Raspberry sauce', 'CAKE', 0.30, 30, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Toffee sauce', 'CAKE', 0.30, 50, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Coconut topping', 'CAKE', 0.30, 30, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Nut topping', 'CAKE', 0.30, 45, 1)""")
                db.execSQL("""INSERT OR IGNORE INTO add_ons(name, category, price, estimatedCalories, isActive) VALUES ('Icing sugar', 'CAKE', 0.30, 20, 1)""")

                db.execSQL("""
            INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
            SELECT a.addOnId, al.allergenId
            FROM add_ons a, allergens al
            WHERE a.name = 'Milk' AND al.name = 'Milk'
              AND NOT EXISTS (
                  SELECT 1 FROM add_on_allergen_cross_ref x
                  WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
            SELECT a.addOnId, al.allergenId
            FROM add_ons a, allergens al
            WHERE a.name IN ('Hazelnut syrup', 'Nut topping') AND al.name = 'Nuts'
              AND NOT EXISTS (
                  SELECT 1 FROM add_on_allergen_cross_ref x
                  WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
            SELECT a.addOnId, al.allergenId
            FROM add_ons a, allergens al
            WHERE a.name = 'Coconut topping' AND al.name = 'Coconut'
              AND NOT EXISTS (
                  SELECT 1 FROM add_on_allergen_cross_ref x
                  WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'SMALL', 'Small', 250, 'ML', 0.0, 120, 1
            FROM products p
            WHERE p.category = 'COFFEE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'SMALL'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'MEDIUM', 'Medium', 350, 'ML', 0.5, 170, 0
            FROM products p
            WHERE p.category = 'COFFEE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'MEDIUM'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'LARGE', 'Large', 450, 'ML', 1.0, 220, 0
            FROM products p
            WHERE p.category = 'COFFEE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'LARGE'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'STANDARD', 'Standard slice', 120, 'G', 0.0, 300, 1
            FROM products p
            WHERE p.category = 'CAKE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'STANDARD'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'MEDIUM', 'Medium', 220, 'G', 0.5, 550, 0
            FROM products p
            WHERE p.category = 'CAKE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'MEDIUM'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
            SELECT p.productId, 'LARGE', 'Large', 330, 'G', 1.0, 820, 0
            FROM products p
            WHERE p.category = 'CAKE'
              AND NOT EXISTS (
                  SELECT 1 FROM product_options po
                  WHERE po.productId = p.productId AND po.optionName = 'LARGE'
              )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO product_add_on_cross_ref(productId, addOnId)
            SELECT p.productId, a.addOnId
            FROM products p
            INNER JOIN add_ons a ON a.category = p.category
            WHERE p.category IN ('COFFEE', 'CAKE')
              AND NOT EXISTS (
                  SELECT 1 FROM product_add_on_cross_ref x
                  WHERE x.productId = p.productId AND x.addOnId = a.addOnId
              )
        """.trimIndent())
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS customer_favourite_presets (
                presetId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                customerId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                optionId INTEGER NOT NULL,
                totalPriceSnapshot REAL NOT NULL,
                totalCaloriesSnapshot INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS customer_favourite_preset_add_on_cross_ref (
                presetId INTEGER NOT NULL,
                addOnId INTEGER NOT NULL,
                PRIMARY KEY(presetId, addOnId)
            )
            """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_customerId ON customer_favourite_presets(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_productId ON customer_favourite_presets(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_optionId ON customer_favourite_presets(optionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_createdAt ON customer_favourite_presets(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_preset_add_on_cross_ref_presetId ON customer_favourite_preset_add_on_cross_ref(presetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_preset_add_on_cross_ref_addOnId ON customer_favourite_preset_add_on_cross_ref(addOnId)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "products", "rewardEnabled", "INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
            UPDATE products
            SET rewardEnabled = 1
            WHERE category = 'REWARD'
            """.trimIndent()
                )

                db.execSQL(
                    """
            UPDATE products
            SET category = 'MERCH'
            WHERE category = 'REWARD'
            """.trimIndent()
                )
            }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "customers", "beansBoosterProgress", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "customers", "pendingBeansBoosters", "INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
            UPDATE customers
            SET beansBoosterProgress = CASE
                WHEN beansBalance < 0 THEN 0
                ELSE beansBalance % 10
            END,
            pendingBeansBoosters = 0
            """.trimIndent()
                )

                db.execSQL(
                    """
            UPDATE products
            SET rewardEnabled = 1
            WHERE category = 'COFFEE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM products
                  WHERE category = 'COFFEE'
                    AND rewardEnabled = 1
              )
            """.trimIndent()
                )

                db.execSQL(
                    """
            UPDATE products
            SET rewardEnabled = 1
            WHERE category = 'CAKE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM products
                  WHERE category = 'CAKE'
                    AND rewardEnabled = 1
              )
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "admins", "fullName", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "admins", "phone", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "admins", "username", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "admins", "isActive", "INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
            UPDATE admins
            SET fullName = CASE
                WHEN trim(fullName) = '' THEN 'KoffeeCraft Admin'
                ELSE fullName
            END,
            phone = CASE
                WHEN trim(phone) = '' THEN 'Not set'
                ELSE phone
            END,
            username = CASE
                WHEN trim(username) = '' THEN 'admin_' || adminId
                ELSE lower(trim(username))
            END
            """.trimIndent()
                )

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_admins_username ON admins(username)")
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
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15
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
                    rewardEnabled = true
                ),
                Product(
                    name = "Cappuccino",
                    productFamily = "COFFEE",
                    description = "Espresso with steamed milk and foam.",
                    price = 3.40,
                    rewardEnabled = true
                ),
                Product(
                    name = "Latte",
                    productFamily = "COFFEE",
                    description = "Smooth espresso with lots of milk.",
                    price = 3.60,
                    rewardEnabled = true
                ),
                Product(
                    name = "Cheesecake",
                    productFamily = "CAKE",
                    description = "Classic creamy cheesecake slice.",
                    price = 4.20,
                    rewardEnabled = true
                ),
                Product(
                    name = "Chocolate Brownie",
                    productFamily = "CAKE",
                    description = "Rich chocolate brownie.",
                    price = 3.00,
                    rewardEnabled = true
                ),
                Product(
                    name = "Carrot Cake",
                    productFamily = "CAKE",
                    description = "Moist carrot cake with frosting.",
                    price = 3.80,
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
    }
}