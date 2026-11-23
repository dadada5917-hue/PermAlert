package com.permalert.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringPolicyTest {
    private val selected = setOf("ru.max", "ru.mchs", "com.example.weather")

    @Test fun selectedAppIsProcessedWhenMonitoringEnabled() {
        assertTrue(MonitoringPolicy.shouldProcessNotifications(true, "com.example.weather", selected, "com.permalert"))
    }

    @Test fun unselectedAppIsIgnored() {
        assertFalse(MonitoringPolicy.shouldProcessNotifications(true, "com.example.mail", selected, "com.permalert"))
    }

    @Test fun doNotDisturbPausesNotificationMonitoring() {
        assertFalse(MonitoringPolicy.shouldProcessNotifications(false, "ru.max", selected, "com.permalert"))
    }

    @Test fun ownNotificationsAreNeverMonitored() {
        assertFalse(MonitoringPolicy.shouldProcessNotifications(true, "com.permalert", selected + "com.permalert", "com.permalert"))
    }

    @Test fun blankPackageIsIgnored() {
        assertFalse(MonitoringPolicy.shouldProcessNotifications(true, "", selected, "com.permalert"))
    }

    @Test fun smsRequiresBothGlobalMonitoringAndSmsSetting() {
        assertTrue(MonitoringPolicy.shouldProcessSms(true, true))
        assertFalse(MonitoringPolicy.shouldProcessSms(false, true))
        assertFalse(MonitoringPolicy.shouldProcessSms(true, false))
    }
}
