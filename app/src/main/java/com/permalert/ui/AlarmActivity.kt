package com.permalert.ui

import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permalert.alarm.AlarmController
import com.permalert.alarm.AlertDispatcher
import com.permalert.data.SourceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.permalert.domain.AlertType

class AlarmActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (!SourceStore.monitoringEnabled(this)) {
            AlarmController.stop(this)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val type = AlertType.valueOf(intent.getStringExtra("type") ?: "START")
        val text = intent.getStringExtra("text").orEmpty()
        val link = intent.getStringExtra("link").orEmpty()
        val source = intent.getStringExtra("source").orEmpty()
        val pkg = intent.getStringExtra("pkg").orEmpty()
        setContent {
            val isStart = type == AlertType.START
            Column(
                Modifier.fillMaxSize()
                    .background(if (isStart) Color(0xFFB71C1C) else Color(0xFF1B5E20))
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        if (isStart) "ВНИМАНИЕ, УГРОЗА" else "ОТБОЙ, УГРОЗА МИНОВАЛА",
                        color = Color.White, fontSize = 32.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    if (source.isNotEmpty()) Text("Источник: $source", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(text, color = Color.White, fontSize = 18.sp, maxLines = 8)
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                AlertDispatcher.acknowledge(this@AlarmActivity)
                                withContext(Dispatchers.Main) {
                                    AlarmController.stop(this@AlarmActivity)
                                    finish()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(96.dp)
                    ) { Text("Я в безопасности", fontSize = 24.sp) }
                    OutlinedButton(
                        onClick = {
                            if (link.isNotEmpty())
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                            else if (pkg.isNotEmpty())
                                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    ) { Text("Открыть источник", fontSize = 20.sp, color = Color.White) }
                }
            }
        }
    }
}
