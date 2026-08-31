package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.structs.Quest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class NpcAttachmentsTest {
    @Test
    void returnsValidAttachmentsWithQuestIdentifiers() {
        final Quest quest = new Quest("daily");
        final NQNPCID npcId = NQNPCID.fromUUID(java.util.UUID.randomUUID());
        quest.addNpcAttachment("armorstand", npcId, "Guide", true);

        final NpcAttachments.QuestAttachments plan = NpcAttachments.collect(java.util.List.of(quest));

        assertEquals(1, plan.attachments().size());
        assertEquals("daily", plan.attachments().getFirst().questIdentifier());
        assertEquals(npcId, plan.attachments().getFirst().attachment().npcId());
        assertEquals(0, plan.warnings().size());
    }

    @Test
    void returnsConversationWarningsForInvalidAttachments() {
        final ConversationManager.Conversation conversation =
                new ConversationManager.Conversation(
                        "intro",
                        List.of(),
                        "default",
                        List.of(),
                        List.of(new NpcAttachment("", null, "", false)),
                        0,
                        List.of(),
                        List.of(),
                        false);

        final NpcAttachments.ConversationAttachments plan =
                NpcAttachments.collectConversations(List.of(conversation));

        assertEquals(0, plan.attachments().size());
        assertEquals(
                List.of("Skipping invalid NPC attachment for conversation <highlight>intro</highlight>."),
                plan.warnings());
    }

    @Test
    void filtersQuestAttachmentsByVisibilityAndSkipsInvalidNpcIds() {
        final NQNPCID visible = NQNPCID.fromInteger(1);
        final NQNPCID hidden = NQNPCID.fromInteger(2);
        final List<NpcAttachment> attachments = List.of(
                new NpcAttachment("citizens", visible, "Visible", true),
                new NpcAttachment("citizens", hidden, "Hidden", false),
                new NpcAttachment("citizens", null, "Invalid", true));

        assertEquals(
                List.of(visible, hidden),
                NpcAttachments.questAttachments(attachments, null).stream()
                        .map(NpcAttachment::npcId)
                        .toList());
        assertEquals(
                List.of(visible),
                NpcAttachments.questAttachments(attachments, true).stream()
                        .map(NpcAttachment::npcId)
                        .toList());
        assertEquals(
                List.of(hidden),
                NpcAttachments.questAttachments(attachments, false).stream()
                        .map(NpcAttachment::npcId)
                        .toList());
    }

    @Test
    void findsQuestAndConversationAttachmentsWithoutPlatformLookups() {
        final NQNPCID questNpc = NQNPCID.fromInteger(4);
        final Quest quest = new Quest("daily");
        quest.addNpcAttachment("citizens", questNpc, "Guide", true);
        final NQNPCID conversationNpc = NQNPCID.fromString("greeter");
        final ConversationManager.Conversation conversation =
                new ConversationManager.Conversation(
                        "intro",
                        List.of(),
                        "default",
                        List.of(),
                        List.of(new NpcAttachment("fancynpcs", conversationNpc, "Greeter", false)),
                        0,
                        List.of(),
                        List.of(),
                        false);

        assertTrue(NpcAttachments.hasAttachment(List.of(quest), List.of(), "CITIZENS", questNpc));
        assertTrue(NpcAttachments.hasAttachment(
                List.of(), List.of(conversation), "FancyNPCs", conversationNpc));
        assertFalse(NpcAttachments.hasAttachment(
                List.of(quest), List.of(conversation), "citizens", NQNPCID.fromInteger(99)));
    }

    @Test
    void completesNpcSelectionOnlyOnce() {
        final NpcAttachments.Selections selections = new NpcAttachments.Selections();
        final AtomicReference<com.notquests.core.platform.NotQuestsAdapter.NpcSelection> selected =
                new AtomicReference<>();
        final int selectionId = selections.register(selected::set);
        final NQNPCID npcId = NQNPCID.fromInteger(17);

        assertTrue(selections.complete(selectionId, "citizens", npcId, "Guide"));
        assertEquals("citizens:17", selected.get().selector());
        assertEquals("citizens:17 (Guide)", selected.get().label());
        assertFalse(selections.complete(selectionId, "citizens", npcId, "Guide"));
    }

    @Test
    void identifiesNativeNpcTraitsThatNoLongerHaveCoreAttachments() {
        final NQNPCID attached = NQNPCID.fromInteger(3);
        final NQNPCID stale = NQNPCID.fromInteger(4);

        assertEquals(
                List.of(stale),
                NpcAttachments.staleNativeAttachments(
                        List.of(attached),
                        List.of(attached, stale, stale)));
    }
}
