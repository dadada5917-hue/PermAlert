package com.permalert.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertStatePolicyTest {
    private val now = 1_000_000L

    @Test fun firstStartIsAccepted() {
        assertTrue(AlertStatePolicy.shouldFire(AlertType.START, null, null, now))
    }

    @Test fun repeatedStartDuringActiveAlertIsIgnored() {
        assertFalse(AlertStatePolicy.shouldFire(AlertType.START, "START", now - 1_000, now))
    }

    @Test fun endIsAcceptedOnlyDuringActiveAlert() {
        assertTrue(AlertStatePolicy.shouldFire(AlertType.END, "START", now - 1_000, now))
        assertFalse(AlertStatePolicy.shouldFire(AlertType.END, "END", now - 1_000, now))
        assertFalse(AlertStatePolicy.shouldFire(AlertType.END, null, null, now))
    }

    @Test fun staleStartDoesNotBlockNewAlert() {
        assertTrue(AlertStatePolicy.shouldFire(AlertType.START, "START", now - AlertStatePolicy.ACTIVE_WINDOW_MS, now))
    }

    @Test fun futureTimestampIsNotTreatedAsActive() {
        assertFalse(AlertStatePolicy.isActive("START", now + 1_000, now))
    }

    @Test fun noneNeverFires() {
        assertFalse(AlertStatePolicy.shouldFire(AlertType.NONE, "START", now - 1_000, now))
    }

    @Test fun staleNotificationCannotReopenAnOlderAlert() {
        assertFalse(AlertStatePolicy.isNewerEvent(now - 5_000, now))
        assertTrue(AlertStatePolicy.isNewerEvent(now + 1, now))
        assertTrue(AlertStatePolicy.isNewerEvent(now, null))
    }
}
