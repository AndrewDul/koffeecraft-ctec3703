package uk.ac.dmu.koffeecraft.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings

class AdminSettingsFragment : Fragment(R.layout.fragment_admin_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardManageAccounts = view.findViewById<MaterialCardView>(R.id.cardManageAccounts)
        val cardNotifications = view.findViewById<MaterialCardView>(R.id.cardNotificationsShortcut)
        val cardInbox = view.findViewById<MaterialCardView>(R.id.cardInboxShortcut)
        val cardSignOut = view.findViewById<MaterialCardView>(R.id.cardSignOut)
        val switchSimulation = view.findViewById<SwitchMaterial>(R.id.switchAppSimulation)
        val tvSimulationStatus = view.findViewById<TextView>(R.id.tvSimulationStatus)

        fun updateSimulationText(enabled: Boolean) {
            tvSimulationStatus.text = if (enabled) {
                "Simulation mode is ON"
            } else {
                "Simulation mode is OFF"
            }
        }

        val initialState = SimulationSettings.isEnabled(requireContext())
        switchSimulation.isChecked = initialState
        updateSimulationText(initialState)

        switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            SimulationSettings.setEnabled(requireContext(), isChecked)
            updateSimulationText(isChecked)
        }

        cardManageAccounts.setOnClickListener {
            findNavController().navigate(R.id.adminManageCustomerAccountsFragment)
        }

        cardNotifications.setOnClickListener {
            findNavController().navigate(R.id.adminNotificationsFragment)
        }

        cardInbox.setOnClickListener {
            findNavController().navigate(R.id.adminInboxFragment)
        }

        cardSignOut.setOnClickListener {
            SessionManager.clear()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }
}