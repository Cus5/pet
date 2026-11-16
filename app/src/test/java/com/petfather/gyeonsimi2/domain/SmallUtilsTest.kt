package com.petfather.gyeonsimi2.domain

import com.petfather.gyeonsimi2.notify.ReminderScheduler
import com.petfather.gyeonsimi2.ui.components.labelIndices
import com.petfather.gyeonsimi2.ui.home.shortAddress
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class SmallUtilsTest {
    @Test
    fun chartLabelsAreSpreadAndIncludeEnds() {
        assertEquals(listOf(0, 1, 2), labelIndices(3, 6))
        assertEquals(listOf(0, 3, 5, 8, 10, 13), labelIndices(14, 6))
        assertEquals(emptyList<Int>(), labelIndices(0, 6))
    }

    @Test
    fun addressIsShortened() {
        assertEquals("서울특별시 중구 명동2가", shortAddress("대한민국 서울특별시 중구 명동2가 31-1"))
        assertEquals("경기도 성남시 분당구", shortAddress("경기도 성남시 분당구 정자동 1"))
    }

    @Test
    fun reminderRunsAtNextEightPm() {
        assertEquals(Duration.ofHours(2), ReminderScheduler.delayUntilNext(LocalDateTime.of(2026, 11, 5, 18, 0)))
        assertEquals(Duration.ofHours(23), ReminderScheduler.delayUntilNext(LocalDateTime.of(2026, 11, 5, 21, 0)))
        assertEquals(Duration.ofDays(1), ReminderScheduler.delayUntilNext(LocalDateTime.of(2026, 11, 5, 20, 0)))
    }
}
