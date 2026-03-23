package uk.ac.dmu.koffeecraft.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

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

    private lateinit var viewModel: OnboardingViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = requireArguments().getLong("customerId")

        viewModel = ViewModelProvider(
            this,
            OnboardingViewModel.Factory(appContainer.onboardingRepository)
        )[OnboardingViewModel::class.java]

        val tvIcon = view.findViewById<TextView>(R.id.tvIntroIcon)
        val tvTitle = view.findViewById<TextView>(R.id.tvIntroTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvIntroDescription)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)
        val dot1 = view.findViewById<View>(R.id.dot1)
        val dot2 = view.findViewById<View>(R.id.dot2)
        val dot3 = view.findViewById<View>(R.id.dot3)
        val switchPromoOnboarding = view.findViewById<SwitchMaterial>(R.id.switchPromoOnboarding)

        switchPromoOnboarding.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPromoConsentChoice(isChecked)
        }

        btnNext.setOnClickListener {
            val currentIndex = viewModel.state.value.currentIndex
            if (currentIndex < pages.lastIndex) {
                viewModel.nextPage(pages.lastIndex)
            } else {
                viewModel.finishOnboarding(customerId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                val page = pages[state.currentIndex]

                tvIcon.text = page.icon
                tvTitle.text = page.title
                tvDescription.text = page.description

                dot1.setBackgroundResource(
                    if (state.currentIndex == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
                dot2.setBackgroundResource(
                    if (state.currentIndex == 1) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
                dot3.setBackgroundResource(
                    if (state.currentIndex == 2) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )

                btnNext.text = if (state.currentIndex == pages.lastIndex) "Finish" else "Next"

                if (state.currentIndex == pages.lastIndex) {
                    switchPromoOnboarding.visibility = View.VISIBLE
                    switchPromoOnboarding.text = "I agree to receive promotional messages in my Inbox"
                    if (switchPromoOnboarding.isChecked != state.promoConsentChoice) {
                        switchPromoOnboarding.isChecked = state.promoConsentChoice
                    }
                } else {
                    switchPromoOnboarding.visibility = View.GONE
                }

                btnNext.isEnabled = !state.isLoading
                btnNext.alpha = if (state.isLoading) 0.7f else 1f
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is OnboardingViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    OnboardingViewModel.UiEffect.NavigateToHome -> {
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

        viewModel.start(customerId)
    }
}