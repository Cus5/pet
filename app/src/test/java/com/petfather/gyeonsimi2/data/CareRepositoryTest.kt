package com.petfather.gyeonsimi2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petfather.gyeonsimi2.data.db.AppDatabase
import com.petfather.gyeonsimi2.data.db.HealthRecord
import com.petfather.gyeonsimi2.data.db.MealSlot
import com.petfather.gyeonsimi2.data.db.Pet
import com.petfather.gyeonsimi2.data.db.Sex
import com.petfather.gyeonsimi2.data.db.Species
import com.petfather.gyeonsimi2.data.repo.CareRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CareRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var care: CareRepository
    private val today = LocalDate.of(2026, 11, 5)
    private var petId = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        care = CareRepository(db.mealDao(), db.healthDao())
        petId = db.petDao().insert(Pet(name = "안녕", species = Species.DOG, breed = "진돗개", birthDate = null, sex = Sex.MALE, neutered = true, weightKg = 19.3))
    }

    @After
    fun tearDown() = db.close()

    private fun record() = HealthRecord(petId, today, 19.3, 38.6, 90, listOf("배변"))

    @Test
    fun healthCannotBeSavedBeforeAnyMeal() = runTest {
        assertFalse(care.saveHealth(record()))
        assertNull(care.healthRecord(petId, today))
    }

    @Test
    fun healthIsSavedAfterMealAndTagsRoundTrip() = runTest {
        care.setMeal(petId, today, MealSlot.BREAKFAST, done = true)
        assertTrue(care.saveHealth(record()))
        val saved = care.healthOn(petId, today).first()!!
        assertEquals(listOf("배변"), saved.tags)
        assertEquals(38.6, saved.temperatureC!!, 0.0)
    }

    @Test
    fun mealToggleAddsAndRemoves() = runTest {
        care.setMeal(petId, today, MealSlot.DINNER, done = true)
        assertEquals(listOf(MealSlot.DINNER), care.mealsOn(petId, today).first().map { it.slot })
        care.setMeal(petId, today, MealSlot.DINNER, done = false)
        assertTrue(care.mealsOn(petId, today).first().isEmpty())
    }

    @Test
    fun deletingPetCascadesRecords() = runTest {
        care.setMeal(petId, today, MealSlot.LUNCH, done = true)
        care.saveHealth(record())
        db.petDao().delete(db.petDao().get(petId)!!)
        assertFalse(care.hasMealOn(petId, today))
        assertNull(care.healthRecord(petId, today))
    }
}
