package com.petfather.gyeonsimi2.domain

import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.MealLog
import com.petfather.gyeonsimi2.data.db.MealSlot
import com.petfather.gyeonsimi2.data.db.Pet
import com.petfather.gyeonsimi2.data.db.Sex
import com.petfather.gyeonsimi2.data.db.Species
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HealthAnalyzerTest {
    private val today = LocalDate.of(2026, 11, 5)

    private fun dog(breed: String = "진돗개", weight: Double? = 19.3, birth: LocalDate? = today.minusYears(6), neutered: Boolean = true) =
        Pet(id = 1, name = "안녕", species = Species.DOG, breed = breed, birthDate = birth, sex = Sex.MALE, neutered = neutered, weightKg = weight)

    private fun record(daysAgo: Long, weight: Double? = null, temp: Double? = null, hr: Int? = null, tags: List<String> = emptyList()) =
        HealthRecord(1, today.minusDays(daysAgo), weight, temp, hr, tags)

    @Test
    fun breedRangeIsUsedWhenBreedIsKnown() {
        val range = HealthAnalyzer.weightRange(dog(), 19.3)!!
        assertTrue(range.basedOnBreed)
        assertEquals(15.0, range.minKg, 0.0)
        assertEquals(23.0, range.maxKg, 0.0)
    }

    @Test
    fun breedAliasesAndSpacingMatch() {
        assertEquals("진돗개", BreedCatalog.find(Species.DOG, "진도개")?.name)
        assertEquals("골든 리트리버", BreedCatalog.find(Species.DOG, "골든리트리버")?.name)
        assertEquals("코리안 숏헤어", BreedCatalog.find(Species.CAT, "코숏")?.name)
        assertNull(BreedCatalog.find(Species.CAT, "진돗개"))
    }

    @Test
    fun unknownBreedFallsBackToBaselinePlusMinusTenPercent() {
        val range = HealthAnalyzer.weightRange(dog(breed = "믹스"), 10.0)!!
        assertEquals(9.0, range.minKg, 0.001)
        assertEquals(11.0, range.maxKg, 0.001)
        assertTrue(!range.basedOnBreed)
    }

    @Test
    fun normalDogHasStableWeightAndNoAlerts() {
        val records = listOf(record(2, 19.2, 38.6, 95), record(1, 19.3, 38.5, 90), record(0, 19.3, 38.7, 92))
        val meals = (0L..6L).map { MealLog(1, today.minusDays(it), MealSlot.BREAKFAST) }
        val report = HealthAnalyzer.analyze(dog(), records, meals, today)
        assertEquals(WeightStatus.NORMAL, report.weightStatus)
        assertTrue(report.alerts.isEmpty())
        assertEquals(listOf("안정 체중", "6세 · 수컷", "진돗개"), report.chips)
        assertTrue(report.headline.startsWith("좋아요!"))
    }

    @Test
    fun highTemperatureRaisesWarning() {
        val report = HealthAnalyzer.analyze(dog(), listOf(record(0, 19.3, 40.0, 120)), emptyList(), today)
        val alert = report.alerts.first()
        assertEquals(Severity.WARNING, alert.severity)
        assertTrue(alert.title.contains("40.0"))
    }

    @Test
    fun oldVitalsAreIgnored() {
        val report = HealthAnalyzer.analyze(dog(), listOf(record(10, 19.3, 40.5)), emptyList(), today)
        assertTrue(report.alerts.none { it.title.contains("체온") })
    }

    @Test
    fun rapidWeightChangeIsDetected() {
        val alert = HealthAnalyzer.weightChangeAlert(listOf(record(6, 5.0), record(1, 12.6)), today)
        assertNotNull(alert)
        assertEquals(Severity.WARNING, alert!!.severity)
        assertTrue(alert.title.contains("늘었어요"))
    }

    @Test
    fun smallWeightChangeIsFine() {
        assertNull(HealthAnalyzer.weightChangeAlert(listOf(record(6, 10.0), record(0, 10.5)), today))
    }

    @Test
    fun repeatedSymptomBecomesWarningSingleIsInfo() {
        val alerts = HealthAnalyzer.symptomAlerts(
            listOf(record(2, tags = listOf("설사")), record(1, tags = listOf("설사", "눈곱")), record(0, tags = listOf("구토"))),
            today,
        )
        assertEquals(Severity.WARNING, alerts.first { it.title.contains("설사") }.severity)
        assertEquals(Severity.INFO, alerts.first { it.title.contains("구토") }.severity)
        assertTrue(alerts.none { it.title.contains("눈곱") })
    }

    @Test
    fun feedingPlanUsesRerTimesFactor() {
        // 10kg 중성화 성견: RER = 70 × 10^0.75 ≈ 393.6, × 1.6 ≈ 630kcal, 3.7kcal/g → 약 170g
        val pet = dog(breed = "믹스", weight = 10.0)
        val plan = HealthAnalyzer.feedingPlan(pet, 10.0, 48, WeightStatus.NORMAL, HealthAnalyzer.weightRange(pet, 10.0))!!
        assertEquals(630, plan.kcalPerDay)
        assertEquals(170, plan.gramsPerDay)
        assertEquals(57, plan.gramsPerMeal)
    }

    @Test
    fun overweightPlanTargetsUpperBound() {
        val pet = dog(breed = "말티즈", weight = 5.0)
        val range = HealthAnalyzer.weightRange(pet, 5.0)!!
        val status = HealthAnalyzer.weightStatus(5.0, range)
        assertEquals(WeightStatus.OVER, status)
        val plan = HealthAnalyzer.feedingPlan(pet, 5.0, 48, status, range)!!
        // 목표 4kg, 계수 1.0 → 70 × 4^0.75 ≈ 198kcal
        assertEquals(198, plan.kcalPerDay)
    }

    @Test
    fun puppyUnderBreedRangeIsGrowingNotUnderweight() {
        val puppy = dog(breed = "진돗개", weight = 6.0, birth = today.minusMonths(4))
        val report = HealthAnalyzer.analyze(puppy, emptyList(), emptyList(), today)
        assertEquals("성장 중", report.chips.first())
        assertTrue(report.alerts.none { it.title.contains("낮아요") })
    }

    @Test
    fun lifeStageThresholds() {
        assertEquals(LifeStage.BABY, HealthAnalyzer.lifeStage(Species.DOG, 11))
        assertEquals(LifeStage.ADULT, HealthAnalyzer.lifeStage(Species.DOG, 83))
        assertEquals(LifeStage.SENIOR, HealthAnalyzer.lifeStage(Species.DOG, 84))
        assertEquals(LifeStage.ADULT, HealthAnalyzer.lifeStage(Species.CAT, 119))
        assertEquals(LifeStage.SENIOR, HealthAnalyzer.lifeStage(Species.CAT, 120))
    }

    @Test
    fun ageLabel() {
        assertEquals("6세", HealthAnalyzer.ageLabel(today.minusYears(6), today))
        assertEquals("4개월", HealthAnalyzer.ageLabel(today.minusMonths(4), today))
        assertEquals("10일", HealthAnalyzer.ageLabel(today.minusDays(10), today))
    }

    @Test
    fun feedRecommendationsAreTenAndMatchSpecies() {
        val dogs = FeedCatalog.recommend(Species.DOG, LifeStage.ADULT, WeightStatus.OVER)
        assertEquals(10, dogs.size)
        assertTrue(dogs.all { it.species == Species.DOG })
        assertTrue(dogs.first().weightControl)
        val kittens = FeedCatalog.recommend(Species.CAT, LifeStage.BABY, WeightStatus.NORMAL)
        assertTrue(kittens.all { it.species == Species.CAT })
        assertTrue(LifeStage.BABY in kittens.first().stages)
    }
}
