package com.example.tabletopreserve

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service for handling account deletion and cleaning up all associated user data
 */
class AccountDeletionService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "AccountDeletionService"

        // Collection names
        private const val USERS_COLLECTION = "Users"
        private const val RESERVATIONS_COLLECTION = "Reservations"
        private const val USER_FAVORITES_COLLECTION = "UserFavorites"
    }

    /**
     * Delete the current user's account and all associated data
     * @param onSuccess Callback when the deletion is successful
     * @param onError Callback when an error occurs during deletion
     */
    suspend fun deleteUserAccount(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("No user is currently logged in")
            val userId = currentUser.uid

            Log.d(TAG, "Starting account deletion for user: $userId")

            // 1. Delete all reservations made by the user
            deleteUserReservations(userId)

            // 2. Delete user favorites
            deleteUserFavorites(userId)

            // 3. Delete user profile data
            deleteUserProfile(userId)

            // 4. Finally delete the Firebase Auth account
            currentUser.delete().await()

            Log.d(TAG, "Account deletion completed successfully for user: $userId")

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    /**
     * Delete all reservations made by the user
     */
    private suspend fun deleteUserReservations(userId: String) = withContext(Dispatchers.IO) {
        try {
            // Get all reservations for this user
            val reservations = db.collection(RESERVATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d(TAG, "Found ${reservations.size()} reservations to delete")

            // Delete each reservation
            val batch = db.batch()
            reservations.documents.forEach { document ->
                batch.delete(document.reference)
            }

            if (reservations.size() > 0) {
                batch.commit().await()
                Log.d(TAG, "Successfully deleted user reservations")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user reservations: ${e.message}", e)
            throw e
        }
    }

    /**
     * Delete user favorites
     */
    private suspend fun deleteUserFavorites(userId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete the user's favorites document if it exists
            val userFavoritesDoc = db.collection(USER_FAVORITES_COLLECTION).document(userId)
            if (userFavoritesDoc.get().await().exists()) {
                userFavoritesDoc.delete().await()
                Log.d(TAG, "Successfully deleted user favorites")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user favorites: ${e.message}", e)
            throw e
        }
    }

    /**
     * Delete user profile data
     */
    private suspend fun deleteUserProfile(userId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete the user document
            db.collection(USERS_COLLECTION).document(userId).delete().await()
            Log.d(TAG, "Successfully deleted user profile")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user profile: ${e.message}", e)
            throw e
        }
    }
}