package uk.ac.dmu.koffeecraft.ui.settings

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
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerInboxPreferencesFragment : Fragment(R.layout.fragment_customer_inbox_preferences) {

    private lateinit var vm: CustomerInboxPreferencesViewModel
    private lateinit var switchPromoConsent: SwitchMaterial
    private lateinit var btnSavePreferences: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CustomerInboxPreferencesViewModel.Factory(appContainer.customerSettingsRepository)
        )[CustomerInboxPreferencesViewModel::class.java]

        switchPromoConsent = view.findViewById(R.id.switchPromoConsent)
        btnSavePreferences = view.findViewById(R.id.btnSavePreferences)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            btnSavePreferences.isEnabled = false
            return
        }

        switchPromoConsent.setOnCheckedChangeListener { _, isChecked ->
            vm.setMarketingInboxConsent(isChecked)
        }

        btnSavePreferences.setOnClickListener {
            vm.save(customerId)
        }

        vm.start(customerId)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                if (switchPromoConsent.isChecked != state.marketingInboxConsent) {
                    switchPromoConsent.isChecked = state.marketingInboxConsent
                }

                btnSavePreferences.isEnabled = !state.isSaving
                btnSavePreferences.alpha = if (state.isSaving) 0.7f else 1f
                btnSavePreferences.text = if (state.isSaving) "Saving..." else "Save preferences"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is CustomerInboxPreferencesViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}