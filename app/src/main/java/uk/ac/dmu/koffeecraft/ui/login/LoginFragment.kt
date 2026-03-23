package uk.ac.dmu.koffeecraft.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.AdminActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var vm: LoginViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnSignIn = view.findViewById<MaterialButton>(R.id.btnSignIn)
        val tvGoToRegister = view.findViewById<TextView>(R.id.tvGoToRegister)

        val appContext = requireContext().applicationContext
        val container = appContext.appContainer

        vm = ViewModelProvider(
            this,
            LoginViewModelFactory(container.authSessionRepository)
        )[LoginViewModel::class.java]

        tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        btnSignIn.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null
            tvError.visibility = View.GONE
            vm.clearError()

            val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
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

            vm.login(email = email, password = password)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { state ->
                    renderLoginState(
                        state = state,
                        btnSignIn = btnSignIn,
                        tvError = tvError
                    )

                    when (val success = state.loginSuccess) {
                        null -> Unit

                        is LoginViewModel.LoginSuccess.Admin -> {
                            vm.consumeLoginSuccess()

                            val intent = Intent(requireContext(), AdminActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }

                        is LoginViewModel.LoginSuccess.Customer -> {
                            vm.consumeLoginSuccess()
                            findNavController().navigate(R.id.action_login_to_menu)
                        }
                    }
                }
            }
        }
    }

    private fun renderLoginState(
        state: LoginViewModel.UiState,
        btnSignIn: MaterialButton,
        tvError: TextView
    ) {
        btnSignIn.isEnabled = !state.isLoading
        btnSignIn.text = if (state.isLoading) "Signing in..." else "Sign in"
        btnSignIn.alpha = if (state.isLoading) 0.75f else 1f

        tvError.text = state.error.orEmpty()
        tvError.visibility = if (state.error.isNullOrBlank()) View.GONE else View.VISIBLE
    }
}