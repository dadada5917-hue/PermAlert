package com.permalert.domain

/** Pure state rules shared by live alert handling and reboot recovery. */
object AlertStatePolicy {
    const val ACTIVE_WINDOW_MS = 3 * 60 * 60 * 1000L

    fun isActive(lastType: String?, lastTimestampMs: Long?, nowMs: Long): Boolean {
        if (lastType != AlertType.START.name || lastTimestampMs == null) return false
        val age = nowMs - lastTimestampMs
        return age in 0 until ACTIVE_WINDOW_MS
    }

    fun isNewerEvent(eventTimestampMs: Long, lastEventTimestampMs: Long?): Boolean =
        lastEventTimestampMs == null || eventTimestampMs > lastEventTimestampMs

    fun shouldFire(type: AlertType, lastType: String?, lastTimestampMs: Long?, nowMs: Long): Boolean {
        if (type == AlertType.NONE) return false
        val active = isActive(lastType, lastTimestampMs, nowMs)
        return when (type) {
            AlertType.START -> !active
            AlertType.END -> active
            AlertType.NONE -> false
        }
    }
}
