package uk.ac.dmu.koffeecraft.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class AuthRepositoryInstrumentedTest : BaseInstrumentedDatabaseTest() {

    private lateinit var repository: AuthRepository

    @Before
    fun setUpRepository() {
        repository = AuthRepository(db)
    }

    @Test
    fun registerCustomer_success_persistsCustomer_andLoginSucceeds() = runBlocking {
        val registerPassword = "Strong1!".toCharArray()

        val registerResult = repository.registerCustomer(
            firstName = "Andrew",
            lastName = "Dul",
            country = "UK",
            dateOfBirth = "2000-12-31",
            email = "  Andrew@example.com ",
            password = registerPassword,
            marketingInboxConsent = true,
            termsAccepted = true,
            privacyAccepted = true
        )

        assertTrue(registerResult is AuthRepository.RegisterResult.Success)
        assertTrue(registerPassword.all { it == '\u0000' })

        val storedCustomer = db.customerDao().findByEmail("andrew@example.com")
        assertNotNull(storedCustomer)
        assertEquals("andrew@example.com", storedCustomer!!.email)
        assertNotEquals("Strong1!", storedCustomer.passwordHash)

        val loginPassword = "Strong1!".toCharArray()
        val loginResult = repository.login(" andrew@example.com ", loginPassword)

        assertTrue(loginResult is AuthRepository.LoginResult.Success)
        assertTrue(loginPassword.all { it == '\u0000' })

        val success = loginResult as AuthRepository.LoginResult.Success
        assertEquals(AuthRepository.UserRole.CUSTOMER, success.role)
        assertEquals(storedCustomer.customerId, success.userId)
    }

    @Test
    fun registerCustomer_returnsError_whenEmailAlreadyExists() = runBlocking {
        TestSeedData.insertCustomer(
            db = db,
            email = "taken@example.com"
        )

        val password = "Strong1!".toCharArray()

        val result = repository.registerCustomer(
            firstName = "Andrew",
            lastName = "Dul",
            country = "UK",
            dateOfBirth = "2000-12-31",
            email = "  TAKEN@example.com ",
            password = password,
            marketingInboxConsent = true,
            termsAccepted = true,
            privacyAccepted = true
        )

        assertTrue(result is AuthRepository.RegisterResult.Error)
        assertTrue(password.all { it == '\u0000' })

        val error = result as AuthRepository.RegisterResult.Error
        assertEquals("This email is already registered.", error.message)
    }

    @Test
    fun login_returnsInactiveAdminError_forInactiveAdminAccount() = runBlocking {
        TestSeedData.insertAdmin(
            db = db,
            email = "admin@example.com",
            username = "inactiveAdmin",
            password = "Strong1!",
            isActive = false
        )

        val password = "Strong1!".toCharArray()
        val result = repository.login("admin@example.com", password)

        assertTrue(result is AuthRepository.LoginResult.Error)
        assertTrue(password.all { it == '\u0000' })

        val error = result as AuthRepository.LoginResult.Error
        assertEquals(
            "This admin account is inactive. Please contact an active administrator.",
            error.message
        )
    }
}