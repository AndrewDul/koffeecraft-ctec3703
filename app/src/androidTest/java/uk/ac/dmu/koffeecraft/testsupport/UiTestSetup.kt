package uk.ac.dmu.koffeecraft.testsupport

import android.content.Context
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import java.util.Locale
import kotlinx.coroutines.runBlocking

object UiTestSetup {

    fun resetAppState(context: Context) {
        RememberedSessionStore.clear(context)
        SessionManager.clear()
        context.appContainer.sessionRepository.clear()
        context.appContainer.cartRepository.clearInMemoryOnly()
    }

    fun uniqueEmail(prefix: String): String {
        return "${prefix.lowercase(Locale.UK)}.${System.currentTimeMillis()}@example.com"
    }

    fun uniqueAdminUsername(prefix: String = "admin"): String {
        return "${prefix.lowercase(Locale.UK)}_${System.currentTimeMillis()}"
    }

    fun ensureCustomerExists(
        context: Context,
        email: String,
        password: String = "Strong1!"
    ): Long = runBlocking {
        val db = context.appContainer.database
        val normalisedEmail = email.trim().lowercase(Locale.UK)

        db.customerDao().findByEmail(normalisedEmail)?.customerId
            ?: TestSeedData.insertCustomer(
                db = db,
                email = normalisedEmail,
                password = password
            )
    }

    fun ensureAdminExists(
        context: Context,
        email: String = "admin@koffeecraft.local",
        username: String = "admin",
        password: String = "KoffeeCraft@123"
    ): Long = runBlocking {
        val db = context.appContainer.database
        val normalisedEmail = email.trim().lowercase(Locale.UK)
        val normalisedUsername = username.trim()

        val existingByEmail = db.adminDao().findByEmail(normalisedEmail)
        if (existingByEmail != null) return@runBlocking existingByEmail.adminId

        val existingByUsername = db.adminDao().findByUsername(normalisedUsername)
        if (existingByUsername != null) return@runBlocking existingByUsername.adminId

        TestSeedData.insertAdmin(
            db = db,
            email = normalisedEmail,
            username = normalisedUsername,
            password = password
        )
    }

    fun hasTextInputLayoutErrorText(expectedError: String): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("TextInputLayout error text: $expectedError")
            }

            override fun matchesSafely(view: View): Boolean {
                return view is TextInputLayout &&
                        view.error?.toString() == expectedError
            }
        }
    }

    fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "Wait for $millis milliseconds"

            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
}