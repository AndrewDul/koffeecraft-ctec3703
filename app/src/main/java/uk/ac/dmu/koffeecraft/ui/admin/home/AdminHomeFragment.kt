package uk.ac.dmu.koffeecraft.ui.admin.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings

class AdminHomeFragment : Fragment(R.layout.fragment_admin_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val simulationSwitch = view.findViewById<SwitchMaterial>(R.id.switchSimulation)

        // I load the current value and show it in the switch.
        simulationSwitch.isChecked = SimulationSettings.isEnabled(requireContext())

        // I save the admin's choice whenever the switch changes.
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            SimulationSettings.setEnabled(requireContext(), isChecked)
        }
    }
}