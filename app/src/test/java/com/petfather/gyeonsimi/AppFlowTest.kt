package com.petfather.gyeonsimi

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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    }
}
