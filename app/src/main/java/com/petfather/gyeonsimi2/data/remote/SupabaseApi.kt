package com.petfather.gyeonsimi2.data.remote

import com.petfather.gyeonsimi2.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supabase REST API 로 익명 데이터를 보냅니다.
 * 앱에 들어 있는 것은 공개 키뿐이고, 서버 규칙상 이 키로는 "추가"만 할 수 있습니다
 * (표·규칙 정의: supabase/migrations).
 */
class SupabaseApi(baseUrl: String, private val apiKey: String) : RemoteApi {
    private val base = baseUrl.trimEnd('/')

    override suspend fun sendEvent(event: UsageEventPayload): SendResult = post(
        "/rest/v1/usage_events",
        JSONObject()
            .put("install_id", event.installId)
            .put("day", event.day.toString())
            .put("name", event.name)
            .put("app_version", event.appVersion)
            .put("debug", event.debug)
            .toString(),
    )

    override suspend fun sendFeedback(feedback: FeedbackPayload): SendResult = post(
        "/rest/v1/feedback",
        JSONObject()
            .put("install_id", feedback.installId)
            .put("category", feedback.category.wire)
            .put("rating", feedback.rating ?: JSONObject.NULL)
            .put("message", feedback.message)
            .put("app_version", feedback.appVersion)
            .put("android_sdk", feedback.androidSdk)
            .put("debug", feedback.debug)
            .toString(),
    )

    override suspend fun sendWeights(rows: List<WeightPayload>): SendResult {
        if (rows.isEmpty()) return SendResult.SENT
        val array = JSONArray()
        rows.forEach { r ->
            array.put(
                JSONObject()
                    .put("install_id", r.installId)
                    .put("pet_ref", r.petRef)
                    .put("day", r.day.toString())
                    .put("weight_kg", r.weightKg)
                    .put("species", r.species)
                    .put("breed", r.breed)
                    .put("birth_month", r.birthMonth?.toString() ?: JSONObject.NULL)
                    .put("sex", r.sex)
                    .put("neutered", r.neutered)
                    .put("app_version", r.appVersion)
                    .put("debug", r.debug),
            )
        }
        return post("/rest/v1/weight_records", array.toString())
    }

    override suspend fun deleteWeights(installId: String): SendResult = post(
        "/rest/v1/rpc/delete_my_weight_records",
        JSONObject().put("p_install_id", installId).toString(),
    )

    private suspend fun post(path: String, body: String): SendResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Prefer", "return=minimal")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            classify(connection.responseCode)
        } catch (_: IOException) {
            SendResult.RETRY_LATER
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000

        /** local.properties 에 서버 설정이 없으면 null → 앱은 아무것도 보내지 않습니다. */
        fun fromBuildConfig(): SupabaseApi? {
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_KEY
            return if (url.isBlank() || key.isBlank()) null else SupabaseApi(url, key)
        }

        fun classify(status: Int): SendResult = when (status) {
            in 200..299 -> SendResult.SENT
            409 -> SendResult.SENT // 같은 날 같은 이벤트가 이미 있음
            408, 429 -> SendResult.RETRY_LATER
            in 400..499 -> SendResult.REJECTED
            else -> SendResult.RETRY_LATER
        }
    }
}
