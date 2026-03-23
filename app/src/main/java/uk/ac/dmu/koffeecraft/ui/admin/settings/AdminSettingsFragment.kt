package uk.ac.dmu.koffeecraft.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class AdminSettingsFragment : Fragment(R.layout.fragment_admin_settings) {

    private lateinit var viewModel: AdminSettingsViewModel

    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var tvThemeModeStatus: TextView
    private lateinit var tvSimulationStatus: TextView
    private lateinit var switchSimulation: SwitchMaterial
    private lateinit var switchThemeMode: SwitchMaterial

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminSettingsViewModel.Factory(
                repository = appContainer.adminSettingsRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[AdminSettingsViewModel::class.java]

        tvAdminName = view.findViewById(R.id.tvAdminName)
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail)
        tvThemeModeStatus = view.findViewById(R.id.tvThemeModeStatus)
        tvSimulationStatus = view.findViewById(R.id.tvSimulationStatus)
        switchSimulation = view.findViewById(R.id.switchAppSimulation)
        switchThemeMode = view.findViewById(R.id.switchThemeMode)

        val rowCreateAdminAccount = view.findViewById<LinearLayout>(R.id.rowCreateAdminAccount)
        val rowAccountAccessCenter = view.findViewById<LinearLayout>(R.id.rowAccountAccessCenter)
        val rowNotificationsCenter = view.findViewById<LinearLayout>(R.id.rowNotificationsCenter)
        val rowInboxTools = view.findViewById<LinearLayout>(R.id.rowInboxTools)
        val rowSignOut = view.findViewById<LinearLayout>(R.id.rowSignOut)

        rowCreateAdminAccount.setOnClickListener {
            findNavController().navigate(R.id.adminCreateAccountFragment)
        }

        rowAccountAccessCenter.setOnClickListener {
            findNavController().navigate(R.id.adminManageCustomerAccountsFragment)
        }

        rowNotificationsCenter.setOnClickListener {
            findNavController().navigate(R.id.adminNotificationsFragment)
        }

        rowInboxTools.setOnClickListener {
            findNavController().navigate(R.id.adminInboxFragment)
        }

        rowSignOut.setOnClickListener {
            viewModel.signOut()
        }

        switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSimulationEnabled(isChecked)
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
                    is AdminSettingsViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    AdminSettingsViewModel.UiEffect.NavigateToSignedOut -> {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    private fun bindState(state: AdminSettingsUiState) {
        tvAdminName.text = if (state.adminName.isBlank()) {
            getString(R.string.admin_settings_fallback_name)
        } else {
            state.adminName
        }

        tvAdminEmail.text = if (state.adminEmail.isBlank()) {
            getString(R.string.admin_settings_fallback_email)
        } else {
            state.adminEmail
        }

        switchSimulation.setOnCheckedChangeListener(null)
        switchThemeMode.setOnCheckedChangeListener(null)

        switchSimulation.isChecked = state.simulationEnabled
        switchThemeMode.isChecked = state.darkModeEnabled

        tvSimulationStatus.text = if (state.simulationEnabled) {
            getString(R.string.admin_simulation_desc_on)
        } else {
            getString(R.string.admin_simulation_desc_off)
        }

        tvThemeModeStatus.text = if (state.darkModeEnabled) {
            getString(R.string.settings_theme_toggle_desc_dark)
        } else {
            getString(R.string.settings_theme_toggle_desc_light)
        }

        switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSimulationEnabled(isChecked)
        }

        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkModeEnabled(isChecked)
        }
    }
}