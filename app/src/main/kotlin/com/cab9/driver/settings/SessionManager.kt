package com.cab9.driver.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.LoginConfig
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.RejectionReason
import com.cab9.driver.di.qualifiers.UserSessionPreferences
import com.cab9.driver.ui.messages.Message
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONObject
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @UserSessionPreferences private val preference: SharedPreferences,
    moshi: Moshi
) {

    companion object {
        private const val KEY_MESSAGE = "msg"
        private const val KEY_TIME = "time"

        private const val PREF_KEY_LOGIN_CONFIG = "prefKeyLoginConfig"
        private const val PREF_KEY_AUTH_USER = "prefKeyAuthUser"

        private const val PREF_KEY_AUTH_TOKEN = "prefKeyAuthToken"
        private const val PREF_KEY_AUTH_TOKEN_TYPE = "prefKeyAuthTokenType"

        private const val PREF_KEY_CLIENT_GOOGLE_API_KEY = "prefKeyClientGoogleApiKey"
        private const val PREF_KEY_TIME_ZONE = "prefKeyTimeZone"

        private const val PREF_KEY_FIREBASE_TOKEN = "prefKeyFirebaseToken"
        private const val PREF_KEY_DEVICE_ID = "prefKeyDeviceId"

        private const val PREF_KEY_SAVED_OFFER = "prefKeySavedOffer"
        private const val PREF_KEY_CANCELLED_BOOKING_ID = "prefKeyCancelledBookingId"
        private const val PREF_KEY_PERSISTENT_MESSAGES = "prefKeyPersistentMessages"

        private const val PREF_KEY_SUMUP_REFRESH_TOKEN = "prefKeySumUpRefreshToken"

        private const val PREF_KEY_CHAT_MESSAGE_COUNT = "prefKeyChatMessageCount"
        private const val PREF_KEY_CHANGE_IN_JOB_POOL = "prefKeyChangeInJobPool"

        private const val PREF_KEY_SHOW_OFFER_COMMISSION = "prefKeyShowBookingOfferCommission"
        private const val PREF_KEY_SHOW_OFFER_DESTINATION = "prefKeyShowBookingOfferDestination"
        private const val PREF_KEY_SHOW_OFFER_PICKUP = "prefKeyShowBookingOfferPickup"
        private const val PREF_KEY_SHOW_TIME_ON_PAST_BOOKING = "prefKeyShowTimeOnPastBooking"

        private const val PREF_KEY_COMPANY_CODE = "prefKeyCompanyCode"
        private const val PREF_KEY_USERNAME = "prefKeyUsername"
        private const val PREF_KEY_PASSWORD = "prefKeyPassword"
        private const val PREF_KEY_USER_ID = "prefKeyUserId"
        private const val PREF_KEY_USER_DISPLAY_NAME = "prefKeyUserDisplayName"

        private const val PREF_KEY_REJECTION_REASONS = "prefKeyRejectionReasons"
        private const val PREF_KEY_IS_LOGGED_IN = "prefKeyIsLoggedIn"
    }

    private val loginConfigAdapter: JsonAdapter<LoginConfig> =
        moshi.adapter(LoginConfig::class.java)

    private val userAdapter: JsonAdapter<Driver> =
        moshi.adapter(Driver::class.java)

    private val rejectionReasonAdapter: JsonAdapter<List<RejectionReason>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, RejectionReason::class.java))

    val username: String
        get() = preference.getString(PREF_KEY_USERNAME, null).orEmpty()

    val displayName: String
        get() = preference.getString(PREF_KEY_USER_DISPLAY_NAME, null).orEmpty()

    val password: String
        get() = preference.getString(PREF_KEY_PASSWORD, null).orEmpty()

    val companyCode: String
        get() = preference.getString(PREF_KEY_COMPANY_CODE, null).orEmpty()

    val authToken: String
        get() = preference.getString(PREF_KEY_AUTH_TOKEN, null).orEmpty()

    val fcmToken: String?
        get() = preference.getString(PREF_KEY_FIREBASE_TOKEN, null)

    val userId: String?
        get() = preference.getString(PREF_KEY_USER_ID, null)

    val loggedInUser: Driver?
        get() {
            val jsonUser = preference.getString(PREF_KEY_AUTH_USER, null)
            return if (jsonUser.isNullOrEmpty()) null else userAdapter.fromJson(jsonUser)
        }

    val loginConfig: LoginConfig?
        get() {
            val jsonLoginConfig = preference.getString(PREF_KEY_LOGIN_CONFIG, null)
            return if (jsonLoginConfig.isNullOrEmpty()) null
            else loginConfigAdapter.fromJson(jsonLoginConfig)
        }

    val timeZone: ZoneId
        get() {
            val strTimeZone = loginConfig?.timeZone
            return if (!strTimeZone.isNullOrEmpty()) {
                try {
                    ZoneId.of(strTimeZone)
                } catch (ex: Exception) {
                    ZoneId.systemDefault()
                }
            } else ZoneId.systemDefault()
        }

    val rejectionReasons: List<RejectionReason>
        get() = try {
            val jsonStr = preference.getString(PREF_KEY_REJECTION_REASONS, null)
            if (jsonStr.isNullOrEmpty()) mutableListOf()
            else rejectionReasonAdapter.fromJson(jsonStr).orEmpty()
        } catch (ex: Exception) {
            mutableListOf()
        }

    val isLoginCredentialAvailable: Boolean
        get() = username.isNotEmpty()

    val unreadChatMessageCount: Int
        get() = preference.getInt(PREF_KEY_CHAT_MESSAGE_COUNT, 0)

    val googleApiKey: String?
        get() = preference.getString(PREF_KEY_CLIENT_GOOGLE_API_KEY, null)

    val isOfferCommissionVisibilityEnabled: Boolean
        get() = preference.getBoolean(PREF_KEY_SHOW_OFFER_COMMISSION, true)

    val isOfferDropVisibilityEnabled: Boolean
        get() = preference.getBoolean(PREF_KEY_SHOW_OFFER_DESTINATION, true)

    val isOfferPickupVisibilityEnabled: Boolean
        get() = preference.getBoolean(PREF_KEY_SHOW_OFFER_PICKUP, true)

    val isTimeOnPastBookingEnabled: Boolean
        get() = preference.getBoolean(PREF_KEY_SHOW_TIME_ON_PAST_BOOKING, true)

    val recentCancelledBookingId: String?
        get() = preference.getString(PREF_KEY_CANCELLED_BOOKING_ID, null)

    val savedPersistentMessages: List<Message>
        get() {
            val savedMessages = preference.getStringSet(PREF_KEY_PERSISTENT_MESSAGES, null)
            return if (savedMessages.isNullOrEmpty()) emptyList()
            else {
                buildList {
                    savedMessages.map { msg ->
                        try {
                            val jsonObj = JSONObject(msg)
                            this.add(
                                Message(
                                    jsonObj.getString(KEY_MESSAGE),
                                    jsonObj.getString(KEY_TIME)
                                )
                            )
                        } catch (ex: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }

    val deviceId: String?
        get() = preference.getString(PREF_KEY_DEVICE_ID, null)

    var sumUpRefreshToken: String?
        get() = preference.getString(PREF_KEY_SUMUP_REFRESH_TOKEN, null)
        set(value) = preference.edit().putString(PREF_KEY_SUMUP_REFRESH_TOKEN, value).apply()

    var isJobPoolUpdated: Boolean
        get() = preference.getBoolean(PREF_KEY_CHANGE_IN_JOB_POOL, false)
        set(value) = preference.edit().putBoolean(PREF_KEY_CHANGE_IN_JOB_POOL, value).apply()

    var isUserLoggedIn: Boolean
        get() = preference.getBoolean(PREF_KEY_IS_LOGGED_IN, false)
        set(value) = preference.edit().putBoolean(PREF_KEY_IS_LOGGED_IN, value).apply()

    /**
     * Use this to create new login session
     */
    fun createLoginSession(
        newCompanyCode: String,
        newUsername: String,
        newPassword: String,
        newConfig: LoginConfig,
        newUser: Driver,
        newFcmToken: String,
        newDeviceId: String?
    ) {
        // Clear any existing values saved
        preference.edit().clear().apply()
        // Store all latest changes
        preference.edit {
            putString(PREF_KEY_LOGIN_CONFIG, loginConfigAdapter.toJson(newConfig))
            putString(PREF_KEY_AUTH_TOKEN, newConfig.accessToken)
            putString(PREF_KEY_AUTH_TOKEN_TYPE, newConfig.tokenType)
            putString(PREF_KEY_TIME_ZONE, newConfig.timeZone)

            putString(PREF_KEY_FIREBASE_TOKEN, newFcmToken)
            putString(PREF_KEY_DEVICE_ID, newDeviceId)

            putString(PREF_KEY_AUTH_USER, userAdapter.toJson(newUser))
            putString(PREF_KEY_USERNAME, newUsername)
            putString(PREF_KEY_PASSWORD, newPassword)
            putString(PREF_KEY_COMPANY_CODE, newCompanyCode)

            putString(PREF_KEY_USER_ID, newUser.id)
            putString(PREF_KEY_USER_DISPLAY_NAME, newUser.displayName)
            putString(PREF_KEY_CLIENT_GOOGLE_API_KEY, newConfig.googleApiKey)

            putBoolean(PREF_KEY_IS_LOGGED_IN, true)
        }
    }

    /**
     * Use this to update current logged in user detail.
     */
    fun updateUserDetail(updatedUser: Driver) {
        preference.edit()
            .putString(PREF_KEY_AUTH_USER, userAdapter.toJson(updatedUser))
            .apply()
    }

    /**
     * Use this to update current logged in user config
     */
    fun updateLoginConfig(updatedConfig: LoginConfig) {
        preference.edit {
            putString(PREF_KEY_LOGIN_CONFIG, loginConfigAdapter.toJson(updatedConfig))
            putString(PREF_KEY_AUTH_TOKEN, updatedConfig.accessToken)
            putString(PREF_KEY_AUTH_TOKEN_TYPE, updatedConfig.tokenType)
        }
    }

    /**
     * Use this to update Firebase token. This token will later be used
     * at the time to logout to clear it from server.
     */
    fun updateFirebaseToken(newFcmToken: String, deviceId: String?) {
        preference.edit {
            putString(PREF_KEY_FIREBASE_TOKEN, newFcmToken)
            putString(PREF_KEY_DEVICE_ID, deviceId)
        }
    }

    fun updatePassword(newPassword: String) {
        Timber.w("New Password updated".uppercase())
        preference.edit().putString(PREF_KEY_PASSWORD, newPassword).apply()
    }

    /**
     * Update [MobileState] quick settings.
     */
    fun updateClientSettings(mobileState: MobileState) {
        preference.edit {
            putBoolean(PREF_KEY_SHOW_OFFER_PICKUP, mobileState.isPickupOnOfferEnabled ?: true)
            putBoolean(
                PREF_KEY_SHOW_OFFER_DESTINATION,
                mobileState.isDestinationOnOfferEnabled ?: true
            )
            putBoolean(
                PREF_KEY_SHOW_OFFER_COMMISSION,
                mobileState.isCommissionOnOfferEnabled ?: true
            )
            putBoolean(
                PREF_KEY_SHOW_TIME_ON_PAST_BOOKING,
                mobileState.isTimeOnLateBookingEnabled ?: true
            )
        }
    }

    /**
     * Clear any saved booking offers.
     */
    fun clearSavedBookingOfferData() {
        if (preference.contains(PREF_KEY_SAVED_OFFER)) {
            preference.edit().remove(PREF_KEY_SAVED_OFFER).apply()
        }
    }

    /**
     * Use this to save any new persistent message notifications.
     */
    fun saveNewPersistentMessage(message: String) {
        if (message.isEmpty()) return
        try {
            val jsonObj = JSONObject()
            jsonObj.put(KEY_MESSAGE, message)
            jsonObj.put(KEY_TIME, DateTimeFormatter.ofPattern("HH:mm").format(OffsetDateTime.now()))
            val savedMessages = preference.getStringSet(PREF_KEY_PERSISTENT_MESSAGES, null)
            val tempMessages = LinkedHashSet(savedMessages.orEmpty())
            tempMessages.add(jsonObj.toString())
            preference.edit().putStringSet(PREF_KEY_PERSISTENT_MESSAGES, tempMessages).apply()
        } catch (ex: Exception) {
            Timber.e(ex, "Unable to save persistent message!")
        }
    }

    fun updateRejectionReasons(jsonStr: String?) {
        preference.edit().putString(PREF_KEY_REJECTION_REASONS, jsonStr).apply()
    }

    /**
     * Clears all saved persistent messages.
     */
    fun clearAllPersistentMessages() {
        if (preference.contains(PREF_KEY_PERSISTENT_MESSAGES)) {
            preference.edit().remove(PREF_KEY_PERSISTENT_MESSAGES).apply()
        }
    }

    /**
     * Save most recent cancelled booking id when app is background.
     */
    fun saveCancelledBookingId(bookingId: String) {
        if (bookingId.isEmpty()) return
        preference.edit()
            .putString(PREF_KEY_CANCELLED_BOOKING_ID, bookingId)
            .apply()
    }

    /**
     * Clear cancelled booking id.
     */
    fun clearCancelledBookingIds() {
        if (preference.contains(PREF_KEY_CANCELLED_BOOKING_ID)) {
            preference.edit().remove(PREF_KEY_CANCELLED_BOOKING_ID).apply()
        }
    }

    /**
     * Use this to increment unread chat message count by 1.
     */
    fun incrementMessageCount() {
        preference.edit()
            .putInt(PREF_KEY_CHAT_MESSAGE_COUNT, unreadChatMessageCount + 1)
            .apply()
    }

    /**
     * Reset unread chat message count.
     */
    fun resetChatMessageCount() {
        preference.edit()
            .putInt(PREF_KEY_CHAT_MESSAGE_COUNT, 0)
            .apply()
    }
}