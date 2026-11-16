package com.petfather.gyeonsimi2.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petfather.gyeonsimi2.data.remote.FeedbackCategory
import com.petfather.gyeonsimi2.data.remote.FeedbackPayload
import com.petfather.gyeonsimi2.data.remote.SendResult
import com.petfather.gyeonsimi2.data.remote.SupabaseApi
import com.petfather.gyeonsimi2.data.remote.UsageEventPayload
import com.petfather.gyeonsimi2.data.remote.WeightPayload
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.time.LocalDate

/** 이 컴퓨터 안에 띄운 임시 서버로 요청 모양(주소·헤더·본문)과 응답 해석을 확인합니다. */
@RunWith(AndroidJUnit4::class)
class SupabaseApiTest {
    private data class Request(val method: String, val path: String, val apiKey: String?, val prefer: String?, val body: String)

    private lateinit var server: HttpServer
    private lateinit var api: SupabaseApi
    private val requests = mutableListOf<Request>()
    private var status = 201

    private val id = "123e4567-e89b-42d3-a456-426614174000"

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests += Request(
                exchange.requestMethod,
                exchange.requestURI.path,
                exchange.requestHeaders.getFirst("apikey"),
                exchange.requestHeaders.getFirst("Prefer"),
                exchange.requestBody.readBytes().toString(Charsets.UTF_8),
            )
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        }
        server.start()
        api = SupabaseApi("http://127.0.0.1:${server.address.port}/", "sb_publishable_test")
    }

    @After
    fun tearDown() = server.stop(0)

    @Test
    fun eventRequestShape() = runTest {
        val result = api.sendEvent(UsageEventPayload(id, LocalDate.of(2027, 2, 10), "app_open", "2.0.0", debug = false))

        assertEquals(SendResult.SENT, result)
        val r = requests.single()
        assertEquals("POST", r.method)
        assertEquals("/rest/v1/usage_events", r.path)
        assertEquals("sb_publishable_test", r.apiKey)
        assertEquals("return=minimal", r.prefer)
        val json = JSONObject(r.body)
        assertEquals(setOf("install_id", "day", "name", "app_version", "debug"), json.keys().asSequence().toSet())
        assertEquals("2027-02-10", json.getString("day"))
        assertFalse(json.getBoolean("debug"))
    }

    @Test
    fun weightsAreSentAsOneArrayWithOnlyAllowedColumns() = runTest {
        val row = WeightPayload(id, 3, LocalDate.of(2027, 2, 10), 4.25, "CAT", "코리안 숏헤어", null, "FEMALE", true, "2.0.0", false)
        api.sendWeights(listOf(row, row.copy(day = LocalDate.of(2027, 2, 11), birthMonth = LocalDate.of(2023, 3, 1))))

        val r = requests.single()
        assertEquals("/rest/v1/weight_records", r.path)
        val array = JSONArray(r.body)
        assertEquals(2, array.length())
        val first = array.getJSONObject(0)
        assertEquals(
            setOf("install_id", "pet_ref", "day", "weight_kg", "species", "breed", "birth_month", "sex", "neutered", "app_version", "debug"),
            first.keys().asSequence().toSet(),
        )
        assertTrue(first.isNull("birth_month"))
        assertEquals(4.25, first.getDouble("weight_kg"), 0.0)
        assertEquals("코리안 숏헤어", first.getString("breed"))
        assertEquals("2023-03-01", array.getJSONObject(1).getString("birth_month"))
    }

    @Test
    fun emptyWeightListSendsNothing() = runTest {
        assertEquals(SendResult.SENT, api.sendWeights(emptyList()))
        assertTrue(requests.isEmpty())
    }

    @Test
    fun feedbackAndDeleteRequests() = runTest {
        api.sendFeedback(FeedbackPayload(id, FeedbackCategory.PRAISE, null, "좋아요 \"정말\"", "2.0.0", 35, false))
        api.deleteWeights(id)

        val feedback = JSONObject(requests[0].body)
        assertEquals("/rest/v1/feedback", requests[0].path)
        assertEquals("praise", feedback.getString("category"))
        assertTrue(feedback.isNull("rating"))
        assertEquals("좋아요 \"정말\"", feedback.getString("message"))
        assertEquals("/rest/v1/rpc/delete_my_weight_records", requests[1].path)
        assertEquals(id, JSONObject(requests[1].body).getString("p_install_id"))
    }

    @Test
    fun responseCodesAreClassified() = runTest {
        val event = UsageEventPayload(id, LocalDate.of(2027, 2, 10), "app_open", "2.0.0", false)
        val expected = mapOf(
            201 to SendResult.SENT,
            409 to SendResult.SENT, // 이미 보낸 이벤트
            400 to SendResult.REJECTED,
            401 to SendResult.REJECTED,
            429 to SendResult.RETRY_LATER,
            503 to SendResult.RETRY_LATER,
        )
        expected.forEach { (code, result) ->
            status = code
            assertEquals("HTTP $code", result, api.sendEvent(event))
        }
    }

    @Test
    fun unreachableServerMeansRetryLater() = runTest {
        server.stop(0)
        val event = UsageEventPayload(id, LocalDate.of(2027, 2, 10), "app_open", "2.0.0", false)
        assertEquals(SendResult.RETRY_LATER, api.sendEvent(event))
    }
}
