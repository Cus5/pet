package com.petfather.gyeonsimi2

import android.app.Application
import androidx.work.Configuration
import com.petfather.gyeonsimi2.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class GyeonsimiApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    /** 테스트에서는 가짜 서버를 넣은 컨테이너로 바꿉니다. */
    protected open fun createContainer(): AppContainer = AppContainer(this, appScope)

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        ReminderScheduler.createChannel(this)
        appScope.launch {
            container.community.seedSamplesIfNeeded()
            if (container.settings.current().reminderEnabled) ReminderScheduler.schedule(this@GyeonsimiApplication)
        }
    }
}
