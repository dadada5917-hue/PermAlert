package com.permalert.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.permalert.App
import com.permalert.R
import com.permalert.data.AlertEntity
import com.permalert.data.SourceStore
import com.permalert.domain.AlertType
import com.permalert.domain.AlertStatePolicy
import com.permalert.service.CH_ALARM
import com.permalert.ui.AlarmActivity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единая точка для локальных источников: уведомления MAX/МЧС и SMS телефона.
 * Здесь же дедупликация: START при активной тревоге игнорируется, END — только после START.
 * Каждое сработавшее оповещение пишется в историю.
 */
object AlertDispatcher {
    private val mutex = Mutex()

    /** @return true, если оповещение реально сработало */
    suspend fun fire(
        c: Context,
        type: AlertType,
        source: String,
        text: String,
        link: String = "",
        pkg: String = "",
        eventTimestampMs: Long? = null
    ): Boolean {
        val ctx = c.applicationContext
        val dao = (ctx as App).db.alerts()
        return mutex.withLock {
            // Последний рубеж защиты: пауза блокирует все источники, включая уже поставленные в очередь события.
            if (!SourceStore.monitoringEnabled(ctx)) return@withLock false
            val now = System.currentTimeMillis()
            val eventTs = eventTimestampMs?.takeIf { it > 0 } ?: now
            val statePrefs = ctx.getSharedPreferences("alert_state", Context.MODE_PRIVATE)
            val watermark = statePrefs.getLong("last_event_timestamp", 0L)
            if (!AlertStatePolicy.isNewerEvent(eventTs, watermark)) return@withLock false

            // Фиксируем хронологию даже для отбоя без активной тревоги: это не даст
            // запоздавшему старому START из другого источника снова открыть тревогу.
            if (!statePrefs.edit().putLong("last_event_timestamp", eventTs).commit()) {
                return@withLock false
            }

            val prev = dao.last()
            if (!AlertStatePolicy.shouldFire(type, prev?.type, prev?.ts, now)) return@withLock false
            dao.insert(AlertEntity(ts = eventTs, type = type.name, source = source, text = text.take(3_000), link = link))
            show(ctx, type, source, text, link, pkg)
            true
        }
    }

    /** Восстанавливает активную тревогу после перезагрузки телефона, если она ещё актуальна. */
    suspend fun restoreActive(c: Context): Boolean {
        val ctx = c.applicationContext
        val dao = (ctx as App).db.alerts()
        return mutex.withLock {
            if (!SourceStore.monitoringEnabled(ctx)) return@withLock false
            val previous = dao.last() ?: return@withLock false
            val acknowledgedAt = ctx.getSharedPreferences("alert_state", Context.MODE_PRIVATE)
                .getLong("acknowledged_at", 0L)
            if (previous.ts <= acknowledgedAt) return@withLock false
            if (!AlertStatePolicy.isActive(previous.type, previous.ts, System.currentTimeMillis())) return@withLock false
            show(ctx, AlertType.START, previous.source, previous.text, previous.link, "")
            true
        }
    }

    /** Отмечает текущее оповещение как подтверждённое пользователем. */
    suspend fun acknowledge(c: Context) {
        val ctx = c.applicationContext
        val previous = (ctx as App).db.alerts().last() ?: return
        ctx.getSharedPreferences("alert_state", Context.MODE_PRIVATE)
            .edit().putLong("acknowledged_at", previous.ts).commit()
    }

    private fun show(ctx: Context, type: AlertType, source: String, text: String, link: String, pkg: String) {
        val i = Intent(ctx, AlarmActivity::class.java)
            .putExtra("type", type.name).putExtra("text", text).putExtra("link", link)
            .putExtra("source", source).putExtra("pkg", pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, type.ordinal, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (type == AlertType.START) "ВНИМАНИЕ, УГРОЗА" else "ОТБОЙ, УГРОЗА МИНОВАЛА"
        val n = NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText("$source: ${text.take(100)}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setOngoing(true).build()
        ctx.getSystemService(NotificationManager::class.java).notify(100 + type.ordinal, n)
        AlarmController.start(ctx, type)       // звук работает, даже если Activity заблокирована ОС
        runCatching { ctx.startActivity(i) }   // прямой запуск (SYSTEM_ALERT_WINDOW)
    }
}
