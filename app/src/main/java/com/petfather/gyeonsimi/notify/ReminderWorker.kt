package com.petfather.gyeonsimi.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.petfather.gyeonsimi.GyeonsimiApplication
import com.petfather.gyeonsimi.MainActivity
import com.petfather.gyeonsimi.R
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as GyeonsimiApplication).container
        val pet = container.pets.activePet.first() ?: return Result.success()
        val today = container.today()
        val hasMeal = container.care.hasMealOn(pet.id, today)
        val hasHealth = container.care.healthRecord(pet.id, today) != null
        if (hasMeal && hasHealth) return Result.success()

        val text = if (!hasMeal) {
            "오늘 ${pet.name}의 먹이 정보를 입력하고, 건강 정보도 기록해 주세요."
        } else {
            "오늘 ${pet.name}의 체중·체온·심박수를 기록할 시간이에요."
        }
        notify(text)
        return Result.success()
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() 가 권한 여부까지 확인합니다
    private fun notify(text: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_paw)
            .setContentTitle("오늘의 기록")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
    }
}
