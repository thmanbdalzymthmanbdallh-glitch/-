package com.example.notifications

import android.util.Log
import com.example.R
import com.example.ROUTE_ORDERS
import com.example.data.model.OrderStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MatjariMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val cleanToken = token.trim().take(512)
        if (cleanToken.isEmpty()) return

        val uid = Firebase.auth.currentUser?.uid ?: return
        serviceScope.launch {
            try {
                val databaseId = applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val docRef = db.collection("users").document(uid)
                val snap = docRef.get().await()
                if (snap.exists()) {
                    docRef.update(
                        mapOf(
                            "fcmToken" to cleanToken,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist refreshed FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val statusKey = message.data["status"]
        val productTitle = message.data["productTitle"] ?: message.data["product_title"]
        val (defaultTitle, defaultBody) = if (!statusKey.isNullOrBlank()) {
            OrderNotificationHelper.statusNotificationCopy(
                productTitle = productTitle.orEmpty(),
                status = OrderStatus.fromKey(statusKey)
            )
        } else {
            "تحديث طلبات متجري" to "تم تحديث حالة أحد طلباتك، اضغط للمتابعة."
        }

        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: message.data["title"]?.takeIf { it.isNotBlank() }
            ?: defaultTitle
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: message.data["body"]?.takeIf { it.isNotBlank() }
            ?: defaultBody
        val navigateRoute = message.data["navigate_route"]?.takeIf { it.isNotBlank() }
            ?: ROUTE_ORDERS

        val notificationKey = message.data["orderId"]
            ?: message.messageId
            ?: System.currentTimeMillis().toString()

        OrderNotificationHelper.showOrderStatusNotification(
            context = applicationContext,
            notificationId = notificationKey.hashCode(),
            title = title,
            body = body,
            navigateRoute = navigateRoute
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "MatjariMessaging"
    }
}
