package com.permalert.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.media.RingtoneManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.permalert.alarm.AlarmController
import com.permalert.data.SoundStore
import com.permalert.data.SourceStore
import com.permalert.domain.AlertType
import com.permalert.domain.Classifier

class MainActivity : ComponentActivity() {
    private var smsOn by mutableStateOf(false)
    private var monitoringOn by mutableStateOf(true)
    private var soundStart by mutableStateOf("")
    private var soundEnd by mutableStateOf("")
    private var selfTest by mutableStateOf("")
    private var pickType = AlertType.START

    private val soundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            SoundStore.set(this, pickType, uri?.toString() ?: "")
            refreshSounds()
        }
    }
    private val smsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> setSms(granted) }
    private val notificationsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun refreshSounds() {
        soundStart = SoundStore.label(this, AlertType.START)
        soundEnd = SoundStore.label(this, AlertType.END)
    }

    private fun pickSound(type: AlertType) {
        pickType = type
        soundLauncher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_NOTIFICATION)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, SoundStore.resolve(this, type))
        )
    }

    private fun setSms(enabled: Boolean) {
        smsOn = enabled
        SourceStore.setSms(this, enabled)
    }

    private fun toggleSms(enabled: Boolean) {
        if (!enabled) setSms(false)
        else if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) setSms(true)
        else smsLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshSounds()
        smsOn = SourceStore.sms(this) && checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        monitoringOn = SourceStore.monitoringEnabled(this)

        setContent {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("PermAlert 1.8\nПолностью локальная работа на телефоне")
                Text("Источники оповещений: MAX, МЧС, SMS и любые выбранные приложения телефона.")

                Button(
                    onClick = { setMonitoring(!monitoringOn) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (monitoringOn) "Включить режим «Не беспокоить»" else "Выключить режим «Не беспокоить»")
                }
                Text(if (monitoringOn) "PermAlert сейчас анализирует выбранные источники и может запускать оповещения." else "ПАУЗА: уведомления и SMS не анализируются, тревоги не запускаются.")

                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else selfTest = "Разрешение на уведомления уже выдано."
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Разрешить уведомления PermAlert")
                }
                Button(onClick = { openNotificationAccessSettings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("1. Дать PermAlert доступ к уведомлениям выбранных приложений")
                }
                Button(onClick = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Разрешить показ тревоги поверх окон")
                }
                Button(onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Разрешить звук при режиме «Не беспокоить»")
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    Button(onClick = { startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Разрешить полноэкранные тревоги")
                    }
                }
                Button(onClick = { startActivity(Intent(this@MainActivity, SourcesActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("2. Выбрать приложения для мониторинга")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = smsOn, onCheckedChange = { toggleSms(it) })
                    Text("Получать сигналы из SMS")
                }

                Spacer(Modifier.height(8.dp))
                Text("Проверка")
                Button(onClick = { runSelfTest() }, modifier = Modifier.fillMaxWidth()) { Text("Самопроверка без запуска сирены") }
                if (selfTest.isNotBlank()) Text(selfTest)
                Button(onClick = { testAlarm(AlertType.START) }, modifier = Modifier.fillMaxWidth()) { Text("Тест тревоги") }
                Button(onClick = { testAlarm(AlertType.END) }, modifier = Modifier.fillMaxWidth()) { Text("Тест отбоя") }

                Spacer(Modifier.height(8.dp))
                Text("Звуки")
                Button(onClick = { pickSound(AlertType.START) }, modifier = Modifier.fillMaxWidth()) { Text("Звук тревоги: $soundStart") }
                Button(onClick = { pickSound(AlertType.END) }, modifier = Modifier.fillMaxWidth()) { Text("Звук отбоя: $soundEnd") }
                Button(onClick = { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("История оповещений")
                }
                Text("Экономия батареи: приложение не опрашивает интернет, не запускает постоянный фоновый цикл и реагирует на системные уведомления Android и SMS.")
            }
        }
    }

    private fun setMonitoring(enabled: Boolean) {
        SourceStore.setMonitoringEnabled(this, enabled)
        monitoringOn = enabled
        if (!enabled) {
            // При включении «Не беспокоить» прекращаем уже звучащее оповещение.
            AlarmController.stop(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (SourceStore.monitoringEnabled(this) != monitoringOn) monitoringOn = SourceStore.monitoringEnabled(this)
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun runSelfTest() {
        val cases = listOf(
            "Внимание! Угроза атаки БПЛА" to AlertType.START,
            "Объявлен отбой угрозы БПЛА" to AlertType.END,
            "МЧС: проводятся учебные учения с БПЛА" to AlertType.NONE,
            "В новостях показали новый дрон" to AlertType.NONE,
            "Воздушная тревога" to AlertType.START,
            "Угроза миновала" to AlertType.END,
            "Сообщение: угроза БПЛА не подтверждена" to AlertType.NONE,
            "Внимание: угроза атаки БПЛА" to AlertType.START
        )
        val passed = cases.count { (text, expected) -> Classifier.classifyStrict(text) == expected }
        selfTest = "Классификатор: $passed/${cases.size}. Сирена не запускалась."
    }

    private fun testAlarm(type: AlertType) {
        if (!monitoringOn) {
            selfTest = "Тестовые оповещения отключены режимом «Не беспокоить»."
            return
        }
        AlarmController.start(this, type)
        startActivity(
            Intent(this, AlarmActivity::class.java)
                .putExtra("type", type.name)
                .putExtra("text", if (type == AlertType.START) "Тестовая тревога" else "Тестовый отбой")
                .putExtra("source", "Тест")
                .putExtra("link", "")
        )
    }
}
