package com.moneykeeper.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.moneykeeper.app.service.DebugNotificationSender
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoneyKeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            DebugNotificationSender.CHANNEL_ID,
            "MoneyKeeper Debug 測試",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "用於測試通知解析 pipeline 的 debug 通知"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
