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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.validation.CustomerPersonalInfoValidator

class CustomerPersonalInfoFragment : Fragment(R.layout.fragment_customer_personal_info) {

    private lateinit var vm: CustomerPersonalInfoViewModel

    private lateinit var tilFirstName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var tvError: TextView
    private lateinit var btnSaveChanges: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CustomerPersonalInfoViewModel.Factory(appContainer.customerSettingsRepository)
        )[CustomerPersonalInfoViewModel::class.java]

        tilFirstName = view.findViewById(R.id.tilFirstName)
        tilLastName = view.findViewById(R.id.tilLastName)
        tilEmail = view.findViewById(R.id.tilEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etEmail = view.findViewById(R.id.etEmail)
        etDob = view.findViewById(R.id.etDob)
        tvError = view.findViewById(R.id.tvError)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvError.text = "You are not logged in as a customer."
            tvError.visibility = View.VISIBLE
            btnSaveChanges.isEnabled = false
            return
        }

        btnSaveChanges.setOnClickListener {
            tilFirstName.error = null
            tilLastName.error = null
            tilEmail.error = null
            tvError.visibility = View.GONE

            val firstName = etFirstName.text?.toString().orEmpty()
            val lastName = etLastName.text?.toString().orEmpty()
            val email = etEmail.text?.toString().orEmpty()

            val validation = CustomerPersonalInfoValidator.validate(
                firstName = firstName,
                lastName = lastName,
                email = email
            )

            tilFirstName.error = validation.firstNameError
            tilLastName.error = validation.lastNameError
            tilEmail.error = validation.emailError

            if (!validation.isValid) return@setOnClickListener

            vm.save(
                customerId = customerId,
                firstName = firstName,
                lastName = lastName,
                email = email
            )
        }

        vm.start(customerId)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                setTextIfDifferent(etFirstName, state.firstName)
                setTextIfDifferent(etLastName, state.lastName)
                setTextIfDifferent(etEmail, state.email)
                setTextIfDifferent(etDob, state.dateOfBirth)

                btnSaveChanges.isEnabled = !state.isSaving
                btnSaveChanges.alpha = if (state.isSaving) 0.7f else 1f
                btnSaveChanges.text = if (state.isSaving) "Saving..." else "Save changes"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is CustomerPersonalInfoViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setTextIfDifferent(
        editText: TextInputEditText,
        value: String
    ) {
        if (editText.text?.toString() != value) {
            editText.setText(value)
        }
    }
}