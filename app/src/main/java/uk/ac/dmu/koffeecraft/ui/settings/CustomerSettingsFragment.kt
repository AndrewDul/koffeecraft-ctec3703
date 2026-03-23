package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
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

class CustomerSettingsFragment : Fragment(R.layout.fragment_customer_settings) {

    private lateinit var viewModel: CustomerSettingsViewModel

    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerEmail: TextView
    private lateinit var switchThemeMode: SwitchMaterial
    private lateinit var tvThemeModeStatus: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            CustomerSettingsViewModel.Factory(
                repository = appContainer.customerSettingsRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[CustomerSettingsViewModel::class.java]

        tvCustomerName = view.findViewById(R.id.tvCustomerName)
        tvCustomerEmail = view.findViewById(R.id.tvCustomerEmail)
        switchThemeMode = view.findViewById(R.id.switchThemeMode)
        tvThemeModeStatus = view.findViewById(R.id.tvThemeModeStatus)

        view.findViewById<View>(R.id.rowPersonalInfo).setOnClickListener {
            findNavController().navigate(R.id.customerPersonalInfoFragment)
        }

        view.findViewById<View>(R.id.rowChangePassword).setOnClickListener {
            findNavController().navigate(R.id.customerChangePasswordFragment)
        }

        view.findViewById<View>(R.id.rowPaymentMethods).setOnClickListener {
            findNavController().navigate(R.id.customerPaymentMethodsFragment)
        }

        view.findViewById<View>(R.id.rowInboxPreferences).setOnClickListener {
            findNavController().navigate(R.id.customerInboxPreferencesFragment)
        }

        view.findViewById<View>(R.id.rowHelp).setOnClickListener {
            findNavController().navigate(
                R.id.settingsInfoPageFragment,
                bundleOf("pageType" to "help")
            )
        }

        view.findViewById<View>(R.id.rowTerms).setOnClickListener {
            findNavController().navigate(
                R.id.settingsInfoPageFragment,
                bundleOf("pageType" to "terms")
            )
        }

        view.findViewById<View>(R.id.rowPrivacy).setOnClickListener {
            findNavController().navigate(
                R.id.settingsInfoPageFragment,
                bundleOf("pageType" to "privacy")
            )
        }

        view.findViewById<View>(R.id.rowDeleteAccount).setOnClickListener {
            findNavController().navigate(R.id.customerDeleteAccountFragment)
        }

        view.findViewById<MaterialButton>(R.id.btnSignOut).setOnClickListener {
            viewModel.signOut()
        }

        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkModeEnabled(isChecked)
        }

        observeState()
        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                bindState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is CustomerSettingsViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    CustomerSettingsViewModel.UiEffect.NavigateToSignedOut -> {
                        findNavController().navigate(R.id.action_global_logout)
                    }
                }
            }
        }
    }

    private fun bindState(state: CustomerSettingsUiState) {
        tvCustomerName.text = if (state.customerName.isBlank()) {
            getString(R.string.settings_customer_fallback_name)
        } else {
            state.customerName
        }

        tvCustomerEmail.text = if (state.customerEmail.isBlank()) {
            getString(R.string.settings_customer_fallback_email)
        } else {
            state.customerEmail
        }

        switchThemeMode.setOnCheckedChangeListener(null)
        switchThemeMode.isChecked = state.darkModeEnabled
        updateThemeText(state.darkModeEnabled)

        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkModeEnabled(isChecked)
        }
    }

    private fun updateThemeText(isDarkModeEnabled: Boolean) {
        tvThemeModeStatus.text = if (isDarkModeEnabled) {
            getString(R.string.settings_theme_toggle_desc_dark)
        } else {
            getString(R.string.settings_theme_toggle_desc_light)
        }
    }
}