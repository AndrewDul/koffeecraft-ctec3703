package uk.ac.dmu.koffeecraft.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

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
            addColumnIfMissing(db, "products", "isAvailable", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "products", "isNew", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "products", "imageKey", "TEXT")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
            addColumnIfMissing(db, "customers", "dateOfBirth", "TEXT")

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
            db.execSQL(
                """
                INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
                SELECT 'KoffeeCraft Mug', 'REWARD', 'Crafted mug with the KoffeeCraft logo.', 0.0, 1, 0, 'reward_mug'
                WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'KoffeeCraft Mug')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
                SELECT 'KoffeeCraft Teddy Bear', 'REWARD', 'Soft teddy bear with KoffeeCraft branding.', 0.0, 1, 0, 'reward_teddy'
                WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'KoffeeCraft Teddy Bear')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO products (name, category, description, price, isAvailable, isNew, imageKey)
                SELECT '1kg Crafted Coffee Beans', 'REWARD', 'One kilogram of crafted KoffeeCraft coffee beans.', 0.0, 1, 0, 'reward_beans_1kg'
                WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = '1kg Crafted Coffee Beans')
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_options_productId ON product_options(productId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_options_productId_optionName ON product_options(productId, optionName)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS add_ons (
                    addOnId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    price REAL NOT NULL,
                    estimatedCalories INTEGER NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_add_ons_category ON add_ons(category)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_add_ons_name_category ON add_ons(name, category)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS allergens (
                    allergenId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_allergens_name ON allergens(name)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_add_on_cross_ref (
                    productId INTEGER NOT NULL,
                    addOnId INTEGER NOT NULL,
                    PRIMARY KEY(productId, addOnId)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_allergen_cross_ref (
                    productId INTEGER NOT NULL,
                    allergenId INTEGER NOT NULL,
                    PRIMARY KEY(productId, allergenId)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS add_on_allergen_cross_ref (
                    addOnId INTEGER NOT NULL,
                    allergenId INTEGER NOT NULL,
                    PRIMARY KEY(addOnId, allergenId)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favourites (
                    customerId INTEGER NOT NULL,
                    productId INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(customerId, productId)
                )
                """.trimIndent()
            )
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

            db.execSQL(
                """
                INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
                SELECT a.addOnId, al.allergenId
                FROM add_ons a, allergens al
                WHERE a.name = 'Milk' AND al.name = 'Milk'
                  AND NOT EXISTS (
                      SELECT 1 FROM add_on_allergen_cross_ref x
                      WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
                SELECT a.addOnId, al.allergenId
                FROM add_ons a, allergens al
                WHERE a.name IN ('Hazelnut syrup', 'Nut topping') AND al.name = 'Nuts'
                  AND NOT EXISTS (
                      SELECT 1 FROM add_on_allergen_cross_ref x
                      WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO add_on_allergen_cross_ref(addOnId, allergenId)
                SELECT a.addOnId, al.allergenId
                FROM add_ons a, allergens al
                WHERE a.name = 'Coconut topping' AND al.name = 'Coconut'
                  AND NOT EXISTS (
                      SELECT 1 FROM add_on_allergen_cross_ref x
                      WHERE x.addOnId = a.addOnId AND x.allergenId = al.allergenId
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'SMALL', 'Small', 250, 'ML', 0.0, 120, 1
                FROM products p
                WHERE p.category = 'COFFEE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'SMALL'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'MEDIUM', 'Medium', 350, 'ML', 0.5, 170, 0
                FROM products p
                WHERE p.category = 'COFFEE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'MEDIUM'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'LARGE', 'Large', 450, 'ML', 1.0, 220, 0
                FROM products p
                WHERE p.category = 'COFFEE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'LARGE'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'STANDARD', 'Standard slice', 120, 'G', 0.0, 300, 1
                FROM products p
                WHERE p.category = 'CAKE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'STANDARD'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'MEDIUM', 'Medium', 220, 'G', 0.5, 550, 0
                FROM products p
                WHERE p.category = 'CAKE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'MEDIUM'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options(productId, optionName, displayLabel, sizeValue, sizeUnit, extraPrice, estimatedCalories, isDefault)
                SELECT p.productId, 'LARGE', 'Large', 330, 'G', 1.0, 820, 0
                FROM products p
                WHERE p.category = 'CAKE'
                  AND NOT EXISTS (
                      SELECT 1 FROM product_options po
                      WHERE po.productId = p.productId AND po.optionName = 'LARGE'
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_add_on_cross_ref(productId, addOnId)
                SELECT p.productId, a.addOnId
                FROM products p
                INNER JOIN add_ons a ON a.category = p.category
                WHERE p.category IN ('COFFEE', 'CAKE')
                  AND NOT EXISTS (
                      SELECT 1 FROM product_add_on_cross_ref x
                      WHERE x.productId = p.productId AND x.addOnId = a.addOnId
                  )
                """.trimIndent()
            )
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

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS customer_payment_cards (
                    cardId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId INTEGER NOT NULL,
                    nickname TEXT NOT NULL,
                    cardholderName TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    maskedCardNumber TEXT NOT NULL,
                    last4 TEXT NOT NULL,
                    expiryMonth INTEGER NOT NULL,
                    expiryYear INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId ON customer_payment_cards(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId_isDefault ON customer_payment_cards(customerId, isDefault)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId_createdAt ON customer_payment_cards(customerId, createdAt)")
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")

            db.execSQL(
                """
                DELETE FROM orders
                WHERE customerId NOT IN (SELECT customerId FROM customers)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM order_items
                WHERE orderId NOT IN (SELECT orderId FROM orders)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM payments
                WHERE orderId NOT IN (SELECT orderId FROM orders)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM feedback
                WHERE orderItemId NOT IN (SELECT orderItemId FROM order_items)
                   OR customerId NOT IN (SELECT customerId FROM customers)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM customer_payment_cards
                WHERE customerId NOT IN (SELECT customerId FROM customers)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM favourites
                WHERE customerId NOT IN (SELECT customerId FROM customers)
                   OR productId NOT IN (SELECT productId FROM products)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS orders_new (
                    orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    totalAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(customerId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO orders_new (orderId, customerId, status, totalAmount, createdAt)
                SELECT orderId, customerId, status, totalAmount, createdAt
                FROM orders
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS order_items_new (
                    orderItemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    orderId INTEGER NOT NULL,
                    productId INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    unitPrice REAL NOT NULL,
                    selectedOptionLabel TEXT,
                    selectedOptionSizeValue INTEGER,
                    selectedOptionSizeUnit TEXT,
                    selectedAddOnsSummary TEXT,
                    estimatedCalories INTEGER,
                    FOREIGN KEY(orderId) REFERENCES orders(orderId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO order_items_new (
                    orderItemId,
                    orderId,
                    productId,
                    quantity,
                    unitPrice,
                    selectedOptionLabel,
                    selectedOptionSizeValue,
                    selectedOptionSizeUnit,
                    selectedAddOnsSummary,
                    estimatedCalories
                )
                SELECT
                    orderItemId,
                    orderId,
                    productId,
                    quantity,
                    unitPrice,
                    selectedOptionLabel,
                    selectedOptionSizeValue,
                    selectedOptionSizeUnit,
                    selectedAddOnsSummary,
                    estimatedCalories
                FROM order_items
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS payments_new (
                    paymentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    orderId INTEGER NOT NULL,
                    paymentType TEXT NOT NULL,
                    amount REAL NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    FOREIGN KEY(orderId) REFERENCES orders(orderId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO payments_new (paymentId, orderId, paymentType, amount, paymentDate)
                SELECT paymentId, orderId, paymentType, amount, paymentDate
                FROM payments
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS feedback_new (
                    feedbackId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    orderItemId INTEGER NOT NULL,
                    customerId INTEGER NOT NULL,
                    rating INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    isHidden INTEGER NOT NULL DEFAULT 0,
                    isModerated INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(orderItemId) REFERENCES order_items(orderItemId) ON DELETE CASCADE,
                    FOREIGN KEY(customerId) REFERENCES customers(customerId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO feedback_new (
                    feedbackId,
                    orderItemId,
                    customerId,
                    rating,
                    comment,
                    isHidden,
                    isModerated,
                    createdAt,
                    updatedAt
                )
                SELECT
                    feedbackId,
                    orderItemId,
                    customerId,
                    rating,
                    comment,
                    isHidden,
                    isModerated,
                    createdAt,
                    updatedAt
                FROM feedback
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS customer_payment_cards_new (
                    cardId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId INTEGER NOT NULL,
                    nickname TEXT NOT NULL,
                    cardholderName TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    maskedCardNumber TEXT NOT NULL,
                    last4 TEXT NOT NULL,
                    expiryMonth INTEGER NOT NULL,
                    expiryYear INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(customerId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO customer_payment_cards_new (
                    cardId,
                    customerId,
                    nickname,
                    cardholderName,
                    brand,
                    maskedCardNumber,
                    last4,
                    expiryMonth,
                    expiryYear,
                    isDefault,
                    createdAt
                )
                SELECT
                    cardId,
                    customerId,
                    nickname,
                    cardholderName,
                    brand,
                    maskedCardNumber,
                    last4,
                    expiryMonth,
                    expiryYear,
                    isDefault,
                    createdAt
                FROM customer_payment_cards
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favourites_new (
                    customerId INTEGER NOT NULL,
                    productId INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(customerId, productId),
                    FOREIGN KEY(customerId) REFERENCES customers(customerId) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO favourites_new (customerId, productId, createdAt)
                SELECT customerId, productId, createdAt
                FROM favourites
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS feedback")
            db.execSQL("DROP TABLE IF EXISTS payments")
            db.execSQL("DROP TABLE IF EXISTS order_items")
            db.execSQL("DROP TABLE IF EXISTS customer_payment_cards")
            db.execSQL("DROP TABLE IF EXISTS favourites")
            db.execSQL("DROP TABLE IF EXISTS orders")

            db.execSQL("ALTER TABLE orders_new RENAME TO orders")
            db.execSQL("ALTER TABLE order_items_new RENAME TO order_items")
            db.execSQL("ALTER TABLE payments_new RENAME TO payments")
            db.execSQL("ALTER TABLE feedback_new RENAME TO feedback")
            db.execSQL("ALTER TABLE customer_payment_cards_new RENAME TO customer_payment_cards")
            db.execSQL("ALTER TABLE favourites_new RENAME TO favourites")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_customerId ON orders(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_productId ON order_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_orderId ON payments(orderId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feedback_orderItemId ON feedback(orderItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_customerId ON feedback(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId ON customer_payment_cards(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId_isDefault ON customer_payment_cards(customerId, isDefault)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_cards_customerId_createdAt ON customer_payment_cards(customerId, createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_customerId ON favourites(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_productId ON favourites(productId)")

            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE order_items ADD COLUMN productNameSnapshot TEXT")
            db.execSQL("ALTER TABLE order_items ADD COLUMN productDescriptionSnapshot TEXT")

            db.execSQL(
                """
                UPDATE order_items
                SET productNameSnapshot = (
                    SELECT p.name
                    FROM products p
                    WHERE p.productId = order_items.productId
                )
                WHERE productNameSnapshot IS NULL
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE order_items
                SET productDescriptionSnapshot = (
                    SELECT p.description
                    FROM products p
                    WHERE p.productId = order_items.productId
                )
                WHERE productDescriptionSnapshot IS NULL
                """.trimIndent()
            )
        }
    }
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")

            db.execSQL(
                """
                DELETE FROM product_options
                WHERE productId NOT IN (SELECT productId FROM products)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM product_add_on_cross_ref
                WHERE productId NOT IN (SELECT productId FROM products)
                   OR addOnId NOT IN (SELECT addOnId FROM add_ons)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM product_allergen_cross_ref
                WHERE productId NOT IN (SELECT productId FROM products)
                   OR allergenId NOT IN (SELECT allergenId FROM allergens)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM add_on_allergen_cross_ref
                WHERE addOnId NOT IN (SELECT addOnId FROM add_ons)
                   OR allergenId NOT IN (SELECT allergenId FROM allergens)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM customer_favourite_presets
                WHERE customerId NOT IN (SELECT customerId FROM customers)
                   OR productId NOT IN (SELECT productId FROM products)
                   OR optionId NOT IN (SELECT optionId FROM product_options)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM customer_favourite_preset_add_on_cross_ref
                WHERE presetId NOT IN (SELECT presetId FROM customer_favourite_presets)
                   OR addOnId NOT IN (SELECT addOnId FROM add_ons)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM inbox_messages
                WHERE recipientCustomerId NOT IN (SELECT customerId FROM customers)
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM app_notifications
                WHERE (recipientCustomerId IS NOT NULL AND recipientCustomerId NOT IN (SELECT customerId FROM customers))
                   OR (orderId IS NOT NULL AND orderId NOT IN (SELECT orderId FROM orders))
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_options_new (
                    optionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productId INTEGER NOT NULL,
                    optionName TEXT NOT NULL,
                    displayLabel TEXT NOT NULL,
                    sizeValue INTEGER NOT NULL,
                    sizeUnit TEXT NOT NULL,
                    extraPrice REAL NOT NULL,
                    estimatedCalories INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_options_new (
                    optionId,
                    productId,
                    optionName,
                    displayLabel,
                    sizeValue,
                    sizeUnit,
                    extraPrice,
                    estimatedCalories,
                    isDefault
                )
                SELECT
                    optionId,
                    productId,
                    optionName,
                    displayLabel,
                    sizeValue,
                    sizeUnit,
                    extraPrice,
                    estimatedCalories,
                    isDefault
                FROM product_options
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_add_on_cross_ref_new (
                    productId INTEGER NOT NULL,
                    addOnId INTEGER NOT NULL,
                    PRIMARY KEY(productId, addOnId),
                    FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE CASCADE,
                    FOREIGN KEY(addOnId) REFERENCES add_ons(addOnId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_add_on_cross_ref_new (productId, addOnId)
                SELECT productId, addOnId
                FROM product_add_on_cross_ref
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_allergen_cross_ref_new (
                    productId INTEGER NOT NULL,
                    allergenId INTEGER NOT NULL,
                    PRIMARY KEY(productId, allergenId),
                    FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE CASCADE,
                    FOREIGN KEY(allergenId) REFERENCES allergens(allergenId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO product_allergen_cross_ref_new (productId, allergenId)
                SELECT productId, allergenId
                FROM product_allergen_cross_ref
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS add_on_allergen_cross_ref_new (
                    addOnId INTEGER NOT NULL,
                    allergenId INTEGER NOT NULL,
                    PRIMARY KEY(addOnId, allergenId),
                    FOREIGN KEY(addOnId) REFERENCES add_ons(addOnId) ON DELETE CASCADE,
                    FOREIGN KEY(allergenId) REFERENCES allergens(allergenId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO add_on_allergen_cross_ref_new (addOnId, allergenId)
                SELECT addOnId, allergenId
                FROM add_on_allergen_cross_ref
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS customer_favourite_presets_new (
                    presetId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId INTEGER NOT NULL,
                    productId INTEGER NOT NULL,
                    optionId INTEGER NOT NULL,
                    totalPriceSnapshot REAL NOT NULL,
                    totalCaloriesSnapshot INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(customerId) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE CASCADE,
                    FOREIGN KEY(optionId) REFERENCES product_options(optionId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO customer_favourite_presets_new (
                    presetId,
                    customerId,
                    productId,
                    optionId,
                    totalPriceSnapshot,
                    totalCaloriesSnapshot,
                    createdAt
                )
                SELECT
                    presetId,
                    customerId,
                    productId,
                    optionId,
                    totalPriceSnapshot,
                    totalCaloriesSnapshot,
                    createdAt
                FROM customer_favourite_presets
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS customer_favourite_preset_add_on_cross_ref_new (
                    presetId INTEGER NOT NULL,
                    addOnId INTEGER NOT NULL,
                    PRIMARY KEY(presetId, addOnId),
                    FOREIGN KEY(presetId) REFERENCES customer_favourite_presets(presetId) ON DELETE CASCADE,
                    FOREIGN KEY(addOnId) REFERENCES add_ons(addOnId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO customer_favourite_preset_add_on_cross_ref_new (presetId, addOnId)
                SELECT presetId, addOnId
                FROM customer_favourite_preset_add_on_cross_ref
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS inbox_messages_new (
                    inboxMessageId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recipientCustomerId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    body TEXT NOT NULL,
                    deliveryType TEXT NOT NULL,
                    isRead INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(recipientCustomerId) REFERENCES customers(customerId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO inbox_messages_new (
                    inboxMessageId,
                    recipientCustomerId,
                    title,
                    body,
                    deliveryType,
                    isRead,
                    createdAt
                )
                SELECT
                    inboxMessageId,
                    recipientCustomerId,
                    title,
                    body,
                    deliveryType,
                    isRead,
                    createdAt
                FROM inbox_messages
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_notifications_new (
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
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(recipientCustomerId) REFERENCES customers(customerId) ON DELETE CASCADE,
                    FOREIGN KEY(orderId) REFERENCES orders(orderId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO app_notifications_new (
                    notificationId,
                    recipientRole,
                    recipientCustomerId,
                    title,
                    message,
                    notificationType,
                    orderId,
                    orderCreatedAt,
                    orderStatus,
                    isRead,
                    createdAt
                )
                SELECT
                    notificationId,
                    recipientRole,
                    recipientCustomerId,
                    title,
                    message,
                    notificationType,
                    orderId,
                    orderCreatedAt,
                    orderStatus,
                    isRead,
                    createdAt
                FROM app_notifications
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS product_options")
            db.execSQL("DROP TABLE IF EXISTS product_add_on_cross_ref")
            db.execSQL("DROP TABLE IF EXISTS product_allergen_cross_ref")
            db.execSQL("DROP TABLE IF EXISTS add_on_allergen_cross_ref")
            db.execSQL("DROP TABLE IF EXISTS customer_favourite_preset_add_on_cross_ref")
            db.execSQL("DROP TABLE IF EXISTS customer_favourite_presets")
            db.execSQL("DROP TABLE IF EXISTS inbox_messages")
            db.execSQL("DROP TABLE IF EXISTS app_notifications")

            db.execSQL("ALTER TABLE product_options_new RENAME TO product_options")
            db.execSQL("ALTER TABLE product_add_on_cross_ref_new RENAME TO product_add_on_cross_ref")
            db.execSQL("ALTER TABLE product_allergen_cross_ref_new RENAME TO product_allergen_cross_ref")
            db.execSQL("ALTER TABLE add_on_allergen_cross_ref_new RENAME TO add_on_allergen_cross_ref")
            db.execSQL("ALTER TABLE customer_favourite_presets_new RENAME TO customer_favourite_presets")
            db.execSQL("ALTER TABLE customer_favourite_preset_add_on_cross_ref_new RENAME TO customer_favourite_preset_add_on_cross_ref")
            db.execSQL("ALTER TABLE inbox_messages_new RENAME TO inbox_messages")
            db.execSQL("ALTER TABLE app_notifications_new RENAME TO app_notifications")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_options_productId ON product_options(productId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_options_productId_optionName ON product_options(productId, optionName)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_add_on_cross_ref_addOnId ON product_add_on_cross_ref(addOnId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_allergen_cross_ref_allergenId ON product_allergen_cross_ref(allergenId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_add_on_allergen_cross_ref_allergenId ON add_on_allergen_cross_ref(allergenId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_customerId ON customer_favourite_presets(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_productId ON customer_favourite_presets(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_optionId ON customer_favourite_presets(optionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_presets_createdAt ON customer_favourite_presets(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_preset_add_on_cross_ref_presetId ON customer_favourite_preset_add_on_cross_ref(presetId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_favourite_preset_add_on_cross_ref_addOnId ON customer_favourite_preset_add_on_cross_ref(addOnId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_recipientCustomerId ON inbox_messages(recipientCustomerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_isRead ON inbox_messages(isRead)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inbox_messages_createdAt ON inbox_messages(createdAt)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_recipientRole ON app_notifications(recipientRole)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_recipientCustomerId ON app_notifications(recipientCustomerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_isRead ON app_notifications(isRead)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_createdAt ON app_notifications(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_orderId ON app_notifications(orderId)")

            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }
    val ALL = arrayOf(
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
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19
    )

}