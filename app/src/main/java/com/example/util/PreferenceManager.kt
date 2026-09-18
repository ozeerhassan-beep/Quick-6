package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Property delegate to create a DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferenceManager(private val context: Context) {
    companion object {
        val IS_FEATURES_ENABLED = booleanPreferencesKey("is_features_enabled")
    }

    val isFeaturesEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FEATURES_ENABLED] ?: true
        }

    suspend fun setFeaturesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FEATURES_ENABLED] = enabled
        }
    }
}
