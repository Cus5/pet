package com.petfather.gyeonsimi.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val selectedPetId: Long?,
    val nickname: String,
    val reminderEnabled: Boolean,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            selectedPetId = p[SELECTED_PET],
            nickname = p[NICKNAME]?.takeIf { it.isNotBlank() } ?: DEFAULT_NICKNAME,
            reminderEnabled = p[REMINDER] ?: false,
        )
    }

    suspend fun current(): UserSettings = settings.first()

    suspend fun selectPet(id: Long?) {
        context.dataStore.edit { p -> if (id == null) p.remove(SELECTED_PET) else p[SELECTED_PET] = id }
    }

    suspend fun setNickname(name: String) {
        context.dataStore.edit { it[NICKNAME] = name.trim() }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER] = enabled }
    }

    /** 예시 글을 한 번만 넣기 위한 플래그. 처음 호출이면 true 를 돌려줍니다. */
    suspend fun markSamplesSeeded(): Boolean {
        var first = false
        context.dataStore.edit { p ->
            if (p[SAMPLES_SEEDED] != true) {
                first = true
                p[SAMPLES_SEEDED] = true
            }
        }
        return first
    }

    companion object {
        const val DEFAULT_NICKNAME = "보호자"
        private val SELECTED_PET = longPreferencesKey("selected_pet_id")
        private val NICKNAME = stringPreferencesKey("nickname")
        private val REMINDER = booleanPreferencesKey("reminder_enabled")
        private val SAMPLES_SEEDED = booleanPreferencesKey("samples_seeded")
    }
}
