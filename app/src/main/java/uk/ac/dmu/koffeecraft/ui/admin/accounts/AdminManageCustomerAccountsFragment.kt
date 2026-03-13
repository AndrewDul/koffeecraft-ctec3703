package uk.ac.dmu.koffeecraft.ui.admin.accounts

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class AdminManageCustomerAccountsFragment : Fragment(R.layout.fragment_admin_manage_customer_accounts) {

    private enum class SearchMode {
        ORDER_ID,
        CUSTOMER_ID
    }

    private lateinit var spinnerSearchMode: Spinner
    private lateinit var etSearchValue: EditText
    private lateinit var btnFindAccount: MaterialButton
    private lateinit var tvAccountResult: TextView
    private lateinit var tvAccountStatus: TextView
    private lateinit var tvSearchError: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var cardWarning: MaterialCardView
    private lateinit var cardActions: MaterialCardView
    private lateinit var btnActivateAccount: MaterialButton
    private lateinit var btnDeactivateAccount: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton

    private var currentSearchMode = SearchMode.ORDER_ID
    private var selectedAccount: CustomerAccountTarget? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerSearchMode = view.findViewById(R.id.spinnerSearchMode)
        etSearchValue = view.findViewById(R.id.etSearchValue)
        btnFindAccount = view.findViewById(R.id.btnFindAccount)
        tvAccountResult = view.findViewById(R.id.tvAccountResult)
        tvAccountStatus = view.findViewById(R.id.tvAccountStatus)
        tvSearchError = view.findViewById(R.id.tvSearchError)
        cardResult = view.findViewById(R.id.cardResult)
        cardWarning = view.findViewById(R.id.cardWarning)
        cardActions = view.findViewById(R.id.cardActions)
        btnActivateAccount = view.findViewById(R.id.btnActivateAccount)
        btnDeactivateAccount = view.findViewById(R.id.btnDeactivateAccount)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)

        setupSearchMode()

        btnFindAccount.setOnClickListener {
            findAccount()
        }

        btnActivateAccount.setOnClickListener {
            selectedAccount?.let { updateAccountStatus(it.customerId, true) }
        }

        btnDeactivateAccount.setOnClickListener {
            selectedAccount?.let { updateAccountStatus(it.customerId, false) }
        }

        btnDeleteAccount.setOnClickListener {
            selectedAccount?.let { confirmDelete(it.customerId) }
        }
    }

    private fun setupSearchMode() {
        val options = listOf("Find by Order ID", "Find by Customer ID")

        spinnerSearchMode.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerSearchMode.setSelection(0)
        etSearchValue.hint = "Enter order ID"
        etSearchValue.inputType = InputType.TYPE_CLASS_NUMBER

        spinnerSearchMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSearchMode = if (position == 0) SearchMode.ORDER_ID else SearchMode.CUSTOMER_ID
                etSearchValue.hint = if (currentSearchMode == SearchMode.ORDER_ID) "Enter order ID" else "Enter customer ID"
                etSearchValue.inputType = InputType.TYPE_CLASS_NUMBER
                clearResult()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun findAccount() {
        val raw = etSearchValue.text.toString().trim()
        val value = raw.toLongOrNull()

        if (value == null) {
            Toast.makeText(requireContext(), "Enter a valid numeric value.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = when (currentSearchMode) {
                SearchMode.ORDER_ID -> db.customerDao().getAccountTargetByOrderId(value)
                SearchMode.CUSTOMER_ID -> db.customerDao().getAccountTargetByCustomerId(value)
            }

            withContext(Dispatchers.Main) {
                if (result == null) {
                    clearResult()
                    tvSearchError.visibility = View.VISIBLE
                } else {
                    selectedAccount = result
                    tvSearchError.visibility = View.GONE
                    cardResult.visibility = View.VISIBLE
                    cardWarning.visibility = View.VISIBLE
                    cardActions.visibility = View.VISIBLE

                    tvAccountResult.text = "Customer #${result.customerId}"
                    tvAccountStatus.text = if (result.isActive) {
                        "Status: Active"
                    } else {
                        "Status: Inactive"
                    }

                    btnActivateAccount.isEnabled = !result.isActive
                    btnActivateAccount.alpha = if (result.isActive) 0.5f else 1f

                    btnDeactivateAccount.isEnabled = result.isActive
                    btnDeactivateAccount.alpha = if (result.isActive) 1f else 0.5f
                }
            }
        }
    }

    private fun updateAccountStatus(customerId: Long, isActive: Boolean) {
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
                        if (refreshed != null) {
                            selectedAccount = refreshed
                            tvAccountStatus.text = if (refreshed.isActive) {
                                "Status: Active"
                            } else {
                                "Status: Inactive"
                            }

                            btnActivateAccount.isEnabled = !refreshed.isActive
                            btnActivateAccount.alpha = if (refreshed.isActive) 0.5f else 1f

                            btnDeactivateAccount.isEnabled = refreshed.isActive
                            btnDeactivateAccount.alpha = if (refreshed.isActive) 1f else 0.5f
                        }

                        Toast.makeText(
                            requireContext(),
                            "Customer #$customerId updated successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(customerId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This will permanently remove Customer #$customerId and related data.")
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
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val sqlDb = db.openHelper.writableDatabase

            sqlDb.beginTransaction()
            try {
                sqlDb.execSQL("DELETE FROM inbox_messages WHERE recipientCustomerId = ?", arrayOf(customerId))
                sqlDb.execSQL("DELETE FROM app_notifications WHERE recipientCustomerId = ?", arrayOf(customerId))
                sqlDb.execSQL("DELETE FROM feedback WHERE customerId = ?", arrayOf(customerId))
                sqlDb.execSQL(
                    "DELETE FROM payments WHERE orderId IN (SELECT orderId FROM orders WHERE customerId = ?)",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM order_items WHERE orderId IN (SELECT orderId FROM orders WHERE customerId = ?)",
                    arrayOf(customerId)
                )
                sqlDb.execSQL("DELETE FROM orders WHERE customerId = ?", arrayOf(customerId))
                sqlDb.execSQL("DELETE FROM customers WHERE customerId = ?", arrayOf(customerId))

                sqlDb.setTransactionSuccessful()
            } finally {
                sqlDb.endTransaction()
            }

            withContext(Dispatchers.Main) {
                clearResult()
                Toast.makeText(
                    requireContext(),
                    "Customer #$customerId was deleted permanently.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun clearResult() {
        selectedAccount = null
        cardResult.visibility = View.GONE
        cardWarning.visibility = View.GONE
        cardActions.visibility = View.GONE
        tvSearchError.visibility = View.GONE
    }
}