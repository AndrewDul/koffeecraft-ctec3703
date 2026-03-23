package uk.ac.dmu.koffeecraft.testsupport

import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Payment
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

object TestSeedData {

    suspend fun insertCustomer(
        db: KoffeeCraftDatabase,
        email: String = "customer@example.com",
        password: String = "Strong1!",
        firstName: String = "Andrew",
        lastName: String = "Dul",
        country: String = "UK",
        dateOfBirth: String = "2000-01-01",
        marketingInboxConsent: Boolean = true,
        beansBalance: Int = 0,
        beansBoosterProgress: Int = 0,
        pendingBeansBoosters: Int = 0,
        isActive: Boolean = true
    ): Long {
        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPasswordBase64(password.toCharArray(), salt)

        return db.customerDao().insert(
            Customer(
                firstName = firstName,
                lastName = lastName,
                country = country,
                email = email.trim().lowercase(),
                passwordHash = hash,
                passwordSalt = salt,
                dateOfBirth = dateOfBirth,
                marketingInboxConsent = marketingInboxConsent,
                termsAccepted = true,
                privacyAccepted = true,
                beansBalance = beansBalance,
                beansBoosterProgress = beansBoosterProgress,
                pendingBeansBoosters = pendingBeansBoosters,
                isActive = isActive
            )
        )
    }

    suspend fun insertAdmin(
        db: KoffeeCraftDatabase,
        email: String = "admin@example.com",
        username: String = "admin01",
        password: String = "Strong1!",
        fullName: String = "Admin User",
        phone: String = "07123456789",
        isActive: Boolean = true
    ): Long {
        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPasswordBase64(password.toCharArray(), salt)

        return db.adminDao().insert(
            Admin(
                fullName = fullName,
                email = email.trim().lowercase(),
                phone = phone,
                username = username,
                passwordHash = hash,
                passwordSalt = salt,
                isActive = isActive
            )
        )
    }

    suspend fun insertProduct(
        db: KoffeeCraftDatabase,
        name: String = "Flat White",
        productFamily: String = "COFFEE",
        description: String = "Smooth coffee",
        price: Double = 4.50,
        isActive: Boolean = true,
        isNew: Boolean = false,
        rewardEnabled: Boolean = false
    ): Product {
        val product = Product(
            name = name,
            productFamily = productFamily,
            description = description,
            price = price,
            isActive = isActive,
            isNew = isNew,
            rewardEnabled = rewardEnabled
        )

        val productId = db.productDao().insert(product)
        return product.copy(productId = productId)
    }

    suspend fun insertOrder(
        db: KoffeeCraftDatabase,
        customerId: Long,
        status: String = "PLACED",
        totalAmount: Double = 0.0,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        return db.orderDao().insert(
            Order(
                customerId = customerId,
                status = status,
                totalAmount = totalAmount,
                createdAt = createdAt
            )
        )
    }

    fun buildOrderItem(
        orderId: Long,
        productId: Long,
        quantity: Int = 1,
        unitPrice: Double,
        selectedOptionLabel: String? = null,
        selectedOptionSizeValue: Int? = null,
        selectedOptionSizeUnit: String? = null,
        selectedAddOnsSummary: String? = null,
        estimatedCalories: Int? = null,
        productNameSnapshot: String? = null,
        productDescriptionSnapshot: String? = null
    ): OrderItem {
        return OrderItem(
            orderId = orderId,
            productId = productId,
            quantity = quantity,
            unitPrice = unitPrice,
            selectedOptionLabel = selectedOptionLabel,
            selectedOptionSizeValue = selectedOptionSizeValue,
            selectedOptionSizeUnit = selectedOptionSizeUnit,
            selectedAddOnsSummary = selectedAddOnsSummary,
            estimatedCalories = estimatedCalories,
            productNameSnapshot = productNameSnapshot,
            productDescriptionSnapshot = productDescriptionSnapshot
        )
    }

    suspend fun insertPayment(
        db: KoffeeCraftDatabase,
        orderId: Long,
        paymentType: String = "CARD",
        amount: Double
    ): Long {
        return db.paymentDao().insert(
            Payment(
                orderId = orderId,
                paymentType = paymentType,
                amount = amount
            )
        )
    }

    suspend fun insertFeedback(
        db: KoffeeCraftDatabase,
        orderItemId: Long,
        customerId: Long,
        rating: Int,
        comment: String
    ) {
        db.feedbackDao().upsert(
            Feedback(
                orderItemId = orderItemId,
                customerId = customerId,
                rating = rating,
                comment = comment
            )
        )
    }

    fun buildCartItem(
        product: Product,
        quantity: Int = 1,
        unitPrice: Double = product.price,
        isReward: Boolean = false,
        rewardType: String? = null,
        beansCostPerUnit: Int = 0,
        selectedOptionLabel: String? = null,
        selectedOptionSizeValue: Int? = null,
        selectedOptionSizeUnit: String? = null,
        selectedAddOnsSummary: String? = null,
        estimatedCalories: Int? = null
    ): CartItem {
        return CartItem(
            lineKey = "line_${product.productId}_${System.nanoTime()}",
            product = product,
            quantity = quantity,
            unitPrice = unitPrice,
            isReward = isReward,
            rewardType = rewardType,
            beansCostPerUnit = beansCostPerUnit,
            selectedOptionLabel = selectedOptionLabel,
            selectedOptionSizeValue = selectedOptionSizeValue,
            selectedOptionSizeUnit = selectedOptionSizeUnit,
            selectedAddOnsSummary = selectedAddOnsSummary,
            estimatedCalories = estimatedCalories
        )
    }
}