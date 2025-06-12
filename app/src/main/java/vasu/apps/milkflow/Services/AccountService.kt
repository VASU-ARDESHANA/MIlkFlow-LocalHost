package vasu.apps.milkflow.Services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.User
import io.appwrite.services.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.Constant.Constant

class AccountService(client: Client) {

    private val account = Account(client)

    suspend fun getLoggedIn(): User<Map<String, Any>>? {
        return try {
            account.get()
        } catch (e: AppwriteException) {
            Log.e("AccountService", e.message.toString())
            throw e
        }
    }

    suspend fun login(email: String, password: String): User<Map<String, Any>>? {
        return try {
            account.createEmailPasswordSession(email, password)
            getLoggedIn()
        } catch (e: AppwriteException) {
            Log.e("AccountService", e.message.toString())
            throw e
        }
    }

    suspend fun updateName(name: String): User<Map<String, Any>> {
        return try {

            getLoggedIn()
            account.updateName(name)

        } catch (e: AppwriteException) {
            Log.e("AccountService", e.message.toString())
            throw e
        }
    }

    suspend fun passwordUpdate(oldPassword: String, newPassword: String) {
        return try {
            getLoggedIn()
            account.updatePassword(newPassword, oldPassword)
            setUserPrefs(passwordUpdated = "yes")
        } catch (e: AppwriteException) {
            Log.e("AccountService", e.message.toString())
            throw e
        }
    }

    suspend fun setUserPrefs(profileCreated: String? = null, passwordUpdated: String? = null, profileUpdated: String? = null, productSet: String? = null, pushTargetId: String? = null) {
        val user = try {
            account.get()
        } catch (e: AppwriteException) {
            Log.e("AccountService", "setUserPrefs (get user): ${e.message}")
            throw e
        }

        val currentPrefs = user.prefs.data

        val prefs = mutableMapOf<String, Any>()

        profileCreated?.let { prefs["profileCreated"] = it }
            ?: currentPrefs["profileCreated"]?.let { prefs["profileCreated"] = it }
        passwordUpdated?.let { prefs["passwordUpdated"] = it }
            ?: currentPrefs["passwordUpdated"]?.let { prefs["passwordUpdated"] = it }
        profileUpdated?.let { prefs["profileUpdated"] = it }
            ?: currentPrefs["profileUpdated"]?.let { prefs["profileUpdated"] = it }
        productSet?.let { prefs["productSet"] = it }
            ?: currentPrefs["productSet"]?.let { prefs["productSet"] = it }
        pushTargetId?.let { prefs["pushTargetId"] = it }
            ?: currentPrefs["pushTargetId"]?.let { prefs["pushTargetId"] = it }

        if (prefs.isNotEmpty()) {
            try {
                account.updatePrefs(prefs)
            } catch (e: AppwriteException) {
                Log.e("AccountService", "setUserPrefs (update): ${e.message}")
                throw e
            }
        }
    }

    suspend fun sendEmailVerification() {
        try {

            getLoggedIn()
            val result = account.createVerification(
                url = "https://verification.flareups.in/",
            )
            Log.d("AccountService", "sendEmailVerification: $result")

        } catch (e: AppwriteException) {
            Log.e("AccountService", e.message.toString())
            throw e
        }
    }

    suspend fun registerPushTarget(): String {
        return try {
            val token = withContext(Dispatchers.IO) {
                FirebaseMessaging.getInstance().token.await()
            }

            val target = account.createPushTarget(targetId = ID.unique(), identifier = token, providerId = Constant.GLOBAL_MSG_FCM)

            Log.e("AccountService", target.toString())
            setUserPrefs(pushTargetId = target.id)

            target.id
        } catch (e: AppwriteException) {
            if (e.message?.contains("already exists", ignoreCase = true) == true) {
                Log.w("AccountService", "Push target already exists. Skipping creation.")
                throw e
            } else {
                Log.e("AccountService", "Failed to register push target: ${e.message}")
                throw e
            }
        }
    }

    suspend fun removePushTargetIfExists() {
        try {
            val user = account.get()
            val prefs = user.prefs.data
            val targetId = prefs["pushTargetId"]?.toString()

            if (!targetId.isNullOrEmpty()) {
                account.deletePushTarget(targetId)
                Log.d("AccountService", "Push target removed: $targetId")
                setUserPrefs(pushTargetId = "")
            }
        } catch (e: AppwriteException) {
            Log.e("AccountService", "Failed to remove push target: ${e.message}")
            throw e
        }
    }

    suspend fun logout() {
        account.deleteSession("current")
    }
}