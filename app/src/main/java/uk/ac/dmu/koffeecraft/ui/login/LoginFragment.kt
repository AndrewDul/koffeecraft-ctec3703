package uk.ac.dmu.koffeecraft.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.AdminActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnSignIn = view.findViewById<MaterialButton>(R.id.btnSignIn)
        val tvGoToRegister = view.findViewById<TextView>(R.id.tvGoToRegister)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        btnSignIn.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null
            tvError.visibility = View.GONE

            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString().orEmpty()

            var hasError = false

            if (email.isBlank()) {
                tilEmail.error = "Enter your email"
                hasError = true
            }

            if (password.isBlank()) {
                tilPassword.error = "Enter your password"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val admin = db.adminDao().findByEmail(email)
                if (admin != null) {
                    val adminOk = verifyPassword(
                        rawPassword = password,
                        salt = admin.passwordSalt,
                        expectedHash = admin.passwordHash
                    )

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext

                        if (adminOk) {
                            SessionManager.setAdmin()
                            val intent = Intent(requireContext(), AdminActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            tvError.text = "Invalid email or password."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                    return@launch
                }

                val customer = db.customerDao().findByEmail(email)
                if (customer == null) {
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        tvError.text = "Invalid email or password."
                        tvError.visibility = View.VISIBLE
                    }
                    return@launch
                }

                val customerOk = verifyPassword(
                    rawPassword = password,
                    salt = customer.passwordSalt,
                    expectedHash = customer.passwordHash
                )

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (customerOk) {
                        SessionManager.setCustomer(customer.customerId)
                        findNavController().navigate(R.id.action_login_to_menu)
                    } else {
                        tvError.text = "Invalid email or password."
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun verifyPassword(
        rawPassword: String,
        salt: String,
        expectedHash: String
    ): Boolean {
        val actualHash = PasswordHasher.hashPasswordBase64(rawPassword.toCharArray(), salt)
        return actualHash == expectedHash
    }
}