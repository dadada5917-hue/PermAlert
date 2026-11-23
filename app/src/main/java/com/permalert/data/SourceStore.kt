package com.permalert.data

import android.content.Context

/** Настройки локальных источников уведомлений и глобального режима «Не беспокоить». */
object SourceStore {
    private fun p(c: Context) = c.getSharedPreferences("sources", Context.MODE_PRIVATE)

    fun packages(c: Context): Set<String> = p(c).getStringSet("pkgs", emptySet())?.toSet() ?: emptySet()
    fun setPackages(c: Context, s: Set<String>) { p(c).edit().putStringSet("pkgs", HashSet(s)).apply() }

    fun sms(c: Context): Boolean = p(c).getBoolean("sms", false)
    fun setSms(c: Context, on: Boolean) { p(c).edit().putBoolean("sms", on).apply() }

    /** По умолчанию мониторинг включён, чтобы обновление 1.6 -> 1.7 не выключило защиту неожиданно. */
    fun monitoringEnabled(c: Context): Boolean = p(c).getBoolean("monitoring_enabled", true)
    fun setMonitoringEnabled(c: Context, enabled: Boolean) { p(c).edit().putBoolean("monitoring_enabled", enabled).commit() }

    fun selectedAllowedPackages(c: Context): Set<String> = packages(c).filterTo(mutableSetOf()) { it != c.packageName }
}
