package com.petfather.gyeonsimi

import android.content.Context
import com.petfather.gyeonsimi.data.db.AppDatabase
import com.petfather.gyeonsimi.data.repo.CareRepository
import com.petfather.gyeonsimi.data.repo.CommunityRepository
import com.petfather.gyeonsimi.data.repo.PetRepository
import com.petfather.gyeonsimi.data.repo.SettingsRepository
import java.time.Clock
import java.time.LocalDate

/** 앱 전체에서 공유하는 객체들 (간단한 수동 의존성 주입) */
class AppContainer(context: Context, val clock: Clock = Clock.systemDefaultZone()) {
    private val db = AppDatabase.create(context)
    val settings = SettingsRepository(context)
    val pets = PetRepository(db.petDao(), settings)
    val care = CareRepository(db.mealDao(), db.healthDao())
    val community = CommunityRepository(context, db.postDao(), db.commentDao(), settings)

    fun today(): LocalDate = LocalDate.now(clock)
}
