package uk.ac.dmu.koffeecraft.ui.settings

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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher
import androidx.core.content.ContextCompat
class CustomerChangePasswordFragment : Fragment(R.layout.fragment_customer_change_password) {

    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var tvError: TextView

    private lateinit var tvRuleUpper: TextView
    private lateinit var tvRuleLower: TextView
    private lateinit var tvRuleLength: TextView
    private lateinit var tvRuleSpecial: TextView
    private lateinit var tvRuleNumber: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword)
        tilNewPassword = view.findViewById(R.id.tilNewPassword)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        tvError = view.findViewById(R.id.tvError)

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
                updateRuleTile(tvRuleUpper, password.any { it.isUpperCase() })
                updateRuleTile(tvRuleLower, password.any { it.isLowerCase() })
                updateRuleTile(tvRuleLength, password.length >= 8)
                updateRuleTile(tvRuleSpecial, password.any { !it.isLetterOrDigit() })
                updateRuleTile(tvRuleNumber, password.any { it.isDigit() })
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        view.findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val customerId = SessionManager.currentCustomerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        tilCurrentPassword.error = null
        tilNewPassword.error = null
        tvError.visibility = View.GONE

        val currentPassword = etCurrentPassword.text?.toString().orEmpty()
        val newPassword = etNewPassword.text?.toString().orEmpty()

        var hasError = false

        if (currentPassword.isBlank()) {
            tilCurrentPassword.error = "Enter current password"
            hasError = true
        }

        if (!isPasswordValid(newPassword)) {
            tilNewPassword.error = "New password does not meet all rules"
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

            val currentPasswordChars = currentPassword.toCharArray()
            val currentValid = PasswordHasher.verify(
                currentPasswordChars,
                customer.passwordSalt,
                customer.passwordHash
            )
            currentPasswordChars.fill('\u0000')

            if (!currentValid) {
                withContext(Dispatchers.Main) {
                    tilCurrentPassword.error = "Current password is incorrect"
                }
                return@launch
            }

            val newSalt = PasswordHasher.generateSaltBase64()
            val newPasswordChars = newPassword.toCharArray()
            val newHash = PasswordHasher.hashPasswordBase64(newPasswordChars, newSalt)
            newPasswordChars.fill('\u0000')

            db.customerDao().update(
                customer.copy(
                    passwordHash = newHash,
                    passwordSalt = newSalt
                )
            )

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                etCurrentPassword.setText("")
                etNewPassword.setText("")
                Toast.makeText(requireContext(), "Password changed successfully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRuleTile(view: TextView, isValid: Boolean) {
        if (isValid) {
            view.setBackgroundResource(R.drawable.bg_rule_valid)
            view.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.kc_success_text)
            )
        } else {
            view.setBackgroundResource(R.drawable.bg_rule_invalid)
            view.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.kc_danger_text)
            )
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