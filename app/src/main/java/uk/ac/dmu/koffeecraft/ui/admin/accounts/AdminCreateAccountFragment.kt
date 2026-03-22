package uk.ac.dmu.koffeecraft.ui.admin.accounts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher
import uk.ac.dmu.koffeecraft.util.ui.ValidationUiStyler
import uk.ac.dmu.koffeecraft.util.validation.EmailValidator
import uk.ac.dmu.koffeecraft.util.validation.PasswordRulesValidator
import uk.ac.dmu.koffeecraft.util.validation.PhoneValidator
import uk.ac.dmu.koffeecraft.util.validation.UsernameValidator

class AdminCreateAccountFragment : Fragment(R.layout.fragment_admin_create_account) {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var switchIsActive: SwitchMaterial
    private lateinit var tvAccountStatusValue: TextView
    private lateinit var tvError: TextView
    private lateinit var btnCreateAdmin: MaterialButton

    private lateinit var tvRuleUpper: TextView
    private lateinit var tvRuleLower: TextView
    private lateinit var tvRuleLength: TextView
    private lateinit var tvRuleSpecial: TextView
    private lateinit var tvRuleNumber: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilFullName = view.findViewById(R.id.tilFullName)
        tilEmail = view.findViewById(R.id.tilEmail)
        tilPhone = view.findViewById(R.id.tilPhone)
        tilUsername = view.findViewById(R.id.tilUsername)
        tilPassword = view.findViewById(R.id.tilPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)

        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etPhone = view.findViewById(R.id.etPhone)
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)

        switchIsActive = view.findViewById(R.id.switchIsActive)
        tvAccountStatusValue = view.findViewById(R.id.tvAccountStatusValue)
        tvError = view.findViewById(R.id.tvError)
        btnCreateAdmin = view.findViewById(R.id.btnCreateAdmin)

        tvRuleUpper = view.findViewById(R.id.tvRuleUpper)
        tvRuleLower = view.findViewById(R.id.tvRuleLower)
        tvRuleLength = view.findViewById(R.id.tvRuleLength)
        tvRuleSpecial = view.findViewById(R.id.tvRuleSpecial)
        tvRuleNumber = view.findViewById(R.id.tvRuleNumber)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        switchIsActive.setOnCheckedChangeListener { _, isChecked ->
            tvAccountStatusValue.text = if (isChecked) "Active on creation" else "Inactive on creation"
        }
        tvAccountStatusValue.text = if (switchIsActive.isChecked) "Active on creation" else "Inactive on creation"

        val simpleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updatePasswordRules(etPassword.text?.toString().orEmpty())
                updateCreateButtonState()
            }
        }

        etFullName.addTextChangedListener(simpleWatcher)
        etEmail.addTextChangedListener(simpleWatcher)
        etPhone.addTextChangedListener(simpleWatcher)
        etUsername.addTextChangedListener(simpleWatcher)
        etPassword.addTextChangedListener(simpleWatcher)
        etConfirmPassword.addTextChangedListener(simpleWatcher)

        btnCreateAdmin.setOnClickListener {
            createAdminAccount()
        }

        updatePasswordRules("")
        updateCreateButtonState()
    }

    private fun createAdminAccount() {
        clearErrors()

        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val username = etUsername.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()
        val isActive = switchIsActive.isChecked

        var hasError = false

        if (fullName.length < 3) {
            tilFullName.error = "Enter a full name with at least 3 characters"
            hasError = true
        }

        if (!EmailValidator.isValid(email)) {
            tilEmail.error = "Enter a valid email address"
            hasError = true
        }

        if (!PhoneValidator.isValid(phone)) {
            tilPhone.error = "Enter a valid phone number"
            hasError = true
        }

        if (!UsernameValidator.isValid(username)) {
            tilUsername.error = "Use 4-20 lowercase characters, numbers, dot, dash, or underscore"
            hasError = true
        }

        if (!PasswordRulesValidator.isValid(password)) {
            tilPassword.error = "Password does not meet all rules"
            hasError = true
        }

        if (confirmPassword != password) {
            tilConfirmPassword.error = "Passwords do not match"
            hasError = true
        }

        if (hasError) return

        val db = appContainer.database

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existingEmail = db.adminDao().findByEmail(email)
            val existingUsername = db.adminDao().findByUsername(username)

            if (existingEmail != null || existingUsername != null) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (existingEmail != null) {
                        tilEmail.error = "This email is already used by another admin"
                    }
                    if (existingUsername != null) {
                        tilUsername.error = "This username is already used by another admin"
                    }
                }
                return@launch
            }

            val salt = PasswordHasher.generateSaltBase64()
            val passwordChars = password.toCharArray()
            val hash = PasswordHasher.hashPasswordBase64(passwordChars, salt)
            passwordChars.fill('\u0000')

            db.adminDao().insert(
                Admin(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    username = username,
                    passwordHash = hash,
                    passwordSalt = salt,
                    isActive = isActive
                )
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                clearForm()
                Toast.makeText(requireContext(), "Admin account created successfully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearErrors() {
        tilFullName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilUsername.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
        tvError.visibility = View.GONE
    }

    private fun clearForm() {
        etFullName.setText("")
        etEmail.setText("")
        etPhone.setText("")
        etUsername.setText("")
        etPassword.setText("")
        etConfirmPassword.setText("")
        switchIsActive.isChecked = true
        tvAccountStatusValue.text = "Active on creation"
        updatePasswordRules("")
        updateCreateButtonState()
    }

    private fun updateCreateButtonState() {
        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val username = etUsername.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        val isValid = fullName.length >= 3 &&
                EmailValidator.isValid(email) &&
                PhoneValidator.isValid(phone) &&
                UsernameValidator.isValid(username) &&
                PasswordRulesValidator.isValid(password) &&
                confirmPassword == password

        btnCreateAdmin.isEnabled = isValid
        btnCreateAdmin.alpha = if (isValid) 1f else 0.55f
    }

    private fun updatePasswordRules(password: String) {
        val rules = PasswordRulesValidator.describe(password)

        ValidationUiStyler.applyPasswordRuleStyle(tvRuleUpper, rules.hasUppercase)
        ValidationUiStyler.applyPasswordRuleStyle(tvRuleLower, rules.hasLowercase)
        ValidationUiStyler.applyPasswordRuleStyle(tvRuleLength, rules.hasMinLength)
        ValidationUiStyler.applyPasswordRuleStyle(tvRuleSpecial, rules.hasSpecial)
        ValidationUiStyler.applyPasswordRuleStyle(tvRuleNumber, rules.hasDigit)
    }
}