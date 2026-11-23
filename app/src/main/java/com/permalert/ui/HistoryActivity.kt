package com.permalert.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.permalert.App
import com.permalert.data.AlertEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent {
            var list by remember { mutableStateOf(listOf<AlertEntity>()) }
            LaunchedEffect(Unit) { list = (application as App).db.alerts().recent() }
            val fmt = remember { SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()) }
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("История оповещений", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) Text("Пока пусто")
                LazyColumn {
                    items(list, key = { it.id }) { a ->
                        val start = a.type == "START"
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(
                                (if (start) "НАЧАЛО УГРОЗЫ" else "ОТБОЙ") + " · " + fmt.format(Date(a.ts)),
                                color = if (start) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                            Text("Источник: ${a.source}")
                            Text(a.text, maxLines = 4)
                        }
                    }
                }
            }
        }
    }
}
