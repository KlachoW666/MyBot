package com.fuelmap.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

/** Хранит id текущего пользователя между запусками (автологин). */
class SessionManager(private val context: Context) {

    private val userIdKey = longPreferencesKey("current_user_id")
    private val osmImportedKey = booleanPreferencesKey("osm_imported")

    val currentUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[userIdKey]?.takeIf { it > 0 }
    }

    suspend fun isOsmImported(): Boolean =
        context.dataStore.data.first()[osmImportedKey] ?: false

    suspend fun setOsmImported(value: Boolean) {
        context.dataStore.edit { it[osmImportedKey] = value }
    }

    suspend fun signIn(userId: Long) {
        context.dataStore.edit { it[userIdKey] = userId }
    }

    suspend fun signOut() {
        context.dataStore.edit { it.remove(userIdKey) }
    }
}
