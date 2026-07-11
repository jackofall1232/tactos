package dev.tactos.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User-facing app settings. Defaults are the privacy-safe out-of-box
 * experience: no capture before onboarding consent, nothing auto-deleted
 * until the user opts into retention limits.
 */
data class TactosSettings(
    val onboardingComplete: Boolean = false,
    /** Save whatever is on the clipboard whenever tactos gains focus. Opt-in. */
    val captureOnFocus: Boolean = false,
    /** Auto-delete clips older than this many days; 0 keeps everything. */
    val retentionDays: Int = 0,
    /** Keep at most this many unpinned clips; 0 means unlimited. */
    val retentionMaxItems: Int = 0,
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** DataStore-backed persistence for [TactosSettings]. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<TactosSettings> = context.settingsDataStore.data.map { prefs ->
        TactosSettings(
            onboardingComplete = prefs[KEY_ONBOARDING_COMPLETE] ?: false,
            captureOnFocus = prefs[KEY_CAPTURE_ON_FOCUS] ?: false,
            retentionDays = prefs[KEY_RETENTION_DAYS] ?: 0,
            retentionMaxItems = prefs[KEY_RETENTION_MAX_ITEMS] ?: 0,
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_ONBOARDING_COMPLETE] = value }
    }

    suspend fun setCaptureOnFocus(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_CAPTURE_ON_FOCUS] = value }
    }

    suspend fun setRetentionDays(value: Int) {
        require(value >= 0) { "retentionDays must be >= 0" }
        context.settingsDataStore.edit { it[KEY_RETENTION_DAYS] = value }
    }

    suspend fun setRetentionMaxItems(value: Int) {
        require(value >= 0) { "retentionMaxItems must be >= 0" }
        context.settingsDataStore.edit { it[KEY_RETENTION_MAX_ITEMS] = value }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_CAPTURE_ON_FOCUS = booleanPreferencesKey("capture_on_focus")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
        val KEY_RETENTION_MAX_ITEMS = intPreferencesKey("retention_max_items")
    }
}
