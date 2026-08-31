package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NeoForgeWorldNamesTest {
    @Test
    void beamPreviewAcceptsPaperStyleVanillaWorldAliases() {
        assertTrue(NeoForgeWorldNames.matches("minecraft", "overworld", "world"));
        assertTrue(NeoForgeWorldNames.matches("minecraft", "the_nether", "world_nether"));
        assertTrue(NeoForgeWorldNames.matches("minecraft", "the_end", "world_the_end"));
        assertTrue(NeoForgeWorldNames.matches("minecraft", "overworld", "minecraft:overworld"));
        assertFalse(NeoForgeWorldNames.matches("minecraft", "overworld", "world_nether"));
    }
}
