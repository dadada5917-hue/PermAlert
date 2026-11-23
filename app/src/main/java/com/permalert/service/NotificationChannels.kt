package com.permalert.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val CH_ALARM = "alarm"

fun ensureAlarmChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(CH_ALARM, "Экстренная тревога", NotificationManager.IMPORTANCE_HIGH).apply {
        setBypassDnd(true)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        setSound(null, null) // звук воспроизводит AlarmController, чтобы не было двойного сигнала
    }
    manager.createNotificationChannel(channel)
}
