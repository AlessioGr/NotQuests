package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.npc.NpcAttachments.Selector;

import java.util.UUID;

class NpcAttachmentsSelectorTest {
    @Test
    void parsesCitizensStyleNumericSelectors() {
        final Selector selector = Selector.parse("citizens:42").orElseThrow();

        assertEquals("citizens", selector.type());
        assertEquals("42", selector.id().getEitherAsString());
    }

    @Test
    void parsesFancyNpcStringSelectors() {
        final Selector selector = Selector.parse("fancynpcs:guide_npc").orElseThrow();

        assertEquals("fancynpcs", selector.type());
        assertEquals("guide_npc", selector.id().getEitherAsString());
    }

    @Test
    void parsesArmorStandUuidSelectors() {
        final UUID uuid = UUID.fromString("75f08e0a-081b-4834-b59d-e90a41fdae94");
        final Selector selector = Selector.parse("armorstand:" + uuid).orElseThrow();

        assertEquals("armorstand", selector.type());
        assertEquals(uuid.toString(), selector.id().getEitherAsString());
    }

    @Test
    void rejectsInvalidSelectors() {
        assertTrue(Selector.parse("").isEmpty());
        assertTrue(Selector.parse("armorstand:not-a-uuid").isEmpty());
        assertTrue(Selector.parse("citizens:not-a-number").isEmpty());
    }
}
