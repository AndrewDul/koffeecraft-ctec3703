package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerSettingsFragment : Fragment(R.layout.fragment_customer_settings) {

    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerEmail: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCustomerName = view.findViewById(R.id.tvCustomerName)
        tvCustomerEmail = view.findViewById(R.id.tvCustomerEmail)

        view.findViewById<View>(R.id.rowPersonalInfo).setOnClickListener {
            findNavController().navigate(R.id.customerPersonalInfoFragment)
        }

        view.findViewById<View>(R.id.rowChangePassword).setOnClickListener {
            findNavController().navigate(R.id.customerChangePasswordFragment)
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
            SessionManager.clear()
            CartManager.clear()
            findNavController().navigate(R.id.action_global_logout)
        }
    }

    override fun onResume() {
        super.onResume()
        loadCustomerSummary()
    }

    private fun loadCustomerSummary() {
        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (customer == null) {
                    tvCustomerName.text = "Customer"
                    tvCustomerEmail.text = "No email available"
                    return@withContext
                }

                tvCustomerName.text = "${customer.firstName} ${customer.lastName}"
                tvCustomerEmail.text = customer.email
            }
        }
    }
}