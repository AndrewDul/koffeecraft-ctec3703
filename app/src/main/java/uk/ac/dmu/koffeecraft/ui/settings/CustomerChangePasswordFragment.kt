package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import uk.ac.dmu.koffeecraft.util.ui.ValidationUiStyler
import uk.ac.dmu.koffeecraft.util.validation.PasswordRulesValidator

class CustomerChangePasswordFragment : Fragment(R.layout.fragment_customer_change_password) {

    private lateinit var vm: CustomerChangePasswordViewModel

    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText

    private lateinit var tvRuleUpper: TextView
    private lateinit var tvRuleLower: TextView
    private lateinit var tvRuleLength: TextView
    private lateinit var tvRuleSpecial: TextView
    private lateinit var tvRuleNumber: TextView
    private lateinit var btnChangePassword: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CustomerChangePasswordViewModel.Factory(appContainer.customerSettingsRepository)
        )[CustomerChangePasswordViewModel::class.java]

        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword)
        tilNewPassword = view.findViewById(R.id.tilNewPassword)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)

        tvRuleUpper = view.findViewById(R.id.tvRuleUpper)
        tvRuleLower = view.findViewById(R.id.tvRuleLower)
        tvRuleLength = view.findViewById(R.id.tvRuleLength)
        tvRuleSpecial = view.findViewById(R.id.tvRuleSpecial)
        tvRuleNumber = view.findViewById(R.id.tvRuleNumber)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString().orEmpty()
                val rules = PasswordRulesValidator.describe(password)

                ValidationUiStyler.applyPasswordRuleStyle(tvRuleUpper, rules.hasUppercase)
                ValidationUiStyler.applyPasswordRuleStyle(tvRuleLower, rules.hasLowercase)
                ValidationUiStyler.applyPasswordRuleStyle(tvRuleLength, rules.hasMinLength)
                ValidationUiStyler.applyPasswordRuleStyle(tvRuleSpecial, rules.hasSpecial)
                ValidationUiStyler.applyPasswordRuleStyle(tvRuleNumber, rules.hasDigit)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            btnChangePassword.isEnabled = false
            return
        }

        btnChangePassword.setOnClickListener {
            tilCurrentPassword.error = null
            tilNewPassword.error = null

            val currentPassword = etCurrentPassword.text?.toString().orEmpty()
            val newPassword = etNewPassword.text?.toString().orEmpty()

            var hasError = false

            if (currentPassword.isBlank()) {
                tilCurrentPassword.error = "Enter current password"
                hasError = true
            }

            if (!PasswordRulesValidator.isValid(newPassword)) {
                tilNewPassword.error = "New password does not meet all rules"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            vm.changePassword(
                customerId = customerId,
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                btnChangePassword.isEnabled = !state.isSaving
                btnChangePassword.alpha = if (state.isSaving) 0.7f else 1f
                btnChangePassword.text = if (state.isSaving) "Saving..." else "Change password"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is CustomerChangePasswordViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    CustomerChangePasswordViewModel.UiEffect.ClearForm -> {
                        etCurrentPassword.setText("")
                        etNewPassword.setText("")
                    }

                    CustomerChangePasswordViewModel.UiEffect.MarkCurrentPasswordIncorrect -> {
                        tilCurrentPassword.error = "Current password is incorrect"
                    }
                }
            }
        }
    }
}