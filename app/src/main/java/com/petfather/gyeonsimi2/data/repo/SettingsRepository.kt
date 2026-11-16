package com.petfather.gyeonsimi2.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

data class UserSettings(
    val selectedPetId: Long?,
    val nickname: String,
    val reminderEnabled: Boolean,
    /** 첫 실행 때 '데이터 사용 안내'에 답했는지. 답하기 전에는 아무것도 서버로 보내지 않습니다. */
    val dataNoticeAnswered: Boolean = false,
    val usageStatsEnabled: Boolean = false,
    val shareWeights: Boolean = false,
    /** 체중 공유를 한 번이라도 켠 적이 있는지 ('보낸 기록 삭제' 메뉴 표시용) */
    val everSharedWeights: Boolean = false,
)

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = store.data.map { p ->
        UserSettings(
            selectedPetId = p[SELECTED_PET],
            nickname = p[NICKNAME]?.takeIf { it.isNotBlank() } ?: DEFAULT_NICKNAME,
            reminderEnabled = p[REMINDER] ?: false,
            dataNoticeAnswered = p[NOTICE_ANSWERED] ?: false,
            usageStatsEnabled = p[USAGE_STATS] ?: false,
            shareWeights = p[SHARE_WEIGHTS] ?: false,
            everSharedWeights = p[EVER_SHARED] ?: false,
        )
    }

    suspend fun current(): UserSettings = settings.first()

    suspend fun selectPet(id: Long?) {
        store.edit { p -> if (id == null) p.remove(SELECTED_PET) else p[SELECTED_PET] = id }
    }

    suspend fun setNickname(name: String) {
        store.edit { it[NICKNAME] = name.trim() }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[REMINDER] = enabled }
    }

    /** 첫 실행 안내에서 고른 값을 저장합니다. */
    suspend fun answerDataNotice(usageStats: Boolean, shareWeights: Boolean) {
        store.edit { p ->
            p[NOTICE_ANSWERED] = true
            p[USAGE_STATS] = usageStats
            p[SHARE_WEIGHTS] = shareWeights
            if (shareWeights) p[EVER_SHARED] = true
        }
    }

    suspend fun setUsageStatsEnabled(enabled: Boolean) {
        store.edit { it[USAGE_STATS] = enabled }
    }

    suspend fun setShareWeights(enabled: Boolean) {
        store.edit { p ->
            p[SHARE_WEIGHTS] = enabled
            if (enabled) p[EVER_SHARED] = true
        }
    }

    /** 서버에 보낸 체중 기록을 지운 뒤 호출합니다. */
    suspend fun clearWeightSharing() {
        store.edit { p ->
            p[SHARE_WEIGHTS] = false
            p[EVER_SHARED] = false
        }
    }

    /**
     * 이 설치를 구분하는 무작위 ID. 처음 필요할 때 만들어 저장합니다.
     * 기기 정보나 계정과 무관한 값이라 앱을 지우면 사라집니다.
     */
    suspend fun installId(): String {
        var id = ""
        store.edit { p ->
            id = p[INSTALL_ID] ?: UUID.randomUUID().toString().also { p[INSTALL_ID] = it }
        }
        return id
    }

    /** 예시 글을 한 번만 넣기 위한 플래그. 처음 호출이면 true 를 돌려줍니다. */
    suspend fun markSamplesSeeded(): Boolean {
        var first = false
        store.edit { p ->
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
        private val INSTALL_ID = stringPreferencesKey("install_id")
        private val NOTICE_ANSWERED = booleanPreferencesKey("data_notice_answered")
        private val USAGE_STATS = booleanPreferencesKey("usage_stats_enabled")
        private val SHARE_WEIGHTS = booleanPreferencesKey("share_weights")
        private val EVER_SHARED = booleanPreferencesKey("ever_shared_weights")

        /** 앱 전체에서 한 번만 만들어 써야 합니다 (AppContainer 가 보관). */
        fun createStore(context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    }
}
