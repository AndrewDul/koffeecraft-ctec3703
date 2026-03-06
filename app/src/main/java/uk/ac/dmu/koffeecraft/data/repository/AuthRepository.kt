package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

class AuthRepository(private val db: KoffeeCraftDatabase) {

    sealed class LoginResult {
        data class CustomerSuccess(val customerId: Long) : LoginResult()
        object AdminSuccess : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    sealed class RegisterResult {
        object Success : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }

    suspend fun login(email: String, password: CharArray): LoginResult {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.isEmpty()) {
            return LoginResult.Error("Email and password are required.")
        }

        // 1) Try admin first
        val admin = db.adminDao().findByEmail(cleanEmail)
        if (admin != null) {
            val ok = PasswordHasher.verify(password, admin.passwordSalt, admin.passwordHash)
            password.fill('\u0000')
            return if (ok) LoginResult.AdminSuccess else LoginResult.Error("Invalid credentials.")
        }

        // 2) Try customer
        val customer = db.customerDao().findByEmail(cleanEmail)
        if (customer != null) {
            val ok = PasswordHasher.verify(password, customer.passwordSalt, customer.passwordHash)
            password.fill('\u0000')
            return if (ok) LoginResult.CustomerSuccess(customer.customerId)
            else LoginResult.Error("Invalid credentials.")
        }

        password.fill('\u0000')
        return LoginResult.Error("Account not found.")
    }

    suspend fun registerCustomer(
        firstName: String,
        lastName: String,
        email: String,
        password: CharArray
    ): RegisterResult {

        val fn = firstName.trim()
        val ln = lastName.trim()
        val em = email.trim().lowercase()

        if (fn.isBlank() || ln.isBlank() || em.isBlank() || password.isEmpty()) {
            return RegisterResult.Error("All fields are required.")
        }
        if (!em.contains("@") || !em.contains(".")) {
            return RegisterResult.Error("Please enter a valid email address.")
        }
        if (password.size < 8) {
            return RegisterResult.Error("Password must be at least 8 characters.")
        }

        val existing = db.customerDao().findByEmail(em)
        if (existing != null) {
            password.fill('\u0000')
            return RegisterResult.Error("This email is already registered.")
        }

        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPasswordBase64(password, salt)
        password.fill('\u0000')

        db.customerDao().insert(
            Customer(
                firstName = fn,
                lastName = ln,
                email = em,
                passwordHash = hash,
                passwordSalt = salt
            )
        )

        return RegisterResult.Success
    }
}