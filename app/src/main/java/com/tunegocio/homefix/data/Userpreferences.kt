package com.tunegocio.homefix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "homefix_settings")

object PreferencesKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LANGUAGE = stringPreferencesKey("language")         // "espanol", "ingles", "portgues"
    val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
    val NOTIF_VIBRATION = booleanPreferencesKey("notif_vibration")
}

class UserPreferences(private val context: Context) {

    val darkMode: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.DARK_MODE] ?: false }

    val language: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.LANGUAGE] ?: "es" }

    val notifSound: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.NOTIF_SOUND] ?: true }

    val notifVibration: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.NOTIF_VIBRATION] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[PreferencesKeys.LANGUAGE] = lang }
    }

    suspend fun setNotifSound(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIF_SOUND] = enabled }
    }

    suspend fun setNotifVibration(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIF_VIBRATION] = enabled }
    }
}