package com.permalert.domain

/** Общая политика паузы и выбора локальных приложений-источников. */
object MonitoringPolicy {
    fun shouldProcessNotifications(enabled: Boolean, sourcePackage: String, selectedPackages: Set<String>, ownPackage: String): Boolean {
        return enabled && sourcePackage.isNotBlank() && sourcePackage != ownPackage && sourcePackage in selectedPackages
    }

    fun shouldProcessSms(enabled: Boolean, smsEnabled: Boolean): Boolean = enabled && smsEnabled
}
