package com.petfather.gyeonsimi2.remote

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petfather.gyeonsimi2.FakeRemoteApi
import com.petfather.gyeonsimi2.data.db.AppDatabase
import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.Pet
import com.petfather.gyeonsimi2.data.db.Sex
import com.petfather.gyeonsimi2.data.db.Species
import com.petfather.gyeonsimi2.data.remote.FeedbackCategory
import com.petfather.gyeonsimi2.data.remote.SendResult
import com.petfather.gyeonsimi2.data.remote.Telemetry
import com.petfather.gyeonsimi2.data.remote.UsageEventName
import com.petfather.gyeonsimi2.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TelemetryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var remote: FakeRemoteApi
    private lateinit var telemetry: Telemetry
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var today = LocalDate.of(2027, 2, 10)
    private var clock = 1_000L
    private var petId = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = storeScope) { tmp.newFile("settings.preferences_pb").also { it.delete() } },
        )
        remote = FakeRemoteApi()
        telemetry = telemetry(remote)
        petId = db.petDao().insert(
            Pet(name = "비밀이름", species = Species.DOG, breed = "진돗개", birthDate = LocalDate.of(2020, 5, 17), sex = Sex.MALE, neutered = true, weightKg = 19.0),
        )
    }

    private fun telemetry(api: FakeRemoteApi?) = Telemetry(
        remote = api, settings = settings, events = db.usageEventDao(), health = db.healthDao(), pets = db.petDao(),
        today = { today }, appVersion = "2.0.0", debug = false, scope = storeScope, now = { ++clock },
    )

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
    }

    private suspend fun saveWeight(date: LocalDate, kg: Double?, memo: String = "병원 다녀옴") =
        db.healthDao().upsert(HealthRecord(petId, date, kg, 38.5, 90, listOf("배변"), memo, updatedAt = ++clock))

    // ── 사용 통계 ──────────────────────────────────────────────────────────

    @Test
    fun nothingIsSentBeforeTheNoticeIsAnswered() = runTest {
        telemetry.record(UsageEventName.APP_OPEN)
        telemetry.flush()
        assertTrue(remote.events.isEmpty())

        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        // 안내 전에 기기에만 적어 둔 첫날 이벤트가 동의 후 전송된다
        assertEquals(listOf("app_open"), remote.events.map { it.name })
        assertEquals(today, remote.events.single().day)
    }

    @Test
    fun decliningUsageStatsDiscardsWhatWasRecorded() = runTest {
        telemetry.record(UsageEventName.APP_OPEN)
        telemetry.answerDataNotice(usageStats = false, shareWeights = false)
        telemetry.record(UsageEventName.MEAL_LOGGED)
        telemetry.flush()
        assertTrue(remote.events.isEmpty())
        assertTrue(db.usageEventDao().unsent().isEmpty())
    }

    @Test
    fun eachEventIsSentOncePerDay() = runTest {
        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        repeat(3) {
            telemetry.record(UsageEventName.APP_OPEN)
            telemetry.record(UsageEventName.MEAL_LOGGED)
            telemetry.flush()
        }
        assertEquals(listOf("app_open", "meal_logged"), remote.events.map { it.name }.sorted())

        today = today.plusDays(1)
        telemetry.record(UsageEventName.APP_OPEN)
        telemetry.flush()
        assertEquals(3, remote.events.size)
        assertEquals(1, remote.events.map { it.installId }.distinct().size)
    }

    @Test
    fun failedSendIsRetriedAndRejectedOnesAreDropped() = runTest {
        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        remote.nextResults += SendResult.RETRY_LATER
        telemetry.record(UsageEventName.APP_OPEN)
        telemetry.flush()
        assertTrue(remote.events.isEmpty())
        telemetry.flush()
        assertEquals(1, remote.events.size)

        remote.nextResults += SendResult.REJECTED
        telemetry.record(UsageEventName.HEALTH_SAVED)
        telemetry.flush()
        telemetry.flush()
        assertEquals(1, remote.events.size) // 거절된 이벤트는 다시 보내지 않는다
        assertTrue(db.usageEventDao().unsent().isEmpty())
    }

    @Test
    fun eventsOlderThanAWeekAreNotSent() = runTest {
        telemetry.record(UsageEventName.APP_OPEN)
        today = today.plusDays(10)
        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        assertTrue(remote.events.isEmpty())
    }

    @Test
    fun withoutBackendNothingHappens() = runTest {
        val offline = telemetry(null)
        assertFalse(offline.available)
        offline.record(UsageEventName.APP_OPEN)
        offline.flush()
        assertTrue(db.usageEventDao().unsent().isEmpty())
        assertEquals(SendResult.REJECTED, offline.sendFeedback(FeedbackCategory.IDEA, null, "의견"))
    }

    // ── 체중 기록 공유 ──────────────────────────────────────────────────────

    @Test
    fun weightsAreSentOnlyWithConsent() = runTest {
        saveWeight(today.minusDays(1), 19.2)
        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        assertTrue(remote.weights.isEmpty())

        telemetry.setShareWeights(true)
        assertEquals(listOf(19.2), remote.weights.map { it.weightKg })
    }

    @Test
    fun weightPayloadHasNoNameMemoOrExactBirthday() = runTest {
        saveWeight(today, 19.3)
        saveWeight(today.minusDays(1), null) // 체중이 없는 기록은 보내지 않는다
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)

        val row = remote.weights.single()
        assertEquals(LocalDate.of(2020, 5, 1), row.birthMonth) // 생일은 달까지만
        assertEquals("DOG" to "진돗개", row.species to row.breed)
        assertEquals(petId, row.petRef)
        val text = row.toString()
        assertFalse(text.contains("비밀이름"))
        assertFalse(text.contains("병원"))
        assertFalse(text.contains("38.5"))
    }

    @Test
    fun editedWeightIsSentAgainButUnchangedIsNot() = runTest {
        saveWeight(today, 19.3)
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)
        telemetry.flush()
        assertEquals(1, remote.weights.size)

        saveWeight(today, 19.5)
        telemetry.flush()
        assertEquals(listOf(19.3, 19.5), remote.weights.map { it.weightKg })
    }

    @Test
    fun oneRejectedRowDoesNotBlockTheOthers() = runTest {
        saveWeight(today.minusDays(2), 19.1)
        saveWeight(today.minusDays(1), 99.0)
        saveWeight(today, 19.3)
        remote.weightRule = { if (it.weightKg > 50) SendResult.REJECTED else SendResult.SENT }
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)

        assertEquals(listOf(19.1, 19.3), remote.weights.map { it.weightKg })
        assertTrue(db.healthDao().weightsToUpload(10).isEmpty()) // 거절된 줄도 다시 시도하지 않는다
    }

    @Test
    fun networkFailureKeepsWeightsForLater() = runTest {
        saveWeight(today, 19.3)
        remote.nextResults += SendResult.RETRY_LATER
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)
        assertTrue(remote.weights.isEmpty())
        telemetry.flush()
        assertEquals(1, remote.weights.size)
    }

    @Test
    fun deletingSharedWeightsTurnsSharingOffAndAllowsResend() = runTest {
        saveWeight(today, 19.3)
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)
        assertTrue(settings.current().everSharedWeights)

        assertTrue(telemetry.deleteSharedWeights())
        assertEquals(listOf(settings.installId()), remote.deletedInstalls.toList())
        assertFalse(settings.current().shareWeights)
        assertFalse(settings.current().everSharedWeights)
        assertNull(db.healthDao().get(petId, today)!!.uploadedAt)

        telemetry.setShareWeights(true) // 다시 동의하면 처음부터 다시 보낸다
        assertEquals(2, remote.weights.size)
    }

    @Test
    fun failedDeleteKeepsTheMenuButStopsSharing() = runTest {
        saveWeight(today, 19.3)
        telemetry.answerDataNotice(usageStats = false, shareWeights = true)
        remote.nextResults += SendResult.RETRY_LATER
        assertFalse(telemetry.deleteSharedWeights())
        assertFalse(settings.current().shareWeights)
        assertTrue(settings.current().everSharedWeights) // 다시 삭제를 시도할 수 있어야 한다
    }

    // ── 피드백 ─────────────────────────────────────────────────────────────

    @Test
    fun feedbackIsSentEvenWhenUsageStatsAreOff() = runTest {
        telemetry.answerDataNotice(usageStats = false, shareWeights = false)
        assertEquals(SendResult.SENT, telemetry.sendFeedback(FeedbackCategory.BUG, 4, "  그래프가 안 보여요  "))
        val sent = remote.feedback.single()
        assertEquals("그래프가 안 보여요", sent.message)
        assertEquals(4, sent.rating)
        assertEquals(FeedbackCategory.BUG, sent.category)
        assertTrue(remote.events.isEmpty()) // 통계를 끈 사용자는 '의견 보냄' 이벤트도 보내지 않는다
    }

    @Test
    fun feedbackFailureIsReported() = runTest {
        telemetry.answerDataNotice(usageStats = true, shareWeights = false)
        remote.nextResults += SendResult.RETRY_LATER
        assertEquals(SendResult.RETRY_LATER, telemetry.sendFeedback(FeedbackCategory.IDEA, null, "의견"))
        assertTrue(remote.feedback.isEmpty())
    }
}
