package com.petfather.gyeonsimi2.data.repo

import com.petfather.gyeonsimi2.data.db.HealthDao
import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.MealDao
import com.petfather.gyeonsimi2.data.db.MealLog
import com.petfather.gyeonsimi2.data.db.MealSlot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 먹이 기록과 건강 기록 */
class CareRepository(
    private val mealDao: MealDao,
    private val healthDao: HealthDao,
) {
    fun mealsOn(petId: Long, date: LocalDate): Flow<List<MealLog>> = mealDao.observeForDate(petId, date)

    fun mealsSince(petId: Long, from: LocalDate): Flow<List<MealLog>> = mealDao.observeSince(petId, from)

    suspend fun hasMealOn(petId: Long, date: LocalDate): Boolean = mealDao.countForDate(petId, date) > 0

    suspend fun setMeal(petId: Long, date: LocalDate, slot: MealSlot, done: Boolean) {
        if (done) mealDao.upsert(MealLog(petId, date, slot)) else mealDao.delete(petId, date, slot)
    }

    fun healthOn(petId: Long, date: LocalDate): Flow<HealthRecord?> = healthDao.observeForDate(petId, date)

    fun healthHistory(petId: Long): Flow<List<HealthRecord>> = healthDao.observeAll(petId)

    suspend fun healthRecord(petId: Long, date: LocalDate): HealthRecord? = healthDao.get(petId, date)

    /**
     * 오늘 건강 정보를 저장합니다. 먹이 정보를 먼저 입력해야 저장할 수 있어요.
     * @return 저장했으면 true, 오늘 먹이 기록이 없어 거절됐으면 false
     */
    suspend fun saveHealth(record: HealthRecord): Boolean {
        if (!hasMealOn(record.petId, record.date)) return false
        healthDao.upsert(record.copy(updatedAt = System.currentTimeMillis()))
        return true
    }
}
