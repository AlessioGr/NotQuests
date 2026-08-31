package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PluginStatusTest {
    @Test
    void disablesAndRecordsReasonsForAdminDiagnostics() {
        final NotQuestsPlugin.PluginStatus status = new NotQuestsPlugin.PluginStatus();

        status.disableSavingAndLoading("invalid quest config");

        assertTrue(status.isDisabled());
        assertFalse(status.isSavingEnabled());
        assertFalse(status.isLoadingEnabled());
        assertEquals("invalid quest config", status.disableReasons().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> status.disableReasons().add("mutate"));
    }

    @Test
    void canReEnableSavingAndLoadingWithoutDroppingDiagnosticHistory() {
        final NotQuestsPlugin.PluginStatus status = new NotQuestsPlugin.PluginStatus();

        status.disableSavingAndLoading("database error");
        status.enableSavingAndLoading();

        assertFalse(status.isDisabled());
        assertTrue(status.isSavingEnabled());
        assertTrue(status.isLoadingEnabled());
        assertEquals(1, status.disableReasons().size());
    }

    @Test
    void tracksDataLoadingState() {
        final NotQuestsPlugin.PluginStatus status = new NotQuestsPlugin.PluginStatus();

        assertTrue(status.isDataLoading());
        status.setDataLoading(false);
        assertFalse(status.isDataLoading());
    }
}
