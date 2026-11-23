package com.permalert.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.permalert.data.SourceStore

/** Можно выбрать любое установленное приложение, которое публикует уведомления. */
class SourcesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }
        val validPackages = apps.map { it.first }.toSet()
        setContent {
            var selected by remember { mutableStateOf(SourceStore.packages(this@SourcesActivity).intersect(validPackages)) }
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Выберите приложения, уведомления которых PermAlert будет анализировать. Доступно приложений: ${apps.size}")
                Text("Выбирайте только доверенные источники. Текст уведомлений выбранных приложений будет обрабатываться локально на телефоне.")
                if (apps.isEmpty()) Text("Не найдено приложений с ярлыком запуска.")
                LazyColumn {
                    items(apps, key = { it.first }) { (pkg, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selected = if (pkg in selected) selected - pkg else selected + pkg
                                SourceStore.setPackages(this@SourcesActivity, selected)
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = pkg in selected, onCheckedChange = {
                                selected = if (it) selected + pkg else selected - pkg
                                SourceStore.setPackages(this@SourcesActivity, selected)
                            })
                            Text("$label  ($pkg)", modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
