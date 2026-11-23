package com.permalert.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.permalert.domain.AlertType

/** Выбор звуков: по умолчанию — системные мелодии будильника (для отбоя — другая, чем для тревоги). */
object SoundStore {
    private fun p(c: Context) = c.getSharedPreferences("sound", Context.MODE_PRIVATE)
    private fun key(t: AlertType) = if (t == AlertType.START) "start" else "end"

    fun set(c: Context, t: AlertType, uri: String) = p(c).edit().putString(key(t), uri).apply()

    fun resolve(c: Context, t: AlertType): Uri {
        val saved = p(c).getString(key(t), "").orEmpty()
        if (saved.isNotEmpty()) return Uri.parse(saved)
        val alarm = RingtoneManager.getActualDefaultRingtoneUri(c, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (t == AlertType.START) return alarm
        // Отбой: другая мелодия будильника (последняя в списке, не совпадающая с мелодией тревоги)
        return otherAlarm(c, alarm) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun otherAlarm(c: Context, not: Uri): Uri? = runCatching {
        val rm = RingtoneManager(c).apply { setType(RingtoneManager.TYPE_ALARM) }
        val n = rm.cursor.count
        (n - 1 downTo 0).map { rm.getRingtoneUri(it) }.firstOrNull { it != null && it != not }
    }.getOrNull()

    fun label(c: Context, t: AlertType): String =
        runCatching { RingtoneManager.getRingtone(c, resolve(c, t))?.getTitle(c) }.getOrNull() ?: "по умолчанию"
}
