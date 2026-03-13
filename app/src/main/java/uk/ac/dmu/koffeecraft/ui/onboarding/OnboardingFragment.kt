package uk.ac.dmu.koffeecraft.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private data class IntroPage(
        val icon: String,
        val title: String,
        val description: String
    )

    private val pages = listOf(
        IntroPage(
            icon = "◉",
            title = "Your first beans are waiting",
            description = "You collect beans with completed orders. Save 10 beans to unlock a free crafted coffee."
        ),
        IntroPage(
            icon = "★",
            title = "Rewards grow with every order",
            description = "Keep saving beans for larger rewards and explore them later in the Rewards section."
        ),
        IntroPage(
            icon = "✉",
            title = "Inbox keeps special offers close",
            description = "If you agree to promotional messages, KoffeeCraft can send you offers, free coffee codes and reward surprises in your Inbox."
        )
    )

    private var currentIndex = 0
    private var promoConsentChoice = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = requireArguments().getLong("customerId")

        val tvIcon = view.findViewById<TextView>(R.id.tvIntroIcon)
        val tvTitle = view.findViewById<TextView>(R.id.tvIntroTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvIntroDescription)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)
        val dot1 = view.findViewById<View>(R.id.dot1)
        val dot2 = view.findViewById<View>(R.id.dot2)
        val dot3 = view.findViewById<View>(R.id.dot3)
        val switchPromoOnboarding = view.findViewById<SwitchMaterial>(R.id.switchPromoOnboarding)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)
            val initialConsent = customer?.marketingInboxConsent == true

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                promoConsentChoice = initialConsent
                switchPromoOnboarding.isChecked = initialConsent
                renderPage(
                    tvIcon = tvIcon,
                    tvTitle = tvTitle,
                    tvDescription = tvDescription,
                    btnNext = btnNext,
                    dot1 = dot1,
                    dot2 = dot2,
                    dot3 = dot3,
                    switchPromoOnboarding = switchPromoOnboarding
                )
            }
        }

        switchPromoOnboarding.setOnCheckedChangeListener { _, isChecked ->
            promoConsentChoice = isChecked
        }

        btnNext.setOnClickListener {
            if (currentIndex < pages.lastIndex) {
                currentIndex++
                renderPage(
                    tvIcon = tvIcon,
                    tvTitle = tvTitle,
                    tvDescription = tvDescription,
                    btnNext = btnNext,
                    dot1 = dot1,
                    dot2 = dot2,
                    dot3 = dot3,
                    switchPromoOnboarding = switchPromoOnboarding
                )
            } else {
                finishOnboarding(customerId)
            }
        }
    }

    private fun renderPage(
        tvIcon: TextView,
        tvTitle: TextView,
        tvDescription: TextView,
        btnNext: MaterialButton,
        dot1: View,
        dot2: View,
        dot3: View,
        switchPromoOnboarding: SwitchMaterial
    ) {
        val page = pages[currentIndex]
        tvIcon.text = page.icon
        tvTitle.text = page.title
        tvDescription.text = page.description

        dot1.setBackgroundResource(if (currentIndex == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        dot2.setBackgroundResource(if (currentIndex == 1) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        dot3.setBackgroundResource(if (currentIndex == 2) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)

        btnNext.text = if (currentIndex == pages.lastIndex) "Finish" else "Next"

        if (currentIndex == pages.lastIndex) {
            switchPromoOnboarding.visibility = View.VISIBLE
            switchPromoOnboarding.text = "I agree to receive promotional messages in my Inbox"
            switchPromoOnboarding.isChecked = promoConsentChoice
        } else {
            switchPromoOnboarding.visibility = View.GONE
        }
    }

    private fun finishOnboarding(customerId: Long) {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)

            if (customer != null) {
                db.inboxMessageDao().insertAll(
                    listOf(
                        InboxMessage(
                            recipientCustomerId = customerId,
                            title = "Welcome to KoffeeCraft",
                            body = "Welcome to KoffeeCraft. Your account is ready, your rewards journey has started, and your Inbox will keep your coffee moments close.",
                            deliveryType = "WELCOME"
                        )
                    )
                )

                val finalConsent = customer.marketingInboxConsent || promoConsentChoice

                if (finalConsent) {
                    db.customerDao().update(
                        customer.copy(
                            marketingInboxConsent = true,
                            beansBalance = customer.beansBalance + 5
                        )
                    )

                    db.notificationDao().insert(
                        AppNotification(
                            recipientRole = "CUSTOMER",
                            recipientCustomerId = customerId,
                            title = "You received 5 beans",
                            message = "Thanks for enabling promotional messages. Your first 5 beans have been added to your account.",
                            notificationType = "WELCOME_BEANS",
                            orderId = null,
                            orderCreatedAt = null,
                            orderStatus = null,
                            isRead = false
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                findNavController().navigate(
                    R.id.customerHomeFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.onboardingFragment) {
                            inclusive = true
                        }
                    }
                )
            }
        }
    }
}