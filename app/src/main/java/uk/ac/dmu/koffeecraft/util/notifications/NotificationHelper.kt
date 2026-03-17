package uk.ac.dmu.koffeecraft.util.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uk.ac.dmu.koffeecraft.MainActivity
import uk.ac.dmu.koffeecraft.R

object NotificationHelper {

    private const val ORDER_CHANNEL_ID = "koffeecraft_orders"
    private const val ORDER_CHANNEL_NAME = "Order Updates"
    private const val ORDER_CHANNEL_DESC = "Notifications about order status and payments"

    private const val PROMO_CHANNEL_ID = "koffeecraft_promotions"
    private const val PROMO_CHANNEL_NAME = "Offers & Promotions"
    private const val PROMO_CHANNEL_DESC = "Notifications about promotions, offers, and inbox campaigns"

    private const val PREFS_NAME = "koffeecraft_notification_helper"
    private const val KEY_PROMO_DELIVERED_PREFIX = "promo_delivered_"

    const val EXTRA_LAUNCH_TARGET = "launch_target"
    const val EXTRA_ORDER_ID = "launch_order_id"
    const val EXTRA_INBOX_MESSAGE_ID = "launch_inbox_message_id"

    const val TARGET_ORDER_STATUS = "order_status"
    const val TARGET_CUSTOMER_INBOX = "customer_inbox"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val orderChannel = NotificationChannel(
            ORDER_CHANNEL_ID,
            ORDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ORDER_CHANNEL_DESC
        }

        val promoChannel = NotificationChannel(
            PROMO_CHANNEL_ID,
            PROMO_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = PROMO_CHANNEL_DESC
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(orderChannel)
        manager.createNotificationChannel(promoChannel)
    }

    fun showCustomerOrderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        orderId: Long
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val builder = NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(
                buildMainActivityPendingIntent(
                    context = context,
                    requestCode = 100000 + (orderId % 50000).toInt(),
                    target = TARGET_ORDER_STATUS,
                    orderId = orderId,
                    inboxMessageId = null
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        safelyNotify(context, notificationId, builder)
    }

    fun showAdminOrderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val builder = NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        safelyNotify(context, notificationId, builder)
    }

    fun showPromoNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        inboxMessageId: Long
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val builder = NotificationCompat.Builder(context, PROMO_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(
                buildMainActivityPendingIntent(
                    context = context,
                    requestCode = 200000 + (inboxMessageId % 50000).toInt(),
                    target = TARGET_CUSTOMER_INBOX,
                    orderId = null,
                    inboxMessageId = inboxMessageId
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        safelyNotify(context, notificationId, builder)
    }

    fun wasPromoMessageDelivered(
        context: Context,
        inboxMessageId: Long
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PROMO_DELIVERED_PREFIX$inboxMessageId", false)
    }

    fun markPromoMessageDelivered(
        context: Context,
        inboxMessageId: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("$KEY_PROMO_DELIVERED_PREFIX$inboxMessageId", true)
            .apply()
    }

    private fun safelyNotify(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignore notification delivery when permission is not granted yet.
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildMainActivityPendingIntent(
        context: Context,
        requestCode: Int,
        target: String,
        orderId: Long?,
        inboxMessageId: Long?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_LAUNCH_TARGET, target)
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            inboxMessageId?.let { putExtra(EXTRA_INBOX_MESSAGE_ID, it) }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}