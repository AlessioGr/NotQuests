package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class NpcAttachmentsLabelsTest {
    @Test
    void attachedNpcListUsesReadableLabels() {
        final String formatted = NpcAttachments.formatAttachedNPCs(List.of(
                new FakeNpc("citizens", NQNPCID.fromInteger(5), "Guard Captain"),
                new FakeNpc("fancynpcs", NQNPCID.fromString("merchant"), null)));

        assertEquals("citizens:5 (Guard Captain), fancynpcs:merchant", formatted);
    }

    @Test
    void emptyAttachedNpcListShowsNone() {
        assertEquals("none", NpcAttachments.formatAttachedNPCs(List.of()));
    }

    @Test
    void selectorUsesCanonicalLowercaseTypeAndRawId() {
        assertEquals("citizens:5", NpcAttachments.selector("Citizens", NQNPCID.fromInteger(5)));
        assertEquals("", NpcAttachments.selector("", NQNPCID.fromInteger(5)));
        assertEquals("", NpcAttachments.selector("citizens", null));
    }

    private record FakeNpc(String npcType, NQNPCID npcId, String npcName) implements NpcAttachments.Npc {}
}
