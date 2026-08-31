package com.notquests.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.notquests.core.npc.NQNPCID;

class GuiContextTest {
    @Test
    void normalizesMissingIdentifiersToBlankStrings() {
        final GuiContext state = new GuiContext(null, null, null, null);

        assertEquals("", state.questIdentifier());
        assertEquals("", state.categoryIdentifier());
        assertEquals("", state.npcType());
        assertNull(state.npcId());
    }

    @Test
    void keepsNpcIdentityForCoreGuiFiltering() {
        final NQNPCID npcId = NQNPCID.fromInteger(7);
        final GuiContext state = new GuiContext("QuestA", "Story", "Citizens", npcId);

        assertEquals("QuestA", state.questIdentifier());
        assertEquals("Story", state.categoryIdentifier());
        assertEquals("Citizens", state.npcType());
        assertEquals(npcId, state.npcId());
    }
}
