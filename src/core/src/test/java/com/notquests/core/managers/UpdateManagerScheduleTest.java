package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UpdateManagerScheduleTest {
    @Test
    void keepsUpdateTimingPolicyInCore() {
        assertEquals(100L, UpdateManager.STARTUP_DELAY_TICKS);
        assertEquals(8 * 60 * 60 * 20L, UpdateManager.CHECK_INTERVAL_TICKS);
        assertEquals(60L, UpdateManager.OP_JOIN_NOTIFICATION_DELAY_TICKS);
    }
}
