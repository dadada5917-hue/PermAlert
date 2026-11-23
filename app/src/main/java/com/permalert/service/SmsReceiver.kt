package com.permalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.permalert.alarm.AlertDispatcher
import com.permalert.data.SourceStore
import com.permalert.domain.AlertType
import com.permalert.domain.Classifier
import com.permalert.domain.MonitoringPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Локальная обработка входящих SMS без сетевых запросов. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!MonitoringPolicy.shouldProcessSms(SourceStore.monitoringEnabled(context), SourceStore.sms(context))) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return
        val body = messages.joinToString(" ") { it.messageBody.orEmpty() }.trim()
        if (body.isBlank()) return
        val sender = messages.first().originatingAddress ?: "неизвестный отправитель"
        val type = Classifier.classifyStrict(body)
        if (type == AlertType.NONE) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlertDispatcher.fire(context.applicationContext, type, "SMS: $sender", body)
            } finally {
                pending.finish()
            }
        }
    }
}
