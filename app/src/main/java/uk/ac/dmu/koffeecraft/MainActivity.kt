package uk.ac.dmu.koffeecraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var observedCustomerId: Long? = null
    private var cartBadgeJob: Job? = null
    private var inboxBadgeJob: Job? = null
    private var notificationBadgeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.ensureChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val topBar = findViewById<View>(R.id.customerTopBar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.customerBottomNav)

        val btnCart = findViewById<ImageButton>(R.id.btnCustomerCart)
        val btnInbox = findViewById<ImageButton>(R.id.btnCustomerInbox)
        val btnNotifications = findViewById<ImageButton>(R.id.btnCustomerNotifications)
        val btnSettings = findViewById<ImageButton>(R.id.btnCustomerSettings)

        val bottomMenuDestinations = setOf(
            R.id.customerHomeFragment,
            R.id.menuFragment,
            R.id.ordersFragment,
            R.id.customerFavouritesFragment,
            R.id.customerRewardsFragment
        )

        btnCart.setOnClickListener {
            navigateIfNeeded(navController, R.id.cartFragment)
        }

        btnInbox.setOnClickListener {
            navigateIfNeeded(navController, R.id.customerInboxFragment)
        }

        btnNotifications.setOnClickListener {
            navigateIfNeeded(navController, R.id.customerNotificationsFragment)
        }

        btnSettings.setOnClickListener {
            navigateIfNeeded(navController, R.id.customerSettingsFragment)
        }

        bottomNav.setOnItemSelectedListener { item ->
            navigateFromBottom(navController, item.itemId, force = false)
            true
        }

        bottomNav.setOnItemReselectedListener { item ->
            navigateFromBottom(navController, item.itemId, force = true)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val customerShellDestinations = setOf(
                R.id.customerHomeFragment,
                R.id.menuFragment,
                R.id.ordersFragment,
                R.id.customerFavouritesFragment,
                R.id.customerRewardsFragment,
                R.id.customerInboxFragment,
                R.id.customerNotificationsFragment,
                R.id.customerSettingsFragment,
                R.id.cartFragment
            )

            val showShell = destination.id in customerShellDestinations
            topBar.visibility = if (showShell) View.VISIBLE else View.GONE
            bottomNav.visibility = if (showShell) View.VISIBLE else View.GONE

            if (destination.id in bottomMenuDestinations) {
                bottomNav.menu.findItem(destination.id)?.isChecked = true
            } else {
                clearBottomSelection(bottomNav)
            }

            if (showShell) {
                startBadgeObserversIfNeeded()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBadgeObserversIfNeeded()
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

    private fun startBadgeObserversIfNeeded() {
        val customerId = SessionManager.currentCustomerId

        val cartBadge = findViewById<TextView>(R.id.tvCustomerCartBadge)
        val inboxBadge = findViewById<TextView>(R.id.tvCustomerInboxBadge)
        val notificationBadge = findViewById<TextView>(R.id.tvCustomerNotificationBadge)

        if (customerId == null) {
            observedCustomerId = null
            cartBadgeJob?.cancel()
            inboxBadgeJob?.cancel()
            notificationBadgeJob?.cancel()
            cartBadge.visibility = View.GONE
            inboxBadge.visibility = View.GONE
            notificationBadge.visibility = View.GONE
            return
        }

        if (observedCustomerId == customerId) return
        observedCustomerId = customerId

        cartBadgeJob?.cancel()
        inboxBadgeJob?.cancel()
        notificationBadgeJob?.cancel()

        val db = KoffeeCraftDatabase.getInstance(applicationContext)

        cartBadgeJob = lifecycleScope.launch {
            CartManager.itemCount.collect { count ->
                if (count > 0) {
                    cartBadge.visibility = View.VISIBLE
                    cartBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    cartBadge.visibility = View.GONE
                }
            }
        }

        inboxBadgeJob = lifecycleScope.launch {
            db.inboxMessageDao().observeUnreadCountForCustomer(customerId).collect { count ->
                if (count > 0) {
                    inboxBadge.visibility = View.VISIBLE
                    inboxBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    inboxBadge.visibility = View.GONE
                }
            }
        }

        notificationBadgeJob = lifecycleScope.launch {
            db.notificationDao().observeUnreadCustomerCount(customerId).collect { count ->
                if (count > 0) {
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    notificationBadge.visibility = View.GONE
                }
            }
        }
    }
}