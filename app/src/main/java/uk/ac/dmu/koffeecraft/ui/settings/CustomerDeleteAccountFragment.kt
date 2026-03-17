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
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
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
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

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

            val sqlDb = db.openHelper.writableDatabase

            sqlDb.beginTransaction()
            try {
                sqlDb.execSQL(
                    "DELETE FROM customer_favourite_preset_add_on_cross_ref WHERE presetId IN (SELECT presetId FROM customer_favourite_presets WHERE customerId = ?)",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM customer_favourite_presets WHERE customerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM favourites WHERE customerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM inbox_messages WHERE recipientCustomerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM app_notifications WHERE recipientCustomerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM feedback WHERE customerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM payments WHERE orderId IN (SELECT orderId FROM orders WHERE customerId = ?)",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM order_items WHERE orderId IN (SELECT orderId FROM orders WHERE customerId = ?)",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM orders WHERE customerId = ?",
                    arrayOf(customerId)
                )
                sqlDb.execSQL(
                    "DELETE FROM customers WHERE customerId = ?",
                    arrayOf(customerId)
                )

                sqlDb.setTransactionSuccessful()
            } finally {
                sqlDb.endTransaction()
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                CartManager.clear()
                SessionManager.clear()
                RememberedSessionStore.clear(requireContext().applicationContext)

                Toast.makeText(requireContext(), "Account deleted permanently.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_logout)
            }
        }
    }
}