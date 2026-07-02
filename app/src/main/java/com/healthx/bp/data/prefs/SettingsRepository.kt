package com.healthx.bp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "healthx_settings")

/** Default WebDAV endpoint prefilled for new installs. */
const val DEFAULT_DAV_URL = "https://webdav.pcloud.com/"

data class WebDavConfig(
    val enabled: Boolean = false,
    val url: String = DEFAULT_DAV_URL,
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val remotePath: String = "/healthx",
    val lastSyncAt: Long = 0L
)

data class AppSettings(
    val webDav: WebDavConfig = WebDavConfig(),
    val themeMode: String = "system",   // system | light | dark
    val passwordLock: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = ""
) {
    /** Lock is only effective when enabled AND a PIN has actually been set. */
    val lockActive: Boolean get() = passwordLock && pinHash.isNotBlank()
}

/** Persists app settings and WebDAV credentials via Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("dav_enabled")
        val URL = stringPreferencesKey("dav_url")
        val USER = stringPreferencesKey("dav_user")
        val PASS = stringPreferencesKey("dav_pass")
        val PORT = stringPreferencesKey("dav_port")
        val PATH = stringPreferencesKey("dav_path")
        val LAST_SYNC = longPreferencesKey("dav_last_sync")
        val THEME = stringPreferencesKey("theme_mode")
        val PWD_LOCK = booleanPreferencesKey("password_lock")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            webDav = WebDavConfig(
                enabled = p[Keys.ENABLED] ?: false,
                url = p[Keys.URL] ?: DEFAULT_DAV_URL,
                username = p[Keys.USER] ?: "",
                password = p[Keys.PASS] ?: "",
                port = p[Keys.PORT] ?: "",
                remotePath = p[Keys.PATH] ?: "/healthx",
                lastSyncAt = p[Keys.LAST_SYNC] ?: 0L
            ),
            themeMode = p[Keys.THEME] ?: "system",
            passwordLock = p[Keys.PWD_LOCK] ?: false,
            pinHash = p[Keys.PIN_HASH] ?: "",
            pinSalt = p[Keys.PIN_SALT] ?: ""
        )
    }

    suspend fun updateWebDav(config: WebDavConfig) {
        context.dataStore.edit { p ->
            p[Keys.ENABLED] = config.enabled
            p[Keys.URL] = config.url
            p[Keys.USER] = config.username
            p[Keys.PASS] = config.password
            p[Keys.PORT] = config.port
            p[Keys.PATH] = config.remotePath
            p[Keys.LAST_SYNC] = config.lastSyncAt
        }
    }

    suspend fun setLastSyncAt(ts: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = ts }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME] = mode }
    }

    suspend fun setPasswordLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PWD_LOCK] = enabled }
    }

    /** Enable the lock and store the salted hash of the new PIN. */
    suspend fun setPin(hash: String, salt: String) {
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
            it[Keys.PWD_LOCK] = true
        }
    }

    /** Disable the lock and wipe the stored PIN. */
    suspend fun clearPin() {
        context.dataStore.edit {
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
            it[Keys.PWD_LOCK] = false
        }
    }
}
