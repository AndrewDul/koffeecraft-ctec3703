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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var observedCustomerId: Long? = null
    private var cartBadgeJob: Job? = null
    private var inboxBadgeJob: Job? = null
    private var notificationBadgeJob: Job? = null
    private var promoObserverJob: Job? = null

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CartManager.attachContext(applicationContext)
        val rememberedSession = RememberedSessionStore.restoreIntoMemory(applicationContext)

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
        navController = navHost.navController

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

        if (savedInstanceState == null) {
            bootstrapRememberedSession(rememberedSession)
        } else {
            handleDeepLinkIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        startBadgeObserversIfNeeded()
    }

    private fun bootstrapRememberedSession(
        rememberedSession: RememberedSessionStore.RememberedSession?
    ) {
        if (rememberedSession == null) return

        val db = KoffeeCraftDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            when (rememberedSession.role) {
                RememberedSessionStore.Role.ADMIN -> {
                    val admin = withContext(Dispatchers.IO) {
                        db.adminDao().getById(rememberedSession.userId)
                    }

                    if (admin == null || !admin.isActive) {
                        SessionManager.clear()
                        RememberedSessionStore.clear(applicationContext)
                        CartManager.clearInMemoryOnly()
                        return@launch
                    }

                    SessionManager.setAdmin(admin.adminId)
                    CartManager.clearInMemoryOnly()

                    startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                    finish()
                }

                RememberedSessionStore.Role.CUSTOMER -> {
                    val customer = withContext(Dispatchers.IO) {
                        db.customerDao().getById(rememberedSession.userId)
                    }

                    if (customer == null || !customer.isActive) {
                        SessionManager.clear()
                        RememberedSessionStore.clear(applicationContext)
                        CartManager.clearInMemoryOnly()
                        return@launch
                    }

                    SessionManager.setCustomer(customer.customerId)

                    withContext(Dispatchers.IO) {
                        CartManager.restorePersistedCart(
                            context = applicationContext,
                            customerId = customer.customerId,
                            db = db
                        )
                    }

                    if (rememberedSession.onboardingPending) {
                        navController.navigate(
                            R.id.onboardingFragment,
                            bundleOf("customerId" to customer.customerId),
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
            }
        }
    }

    private fun handleDeepLinkIntent(
        sourceIntent: Intent?,
        consumeIntent: Boolean = true
    ): Boolean {
        if (sourceIntent == null) return false
        if (SessionManager.isAdmin) return false

        val customerId = SessionManager.currentCustomerId ?: return false
        val target = sourceIntent.getStringExtra(NotificationHelper.EXTRA_LAUNCH_TARGET) ?: return false
        val db = KoffeeCraftDatabase.getInstance(applicationContext)

        when (target) {
            NotificationHelper.TARGET_ORDER_STATUS -> {
                val orderId = sourceIntent.getLongExtra(NotificationHelper.EXTRA_ORDER_ID, -1L)
                if (orderId <= 0L) return false

                lifecycleScope.launch(Dispatchers.IO) {
                    db.notificationDao().markCustomerOrderNotificationsAsRead(customerId, orderId)
                }

                navController.navigate(
                    R.id.orderStatusFragment,
                    bundleOf(
                        "orderId" to orderId,
                        "simulate" to false
                    ),
                    navOptions {
                        launchSingleTop = true
                        popUpTo(R.id.welcomeFragment) {
                            inclusive = true
                        }
                    }
                )

                if (consumeIntent) clearNotificationIntentExtras(sourceIntent)
                return true
            }

            NotificationHelper.TARGET_CUSTOMER_INBOX -> {
                val inboxMessageId = sourceIntent.getLongExtra(NotificationHelper.EXTRA_INBOX_MESSAGE_ID, -1L)

                navController.navigate(
                    R.id.customerInboxFragment,
                    bundleOf("launchInboxMessageId" to inboxMessageId),
                    navOptions {
                        launchSingleTop = true
                        popUpTo(R.id.welcomeFragment) {
                            inclusive = true
                        }
                    }
                )

                if (consumeIntent) clearNotificationIntentExtras(sourceIntent)
                return true
            }

            else -> return false
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
            promoObserverJob?.cancel()
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
        promoObserverJob?.cancel()

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

        promoObserverJob = lifecycleScope.launch {
            db.inboxMessageDao().observeInboxForCustomer(customerId).collect { items ->
                items.filter { !it.isRead && it.deliveryType.startsWith("PROMO") }
                    .forEach { message ->
                        if (!NotificationHelper.wasPromoMessageDelivered(applicationContext, message.inboxMessageId)) {
                            NotificationHelper.showPromoNotification(
                                context = applicationContext,
                                title = message.title,
                                message = buildPromoPreview(message.body),
                                notificationId = 600000 + (message.inboxMessageId % 50000).toInt(),
                                inboxMessageId = message.inboxMessageId
                            )
                            NotificationHelper.markPromoMessageDelivered(
                                context = applicationContext,
                                inboxMessageId = message.inboxMessageId
                            )
                        }
                    }
            }
        }
    }

    private fun buildPromoPreview(body: String): String {
        val singleLine = body.replace("\n", " ").trim()
        return if (singleLine.length <= 100) {
            singleLine
        } else {
            singleLine.take(97).trimEnd() + "..."
        }
    }
}