package com.petfather.gyeonsimi2

import android.content.Context
import com.petfather.gyeonsimi2.data.db.AppDatabase
import com.petfather.gyeonsimi2.data.remote.RemoteApi
import com.petfather.gyeonsimi2.data.remote.SupabaseApi
import com.petfather.gyeonsimi2.data.remote.Telemetry
import com.petfather.gyeonsimi2.data.repo.CareRepository
import com.petfather.gyeonsimi2.data.repo.CommunityRepository
import com.petfather.gyeonsimi2.data.repo.PetRepository
import com.petfather.gyeonsimi2.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import java.time.Clock
import java.time.LocalDate

/** 앱 전체에서 공유하는 객체들 (간단한 수동 의존성 주입) */
class AppContainer(
    context: Context,
    scope: CoroutineScope,
    /** 서버 창구. null 이면 서버로 아무것도 보내지 않습니다. */
    remote: RemoteApi? = SupabaseApi.fromBuildConfig(),
    val clock: Clock = Clock.systemDefaultZone(),
) {
    private val db = AppDatabase.create(context)
    val settings = SettingsRepository(SettingsRepository.createStore(context))
    val pets = PetRepository(db.petDao(), settings)
    val care = CareRepository(db.mealDao(), db.healthDao())
    val community = CommunityRepository(context, db.postDao(), db.commentDao(), settings)
    val telemetry = Telemetry(
        remote = remote,
        settings = settings,
        events = db.usageEventDao(),
        health = db.healthDao(),
        pets = db.petDao(),
        today = ::today,
        appVersion = BuildConfig.VERSION_NAME,
        debug = BuildConfig.DEBUG,
        scope = scope,
    )

    fun today(): LocalDate = LocalDate.now(clock)
}
