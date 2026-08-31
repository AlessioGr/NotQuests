package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmorStandAttachmentsToolsTest {
    @Test
    void mapsQuestToolsToStableItemIds() {
        assertEquals(
                ArmorStandAttachments.QUEST_CHECK,
                ArmorStandAttachments.questToolId(ArmorStandAttachments.QuestTool.CHECK, false));
        assertEquals(
                ArmorStandAttachments.QUEST_ADD_SHOWING,
                ArmorStandAttachments.questToolId(ArmorStandAttachments.QuestTool.ADD, false));
        assertEquals(
                ArmorStandAttachments.QUEST_ADD_HIDDEN,
                ArmorStandAttachments.questToolId(ArmorStandAttachments.QuestTool.ADD, true));
        assertEquals(
                ArmorStandAttachments.QUEST_REMOVE_SHOWING,
                ArmorStandAttachments.questToolId(ArmorStandAttachments.QuestTool.REMOVE, false));
        assertEquals(
                ArmorStandAttachments.QUEST_REMOVE_HIDDEN,
                ArmorStandAttachments.questToolId(ArmorStandAttachments.QuestTool.REMOVE, true));
    }

    @Test
    void describesQuestToolsForCommandAndItemOutput() {
        assertEquals(
                "Check armor stand quest attachments",
                ArmorStandAttachments.questToolAction(ArmorStandAttachments.QuestTool.CHECK));
        assertEquals(
                "Attach quest to armor stand",
                ArmorStandAttachments.questToolAction(ArmorStandAttachments.QuestTool.ADD));
        assertEquals(
                "Remove quest from armor stand",
                ArmorStandAttachments.questToolAction(ArmorStandAttachments.QuestTool.REMOVE));
    }

    @Test
    void classifiesToolBehavior() {
        assertTrue(ArmorStandAttachments.isQuestAttachTool(ArmorStandAttachments.QUEST_ADD_SHOWING));
        assertTrue(ArmorStandAttachments.isQuestAttachTool(ArmorStandAttachments.QUEST_ADD_HIDDEN));
        assertTrue(ArmorStandAttachments.isQuestRemoveTool(ArmorStandAttachments.QUEST_REMOVE_SHOWING));
        assertTrue(ArmorStandAttachments.isQuestRemoveTool(ArmorStandAttachments.QUEST_REMOVE_HIDDEN));
        assertTrue(ArmorStandAttachments.isShowingQuestTool(ArmorStandAttachments.QUEST_ADD_SHOWING));
        assertTrue(ArmorStandAttachments.isShowingQuestTool(ArmorStandAttachments.QUEST_REMOVE_SHOWING));
        assertFalse(ArmorStandAttachments.isShowingQuestTool(ArmorStandAttachments.QUEST_ADD_HIDDEN));
        assertTrue(ArmorStandAttachments.requiresQuestName(ArmorStandAttachments.OBJECTIVE_COMPLETION_NPC));
        assertFalse(ArmorStandAttachments.requiresQuestName(ArmorStandAttachments.QUEST_CHECK));
        assertEquals(
                "<RED>Error: Your item has no valid quest attached to it.",
                ArmorStandAttachments.missingQuestName());
    }
}
