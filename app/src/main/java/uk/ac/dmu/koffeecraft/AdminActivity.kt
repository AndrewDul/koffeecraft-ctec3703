package uk.ac.dmu.koffeecraft

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val navHost = supportFragmentManager.findFragmentById(R.id.adminNavHost) as NavHostFragment
        val navController = navHost.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNav)
        bottomNav.setupWithNavController(navController)

        val btnNotifications = findViewById<ImageButton>(R.id.btnAdminNotifications)
        val btnSettings = findViewById<ImageButton>(R.id.btnAdminSettings)
        val tvBadge = findViewById<TextView>(R.id.tvAdminNotificationBadge)

        btnNotifications.setOnClickListener {
            navController.navigate(R.id.adminNotificationsFragment)
        }

        btnSettings.setOnClickListener {
            navController.navigate(R.id.adminSettingsFragment)
        }

        val db = KoffeeCraftDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            db.notificationDao().observeUnreadAdminCount().collect { count ->
                if (count > 0) {
                    tvBadge.visibility = View.VISIBLE
                    tvBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    tvBadge.visibility = View.GONE
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomMenuDestinations = setOf(
                R.id.adminHomeFragment,
                R.id.adminOrdersFragment,
                R.id.adminMenuFragment,
                R.id.adminFeedbackFragment,
                R.id.adminInboxFragment
            )

            bottomNav.visibility = if (destination.id in bottomMenuDestinations) {
                View.VISIBLE
            } else {
                View.VISIBLE
            }
        }
    }
}