package com.example.tabletopreserve

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TableTopFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TableTopFMS"
        private const val CHANNEL_ID = "tabletop_notifications"
        private const val CHANNEL_NAME = "Shop Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications from tabletop game shops"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            // Get data payload
            val data = remoteMessage.data
            val notificationId = data["notificationId"] ?: ""
            val shopId = data["shopId"] ?: ""
            val shopName = data["shopName"] ?: "Shop"
            val notificationType = data["type"] ?: "update"

            // Show notification
            sendNotification(
                notification.title ?: "New Update",
                notification.body ?: "",
                notificationId,
                shopId,
                notificationType
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Save new token to Firestore
        saveTokenToFirestore(token)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        notificationId: String,
        shopId: String,
        notificationType: String
    ) {
        val uniqueId = System.currentTimeMillis().toInt()

        // Create intent to open appropriate activity based on notification type
        val intent = when (notificationType) {
            "event" -> Intent(this, MainActivity::class.java).apply {
                putExtra("shop_id", shopId)
                putExtra("notification_id", notificationId)
                putExtra("open_tab", "events")
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            "promo" -> Intent(this, MainActivity::class.java).apply {
                putExtra("shop_id", shopId)
                putExtra("notification_id", notificationId)
                putExtra("open_tab", "discover")
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            else -> Intent(this, MainActivity::class.java).apply {
                putExtra("shop_id", shopId)
                putExtra("notification_id", notificationId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        // Create pending intent
        val pendingIntent = PendingIntent.getActivity(
            this, uniqueId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show notification
        notificationManager.notify(uniqueId, notificationBuilder.build())

        // Track notification open in background
        if (notificationId.isNotEmpty()) {
            trackNotificationDelivery(notificationId)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = getCurrentUserId() ?: return

        val db = FirebaseFirestore.getInstance()
        val tokenData = hashMapOf(
            "userId" to userId,
            "fcmToken" to token,
            "enabled" to true,
            "platform" to "android",
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("UserTokens").document(userId)
            .set(tokenData)
            .addOnSuccessListener {
                Log.d(TAG, "Token saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving token to Firestore", e)
            }
    }

    private fun trackNotificationDelivery(notificationId: String) {
        try {
            // Here we would ideally update the delivered count in Firestore
            // However, this is handled by the Cloud Function when sending the notification
            Log.d(TAG, "Notification delivered: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking notification delivery", e)
        }
    }

    private fun getCurrentUserId(): String? {
        return SharedPreferenceHelper.getCurrentUserId(this)
    }
}