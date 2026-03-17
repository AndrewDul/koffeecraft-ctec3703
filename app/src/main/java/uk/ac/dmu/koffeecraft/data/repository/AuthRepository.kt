package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

class AuthRepository(private val db: KoffeeCraftDatabase) {

    enum class UserRole {
        CUSTOMER,
        ADMIN
    }

    sealed class LoginResult {
        data class Success(
            val userId: Long,
            val role: UserRole
        ) : LoginResult()

        data class Error(val message: String) : LoginResult()
    }

    sealed class RegisterResult {
        data class Success(val customerId: Long) : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }

    suspend fun login(email: String, password: CharArray): LoginResult {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank() || password.isEmpty()) {
            password.fill('\u0000')
            return LoginResult.Error("Email and password are required.")
        }

        val admin = db.adminDao().findByEmail(cleanEmail)
        if (admin != null) {
            val isValidPassword = PasswordHasher.verify(password, admin.passwordSalt, admin.passwordHash)
            password.fill('\u0000')

            return when {
                !isValidPassword -> LoginResult.Error("Invalid email or password.")
                !admin.isActive -> LoginResult.Error(
                    "This admin account is inactive. Please contact an active administrator."
                )
                else -> LoginResult.Success(
                    userId = admin.adminId,
                    role = UserRole.ADMIN
                )
            }
        }

        val customer = db.customerDao().findByEmail(cleanEmail)
        if (customer != null) {
            val isValidPassword = PasswordHasher.verify(password, customer.passwordSalt, customer.passwordHash)
            password.fill('\u0000')

            return when {
                !isValidPassword -> LoginResult.Error("Invalid email or password.")
                !customer.isActive -> LoginResult.Error(
                    "This account is deactivated. Please contact KoffeeCraft support."
                )
                else -> LoginResult.Success(
                    userId = customer.customerId,
                    role = UserRole.CUSTOMER
                )
            }
        }

        password.fill('\u0000')
        return LoginResult.Error("Invalid email or password.")
    }

    suspend fun registerCustomer(
        firstName: String,
        lastName: String,
        country: String,
        dateOfBirth: String,
        email: String,
        password: CharArray,
        marketingInboxConsent: Boolean,
        termsAccepted: Boolean,
        privacyAccepted: Boolean
    ): RegisterResult {

        val fn = firstName.trim()
        val ln = lastName.trim()
        val ct = country.trim()
        val dob = dateOfBirth.trim()
        val em = email.trim().lowercase()

        if (fn.isBlank() || ln.isBlank() || ct.isBlank() || dob.isBlank() || em.isBlank() || password.isEmpty()) {
            return RegisterResult.Error("All required fields must be completed.")
        }

        if (!em.contains("@") || !em.contains(".")) {
            return RegisterResult.Error("Please enter a valid email address.")
        }

        if (!termsAccepted) {
            password.fill('\u0000')
            return RegisterResult.Error("You must accept the Terms of Use.")
        }

        if (!privacyAccepted) {
            password.fill('\u0000')
            return RegisterResult.Error("You must accept the Privacy Statement.")
        }

        val passwordText = password.concatToString()
        if (!isPasswordValid(passwordText)) {
            password.fill('\u0000')
            return RegisterResult.Error("Password does not meet all required rules.")
        }

        if (!isValidDateOfBirth(dob)) {
            password.fill('\u0000')
            return RegisterResult.Error("Date of birth must use the format YYYY-MM-DD.")
        }

        val existing = db.customerDao().findByEmail(em)
        if (existing != null) {
            password.fill('\u0000')
            return RegisterResult.Error("This email is already registered.")
        }

        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPasswordBase64(password, salt)
        password.fill('\u0000')

        val customerId = db.customerDao().insert(
            Customer(
                firstName = fn,
                lastName = ln,
                country = ct,
                email = em,
                passwordHash = hash,
                passwordSalt = salt,
                dateOfBirth = dob,
                marketingInboxConsent = marketingInboxConsent,
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted,
                beansBalance = 0
            )
        )

        return RegisterResult.Success(customerId)
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() } &&
                password.length >= 8
    }

    private fun isValidDateOfBirth(value: String): Boolean {
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""")
        return regex.matches(value)
    }
}