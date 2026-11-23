package com.permalert.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.permalert.alarm.AlertDispatcher
import com.permalert.data.SourceStore
import com.permalert.domain.AlertType
import com.permalert.domain.Classifier
import com.permalert.domain.MonitoringPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch

/** Android сам управляет жизненным циклом listener; опросов и сетевых таймеров нет. */
class AlertNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val incoming = Channel<StatusBarNotification>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            for (notification in incoming) process(notification)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Берём последнее активное уведомление каждого приложения, старые не проигрываем.
        runCatching {
            activeNotifications.orEmpty()
                .groupBy { it.packageName }
                .values
                .mapNotNull { notifications -> notifications.maxByOrNull { it.postTime } }
                .sortedBy { it.postTime }
                .forEach { incoming.trySend(it) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        incoming.trySend(sbn)
    }

    private suspend fun process(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!MonitoringPolicy.shouldProcessNotifications(
                enabled = SourceStore.monitoringEnabled(this),
                sourcePackage = pkg,
                selectedPackages = SourceStore.selectedAllowedPackages(this),
                ownPackage = packageName
            )) return
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val full = NotificationTextExtractor.extract(sbn)
        if (full.isBlank()) return
        val type = Classifier.classifyStrict(full)
        if (type == AlertType.NONE) return
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString()
        }.getOrDefault(pkg)
        AlertDispatcher.fire(this, type, label, full, pkg = pkg, eventTimestampMs = sbn.postTime)
    }

    override fun onDestroy() {
        incoming.close()
        scope.cancel()
        super.onDestroy()
    }
}
