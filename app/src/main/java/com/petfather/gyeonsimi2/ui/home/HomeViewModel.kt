package com.petfather.gyeonsimi2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi2.AppContainer
import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.MealLog
import com.petfather.gyeonsimi2.data.db.MealSlot
import com.petfather.gyeonsimi2.data.db.Pet
import com.petfather.gyeonsimi2.data.remote.UsageEventName
import com.petfather.gyeonsimi2.domain.FeedingPlan
import com.petfather.gyeonsimi2.domain.petOverview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val pet: Pet? = null,
    val today: LocalDate = LocalDate.now(),
    val meals: Map<MealSlot, MealLog> = emptyMap(),
    val todayHealth: HealthRecord? = null,
    val feeding: FeedingPlan? = null,
    /** 서버가 설정된 빌드에서만 '의견 보내기'를 보여 줍니다. */
    val feedbackAvailable: Boolean = false,
) {
    val canRecordHealth: Boolean get() = meals.isNotEmpty()
}

data class HealthInput(
    val weightKg: Double?,
    val temperatureC: Double?,
    val heartRateBpm: Int?,
    val tags: List<String>,
    val memo: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer) : ViewModel() {

    val state: StateFlow<HomeUiState> = c.pets.activePet.flatMapLatest { pet ->
        if (pet == null) {
            flowOf(HomeUiState(loading = false, today = c.today(), feedbackAvailable = c.telemetry.available))
        } else {
            val today = c.today()
            combine(
                c.care.mealsOn(pet.id, today),
                c.care.healthOn(pet.id, today),
                petOverview(c.pets, c.care, c::today).map { it?.report?.feeding },
            ) { meals, health, feeding ->
                HomeUiState(false, pet, today, meals.associateBy { it.slot }, health, feeding, c.telemetry.available)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleMeal(slot: MealSlot) {
        val s = state.value
        val pet = s.pet ?: return
        val done = slot !in s.meals
        viewModelScope.launch {
            c.care.setMeal(pet.id, s.today, slot, done)
            if (done) c.telemetry.track(UsageEventName.MEAL_LOGGED)
        }
    }

    /** @return false 면 먹이 정보가 없어 저장하지 않음 */
    suspend fun saveHealth(input: HealthInput): Boolean {
        val s = state.value
        val pet = s.pet ?: return false
        val saved = c.care.saveHealth(
            HealthRecord(
                petId = pet.id,
                date = s.today,
                weightKg = input.weightKg,
                temperatureC = input.temperatureC,
                heartRateBpm = input.heartRateBpm,
                tags = input.tags,
                memo = input.memo,
            ),
        )
        // 전송 대기열을 비우면서, 체중 공유에 동의한 경우 방금 저장한 체중도 함께 보냅니다.
        if (saved) c.telemetry.track(UsageEventName.HEALTH_SAVED)
        return saved
    }
}
