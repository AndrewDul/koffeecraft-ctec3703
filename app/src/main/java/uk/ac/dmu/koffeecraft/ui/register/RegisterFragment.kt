package uk.ac.dmu.koffeecraft.ui.register

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var vm: RegisterViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val repo = AuthRepository(db)
        vm = ViewModelProvider(this, RegisterViewModelFactory(repo))[RegisterViewModel::class.java]

        val etFirst = view.findViewById<EditText>(R.id.etFirstName)
        val etLast = view.findViewById<EditText>(R.id.etLastName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            vm.register(
                etFirst.text.toString(),
                etLast.text.toString(),
                etEmail.text.toString(),
                etPassword.text.toString()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                tvError.text = state.error ?: ""
                tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE

                if (state.success) {
                    findNavController().navigate(R.id.action_register_to_login)
                }
            }
        }
    }
}