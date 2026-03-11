package uk.ac.dmu.koffeecraft.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class AdminSettingsFragment : Fragment(R.layout.fragment_admin_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogout = view.findViewById<Button>(R.id.btnAdminLogout)

        btnLogout.setOnClickListener {
            // I clear the session so the admin is fully logged out.
            SessionManager.clear()

            // I return to the main login flow and clear the activity back stack.
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}