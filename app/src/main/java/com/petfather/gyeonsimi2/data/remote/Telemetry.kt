package com.petfather.gyeonsimi2.data.remote

import android.os.Build
import com.petfather.gyeonsimi2.data.db.HealthDao
import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.LocalUsageEvent
import com.petfather.gyeonsimi2.data.db.Pet
import com.petfather.gyeonsimi2.data.db.PetDao
import com.petfather.gyeonsimi2.data.db.UsageEventDao
import com.petfather.gyeonsimi2.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * 익명 사용 통계, 피드백, 체중 기록 공유를 한곳에서 다룹니다.
 *
 * 지키는 규칙:
 * - 첫 실행 안내에 답하기 전에는 아무것도 서버로 보내지 않습니다.
 * - 사용 통계는 '익명 사용 통계'가 켜져 있을 때만, 체중 기록은 '체중 기록 익명 공유'에 동의했을 때만 보냅니다.
 * - 사용 이벤트는 날짜·종류마다 하루 한 번만 보냅니다 (무엇을 입력했는지는 보내지 않습니다).
 * - 서버 설정이 없는 빌드([remote] == null)에서는 아무 일도 하지 않습니다.
 */
class Telemetry(
    private val remote: RemoteApi?,
    private val settings: SettingsRepository,
    private val events: UsageEventDao,
    private val health: HealthDao,
    private val pets: PetDao,
    private val today: () -> LocalDate,
    private val appVersion: String,
    private val debug: Boolean,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    /** 서버가 설정된 빌드인지. false 면 안내·설정·의견 보내기 화면을 숨깁니다. */
    val available: Boolean get() = remote != null

    /** 화면 코드에서 부르는 진입점. 기다리지 않고 뒤에서 기록·전송합니다. */
    fun track(name: UsageEventName) {
        if (remote == null) return
        scope.launch {
            record(name)
            flush()
        }
    }

    /** 뒤에서 밀린 전송을 처리합니다 (앱을 열 때, 설정을 바꿨을 때). */
    fun flushInBackground() {
        if (remote == null) return
        scope.launch { flush() }
    }

    suspend fun record(name: UsageEventName) {
        if (remote == null) return
        val s = settings.current()
        // 안내에 답하기 전에는 기기에만 적어 두고, 통계를 끈 뒤에는 적지도 않습니다.
        if (s.dataNoticeAnswered && !s.usageStatsEnabled) return
        events.insert(LocalUsageEvent(day = today(), name = name.wire))
    }

    suspend fun flush() {
        val api = remote ?: return
        mutex.withLock {
            val s = settings.current()
            if (!s.dataNoticeAnswered) return
            if (s.usageStatsEnabled) flushEvents(api) else events.deleteUnsent()
            if (s.shareWeights) flushWeights(api)
        }
    }

    private suspend fun flushEvents(api: RemoteApi) {
        // 서버는 8일보다 오래된 날짜를 받지 않습니다. 오래 못 보낸 이벤트는 버립니다.
        events.deleteOlderThan(today().minusDays(KEEP_DAYS))
        val pending = events.unsent()
        if (pending.isEmpty()) return
        val installId = settings.installId()
        for (e in pending) {
            val result = api.sendEvent(UsageEventPayload(installId, e.day, e.name, appVersion, debug))
            if (result == SendResult.RETRY_LATER) return
            events.markSent(e.day, e.name, now())
        }
    }

    private suspend fun flushWeights(api: RemoteApi) {
        val installId = settings.installId()
        while (true) {
            val batch = health.weightsToUpload(BATCH_SIZE)
            if (batch.isEmpty()) return
            val petById = batch.map { it.petId }.distinct().associateWith { pets.get(it) }
            val rows = batch.mapNotNull { r -> petById[r.petId]?.let { toPayload(installId, r, it) } }
            when (api.sendWeights(rows)) {
                SendResult.SENT -> batch.forEach { markUploaded(it) }
                SendResult.RETRY_LATER -> return
                SendResult.REJECTED -> {
                    // 한 줄 때문에 묶음 전체가 거절됐을 수 있어 한 줄씩 다시 보냅니다. 거절된 줄은 건너뜁니다.
                    for (row in rows) {
                        if (api.sendWeights(listOf(row)) == SendResult.RETRY_LATER) return
                        markUploaded(batch.first { it.petId == row.petRef && it.date == row.day })
                    }
                }
            }
            if (batch.size < BATCH_SIZE) return
        }
    }

    private suspend fun markUploaded(record: HealthRecord) =
        health.markUploaded(record.petId, record.date, record.updatedAt, maxOf(now(), record.updatedAt))

    private fun toPayload(installId: String, record: HealthRecord, pet: Pet): WeightPayload? {
        val weight = record.weightKg ?: return null
        return WeightPayload(
            installId = installId,
            petRef = pet.id,
            day = record.date,
            weightKg = weight,
            species = pet.species.name,
            breed = pet.breed.take(30),
            birthMonth = pet.birthDate?.withDayOfMonth(1),
            sex = pet.sex.name,
            neutered = pet.neutered,
            appVersion = appVersion,
            debug = debug,
        )
    }

    /** 첫 실행 안내에서 고른 값을 저장하고, 그동안 기기에만 적어 둔 이벤트를 처리합니다. */
    suspend fun answerDataNotice(usageStats: Boolean, shareWeights: Boolean) {
        settings.answerDataNotice(usageStats, shareWeights)
        flush()
    }

    suspend fun setUsageStatsEnabled(enabled: Boolean) {
        settings.setUsageStatsEnabled(enabled)
        if (enabled) record(UsageEventName.APP_OPEN)
        flush()
    }

    suspend fun setShareWeights(enabled: Boolean) {
        settings.setShareWeights(enabled)
        flush()
    }

    /** 사용자가 직접 '보내기'를 누른 의견. 사용 통계 설정과 상관없이 그 한 건만 보냅니다. */
    suspend fun sendFeedback(category: FeedbackCategory, rating: Int?, message: String): SendResult {
        val api = remote ?: return SendResult.REJECTED
        val result = api.sendFeedback(
            FeedbackPayload(
                installId = settings.installId(),
                category = category,
                rating = rating,
                message = message.trim().take(MAX_FEEDBACK_LENGTH),
                appVersion = appVersion,
                androidSdk = Build.VERSION.SDK_INT,
                debug = debug,
            ),
        )
        if (result == SendResult.SENT) {
            record(UsageEventName.FEEDBACK_SENT)
            flush()
        }
        return result
    }

    /** 공유를 끄고, 서버에 이미 보낸 체중 기록을 지웁니다. 성공하면 true. */
    suspend fun deleteSharedWeights(): Boolean {
        val api = remote ?: return false
        return mutex.withLock {
            settings.setShareWeights(false)
            if (api.deleteWeights(settings.installId()) != SendResult.SENT) return@withLock false
            health.clearUploaded()
            settings.clearWeightSharing()
            true
        }
    }

    companion object {
        const val MAX_FEEDBACK_LENGTH = 1000
        private const val KEEP_DAYS = 7L
        private const val BATCH_SIZE = 100
    }
}
