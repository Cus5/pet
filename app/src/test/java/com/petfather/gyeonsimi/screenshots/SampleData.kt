package com.petfather.gyeonsimi.screenshots

import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealLog
import com.petfather.gyeonsimi.data.db.MealSlot
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.db.Post
import com.petfather.gyeonsimi.data.db.PostWithCount
import com.petfather.gyeonsimi.data.db.Sex
import com.petfather.gyeonsimi.data.db.Species
import com.petfather.gyeonsimi.domain.HealthAnalyzer
import com.petfather.gyeonsimi.domain.PetOverview
import java.time.LocalDate

object SampleData {
    val today: LocalDate = LocalDate.of(2026, 11, 16)

    val dog = Pet(
        id = 1, name = "안녕", species = Species.DOG, breed = "진돗개", birthDate = LocalDate.of(2020, 5, 2),
        sex = Sex.MALE, neutered = true, weightKg = 19.0, allergies = "닭고기",
    )

    private val weights = listOf(18.9, 19.0, 19.1, 19.0, 19.2, 19.1, 19.3, 19.2, 19.4, 19.3, 19.2, 19.3, 19.4, 19.3)
    private val temps = listOf(38.6, 38.5, 38.7, 38.8, 38.6, 38.5, 38.9, 38.7, 38.6, 38.8, 38.7, 38.6, 38.5, 38.7)
    private val hearts = listOf(92, 88, 95, 90, 98, 94, 91, 89, 96, 93, 90, 92, 94, 91)

    val records: List<HealthRecord> = weights.indices.map { i ->
        val daysAgo = (weights.size - 1 - i).toLong()
        HealthRecord(
            1, today.minusDays(daysAgo), weights[i], temps[i], hearts[i],
            tags = when (i % 4) { 0 -> listOf("배변", "산책"); 1 -> listOf("배변"); 2 -> listOf("산책", "양치"); else -> listOf("배변", "눈곱") },
        )
    }

    val meals: List<MealLog> = (0L..13L).flatMap { d -> MealSlot.entries.map { MealLog(1, today.minusDays(d), it) } }

    fun overview(pet: Pet = dog, recs: List<HealthRecord> = records, mealLogs: List<MealLog> = meals) =
        PetOverview(pet, recs, mealLogs, HealthAnalyzer.analyze(pet, recs, mealLogs, today), today)

    /** 경고가 보이는 상태: 고열 + 반복 설사 */
    val warningRecords = records.dropLast(2) + listOf(
        HealthRecord(1, today.minusDays(1), 19.1, 39.4, 120, listOf("설사")),
        HealthRecord(1, today, 18.9, 40.0, 128, listOf("설사", "식욕 저하")),
    )

    private val now = System.currentTimeMillis()
    val posts = listOf(
        PostWithCount(Post(1, "민수", "강아지 사랑방", "우리 뽀삐 오늘 산책 나갔어요! 너무 좋아하네요 🐕", null, now - 3 * 60_000, 24, false, isSample = true), 5),
        PostWithCount(Post(2, "지은", "고양이 집사들", "고양이 사료 추천해주세요! 6개월 된 페르시안 키우고 있어요. 어떤 브랜드가 좋을까요?", null, now - 60 * 60_000, 8, true, isSample = true), 2),
        PostWithCount(Post(3, "보호자", "질문·답변", "진돗개 산책은 하루에 몇 번이 적당할까요? 요즘 저녁에만 한 번 나가고 있어요.", null, now - 5 * 60 * 60_000, 1, false, isMine = true), 0),
    )
}
