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
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerPersonalInfoFragment : Fragment(R.layout.fragment_customer_personal_info) {

    private lateinit var tilFirstName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilFirstName = view.findViewById(R.id.tilFirstName)
        tilLastName = view.findViewById(R.id.tilLastName)
        tilEmail = view.findViewById(R.id.tilEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etEmail = view.findViewById(R.id.etEmail)
        etDob = view.findViewById(R.id.etDob)
        tvError = view.findViewById(R.id.tvError)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.btnSaveChanges).setOnClickListener {
            saveChanges()
        }

        loadCustomer()
    }

    private fun loadCustomer() {
        val customerId = SessionManager.currentCustomerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)

            withContext(Dispatchers.Main) {
                if (!isAdded || customer == null) return@withContext

                etFirstName.setText(customer.firstName)
                etLastName.setText(customer.lastName)
                etEmail.setText(customer.email)
                etDob.setText(customer.dateOfBirth.orEmpty())
            }
        }
    }

    private fun saveChanges() {
        val customerId = SessionManager.currentCustomerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        tilFirstName.error = null
        tilLastName.error = null
        tilEmail.error = null
        tvError.visibility = View.GONE

        val firstName = etFirstName.text?.toString()?.trim().orEmpty()
        val lastName = etLastName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty().lowercase()

        var hasError = false

        if (firstName.isBlank()) {
            tilFirstName.error = "Enter first name"
            hasError = true
        }

        if (lastName.isBlank()) {
            tilLastName.error = "Enter last name"
            hasError = true
        }

        if (email.isBlank()) {
            tilEmail.error = "Enter email"
            hasError = true
        } else if (!email.contains("@") || !email.contains(".")) {
            tilEmail.error = "Enter a valid email"
            hasError = true
        }

        if (hasError) return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(customerId)
            if (customer == null) {
                withContext(Dispatchers.Main) {
                    tvError.text = "Customer account could not be found."
                    tvError.visibility = View.VISIBLE
                }
                return@launch
            }

            val existing = db.customerDao().findByEmail(email)
            if (existing != null && existing.customerId != customerId) {
                withContext(Dispatchers.Main) {
                    tilEmail.error = "This email is already registered"
                }
                return@launch
            }

            db.customerDao().update(
                customer.copy(
                    firstName = firstName,
                    lastName = lastName,
                    email = email
                )
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                Toast.makeText(requireContext(), "Personal info updated.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}