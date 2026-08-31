package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigurationManagerFallbackMessageTest {
    @Test
    void formatsConciseInvalidValueFallback() {
        assertEquals(
                "Invalid general.yml particle at 'visual.marker.type': 'new_particle'. "
                        + "Falling back to 'ANGRY_VILLAGER' for this server version.",
                ConfigurationManager.invalidValue(
                        "general.yml",
                        "particle",
                        "visual.marker.type",
                        "new_particle",
                        "ANGRY_VILLAGER"));
    }
}
