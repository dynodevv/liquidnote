package com.liquidnote.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val AI_KEY = stringPreferencesKey("ai_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
    }

    val aiEnabled: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED] != false }
    val aiEndpoint: Flow<String> = context.dataStore.data.map { it[AI_ENDPOINT] ?: "" }
    val aiKey: Flow<String> = context.dataStore.data.map { it[AI_KEY] ?: "" }
    val aiModel: Flow<String> = context.dataStore.data.map { it[AI_MODEL] ?: "" }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AI_ENABLED] = enabled }
    }

    suspend fun setAiEndpoint(endpoint: String) {
        context.dataStore.edit { it[AI_ENDPOINT] = endpoint }
    }

    suspend fun setAiKey(key: String) {
        context.dataStore.edit { it[AI_KEY] = key }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[AI_MODEL] = model }
    }
}
