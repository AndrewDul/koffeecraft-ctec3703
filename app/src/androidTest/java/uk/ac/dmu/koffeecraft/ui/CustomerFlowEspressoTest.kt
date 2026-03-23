package uk.ac.dmu.koffeecraft.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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

@RunWith(AndroidJUnit4::class)
class CustomerFlowEspressoTest {

    @get:Rule
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        UiTestSetup.resetAppState(context)
    }

    @After
    fun tearDown() {
        UiTestSetup.resetAppState(context)
    }

    @Test
    fun welcomeNavigation_opensLoginAndRegisterScreens() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSignIn)).perform(click())
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))

        pressBack()

        onView(withId(R.id.btnRegisterNow)).perform(click())
        onView(withId(R.id.etFirstName)).check(matches(isDisplayed()))
        onView(withId(R.id.etLastName)).check(matches(isDisplayed()))
        onView(withId(R.id.etDob)).check(matches(isDisplayed()))
    }

    @Test
    fun registerValidation_showsErrors_whenRequiredDataIsMissing() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnRegisterNow)).perform(click())
        onView(withId(R.id.btnRegister)).perform(scrollTo(), click())

        onView(withId(R.id.tilFirstName))
            .check(matches(UiTestSetup.hasTextInputLayoutErrorText("Enter first name")))
        onView(withId(R.id.tilLastName))
            .check(matches(UiTestSetup.hasTextInputLayoutErrorText("Enter last name")))
        onView(withId(R.id.tilDob))
            .check(matches(UiTestSetup.hasTextInputLayoutErrorText("Enter date of birth")))
        onView(withId(R.id.tilEmail))
            .check(matches(UiTestSetup.hasTextInputLayoutErrorText("Enter email")))
        onView(withId(R.id.tilPassword))
            .check(matches(UiTestSetup.hasTextInputLayoutErrorText("Password does not meet all rules")))

        onView(withId(R.id.tvError))
            .check(matches(withText("You must accept the Privacy Statement.")))
    }

    @Test
    fun registerSuccess_opensOnboarding() {
        val email = UiTestSetup.uniqueEmail("register")

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnRegisterNow)).perform(click())

        onView(withId(R.id.etFirstName)).perform(replaceText("Andrew"))
        onView(withId(R.id.etLastName)).perform(replaceText("Dul"))
        onView(withId(R.id.etDob)).perform(replaceText("2000-12-31"))
        onView(withId(R.id.etEmail)).perform(replaceText(email))
        onView(withId(R.id.etPassword)).perform(replaceText("Strong1!"))
        closeSoftKeyboard()

        onView(withId(R.id.switchTerms)).perform(scrollTo(), click())
        onView(withId(R.id.switchPrivacy)).perform(scrollTo(), click())
        onView(withId(R.id.btnRegister)).perform(scrollTo(), click())

        onView(withId(R.id.tvIntroTitle)).check(matches(isDisplayed()))
        onView(withText("Your first beans are waiting")).check(matches(isDisplayed()))
        onView(withId(R.id.btnNext)).check(matches(isDisplayed()))
    }

    @Test
    fun seededCustomer_canLogin_openMenu_andOpenCart() {
        val email = UiTestSetup.uniqueEmail("customerlogin")
        UiTestSetup.ensureCustomerExists(
            context = context,
            email = email,
            password = "Strong1!"
        )

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSignIn)).perform(click())

        onView(withId(R.id.etEmail)).perform(replaceText(email))
        onView(withId(R.id.etPassword)).perform(replaceText("Strong1!"))
        closeSoftKeyboard()
        onView(withId(R.id.btnSignIn)).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(1200))

        onView(withId(R.id.customerBottomNav)).check(matches(isDisplayed()))
        onView(withText("Beans balance")).check(matches(isDisplayed()))
        onView(withId(R.id.customerTopBar)).check(matches(isDisplayed()))

        onView(withId(R.id.menuFragment)).perform(click())
        onView(withText("Choose your next crafted favourite.")).check(matches(isDisplayed()))
        onView(withId(R.id.rvProducts)).check(matches(isDisplayed()))

        onView(withId(R.id.btnCustomerCart)).perform(click())
        onView(withText("Your cart is empty.")).check(matches(isDisplayed()))
        onView(withId(R.id.btnCheckout)).check(matches(isDisplayed()))
    }
}