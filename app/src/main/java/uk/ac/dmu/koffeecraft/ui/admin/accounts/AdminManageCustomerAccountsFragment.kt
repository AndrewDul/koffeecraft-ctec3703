package uk.ac.dmu.koffeecraft.ui.admin.accounts

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminAccountTarget
import uk.ac.dmu.koffeecraft.data.dao.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.CustomerAccountCleanupRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher
import java.util.Date
import java.util.Locale

class AdminManageCustomerAccountsFragment : Fragment(R.layout.fragment_admin_manage_customer_accounts) {

    private enum class AccountType {
        CUSTOMERS,
        ADMINS
    }

    private enum class CustomerSearchMode {
        ORDER_ID,
        CUSTOMER_ID
    }

    private enum class AdminSearchMode {
        ADMIN_ID,
        EMAIL,
        USERNAME
    }

    private lateinit var toggleAccountType: MaterialButtonToggleGroup
    private lateinit var spinnerSearchMode: Spinner
    private lateinit var etSearchValue: EditText
    private lateinit var tvSearchDescription: TextView
    private lateinit var btnFindAccount: MaterialButton

    private lateinit var tvResultLabel: TextView
    private lateinit var tvAccountResult: TextView
    private lateinit var tvAccountStatus: TextView
    private lateinit var tvAccountMeta: TextView
    private lateinit var tvSearchError: TextView
    private lateinit var tvWarningTitle: TextView
    private lateinit var tvWarningBody: TextView
    private lateinit var tvActionTitle: TextView

    private lateinit var cardResult: MaterialCardView
    private lateinit var cardWarning: MaterialCardView
    private lateinit var cardActions: MaterialCardView

    private lateinit var btnPrimaryAction: MaterialButton
    private lateinit var btnSecondaryAction: MaterialButton
    private lateinit var btnDangerAction: MaterialButton

    private var currentAccountType = AccountType.CUSTOMERS
    private var currentCustomerSearchMode = CustomerSearchMode.ORDER_ID
    private var currentAdminSearchMode = AdminSearchMode.ADMIN_ID

    private var selectedCustomer: CustomerAccountTarget? = null
    private var selectedAdmin: AdminAccountTarget? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleAccountType = view.findViewById(R.id.toggleAccountType)
        spinnerSearchMode = view.findViewById(R.id.spinnerSearchMode)
        etSearchValue = view.findViewById(R.id.etSearchValue)
        tvSearchDescription = view.findViewById(R.id.tvSearchDescription)
        btnFindAccount = view.findViewById(R.id.btnFindAccount)

        tvResultLabel = view.findViewById(R.id.tvResultLabel)
        tvAccountResult = view.findViewById(R.id.tvAccountResult)
        tvAccountStatus = view.findViewById(R.id.tvAccountStatus)
        tvAccountMeta = view.findViewById(R.id.tvAccountMeta)
        tvSearchError = view.findViewById(R.id.tvSearchError)
        tvWarningTitle = view.findViewById(R.id.tvWarningTitle)
        tvWarningBody = view.findViewById(R.id.tvWarningBody)
        tvActionTitle = view.findViewById(R.id.tvActionTitle)

        cardResult = view.findViewById(R.id.cardResult)
        cardWarning = view.findViewById(R.id.cardWarning)
        cardActions = view.findViewById(R.id.cardActions)

        btnPrimaryAction = view.findViewById(R.id.btnPrimaryAction)
        btnSecondaryAction = view.findViewById(R.id.btnSecondaryAction)
        btnDangerAction = view.findViewById(R.id.btnDangerAction)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupAccountTypeToggle()
        setupSearchModeSpinner()
        renderModeUi()

        btnFindAccount.setOnClickListener {
            findAccount()
        }

        btnPrimaryAction.setOnClickListener {
            when (currentAccountType) {
                AccountType.CUSTOMERS -> selectedCustomer?.let { updateCustomerStatus(it.customerId, true) }
                AccountType.ADMINS -> selectedAdmin?.let { updateAdminStatus(it.adminId, true) }
            }
        }

        btnSecondaryAction.setOnClickListener {
            when (currentAccountType) {
                AccountType.CUSTOMERS -> selectedCustomer?.let { updateCustomerStatus(it.customerId, false) }
                AccountType.ADMINS -> selectedAdmin?.let { updateAdminStatus(it.adminId, false) }
            }
        }

        btnDangerAction.setOnClickListener {
            when (currentAccountType) {
                AccountType.CUSTOMERS -> selectedCustomer?.let { confirmDelete(it.customerId) }
                AccountType.ADMINS -> selectedAdmin?.let { showResetPasswordDialog(it) }
            }
        }
    }

    private fun setupAccountTypeToggle() {
        toggleAccountType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            currentAccountType = if (checkedId == R.id.btnCustomerMode) {
                AccountType.CUSTOMERS
            } else {
                AccountType.ADMINS
            }

            clearResult()
            setupSearchModeSpinner()
            renderModeUi()
        }

        toggleAccountType.check(R.id.btnCustomerMode)
    }

    private fun setupSearchModeSpinner() {
        val options = when (currentAccountType) {
            AccountType.CUSTOMERS -> listOf("Find by Order ID", "Find by Customer ID")
            AccountType.ADMINS -> listOf("Find by Admin ID", "Find by Email", "Find by Username")
        }

        spinnerSearchMode.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerSearchMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (currentAccountType) {
                    AccountType.CUSTOMERS -> {
                        currentCustomerSearchMode = if (position == 0) {
                            CustomerSearchMode.ORDER_ID
                        } else {
                            CustomerSearchMode.CUSTOMER_ID
                        }
                    }

                    AccountType.ADMINS -> {
                        currentAdminSearchMode = when (position) {
                            0 -> AdminSearchMode.ADMIN_ID
                            1 -> AdminSearchMode.EMAIL
                            else -> AdminSearchMode.USERNAME
                        }
                    }
                }

                updateSearchFieldForMode()
                clearResult()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        spinnerSearchMode.setSelection(0)
        updateSearchFieldForMode()
    }

    private fun renderModeUi() {
        when (currentAccountType) {
            AccountType.CUSTOMERS -> {
                tvSearchDescription.text = "Find a customer by Order ID or Customer ID and manage customer access safely."
                tvWarningTitle.text = "Customer account warning"
                tvWarningBody.text = "Customer account changes affect sign-in immediately. Permanent deletion removes account-related customer data and cannot be undone."
                tvActionTitle.text = "Customer account actions"
                btnPrimaryAction.text = "Activate account"
                btnSecondaryAction.text = "Deactivate account"
                btnDangerAction.text = "Delete account"
                btnDangerAction.visibility = View.VISIBLE
                btnDangerAction.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_danger))
            }

            AccountType.ADMINS -> {
                tvSearchDescription.text = "Find an admin by Admin ID, email, or username and manage secure internal access."
                tvWarningTitle.text = "Admin account warning"
                tvWarningBody.text = "At least one active admin must remain. The currently signed-in admin account cannot be deactivated from this screen."
                tvActionTitle.text = "Admin access actions"
                btnPrimaryAction.text = "Activate account"
                btnSecondaryAction.text = "Deactivate account"
                btnDangerAction.text = "Reset password"
                btnDangerAction.visibility = View.VISIBLE
                btnDangerAction.backgroundTintList = ColorStateList.valueOf(color(R.color.kc_brand_primary))
            }
        }
    }

    private fun updateSearchFieldForMode() {
        when (currentAccountType) {
            AccountType.CUSTOMERS -> {
                etSearchValue.inputType = InputType.TYPE_CLASS_NUMBER
                etSearchValue.hint = if (currentCustomerSearchMode == CustomerSearchMode.ORDER_ID) {
                    "Enter order ID"
                } else {
                    "Enter customer ID"
                }
            }

            AccountType.ADMINS -> {
                when (currentAdminSearchMode) {
                    AdminSearchMode.ADMIN_ID -> {
                        etSearchValue.inputType = InputType.TYPE_CLASS_NUMBER
                        etSearchValue.hint = "Enter admin ID"
                    }

                    AdminSearchMode.EMAIL -> {
                        etSearchValue.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        etSearchValue.hint = "Enter admin email"
                    }

                    AdminSearchMode.USERNAME -> {
                        etSearchValue.inputType = InputType.TYPE_CLASS_TEXT
                        etSearchValue.hint = "Enter username"
                    }
                }
            }
        }
    }

    private fun findAccount() {
        val rawValue = etSearchValue.text.toString().trim()
        if (rawValue.isBlank()) {
            Toast.makeText(requireContext(), "Enter a search value first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            when (currentAccountType) {
                AccountType.CUSTOMERS -> {
                    val value = rawValue.toLongOrNull()
                    if (value == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Enter a valid numeric value.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val result = when (currentCustomerSearchMode) {
                        CustomerSearchMode.ORDER_ID -> db.customerDao().getAccountTargetByOrderId(value)
                        CustomerSearchMode.CUSTOMER_ID -> db.customerDao().getAccountTargetByCustomerId(value)
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        if (result == null) {
                            clearResult()
                            tvSearchError.text = "No matching customer found."
                            tvSearchError.visibility = View.VISIBLE
                        } else {
                            selectedCustomer = result
                            selectedAdmin = null
                            renderCustomerResult(result)
                        }
                    }
                }

                AccountType.ADMINS -> {
                    val result = when (currentAdminSearchMode) {
                        AdminSearchMode.ADMIN_ID -> {
                            val value = rawValue.toLongOrNull()
                            if (value == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Enter a valid numeric admin ID.", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            db.adminDao().getAccountTargetByAdminId(value)
                        }

                        AdminSearchMode.EMAIL -> db.adminDao().getAccountTargetByEmail(rawValue.lowercase(Locale.UK))
                        AdminSearchMode.USERNAME -> db.adminDao().getAccountTargetByUsername(rawValue.lowercase(Locale.UK))
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        if (result == null) {
                            clearResult()
                            tvSearchError.text = "No matching admin found."
                            tvSearchError.visibility = View.VISIBLE
                        } else {
                            selectedAdmin = result
                            selectedCustomer = null
                            renderAdminResult(result)
                        }
                    }
                }
            }
        }
    }

    private fun renderCustomerResult(result: CustomerAccountTarget) {
        tvSearchError.visibility = View.GONE
        cardResult.visibility = View.VISIBLE
        cardWarning.visibility = View.VISIBLE
        cardActions.visibility = View.VISIBLE

        tvResultLabel.text = "Selected customer"
        tvAccountResult.text = "${result.firstName} ${result.lastName}"
        tvAccountStatus.text = if (result.isActive) "Status: Active" else "Status: Inactive"
        tvAccountMeta.text = "Customer #${result.customerId} • ${result.email}\nCreated ${formatDate(result.createdAt)}"

        btnPrimaryAction.isEnabled = !result.isActive
        btnPrimaryAction.alpha = if (result.isActive) 0.55f else 1f
        btnSecondaryAction.isEnabled = result.isActive
        btnSecondaryAction.alpha = if (result.isActive) 1f else 0.55f
        btnDangerAction.isEnabled = true
        btnDangerAction.alpha = 1f
    }

    private fun renderAdminResult(result: AdminAccountTarget) {
        tvSearchError.visibility = View.GONE
        cardResult.visibility = View.VISIBLE
        cardWarning.visibility = View.VISIBLE
        cardActions.visibility = View.VISIBLE

        tvResultLabel.text = "Selected admin"
        tvAccountResult.text = result.fullName
        tvAccountStatus.text = if (result.isActive) "Status: Active" else "Status: Inactive"
        tvAccountMeta.text = "Admin #${result.adminId} • @${result.username}\n${result.email}\n${result.phone}\nCreated ${formatDate(result.createdAt)}"

        btnPrimaryAction.isEnabled = !result.isActive
        btnPrimaryAction.alpha = if (result.isActive) 0.55f else 1f

        val canAttemptDeactivate = result.isActive
        btnSecondaryAction.isEnabled = canAttemptDeactivate
        btnSecondaryAction.alpha = if (canAttemptDeactivate) 1f else 0.55f

        btnDangerAction.isEnabled = true
        btnDangerAction.alpha = 1f
    }

    private fun updateCustomerStatus(customerId: Long, isActive: Boolean) {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val actionLabel = if (isActive) "activate" else "deactivate"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm action")
            .setMessage("Are you sure you want to $actionLabel Customer #$customerId?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.customerDao().updateActiveStatus(customerId, isActive)
                    val refreshed = db.customerDao().getAccountTargetByCustomerId(customerId)

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        if (refreshed != null) {
                            selectedCustomer = refreshed
                            renderCustomerResult(refreshed)
                        }
                        Toast.makeText(requireContext(), "Customer #$customerId updated successfully.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun updateAdminStatus(adminId: Long, isActive: Boolean) {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (!isActive) {
                if (SessionManager.currentAdminId == adminId) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "You cannot deactivate the currently signed-in admin account.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val activeCount = db.adminDao().countActiveAdmins()
                if (activeCount <= 1) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "At least one active admin account must remain.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                val actionLabel = if (isActive) "activate" else "deactivate"
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm action")
                    .setMessage("Are you sure you want to $actionLabel Admin #$adminId?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Confirm") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            db.adminDao().updateActiveStatus(adminId, isActive)
                            val refreshed = db.adminDao().getAccountTargetByAdminId(adminId)

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                if (refreshed != null) {
                                    selectedAdmin = refreshed
                                    renderAdminResult(refreshed)
                                }
                                Toast.makeText(requireContext(), "Admin #$adminId updated successfully.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun confirmDelete(customerId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This will permanently remove Customer #$customerId and related customer data.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                showDeleteKeywordDialog(customerId)
            }
            .show()
    }

    private fun showDeleteKeywordDialog(customerId: Long) {
        val input = EditText(requireContext()).apply {
            hint = "Type DELETE"
            setSingleLine(true)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Final confirmation")
            .setMessage("Type DELETE to permanently remove Customer #$customerId.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val value = input.text.toString().trim()
                if (value != "DELETE") {
                    Toast.makeText(
                        requireContext(),
                        "Deletion cancelled. You must type DELETE exactly.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                deleteCustomerPermanently(customerId)
            }
            .show()
    }

    private fun deleteCustomerPermanently(customerId: Long) {
        val appContext = requireContext().applicationContext
        val cleanupRepository = CustomerAccountCleanupRepository(appContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            cleanupRepository.deleteCustomerCompletely(customerId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                clearResult()
                Toast.makeText(requireContext(), "Customer #$customerId was deleted permanently.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResetPasswordDialog(target: AdminAccountTarget) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val etNewPassword = EditText(requireContext()).apply {
            hint = "New password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etConfirmPassword = EditText(requireContext()).apply {
            hint = "Confirm password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(etNewPassword)
        container.addView(etConfirmPassword)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset admin password")
            .setMessage("Set a new password for ${target.fullName}.")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                when {
                    !isPasswordValid(newPassword) -> {
                        Toast.makeText(requireContext(), "The new password does not meet all password rules.", Toast.LENGTH_SHORT).show()
                    }

                    confirmPassword != newPassword -> {
                        Toast.makeText(requireContext(), "The password confirmation does not match.", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        resetAdminPassword(target.adminId, newPassword)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun resetAdminPassword(adminId: Long, newPassword: String) {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val newSalt = PasswordHasher.generateSaltBase64()
            val passwordChars = newPassword.toCharArray()
            val newHash = PasswordHasher.hashPasswordBase64(passwordChars, newSalt)
            passwordChars.fill('\u0000')

            db.adminDao().updatePassword(adminId, newHash, newSalt)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                Toast.makeText(requireContext(), "Admin password updated successfully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearResult() {
        selectedCustomer = null
        selectedAdmin = null
        cardResult.visibility = View.GONE
        cardWarning.visibility = View.GONE
        cardActions.visibility = View.GONE
        tvSearchError.visibility = View.GONE
        tvSearchError.text = ""
        tvAccountResult.text = ""
        tvAccountStatus.text = ""
        tvAccountMeta.text = ""
    }

    private fun formatDate(timestamp: Long): String {
        return DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() } &&
                password.length >= 8
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}