package com.example.tabletopreserve

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SharedPreferenceHelper {
    companion object {
        private const val TAG = "SharedPrefHelper"
        private const val PREF_NAME = "tabletop_reserve_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        private fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }

        /**
         * Save FCM token to SharedPreferences
         */
        fun saveFcmToken(context: Context, token: String) {
            getPrefs(context).edit().apply {
                putString(KEY_FCM_TOKEN, token)
                apply()
            }
            Log.d(TAG, "FCM token saved to SharedPreferences")
        }

        /**
         * Get FCM token from SharedPreferences
         */
        fun getFcmToken(context: Context): String? {
            return getPrefs(context).getString(KEY_FCM_TOKEN, null)
        }

        /**
         * Save current user ID to SharedPreferences
         */
        fun saveCurrentUserId(context: Context, userId: String) {
            getPrefs(context).edit().apply {
                putString(KEY_USER_ID, userId)
                apply()
            }
            Log.d(TAG, "User ID saved to SharedPreferences")
        }

        /**
         * Get current user ID from SharedPreferences
         */
        fun getCurrentUserId(context: Context): String? {
            return getPrefs(context).getString(KEY_USER_ID, null)
        }

        /**
         * Clear user data on logout
         */
        fun clearUserData(context: Context) {
            getPrefs(context).edit().apply {
                remove(KEY_USER_ID)
                // Don't remove FCM token as we might need it for guest notifications
                apply()
            }
            Log.d(TAG, "User data cleared from SharedPreferences")
        }

        /**
         * Save notification preferences
         */
        fun setNotificationsEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().apply {
                putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                apply()
            }
            Log.d(TAG, "Notifications enabled preference set: $enabled")
        }

        /**
         * Check if notifications are enabled
         */
        fun areNotificationsEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        }
    }
}