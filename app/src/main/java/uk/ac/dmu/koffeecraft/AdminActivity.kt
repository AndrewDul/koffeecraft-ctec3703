package uk.ac.dmu.koffeecraft

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.core.di.appContainer

class AdminActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminActivityViewModel
    private lateinit var navController: NavController

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnInbox: ImageButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var tvBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminActivityViewModel.Factory(applicationContext.appContainer.adminActivityRepository)
        )[AdminActivityViewModel::class.java]

        setContentView(R.layout.activity_admin)

        bindViews()
        setupNavigationShell()
        observeState()

        viewModel.start()
    }

    private fun bindViews() {
        val navHost = supportFragmentManager.findFragmentById(R.id.adminNavHost) as NavHostFragment
        navController = navHost.navController

        bottomNav = findViewById(R.id.adminBottomNav)
        btnInbox = findViewById(R.id.btnAdminInbox)
        btnNotifications = findViewById(R.id.btnAdminNotifications)
        btnSettings = findViewById(R.id.btnAdminSettings)
        tvBadge = findViewById(R.id.tvAdminNotificationBadge)
    }

    private fun setupNavigationShell() {
        val bottomMenuDestinations = setOf(
            R.id.adminHomeFragment,
            R.id.adminOrdersFragment,
            R.id.adminMenuFragment,
            R.id.adminFeedbackFragment,
            R.id.adminCampaignStudioFragment
        )

        bottomNav.setOnItemSelectedListener { item ->
            navigateFromBottom(navController, item.itemId, force = false)
            true
        }

        bottomNav.setOnItemReselectedListener { item ->
            navigateFromBottom(navController, item.itemId, force = true)
        }

        btnInbox.setOnClickListener {
            navigateIfNeeded(navController, R.id.adminInboxFragment)
        }

        btnNotifications.setOnClickListener {
            navigateIfNeeded(navController, R.id.adminNotificationsFragment)
        }

        btnSettings.setOnClickListener {
            navigateIfNeeded(navController, R.id.adminSettingsFragment)
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

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvBadge.visibility = if (state.showNotificationBadge) View.VISIBLE else View.GONE
                tvBadge.text = state.notificationBadgeText
            }
        }
    }

    private fun navigateIfNeeded(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
            }
        )
    }

    private fun navigateFromBottom(
        navController: NavController,
        destinationId: Int,
        force: Boolean
    ) {
        if (!force && navController.currentDestination?.id == destinationId) return

        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
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