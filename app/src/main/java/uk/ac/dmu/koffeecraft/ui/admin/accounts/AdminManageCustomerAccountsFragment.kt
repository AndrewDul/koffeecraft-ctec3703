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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.dao.AdminAccountTarget
import uk.ac.dmu.koffeecraft.data.dao.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import java.util.Date
import java.util.Locale

class AdminManageCustomerAccountsFragment : Fragment(R.layout.fragment_admin_manage_customer_accounts) {

    private lateinit var vm: AdminManageCustomerAccountsViewModel

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

    private var latestState = AdminManageAccountsUiState()
    private var spinnerBindingInProgress = false
    private var lastSpinnerAccountType: ManagedAccountType? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            AdminManageCustomerAccountsViewModel.Factory(appContainer.adminAccountsRepository)
        )[AdminManageCustomerAccountsViewModel::class.java]

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

        btnFindAccount.setOnClickListener {
            vm.searchAccount(etSearchValue.text.toString())
        }

        btnPrimaryAction.setOnClickListener {
            when (latestState.accountType) {
                ManagedAccountType.CUSTOMERS -> vm.updateSelectedCustomerStatus(true)
                ManagedAccountType.ADMINS -> vm.updateSelectedAdminStatus(
                    isActive = true,
                    currentAdminId = SessionManager.currentAdminId
                )
            }
        }

        btnSecondaryAction.setOnClickListener {
            when (latestState.accountType) {
                ManagedAccountType.CUSTOMERS -> showCustomerStatusDialog(false)
                ManagedAccountType.ADMINS -> showAdminStatusDialog(false)
            }
        }

        btnDangerAction.setOnClickListener {
            when (latestState.accountType) {
                ManagedAccountType.CUSTOMERS -> {
                    val customer = latestState.selectedCustomer ?: return@setOnClickListener
                    confirmDelete(customer.customerId)
                }

                ManagedAccountType.ADMINS -> {
                    val admin = latestState.selectedAdmin ?: return@setOnClickListener
                    showResetPasswordDialog(admin)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                latestState = state
                renderState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is AdminManageCustomerAccountsViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupAccountTypeToggle() {
        toggleAccountType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val selectedType = if (checkedId == R.id.btnCustomerMode) {
                ManagedAccountType.CUSTOMERS
            } else {
                ManagedAccountType.ADMINS
            }

            vm.setAccountType(selectedType)
        }

        toggleAccountType.check(R.id.btnCustomerMode)
    }

    private fun setupSearchModeSpinner() {
        spinnerSearchMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (spinnerBindingInProgress) return

                when (latestState.accountType) {
                    ManagedAccountType.CUSTOMERS -> {
                        vm.setCustomerSearchMode(
                            if (position == 0) {
                                CustomerSearchMode.ORDER_ID
                            } else {
                                CustomerSearchMode.CUSTOMER_ID
                            }
                        )
                    }

                    ManagedAccountType.ADMINS -> {
                        vm.setAdminSearchMode(
                            when (position) {
                                0 -> AdminSearchMode.ADMIN_ID
                                1 -> AdminSearchMode.EMAIL
                                else -> AdminSearchMode.USERNAME
                            }
                        )
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun renderState(state: AdminManageAccountsUiState) {
        bindToggle(state.accountType)
        bindSpinner(state)
        renderModeUi(state.accountType)
        renderSearchFieldForMode(state)
        renderSearchError(state.searchError)

        when {
            state.selectedCustomer != null -> renderCustomerResult(state.selectedCustomer, state.isWorking)
            state.selectedAdmin != null -> renderAdminResult(state.selectedAdmin, state.isWorking)
            else -> clearResultUi()
        }

        btnFindAccount.isEnabled = !state.isWorking
        btnFindAccount.alpha = if (state.isWorking) 0.7f else 1f
    }

    private fun bindToggle(accountType: ManagedAccountType) {
        val targetId = if (accountType == ManagedAccountType.CUSTOMERS) {
            R.id.btnCustomerMode
        } else {
            R.id.btnAdminMode
        }

        if (toggleAccountType.checkedButtonId != targetId) {
            toggleAccountType.check(targetId)
        }
    }

    private fun bindSpinner(state: AdminManageAccountsUiState) {
        if (lastSpinnerAccountType != state.accountType) {
            lastSpinnerAccountType = state.accountType

            val options = when (state.accountType) {
                ManagedAccountType.CUSTOMERS -> listOf("Find by Order ID", "Find by Customer ID")
                ManagedAccountType.ADMINS -> listOf("Find by Admin ID", "Find by Email", "Find by Username")
            }

            spinnerBindingInProgress = true
            spinnerSearchMode.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinnerBindingInProgress = false
        }

        val desiredPosition = when (state.accountType) {
            ManagedAccountType.CUSTOMERS -> {
                if (state.customerSearchMode == CustomerSearchMode.ORDER_ID) 0 else 1
            }

            ManagedAccountType.ADMINS -> {
                when (state.adminSearchMode) {
                    AdminSearchMode.ADMIN_ID -> 0
                    AdminSearchMode.EMAIL -> 1
                    AdminSearchMode.USERNAME -> 2
                }
            }
        }

        if (spinnerSearchMode.selectedItemPosition != desiredPosition) {
            spinnerBindingInProgress = true
            spinnerSearchMode.setSelection(desiredPosition, false)
            spinnerBindingInProgress = false
        }
    }

    private fun renderModeUi(accountType: ManagedAccountType) {
        when (accountType) {
            ManagedAccountType.CUSTOMERS -> {
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

            ManagedAccountType.ADMINS -> {
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

    private fun renderSearchFieldForMode(state: AdminManageAccountsUiState) {
        when (state.accountType) {
            ManagedAccountType.CUSTOMERS -> {
                etSearchValue.inputType = InputType.TYPE_CLASS_NUMBER
                etSearchValue.hint = if (state.customerSearchMode == CustomerSearchMode.ORDER_ID) {
                    "Enter order ID"
                } else {
                    "Enter customer ID"
                }
            }

            ManagedAccountType.ADMINS -> {
                when (state.adminSearchMode) {
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

    private fun renderSearchError(searchError: String?) {
        tvSearchError.text = searchError.orEmpty()
        tvSearchError.visibility = if (searchError.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun renderCustomerResult(
        result: CustomerAccountTarget,
        isWorking: Boolean
    ) {
        cardResult.visibility = View.VISIBLE
        cardWarning.visibility = View.VISIBLE
        cardActions.visibility = View.VISIBLE

        tvResultLabel.text = "Selected customer"
        tvAccountResult.text = "${result.firstName} ${result.lastName}"
        tvAccountStatus.text = if (result.isActive) "Status: Active" else "Status: Inactive"
        tvAccountMeta.text = "Customer #${result.customerId} • ${result.email}\nCreated ${formatDate(result.createdAt)}"

        btnPrimaryAction.isEnabled = !result.isActive && !isWorking
        btnPrimaryAction.alpha = if (!result.isActive && !isWorking) 1f else 0.55f

        btnSecondaryAction.isEnabled = result.isActive && !isWorking
        btnSecondaryAction.alpha = if (result.isActive && !isWorking) 1f else 0.55f

        btnDangerAction.isEnabled = !isWorking
        btnDangerAction.alpha = if (!isWorking) 1f else 0.55f
    }

    private fun renderAdminResult(
        result: AdminAccountTarget,
        isWorking: Boolean
    ) {
        cardResult.visibility = View.VISIBLE
        cardWarning.visibility = View.VISIBLE
        cardActions.visibility = View.VISIBLE

        tvResultLabel.text = "Selected admin"
        tvAccountResult.text = result.fullName
        tvAccountStatus.text = if (result.isActive) "Status: Active" else "Status: Inactive"
        tvAccountMeta.text = "Admin #${result.adminId} • @${result.username}\n${result.email}\n${result.phone}\nCreated ${formatDate(result.createdAt)}"

        btnPrimaryAction.isEnabled = !result.isActive && !isWorking
        btnPrimaryAction.alpha = if (!result.isActive && !isWorking) 1f else 0.55f

        btnSecondaryAction.isEnabled = result.isActive && !isWorking
        btnSecondaryAction.alpha = if (result.isActive && !isWorking) 1f else 0.55f

        btnDangerAction.isEnabled = !isWorking
        btnDangerAction.alpha = if (!isWorking) 1f else 0.55f
    }

    private fun showCustomerStatusDialog(isActive: Boolean) {
        val customer = latestState.selectedCustomer ?: return
        val actionLabel = if (isActive) "activate" else "deactivate"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm action")
            .setMessage("Are you sure you want to $actionLabel Customer #${customer.customerId}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                vm.updateSelectedCustomerStatus(isActive)
            }
            .show()
    }

    private fun showAdminStatusDialog(isActive: Boolean) {
        val admin = latestState.selectedAdmin ?: return
        val actionLabel = if (isActive) "activate" else "deactivate"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm action")
            .setMessage("Are you sure you want to $actionLabel Admin #${admin.adminId}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                vm.updateSelectedAdminStatus(
                    isActive = isActive,
                    currentAdminId = SessionManager.currentAdminId
                )
            }
            .show()
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

                vm.deleteSelectedCustomer()
            }
            .show()
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
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                vm.resetSelectedAdminPassword(
                    newPassword = etNewPassword.text.toString(),
                    confirmPassword = etConfirmPassword.text.toString()
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun clearResultUi() {
        cardResult.visibility = View.GONE
        cardWarning.visibility = View.GONE
        cardActions.visibility = View.GONE
        tvAccountResult.text = ""
        tvAccountStatus.text = ""
        tvAccountMeta.text = ""
    }

    private fun formatDate(timestamp: Long): String {
        return DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}