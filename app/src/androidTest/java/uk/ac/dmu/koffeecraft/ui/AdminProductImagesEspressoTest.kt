package uk.ac.dmu.koffeecraft.ui

import android.Manifest
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.testsupport.UiTestSetup

@RunWith(AndroidJUnit4::class)
class AdminProductImagesEspressoTest {

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

        adminEmail = UiTestSetup.uniqueEmail("adminimage")
        adminUsername = UiTestSetup.uniqueAdminUsername("adminimage")

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
    fun adminCanChooseImageFromAppLibrary_andSavedSelectionPersistsInEditDialog() {
        val productName = "A Image Test ${System.currentTimeMillis()}"

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSignIn)).perform(click())

        onView(withId(R.id.etEmail)).perform(replaceText(adminEmail))
        onView(withId(R.id.etPassword)).perform(replaceText(adminPassword))
        closeSoftKeyboard()
        onView(withId(R.id.btnSignIn)).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(1500))

        onView(withId(R.id.adminBottomNav)).check(matches(isDisplayed()))
        onView(withId(R.id.adminMenuFragment)).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(700))

        onView(withId(R.id.fabAddProduct)).perform(click())
        onView(isRoot()).perform(UiTestSetup.waitFor(400))

        onView(withId(R.id.etName)).perform(scrollTo(), click(), replaceText(productName))
        closeSoftKeyboard()

        onView(withId(R.id.etDescription)).perform(
            scrollTo(),
            click(),
            replaceText("Premium product image UI smoke test")
        )
        closeSoftKeyboard()

        onView(withId(R.id.etPrice)).perform(scrollTo(), click(), replaceText("4.90"))
        closeSoftKeyboard()

        onView(withId(R.id.btnChooseAppLibrary)).perform(scrollTo(), click())
        onView(isRoot()).perform(UiTestSetup.waitFor(250))
        onView(withText("Espresso")).perform(click())

        onView(withId(R.id.tvImageSelectionMeta))
            .check(matches(withText(containsString("App library"))))
        onView(withId(R.id.tvImageSelectionMeta))
            .check(matches(withText(containsString("Espresso"))))
        onView(withId(R.id.ivProductPreview))
            .check(matches(isDisplayed()))

        onView(withText("Add")).perform(click())

        onView(isRoot()).perform(UiTestSetup.waitFor(1000))

        onView(withId(R.id.rvAdminProducts)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withText(productName))
            )
        )

        onView(withId(R.id.rvAdminProducts)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(productName)),
                clickChildViewWithId(R.id.cardRoot)
            )
        )

        onView(isRoot()).perform(UiTestSetup.waitFor(450))

        onView(withId(R.id.rvAdminProducts)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(productName)),
                clickChildViewWithId(R.id.btnEdit)
            )
        )

        onView(isRoot()).perform(UiTestSetup.waitFor(300))

        onView(withId(R.id.tvImageSelectionMeta))
            .check(matches(withText(containsString("App library"))))
        onView(withId(R.id.tvImageSelectionMeta))
            .check(matches(withText(containsString("Espresso"))))
        onView(withId(R.id.ivProductPreview))
            .check(matches(isDisplayed()))
    }

    private fun clickChildViewWithId(viewId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()

            override fun getDescription(): String = "Click child view with id $viewId"

            override fun perform(uiController: UiController, view: View) {
                val child = view.findViewById<View>(viewId)
                    ?: throw NoMatchingViewException.Builder()
                        .withRootView(view)
                        .withViewMatcher(allOf(withId(viewId)))
                        .build()

                child.performClick()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}