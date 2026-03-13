package uk.ac.dmu.koffeecraft.ui.register

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var vm: RegisterViewModel
    private var hasNavigatedAfterSuccess = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val repo = AuthRepository(db)
        vm = ViewModelProvider(this, RegisterViewModelFactory(repo))[RegisterViewModel::class.java]

        val tilFirst = view.findViewById<TextInputLayout>(R.id.tilFirstName)
        val tilLast = view.findViewById<TextInputLayout>(R.id.tilLastName)
        val tilDob = view.findViewById<TextInputLayout>(R.id.tilDob)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)

        val etFirst = view.findViewById<TextInputEditText>(R.id.etFirstName)
        val etLast = view.findViewById<TextInputEditText>(R.id.etLastName)
        val etDob = view.findViewById<TextInputEditText>(R.id.etDob)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)

        val spCountry = view.findViewById<Spinner>(R.id.spCountry)

        val switchPromoConsent = view.findViewById<SwitchMaterial>(R.id.switchPromoConsent)
        val switchTerms = view.findViewById<SwitchMaterial>(R.id.switchTerms)
        val switchPrivacy = view.findViewById<SwitchMaterial>(R.id.switchPrivacy)

        val tvTermsLink = view.findViewById<TextView>(R.id.tvTermsLink)
        val tvPrivacyLink = view.findViewById<TextView>(R.id.tvPrivacyLink)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnBackToLogin = view.findViewById<TextView>(R.id.btnBackToLogin)

        val tvRuleUpper = view.findViewById<TextView>(R.id.tvRuleUpper)
        val tvRuleLower = view.findViewById<TextView>(R.id.tvRuleLower)
        val tvRuleLength = view.findViewById<TextView>(R.id.tvRuleLength)
        val tvRuleSpecial = view.findViewById<TextView>(R.id.tvRuleSpecial)
        val tvRuleNumber = view.findViewById<TextView>(R.id.tvRuleNumber)

        val countries = listOf(
            "United Kingdom",
            "Poland",
            "Ireland",
            "Germany",
            "France",
            "Italy",
            "Spain",
            "India",
            "Pakistan",
            "Nigeria",
            "Other"
        )

        spCountry.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            countries
        )

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString().orEmpty()

                updateRuleTile(tvRuleUpper, password.any { it.isUpperCase() })
                updateRuleTile(tvRuleLower, password.any { it.isLowerCase() })
                updateRuleTile(tvRuleLength, password.length >= 8)
                updateRuleTile(tvRuleSpecial, password.any { !it.isLetterOrDigit() })
                updateRuleTile(tvRuleNumber, password.any { it.isDigit() })
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        tvTermsLink.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Terms of Use")
                .setMessage(
                    "By using KoffeeCraft, I agree to use the platform responsibly. " +
                            "Reward abuse, misuse of offers, or behaviour against the rules may result in account removal without warning."
                )
                .setPositiveButton("Close", null)
                .show()
        }

        tvPrivacyLink.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacy Statement")
                .setMessage(
                    "KoffeeCraft stores my account details, order activity, rewards progress and communication preferences " +
                            "to provide app features, improve the experience and manage rewards, messages and account security."
                )
                .setPositiveButton("Close", null)
                .show()
        }

        view.findViewById<View>(R.id.btnRegister).setOnClickListener {
            tilFirst.error = null
            tilLast.error = null
            tilDob.error = null
            tilEmail.error = null
            tilPassword.error = null
            tvError.visibility = View.GONE

            val firstName = etFirst.text?.toString()?.trim().orEmpty()
            val lastName = etLast.text?.toString()?.trim().orEmpty()
            val country = spCountry.selectedItem?.toString().orEmpty()
            val dateOfBirth = etDob.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString().orEmpty()

            var hasError = false

            if (firstName.isBlank()) {
                tilFirst.error = "Enter first name"
                hasError = true
            }

            if (lastName.isBlank()) {
                tilLast.error = "Enter last name"
                hasError = true
            }

            if (dateOfBirth.isBlank()) {
                tilDob.error = "Enter date of birth"
                hasError = true
            }

            if (email.isBlank()) {
                tilEmail.error = "Enter email"
                hasError = true
            }

            if (!isPasswordValid(password)) {
                tilPassword.error = "Password does not meet all rules"
                hasError = true
            }

            if (!switchTerms.isChecked) {
                tvError.text = "You must accept the Terms of Use."
                tvError.visibility = View.VISIBLE
                hasError = true
            }

            if (!switchPrivacy.isChecked) {
                tvError.text = "You must accept the Privacy Statement."
                tvError.visibility = View.VISIBLE
                hasError = true
            }

            if (hasError) return@setOnClickListener

            vm.register(
                firstName = firstName,
                lastName = lastName,
                country = country,
                dateOfBirth = dateOfBirth,
                email = email,
                password = password,
                marketingInboxConsent = switchPromoConsent.isChecked,
                termsAccepted = switchTerms.isChecked,
                privacyAccepted = switchPrivacy.isChecked
            )
        }

        btnBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                tvError.text = state.error ?: ""
                tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE

                if (state.success && state.registeredCustomerId != null && !hasNavigatedAfterSuccess) {
                    hasNavigatedAfterSuccess = true
                    SessionManager.setCustomer(state.registeredCustomerId)

                    findNavController().navigate(
                        R.id.onboardingFragment,
                        bundleOf("customerId" to state.registeredCustomerId)
                    )
                }
            }
        }
    }

    private fun updateRuleTile(view: TextView, isValid: Boolean) {
        if (isValid) {
            view.setBackgroundResource(R.drawable.bg_rule_valid)
            view.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            view.setBackgroundResource(R.drawable.bg_rule_invalid)
            view.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.length >= 8 &&
                password.any { !it.isLetterOrDigit() } &&
                password.any { it.isDigit() }
    }
}