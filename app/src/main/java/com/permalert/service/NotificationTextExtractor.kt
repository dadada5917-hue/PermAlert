package com.permalert.service

import android.app.Notification
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import java.util.Locale

/**
 * Собирает весь текст, который источник действительно положил в extras уведомления.
 * Не пытается читать экран, обходить приватность или открывать исходное приложение.
 */
internal object NotificationTextExtractor {
    private const val MAX_CHARS = 6_000

    fun extract(sbn: StatusBarNotification): String {
        val notification = sbn.notification
        val extras = runCatching { notification.extras }.getOrNull() ?: return ""
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            if (parts.size >= 100) return
            val text = value?.toString()?.replace('\u0000', ' ')?.trim().orEmpty().take(2_000)
            if (text.isNotBlank()) parts += text
        }

        fun read(key: String): CharSequence? = runCatching { extras.getCharSequence(key) }.getOrNull()

        // Заголовки: BIG_TITLE может содержать более подробную версию обычного заголовка.
        add(read(Notification.EXTRA_TITLE_BIG))
        add(read(Notification.EXTRA_TITLE))
        add(read(Notification.EXTRA_CONVERSATION_TITLE))

        // Основной и расширенный текст, подпись и сводка.
        add(read(Notification.EXTRA_BIG_TEXT))
        add(read(Notification.EXTRA_TEXT))
        add(read(Notification.EXTRA_SUMMARY_TEXT))
        add(read(Notification.EXTRA_SUB_TEXT))
        add(read(Notification.EXTRA_INFO_TEXT))

        // InboxStyle и некоторые мессенджеры передают строки отдельным массивом.
        runCatching {
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach(::add)
        }

        // Сообщения MessagingStyle: сохраняем отправителя и текст каждого доступного сообщения.
        addMessagingMessages(runCatching { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }.getOrNull(), ::add)
        addMessagingMessages(runCatching { extras.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES) }.getOrNull(), ::add)

        // Ответы из RemoteInput могут содержать дополнительный контекст.
        runCatching {
            extras.getCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY)?.forEach(::add)
        }

        // Убираем точные дубликаты, сохраняя первоначальный порядок фрагментов.
        val seen = HashSet<String>()
        val unique = parts.filter { part ->
            val key = part.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
            key.isNotEmpty() && seen.add(key)
        }
        return unique.joinToString(". ").take(MAX_CHARS).trim()
    }

    private inline fun addMessagingMessages(
        rawMessages: Array<out Parcelable>?,
        crossinline add: (CharSequence?) -> Unit
    ) {
        if (rawMessages.isNullOrEmpty()) return
        val messages = rawMessages.mapNotNull { parcelable ->
            runCatching { parcelable as? Notification.MessagingStyle.Message }.getOrNull()
        }.sortedBy { it.timestamp }

        messages.forEach { message ->
            val sender = runCatching { message.sender }.getOrNull()
            val body = runCatching { message.text }.getOrNull()
            if (!sender.isNullOrBlank() && !body.isNullOrBlank()) add("$sender: $body")
            else add(body ?: sender)
        }
    }
}
