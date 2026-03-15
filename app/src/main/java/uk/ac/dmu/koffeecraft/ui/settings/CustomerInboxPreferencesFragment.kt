package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerInboxPreferencesFragment : Fragment(R.layout.fragment_customer_inbox_preferences) {

    private lateinit var switchPromoConsent: SwitchMaterial

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchPromoConsent = view.findViewById(R.id.switchPromoConsent)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.btnSavePreferences).setOnClickListener {
            savePreferences()
        }

        loadPreferences()
    }

    private fun loadPreferences() {
        val customerId = SessionManager.currentCustomerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)

            withContext(Dispatchers.Main) {
                if (!isAdded || customer == null) return@withContext
                switchPromoConsent.isChecked = customer.marketingInboxConsent
            }
        }
    }

    private fun savePreferences() {
        val customerId = SessionManager.currentCustomerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId) ?: return@launch

            db.customerDao().update(
                customer.copy(marketingInboxConsent = switchPromoConsent.isChecked)
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                Toast.makeText(requireContext(), "Inbox preferences updated.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}