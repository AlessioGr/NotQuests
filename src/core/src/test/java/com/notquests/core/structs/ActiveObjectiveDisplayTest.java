package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActiveObjectiveDisplayTest {
    @Test
    void choosesOneProgressTranslationKeys() {
        assertEquals(
                "objective-tracking.actionbar-progress-update.only-one-max-progress",
                ActiveObjective.actionBarTranslationKey(1));
        assertEquals(
                "objective-tracking.bossbar-progress-update.only-one-max-progress",
                ActiveObjective.bossBarTranslationKey(1));
    }

    @Test
    void choosesDefaultTranslationKeys() {
        assertEquals(
                "objective-tracking.actionbar-progress-update.default",
                ActiveObjective.actionBarTranslationKey(5));
        assertEquals(
                "objective-tracking.bossbar-progress-update.default",
                ActiveObjective.bossBarTranslationKey(5));
    }

    @Test
    void clampsProgress() {
        assertEquals(0.0f, ActiveObjective.clampedProgress(-1, 5));
        assertEquals(0.5f, ActiveObjective.clampedProgress(5, 10));
        assertEquals(1.0f, ActiveObjective.clampedProgress(15, 10));
    }

    @Test
    void hidesCompletedBossBarOnlyWhenConfigured() {
        assertTrue(ActiveObjective.shouldHideCompletedBossBar(1.0f, false));
        assertFalse(ActiveObjective.shouldHideCompletedBossBar(1.0f, true));
        assertFalse(ActiveObjective.shouldHideCompletedBossBar(0.99f, false));
    }
}
