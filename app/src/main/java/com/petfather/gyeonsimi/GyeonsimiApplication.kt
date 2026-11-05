package com.petfather.gyeonsimi

import android.app.Application
import androidx.work.Configuration
import com.petfather.gyeonsimi.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GyeonsimiApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
        appScope.launch {
            container.community.seedSamplesIfNeeded()
            if (container.settings.current().reminderEnabled) ReminderScheduler.schedule(this@GyeonsimiApplication)
        }
    }
}
