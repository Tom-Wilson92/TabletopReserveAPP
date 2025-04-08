package com.example.tabletopreserve

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationManager {
    companion object {
        private const val TAG = "NotificationManager"

        /**
         * Request notification permission and register the token
         */
        suspend fun initializeNotifications(context: Context) = withContext(Dispatchers.IO) {
            try {
                // Get FCM token
                val token = FirebaseMessaging.getInstance().token.await()

                // Save token to Firestore
                saveTokenToFirestore(token)

                // Save token locally
                SharedPreferenceHelper.saveFcmToken(context, token)

                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize notifications", e)
                return@withContext false
            }
        }

        /**
         * Track when a user opens a notification
         */
        suspend fun trackNotificationOpen(notificationId: String) = withContext(Dispatchers.IO) {
            try {
                val functions = Firebase.functions
                val data = hashMapOf(
                    "notificationId" to notificationId
                )

                functions
                    .getHttpsCallable("trackNotificationOpen")
                    .call(data)
                    .await()

                Log.d(TAG, "Notification open tracked: $notificationId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking notification open", e)
                return@withContext false
            }
        }

        /**
         * Follow a shop to receive notifications
         */
        suspend fun followShop(shopId: String) = withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false

            try {
                Log.d(TAG, "Following shop with ID: $shopId for user: $userId")

                // Update user's followed shops list - using set with merge instead of update
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .set(
                        mapOf("followedShops" to FieldValue.arrayUnion(shopId)),
                        SetOptions.merge()
                    )
                    .await()

                // Create or update token document
                FirebaseFirestore.getInstance()
                    .collection("UserTokens")
                    .document(userId)
                    .set(
                        mapOf(
                            "userId" to userId,
                            "following" to FieldValue.arrayUnion(shopId),
                            "enabled" to true,
                            "platform" to "android",
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    .await()

                Log.d(TAG, "Shop followed successfully: $shopId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error following shop", e)
                Log.e(TAG, "Exception details: ${e.message}, ${e.cause}")
                return@withContext false
            }
        }


        /**
         * Unfollow a shop to stop receiving notifications
         */
        suspend fun unfollowShop(shopId: String) = withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false

            try {
                // Update user's followed shops list
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .update("followedShops", FieldValue.arrayRemove(shopId))
                    .await()

                // Update token document to remove shop from following array
                FirebaseFirestore.getInstance()
                    .collection("UserTokens")
                    .document(userId)
                    .update("following", FieldValue.arrayRemove(shopId))
                    .await()

                Log.d(TAG, "Shop unfollowed: $shopId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error unfollowing shop", e)
                return@withContext false
            }
        }

        /**
         * Check if user is following a shop
         */
        suspend fun isFollowingShop(shopId: String): Boolean = withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false

            try {
                val docSnapshot = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .get()
                    .await()

                val followedShops = docSnapshot.get("followedShops") as? List<String> ?: listOf()
                return@withContext followedShops.contains(shopId)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if following shop", e)
                return@withContext false
            }
        }

        /**
         * Enable or disable notifications
         */
        suspend fun setNotificationsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false

            try {
                FirebaseFirestore.getInstance()
                    .collection("UserTokens")
                    .document(userId)
                    .update("enabled", enabled)
                    .await()

                Log.d(TAG, "Notifications enabled: $enabled")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification settings", e)
                return@withContext false
            }
        }

        /**
         * Save FCM token to Firestore
         */
        private suspend fun saveTokenToFirestore(token: String) = withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext

            val db = FirebaseFirestore.getInstance()
            val userTokenRef = db.collection("UserTokens").document(userId)

            // Get existing user data to preserve following shops
            val existingTokenDoc = userTokenRef.get().await()
            val following = if (existingTokenDoc.exists()) {
                (existingTokenDoc.get("following") as? List<String>) ?: listOf()
            } else {
                listOf()
            }

            val tokenData = hashMapOf(
                "userId" to userId,
                "fcmToken" to token,
                "enabled" to true,
                "platform" to "android",
                "following" to following,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            userTokenRef.set(tokenData).await()
            Log.d(TAG, "Token saved to Firestore")
        }
    }
}