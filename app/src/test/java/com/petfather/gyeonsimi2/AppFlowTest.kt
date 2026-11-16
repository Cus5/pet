package com.petfather.gyeonsimi2

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 실제 MainActivity 로 등록 → 식사 기록 → 건강 기록 흐름을 확인합니다. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class AppFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 10_000)

    @Test
    fun registerPetThenRecordMealAndHealth() {
        val app = ApplicationProvider.getApplicationContext<TestGyeonsimiApplication>()
        val server = app.fakeRemote

        // 0. 첫 실행: 데이터 사용 안내에 답하기 전에는 아무것도 보내지 않는다
        waitForText("시작하기 전에")
        assertTrue(server.events.isEmpty())
        compose.onNodeWithText("체중 기록 공유하고 시작").performClick()
        compose.waitUntilDoesNotExist(hasText("시작하기 전에"), timeoutMillis = 10_000)

        // 1. 처음에는 등록 안내가 보인다
        waitForText("반려동물 등록하기")
        compose.onNodeWithText("반려동물 등록하기").performClick()

        // 2. 등록 화면에서 이름·성별 입력 후 저장
        waitForText("새 반려동물 등록")
        compose.onNode(hasSetTextAction() and hasText("이름")).performTextInput("콩이")
        compose.onNode(hasSetTextAction() and hasText("몸무게 (kg)")).performTextInput("4.2")
        compose.onNodeWithText("수컷").performClick()
        compose.onNodeWithText("등록하기").performScrollTo().performClick()

        // 3. 홈으로 돌아와 이름과 먹이 카드가 보인다
        waitForText("콩이")
        waitForText("먹이 정보")

        // 4. 먹이 기록 전에는 건강 정보를 입력할 수 없다
        compose.onNodeWithContentDescription("오늘 건강 정보 입력").performClick()
        waitForText("먹이 정보를 먼저 입력해 주세요", substring = true)

        // 5. 아침 식사 완료 → 건강 정보 입력 가능
        compose.onAllNodesWithText("대기 중").onFirst().performClick()
        waitForText("완료", substring = true)
        compose.onNodeWithContentDescription("오늘 건강 정보 입력").performClick()
        waitForText("오늘의 건강 정보")
        compose.onNode(hasSetTextAction() and hasText("체온 (°C)")).performTextInput("38.6")
        compose.onNode(hasSetTextAction() and hasText("체중 (kg)")).apply { performTextClearance(); performTextInput("4.3") }
        compose.onNodeWithText("저장").performClick()

        // 6. 저장 후 홈 카드에 값이 표시된다
        waitForText("오늘의 건강 정보를 저장했어요.")
        waitForText("38.6°C")
        waitForText("4.3kg")

        // 7. 익명 사용 이벤트가 종류별로 한 번씩, 체중은 이름·체온 없이 서버로 갔다
        compose.waitUntil(10_000) { server.events.size >= 4 && server.weights.isNotEmpty() }
        assertEquals(
            listOf("app_open", "health_saved", "meal_logged", "pet_registered"),
            server.events.map { it.name }.sorted(),
        )
        val weight = server.weights.last()
        assertEquals(4.3, weight.weightKg, 0.0)
        assertFalse(weight.toString().contains("콩이"))
        assertFalse(weight.toString().contains("38.6"))
        assertEquals(1, (server.events.map { it.installId } + server.weights.map { it.installId }).distinct().size)
    }

    @Test
    fun feedbackCanBeSentFromHome() {
        val server = ApplicationProvider.getApplicationContext<TestGyeonsimiApplication>().fakeRemote
        waitForText("시작하기 전에")
        compose.onNodeWithText("공유 없이 시작").performClick()
        compose.waitUntilDoesNotExist(hasText("시작하기 전에"), timeoutMillis = 10_000)

        // 반려동물을 등록해야 홈 카드가 보인다
        compose.onNodeWithText("반려동물 등록하기").performClick()
        waitForText("새 반려동물 등록")
        compose.onNode(hasSetTextAction() and hasText("이름")).performTextInput("나비")
        compose.onNodeWithText("암컷").performClick()
        compose.onNodeWithText("등록하기").performScrollTo().performClick()
        waitForText("먹이 정보")

        compose.onNodeWithText("의견 보내기").performScrollTo().performClick()
        waitForText("어떤 의견인가요?")
        compose.onNodeWithText("버그 신고").performClick()
        compose.onNodeWithContentDescription("별 4개").performClick()
        compose.onNode(hasSetTextAction() and hasText("내용")).performTextInput("그래프 글씨가 작아요")
        compose.onNodeWithText("보내기").performScrollTo().performClick()

        waitForText("의견을 보냈어요")
        val sent = server.feedback.single()
        assertEquals("bug", sent.category.wire)
        assertEquals(4, sent.rating)
        assertEquals("그래프 글씨가 작아요", sent.message)
        assertTrue(server.weights.isEmpty()) // 공유에 동의하지 않았으므로 체중은 보내지 않는다
    }
}
