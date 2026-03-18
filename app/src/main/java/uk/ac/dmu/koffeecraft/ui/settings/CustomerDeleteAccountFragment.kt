package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.CustomerAccountCleanupRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

class CustomerDeleteAccountFragment : Fragment(R.layout.fragment_customer_delete_account) {

    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        tvError = view.findViewById(R.id.tvError)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.btnDeleteAccount).setOnClickListener {
            deleteAccount()
        }

        view.findViewById<MaterialButton>(R.id.btnBackToSettings).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun deleteAccount() {
        val customerId = SessionManager.currentCustomerId ?: return
        val appContext = requireContext().applicationContext
        val db = KoffeeCraftDatabase.getInstance(appContext)
        val cleanupRepository = CustomerAccountCleanupRepository(appContext)

        tilCurrentPassword.error = null
        tvError.visibility = View.GONE

        val currentPassword = etCurrentPassword.text?.toString().orEmpty()

        if (currentPassword.isBlank()) {
            tilCurrentPassword.error = "Enter current password"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)
            if (customer == null) {
                withContext(Dispatchers.Main) {
                    tvError.text = "Customer account could not be found."
                    tvError.visibility = View.VISIBLE
                }
                return@launch
            }

            val passwordChars = currentPassword.toCharArray()
            val valid = PasswordHasher.verify(
                passwordChars,
                customer.passwordSalt,
                customer.passwordHash
            )
            passwordChars.fill('\u0000')

            if (!valid) {
                withContext(Dispatchers.Main) {
                    tilCurrentPassword.error = "Current password is incorrect"
                }
                return@launch
            }

            cleanupRepository.deleteCustomerCompletely(customerId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                Toast.makeText(requireContext(), "Account deleted permanently.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_logout)
            }
        }
    }
}