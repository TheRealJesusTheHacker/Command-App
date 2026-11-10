package net.sentinelsafeguard.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The only home for secrets on the device (spec §5): the Command device
 * token, in Android Keystore-backed encrypted
 * storage. This file's preferences are excluded from every backup and
 * device transfer by res/xml/data_extraction_rules.xml and
 * res/xml/backup_rules.xml. Nothing here is ever logged.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var deviceToken: String?
        get() = prefs.getString(KEY_DEVICE_TOKEN, null)
        set(value) = putOrRemove(KEY_DEVICE_TOKEN, value)

    var displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)
        set(value) = putOrRemove(KEY_DISPLAY_NAME, value)

    val isSignedIn: Boolean get() = !deviceToken.isNullOrBlank()

    /** Sign-out wipes every secret this store holds. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun putOrRemove(key: String, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(key) else putString(key, value)
        }.apply()
    }

    companion object {
        const val FILE_NAME = "sentinel_secure_store"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
