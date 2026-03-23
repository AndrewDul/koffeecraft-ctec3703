package uk.ac.dmu.koffeecraft.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.Manifest
import androidx.test.rule.GrantPermissionRule
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.testsupport.UiTestSetup
import androidx.test.espresso.matcher.ViewMatchers.withText



@RunWith(AndroidJUnit4::class)
class AdminFlowEspressoTest {

    @get:Rule
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private lateinit var adminEmail: String
    private lateinit var adminUsername: String
    private val adminPassword = "KoffeeCraft@123"

    @Before
    fun setUp() {
        UiTestSetup.resetAppState(context)

        adminEmail = UiTestSetup.uniqueEmail("adminui")
        adminUsername = UiTestSetup.uniqueAdminUsername("adminui")

        UiTestSetup.ensureAdminExists(
            context = context,
            email = adminEmail,
            username = adminUsername,
            password = adminPassword
        )
    }

    @After
    fun tearDown() {
        UiTestSetup.resetAppState(context)
    }

    @Test
    fun adminLogin_opensAdminShell_andOrdersScreen() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSignIn)).perform(click())

        onView(withId(R.id.etEmail)).perform(replaceText(adminEmail))
        onView(withId(R.id.etPassword)).perform(replaceText(adminPassword))
        closeSoftKeyboard()
        onView(withId(R.id.btnSignIn)).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(1500))

        onView(withId(R.id.adminBottomNav)).check(matches(isDisplayed()))
        onView(withId(R.id.adminOrdersFragment)).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(600))

        onView(withId(R.id.btnFind)).check(matches(isDisplayed()))
        onView(withText("Search & Filters")).check(matches(isDisplayed()))
    }
}