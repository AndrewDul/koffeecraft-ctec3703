package uk.ac.dmu.koffeecraft.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings
import uk.ac.dmu.koffeecraft.data.settings.ThemeSettings

class AdminSettingsFragment : Fragment(R.layout.fragment_admin_settings) {

    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var tvThemeModeStatus: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAdminName = view.findViewById(R.id.tvAdminName)
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail)
        tvThemeModeStatus = view.findViewById(R.id.tvThemeModeStatus)

        val rowCreateAdminAccount = view.findViewById<LinearLayout>(R.id.rowCreateAdminAccount)
        val rowAccountAccessCenter = view.findViewById<LinearLayout>(R.id.rowAccountAccessCenter)
        val rowNotificationsCenter = view.findViewById<LinearLayout>(R.id.rowNotificationsCenter)
        val rowInboxTools = view.findViewById<LinearLayout>(R.id.rowInboxTools)
        val rowSignOut = view.findViewById<LinearLayout>(R.id.rowSignOut)

        val switchSimulation = view.findViewById<SwitchMaterial>(R.id.switchAppSimulation)
        val tvSimulationStatus = view.findViewById<TextView>(R.id.tvSimulationStatus)
        val switchThemeMode = view.findViewById<SwitchMaterial>(R.id.switchThemeMode)

        fun updateSimulationText(enabled: Boolean) {
            tvSimulationStatus.text = if (enabled) {
                getString(R.string.admin_simulation_desc_on)
            } else {
                getString(R.string.admin_simulation_desc_off)
            }
        }

        fun updateThemeText(enabled: Boolean) {
            tvThemeModeStatus.text = if (enabled) {
                getString(R.string.settings_theme_toggle_desc_dark)
            } else {
                getString(R.string.settings_theme_toggle_desc_light)
            }
        }

        val initialSimulationState = SimulationSettings.isEnabled(requireContext())
        switchSimulation.isChecked = initialSimulationState
        updateSimulationText(initialSimulationState)

        switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            SimulationSettings.setEnabled(requireContext(), isChecked)
            updateSimulationText(isChecked)
        }

        val initialThemeState = ThemeSettings.isDarkModeEnabled(requireContext())
        switchThemeMode.isChecked = initialThemeState
        updateThemeText(initialThemeState)

        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            updateThemeText(isChecked)
            ThemeSettings.setDarkModeEnabled(requireContext(), isChecked)
        }

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
            CartManager.clearInMemoryOnly()
            SessionManager.clear()
            RememberedSessionStore.clear(requireContext().applicationContext)
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAdminSummary()
    }

    private fun loadAdminSummary() {
        val adminId = SessionManager.currentAdminId
        if (adminId == null) {
            tvAdminName.text = getString(R.string.admin_settings_fallback_name)
            tvAdminEmail.text = getString(R.string.admin_settings_fallback_email)
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val admin = db.adminDao().getById(adminId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (admin == null) {
                    tvAdminName.text = getString(R.string.admin_settings_fallback_name)
                    tvAdminEmail.text = getString(R.string.admin_settings_fallback_email)
                    Toast.makeText(requireContext(), getString(R.string.admin_settings_profile_missing), Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                tvAdminName.text = admin.fullName
                tvAdminEmail.text = admin.email
            }
        }
    }
}