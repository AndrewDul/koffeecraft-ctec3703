package uk.ac.dmu.koffeecraft.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var vm: LoginViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val repo = AuthRepository(db)
        vm = ViewModelProvider(this, LoginViewModelFactory(repo))[LoginViewModel::class.java]

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            vm.login(etEmail.text.toString(), etPassword.text.toString())
        }

        view.findViewById<Button>(R.id.btnGoRegister).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                tvError.text = state.error ?: ""
                tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE

                if (state.navigateToAdmin) {
                    vm.consumeNavigation()
                    findNavController().navigate(R.id.action_login_to_admin)
                } else if (state.navigateToMenu) {
                    vm.consumeNavigation()
                    findNavController().navigate(R.id.action_login_to_menu)
                }
            }
        }
    }
}