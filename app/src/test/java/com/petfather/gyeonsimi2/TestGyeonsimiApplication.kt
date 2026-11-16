package com.petfather.gyeonsimi2

import com.petfather.gyeonsimi2.data.remote.FeedbackPayload
import com.petfather.gyeonsimi2.data.remote.RemoteApi
import com.petfather.gyeonsimi2.data.remote.SendResult
import com.petfather.gyeonsimi2.data.remote.UsageEventPayload
import com.petfather.gyeonsimi2.data.remote.WeightPayload
import java.util.concurrent.CopyOnWriteArrayList

/** 받은 요청을 기억만 하는 가짜 서버. 테스트가 실제 서버로 데이터를 보내는 일을 막습니다. */
class FakeRemoteApi : RemoteApi {
    val events = CopyOnWriteArrayList<UsageEventPayload>()
    val feedback = CopyOnWriteArrayList<FeedbackPayload>()
    val weights = CopyOnWriteArrayList<WeightPayload>()
    val deletedInstalls = CopyOnWriteArrayList<String>()

    /** 다음 요청들에 돌려줄 결과. 비어 있으면 SENT. */
    val nextResults = ArrayDeque<SendResult>()

    @Volatile
    var weightRule: (WeightPayload) -> SendResult = { SendResult.SENT }

    private fun next(): SendResult = synchronized(nextResults) { nextResults.removeFirstOrNull() } ?: SendResult.SENT

    override suspend fun sendEvent(event: UsageEventPayload): SendResult =
        next().also { if (it == SendResult.SENT) events += event }

    override suspend fun sendFeedback(feedback: FeedbackPayload): SendResult =
        next().also { if (it == SendResult.SENT) this.feedback += feedback }

    override suspend fun sendWeights(rows: List<WeightPayload>): SendResult {
        val forced = next()
        if (forced != SendResult.SENT) return forced
        val worst = rows.map(weightRule).maxByOrNull { it.ordinal } ?: SendResult.SENT
        if (worst == SendResult.SENT) weights += rows
        return worst
    }

    override suspend fun deleteWeights(installId: String): SendResult =
        next().also { if (it == SendResult.SENT) deletedInstalls += installId }
}

/** Robolectric 테스트 전체에서 쓰는 Application (app/src/test/resources/robolectric.properties 에서 지정) */
class TestGyeonsimiApplication : GyeonsimiApplication() {
    val fakeRemote = FakeRemoteApi()

    override fun createContainer(): AppContainer = AppContainer(this, appScope, remote = fakeRemote)
}
