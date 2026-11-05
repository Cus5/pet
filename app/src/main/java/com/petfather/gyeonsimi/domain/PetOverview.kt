package com.petfather.gyeonsimi.domain

import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealLog
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.data.repo.CareRepository
import com.petfather.gyeonsimi.data.repo.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** 선택된 반려동물의 기록과 분석 결과를 한 번에 묶은 값 */
data class PetOverview(
    val pet: Pet,
    val records: List<HealthRecord>,
    val meals: List<MealLog>,
    val report: HealthReport,
    val today: LocalDate,
) {
    val stage: LifeStage get() = HealthAnalyzer.lifeStage(pet.species, HealthAnalyzer.ageInMonths(pet.birthDate, today))
}

@OptIn(ExperimentalCoroutinesApi::class)
fun petOverview(pets: PetRepository, care: CareRepository, today: () -> LocalDate): Flow<PetOverview?> =
    pets.activePet.flatMapLatest { pet ->
        if (pet == null) {
            flowOf(null)
        } else {
            val day = today()
            combine(care.healthHistory(pet.id), care.mealsSince(pet.id, day.minusDays(30))) { records, meals ->
                PetOverview(pet, records, meals, HealthAnalyzer.analyze(pet, records, meals, day), day)
            }
        }
    }
