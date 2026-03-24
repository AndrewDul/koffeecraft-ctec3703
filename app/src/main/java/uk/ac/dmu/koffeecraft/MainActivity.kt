package uk.ac.dmu.koffeecraft

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var navController: NavController

    private lateinit var topBar: View
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var btnCart: ImageButton
    private lateinit var btnInbox: ImageButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnSettings: ImageButton

    private lateinit var tvCartBadge: TextView
    private lateinit var tvInboxBadge: TextView
    private lateinit var tvNotificationBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        viewModel = ViewModelProvider(
            this,
            MainActivityViewModel.Factory(applicationContext.appContainer.mainActivityRepository)
        )[MainActivityViewModel::class.java]

        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()
        bindViews()
        setupNavigationShell()
        observeState()
        btnCart.setImageResource(R.drawable.kc_emptycart)
        btnInbox.setImageResource(R.drawable.kc_emptyinbox)
        btnNotifications.setImageResource(R.drawable.kc_nonotifications)
        btnSettings.setImageResource(R.drawable.kc_settings)

        if (savedInstanceState == null) {
            viewModel.bootstrapRememberedSession()
        } else {
            handleDeepLinkIntent(intent)
            viewModel.bindActiveCustomerBadges()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.bindActiveCustomerBadges()
    }

    private fun bindViews() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        topBar = findViewById(R.id.customerTopBar)
        bottomNav = findViewById(R.id.customerBottomNav)

        btnCart = findViewById(R.id.btnCustomerCart)
        btnInbox = findViewById(R.id.btnCustomerInbox)
        btnNotifications = findViewById(R.id.btnCustomerNotifications)
        btnSettings = findViewById(R.id.btnCustomerSettings)

        tvCartBadge = findViewById(R.id.tvCustomerCartBadge)
        tvInboxBadge = findViewById(R.id.tvCustomerInboxBadge)
        tvNotificationBadge = findViewById(R.id.tvCustomerNotificationBadge)
    }


    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupNavigationShell() {
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
                viewModel.bindActiveCustomerBadges()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        tvCartBadge.visibility = if (state.showCartBadge) View.VISIBLE else View.GONE
                        tvInboxBadge.visibility = if (state.showInboxBadge) View.VISIBLE else View.GONE
                        tvNotificationBadge.visibility =
                            if (state.showNotificationBadge) View.VISIBLE else View.GONE

                        tvCartBadge.text = state.cartBadgeText
                        tvInboxBadge.text = state.inboxBadgeText
                        tvNotificationBadge.text = state.notificationBadgeText

                        updateTopBarIcons(state)
                    }
                }

                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            MainActivityViewModel.UiEffect.LaunchAdminActivity -> {
                                startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                                finish()
                            }

                            is MainActivityViewModel.UiEffect.LaunchCustomerSession -> {
                                viewModel.bindCustomerBadges(effect.customerId)

                                if (effect.onboardingPending) {
                                    navController.navigate(
                                        R.id.onboardingFragment,
                                        bundleOf("customerId" to effect.customerId),
                                        navOptions {
                                            launchSingleTop = true
                                            popUpTo(R.id.welcomeFragment) {
                                                inclusive = true
                                            }
                                        }
                                    )
                                } else {
                                    val handled = handleDeepLinkIntent(intent, consumeIntent = false)
                                    if (!handled) {
                                        navController.navigate(
                                            R.id.customerHomeFragment,
                                            null,
                                            navOptions {
                                                launchSingleTop = true
                                                popUpTo(R.id.welcomeFragment) {
                                                    inclusive = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            is MainActivityViewModel.UiEffect.NavigateOrderStatus -> {
                                navController.navigate(
                                    R.id.orderStatusFragment,
                                    bundleOf(
                                        "orderId" to effect.orderId,
                                        "simulate" to false
                                    ),
                                    navOptions {
                                        launchSingleTop = true
                                        popUpTo(R.id.welcomeFragment) {
                                            inclusive = true
                                        }
                                    }
                                )
                            }

                            is MainActivityViewModel.UiEffect.NavigateCustomerInbox -> {
                                navController.navigate(
                                    R.id.customerInboxFragment,
                                    bundleOf("launchInboxMessageId" to effect.inboxMessageId),
                                    navOptions {
                                        launchSingleTop = true
                                        popUpTo(R.id.welcomeFragment) {
                                            inclusive = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

    }

    private fun handleDeepLinkIntent(
        sourceIntent: Intent?,
        consumeIntent: Boolean = true
    ): Boolean {
        if (sourceIntent == null) return false
        if (viewModel.isAdminSession()) return false

        val customerId = viewModel.currentCustomerId() ?: return false
        val target =
            sourceIntent.getStringExtra(NotificationHelper.EXTRA_LAUNCH_TARGET) ?: return false

        return when (target) {
            NotificationHelper.TARGET_ORDER_STATUS -> {
                val orderId = sourceIntent.getLongExtra(NotificationHelper.EXTRA_ORDER_ID, -1L)
                if (orderId <= 0L) return false

                viewModel.openOrderStatusFromNotification(customerId, orderId)

                if (consumeIntent) clearNotificationIntentExtras(sourceIntent)
                true
            }

            NotificationHelper.TARGET_CUSTOMER_INBOX -> {
                val inboxMessageId =
                    sourceIntent.getLongExtra(NotificationHelper.EXTRA_INBOX_MESSAGE_ID, -1L)

                viewModel.openCustomerInboxFromNotification(inboxMessageId)

                if (consumeIntent) clearNotificationIntentExtras(sourceIntent)
                true
            }

            else -> false
        }
    }

    private fun clearNotificationIntentExtras(intent: Intent) {
        intent.removeExtra(NotificationHelper.EXTRA_LAUNCH_TARGET)
        intent.removeExtra(NotificationHelper.EXTRA_ORDER_ID)
        intent.removeExtra(NotificationHelper.EXTRA_INBOX_MESSAGE_ID)
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

    private fun updateTopBarIcons(state: MainActivityUiState) {
        btnCart.setImageResource(
            if (state.showCartBadge) R.drawable.kc_fullcart else R.drawable.kc_emptycart
        )

        btnInbox.setImageResource(
            if (state.showInboxBadge) R.drawable.kc_fullinbox else R.drawable.kc_emptyinbox
        )

        btnNotifications.setImageResource(
            if (state.showNotificationBadge) {
                R.drawable.kc_notifications
            } else {
                R.drawable.kc_nonotifications
            }
        )

        btnSettings.setImageResource(R.drawable.kc_settings)
    }
}