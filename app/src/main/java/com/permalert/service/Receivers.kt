package com.permalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.permalert.alarm.AlertDispatcher

/** После перезагрузки проверяет сохранённое состояние тревоги; сетевых задач не запускает. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { AlertDispatcher.restoreActive(context) } finally { pending.finish() }
        }
    }
}
