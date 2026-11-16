package com.petfather.gyeonsimi2.data.remote

import java.time.LocalDate

enum class SendResult {
    /** 서버가 받았습니다 (이미 같은 줄이 있는 경우 포함). */
    SENT,

    /** 서버가 내용을 거절했습니다. 다시 보내도 같은 결과라 재시도하지 않습니다. */
    REJECTED,

    /** 네트워크나 서버 문제로 실패했습니다. 나중에 다시 시도합니다. */
    RETRY_LATER,
}

/** 서버에 보내는 익명 사용 이벤트 종류. 이름은 서버 표의 허용 목록과 같아야 합니다. */
enum class UsageEventName(val wire: String) {
    APP_OPEN("app_open"),
    PET_REGISTERED("pet_registered"),
    MEAL_LOGGED("meal_logged"),
    HEALTH_SAVED("health_saved"),
    POST_WRITTEN("post_written"),
    FEEDBACK_SENT("feedback_sent"),
}

enum class FeedbackCategory(val wire: String, val label: String) {
    BUG("bug", "버그 신고"),
    IDEA("idea", "기능 제안"),
    PRAISE("praise", "좋았던 점"),
    OTHER("other", "기타"),
}

data class UsageEventPayload(
    val installId: String,
    val day: LocalDate,
    val name: String,
    val appVersion: String,
    val debug: Boolean,
)

data class FeedbackPayload(
    val installId: String,
    val category: FeedbackCategory,
    val rating: Int?,
    val message: String,
    val appVersion: String,
    val androidSdk: Int,
    val debug: Boolean,
)

/** 체중 공유에 동의한 사용자의 기록 한 줄. 이름·메모·위치는 들어 있지 않습니다. */
data class WeightPayload(
    val installId: String,
    val petRef: Long,
    val day: LocalDate,
    val weightKg: Double,
    val species: String,
    val breed: String,
    /** 태어난 달의 1일. 정확한 생일은 보내지 않습니다. */
    val birthMonth: LocalDate?,
    val sex: String,
    val neutered: Boolean,
    val appVersion: String,
    val debug: Boolean,
)

/** 서버와 주고받는 창구. 실제 구현은 [SupabaseApi], 테스트에서는 가짜 구현을 씁니다. */
interface RemoteApi {
    suspend fun sendEvent(event: UsageEventPayload): SendResult
    suspend fun sendFeedback(feedback: FeedbackPayload): SendResult
    suspend fun sendWeights(rows: List<WeightPayload>): SendResult
    suspend fun deleteWeights(installId: String): SendResult
}
