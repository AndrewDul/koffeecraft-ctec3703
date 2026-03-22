package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
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

class CustomerDeleteAccountFragment : Fragment(R.layout.fragment_customer_delete_account) {

    private lateinit var vm: CustomerDeleteAccountViewModel
    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var tvError: TextView
    private lateinit var btnDeleteAccount: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            CustomerDeleteAccountViewModel.Factory(appContainer.customerSettingsRepository)
        )[CustomerDeleteAccountViewModel::class.java]

        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        tvError = view.findViewById(R.id.tvError)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.btnBackToSettings).setOnClickListener {
            findNavController().navigateUp()
        }

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvError.text = "You are not logged in as a customer."
            tvError.visibility = View.VISIBLE
            btnDeleteAccount.isEnabled = false
            return
        }

        btnDeleteAccount.setOnClickListener {
            tilCurrentPassword.error = null
            tvError.visibility = View.GONE

            val currentPassword = etCurrentPassword.text?.toString().orEmpty()
            if (currentPassword.isBlank()) {
                tilCurrentPassword.error = "Enter current password"
                return@setOnClickListener
            }

            vm.deleteAccount(
                customerId = customerId,
                currentPassword = currentPassword
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                btnDeleteAccount.isEnabled = !state.isDeleting
                btnDeleteAccount.alpha = if (state.isDeleting) 0.7f else 1f
                btnDeleteAccount.text = if (state.isDeleting) "Deleting..." else "Delete account permanently"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is CustomerDeleteAccountViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    CustomerDeleteAccountViewModel.UiEffect.NavigateToLogout -> {
                        findNavController().navigate(R.id.action_global_logout)
                    }
                }
            }
        }
    }
}