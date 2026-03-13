package uk.ac.dmu.koffeecraft.ui.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import uk.ac.dmu.koffeecraft.R

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnRegisterNow).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_register)
        }

        view.findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_login)
        }
    }
}