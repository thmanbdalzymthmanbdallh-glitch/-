package com.example.notifications

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
import com.example.MainActivity
import com.example.R
import com.example.ROUTE_ORDERS
import com.example.data.model.OrderStatus

object OrderNotificationHelper {
    const val EXTRA_NAVIGATE_ROUTE = "navigate_route"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = context.getString(R.string.default_notification_channel_id)
            val channelName = context.getString(R.string.default_notification_channel_name)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند تحديث حالة طلباتك (التأكيد، الشحن، والتسليم)"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun statusNotificationCopy(productTitle: String, status: OrderStatus): Pair<String, String> {
        val cleanTitle = productTitle.trim().ifBlank { "طلبك" }
        return when (status) {
            OrderStatus.CONFIRMED ->
                "تم تأكيد طلبك" to "تأكّد طلبك لمنتج «$cleanTitle» ويجري تجهيزه للشحن."
            OrderStatus.SHIPPED ->
                "تم شحن طلبك" to "طلبك لمنتج «$cleanTitle» أصبح في الطريق إليك الآن."
            OrderStatus.DELIVERED ->
                "تم تسليم طلبك" to "تم تسليم طلبك لمنتج «$cleanTitle». نتمنى لك تجربة رائعة!"
            OrderStatus.CANCELLED ->
                "تم إلغاء الطلب" to "تم تحديث حالة طلب «$cleanTitle» إلى ملغي."
            OrderStatus.PENDING ->
                "تحديث حالة الطلب" to "طلبك لمنتج «$cleanTitle» قيد المراجعة حالياً."
        }
    }

    fun showOrderStatusNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        navigateRoute: String = ROUTE_ORDERS
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val channelId = context.getString(R.string.default_notification_channel_id)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_ROUTE, navigateRoute)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify call.
        }
    }
}
