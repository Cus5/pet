package com.petfather.gyeonsimi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petfather.gyeonsimi.AppContainer
import com.petfather.gyeonsimi.data.db.HealthRecord
import com.petfather.gyeonsimi.data.db.MealLog
import com.petfather.gyeonsimi.data.db.MealSlot
import com.petfather.gyeonsimi.data.db.Pet
import com.petfather.gyeonsimi.domain.FeedingPlan
import com.petfather.gyeonsimi.domain.petOverview
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
            flowOf(HomeUiState(loading = false, today = c.today()))
        } else {
            val today = c.today()
            combine(
                c.care.mealsOn(pet.id, today),
                c.care.healthOn(pet.id, today),
                petOverview(c.pets, c.care, c::today).map { it?.report?.feeding },
            ) { meals, health, feeding ->
                HomeUiState(false, pet, today, meals.associateBy { it.slot }, health, feeding)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleMeal(slot: MealSlot) {
        val s = state.value
        val pet = s.pet ?: return
        viewModelScope.launch { c.care.setMeal(pet.id, s.today, slot, done = slot !in s.meals) }
    }

    /** @return false 면 먹이 정보가 없어 저장하지 않음 */
    suspend fun saveHealth(input: HealthInput): Boolean {
        val s = state.value
        val pet = s.pet ?: return false
        return c.care.saveHealth(
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
    }
}
