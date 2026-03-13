package uk.ac.dmu.koffeecraft

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
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

        val btnNotifications = findViewById<ImageButton>(R.id.btnAdminNotifications)
        val btnSettings = findViewById<ImageButton>(R.id.btnAdminSettings)
        val tvBadge = findViewById<TextView>(R.id.tvAdminNotificationBadge)

        val bottomMenuDestinations = setOf(
            R.id.adminHomeFragment,
            R.id.adminOrdersFragment,
            R.id.adminMenuFragment,
            R.id.adminFeedbackFragment,
            R.id.adminInboxFragment
        )

        bottomNav.setOnItemSelectedListener { item ->
            navigateIfNeeded(navController, item.itemId)
            true
        }

        bottomNav.setOnItemReselectedListener {
            // I intentionally do nothing on reselection.
        }

        btnNotifications.setOnClickListener {
            navigateIfNeeded(navController, R.id.adminNotificationsFragment)
        }

        btnSettings.setOnClickListener {
            navigateIfNeeded(navController, R.id.adminSettingsFragment)
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
            if (destination.id in bottomMenuDestinations) {
                bottomNav.menu.findItem(destination.id)?.isChecked = true
            } else {
                clearBottomSelection(bottomNav)
            }

            bottomNav.visibility = View.VISIBLE
        }
    }

    private fun navigateIfNeeded(navController: androidx.navigation.NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
        )
    }

    private fun clearBottomSelection(bottomNav: BottomNavigationView) {
        bottomNav.menu.setGroupCheckable(0, false, true)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)
    }
}