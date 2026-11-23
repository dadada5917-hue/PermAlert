package com.permalert.alarm

import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.permalert.R
import com.permalert.data.SoundStore
import com.permalert.domain.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Звук (макс. громкость), вибрация и вспышка. Два режима: START (сирена) и END (мягкий сигнал). */
object AlarmController {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var flashJob: Job? = null
    private var endJob: Job? = null
    private var savedVolume = -1

    @Synchronized
    fun start(c: Context, type: AlertType) {
        val ctx = c.applicationContext
        stop(ctx)
        val am = ctx.getSystemService(AudioManager::class.java)
        savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)

        wakeLock = ctx.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "permalert:alarm").apply { acquire(10 * 60_000L) }

        val isStart = type == AlertType.START
        val res = if (isStart) R.raw.alarm_threat else R.raw.alarm_clear
        // Сначала системный/выбранный звук, при ошибке — встроенный из res/raw
        player = runCatching { makePlayer(ctx, SoundStore.resolve(ctx, type), true, res) }.getOrNull()
            ?: runCatching { makePlayer(ctx, null, true, res) }.getOrNull()

        vibrator = if (Build.VERSION.SDK_INT >= 31)
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
        val pattern = if (isStart) longArrayOf(0, 800, 300) else longArrayOf(0, 300, 200, 300, 1500)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, if (isStart) 0 else -1),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        )
        startFlash(ctx, isStart)
        // Отбой: мелодия по кругу, но сама стихает через 20 секунд (тревога звучит до нажатия кнопки)
        if (!isStart) endJob = CoroutineScope(Dispatchers.Default).launch { delay(20_000); stop(ctx) }
    }

    private fun makePlayer(ctx: Context, uri: Uri?, loop: Boolean, res: Int): MediaPlayer {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)            // поток будильника: громкость и режим как у будильника
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            if (uri != null) mp.setDataSource(ctx, uri)
            else ctx.resources.openRawResourceFd(res).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.isLooping = loop
            mp.prepare(); mp.start()
            return mp
        } catch (e: Exception) { mp.release(); throw e }
    }

    private fun startFlash(c: Context, endless: Boolean) {
        val cm = c.getSystemService(CameraManager::class.java)
        val id = runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return
        flashJob = CoroutineScope(Dispatchers.Default).launch {
            var on = false
            var n = 0
            while (endless || n < 6) {
                on = !on
                runCatching { cm.setTorchMode(id, on) }
                delay(400); n++
            }
            runCatching { cm.setTorchMode(id, false) }
        }
    }

    @Synchronized
    fun stop(c: Context) {
        val ctx = c.applicationContext
        player?.runCatching { stop(); release() }; player = null
        vibrator?.cancel()
        flashJob?.cancel(); flashJob = null
        endJob?.cancel(); endJob = null
        runCatching {
            val cm = ctx.getSystemService(CameraManager::class.java)
            cm.cameraIdList.forEach { cm.setTorchMode(it, false) }
        }
        wakeLock?.takeIf { it.isHeld }?.release(); wakeLock = null
        if (savedVolume >= 0) {
            ctx.getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
            savedVolume = -1
        }
        ctx.getSystemService(NotificationManager::class.java).apply { cancel(100); cancel(101) }
    }
}
