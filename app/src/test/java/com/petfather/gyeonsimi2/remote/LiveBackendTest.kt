package com.petfather.gyeonsimi2.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petfather.gyeonsimi2.data.remote.FeedbackCategory
import com.petfather.gyeonsimi2.data.remote.FeedbackPayload
import com.petfather.gyeonsimi2.data.remote.SendResult
import com.petfather.gyeonsimi2.data.remote.SupabaseApi
import com.petfather.gyeonsimi2.data.remote.UsageEventPayload
import com.petfather.gyeonsimi2.data.remote.WeightPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * local.properties 에 적힌 실제 서버로 앱과 같은 코드로 보내 봅니다. 평소에는 건너뜁니다.
 *   ./gradlew :app:testDebugUnitTest --tests '*LiveBackendTest*' -Pgyeonsimi.liveBackendTest=true
 * 보내는 줄은 모두 debug=true 라서 지표(reporting 뷰)에는 잡히지 않습니다.
 */
@RunWith(AndroidJUnit4::class)
class LiveBackendTest {
    private val installId = "00000000-0000-4000-8000-0000000011fe"

    @Test
    fun appClientCanWriteToTheRealServer() = runTest {
        assumeTrue(System.getProperty("gyeonsimi.liveBackendTest") == "true")
        val api = requireNotNull(SupabaseApi.fromBuildConfig()) { "local.properties 에 SUPABASE_URL/SUPABASE_KEY 가 없습니다" }
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        val event = UsageEventPayload(installId, today, "app_open", "2.0.0-livetest", debug = true)
        assertEquals("이벤트", SendResult.SENT, api.sendEvent(event))
        assertEquals("같은 이벤트 다시(중복)", SendResult.SENT, api.sendEvent(event))
        assertEquals("허용되지 않은 이벤트 이름", SendResult.REJECTED, api.sendEvent(event.copy(name = "not_allowed")))
        assertEquals("너무 오래된 날짜", SendResult.REJECTED, api.sendEvent(event.copy(day = today.minusDays(60))))

        assertEquals(
            "피드백",
            SendResult.SENT,
            api.sendFeedback(FeedbackPayload(installId, FeedbackCategory.OTHER, 5, "앱 코드로 보낸 연결 확인", "2.0.0-livetest", 35, debug = true)),
        )

        val weight = WeightPayload(installId, 1, today, 19.3, "DOG", "진돗개", LocalDate.of(2020, 5, 1), "MALE", true, "2.0.0-livetest", debug = true)
        assertEquals("체중 2건", SendResult.SENT, api.sendWeights(listOf(weight, weight.copy(day = today.minusDays(1), weightKg = 19.2))))
        assertEquals("범위를 벗어난 체중", SendResult.REJECTED, api.sendWeights(listOf(weight.copy(weightKg = 500.0))))
        assertEquals("보낸 체중 삭제", SendResult.SENT, api.deleteWeights(installId))
    }
}
