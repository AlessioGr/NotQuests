package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmorStandAttachmentsConversationTest {
    @Test
    void keepsArmorStandConversationMessages() {
        assertEquals(
                "<error>Error: this item has no valid conversation.",
                ArmorStandAttachments.missingItemConversation());
        assertEquals(
                "<error>Error: Conversation <highlight>intro</highlight> does not exist.",
                ArmorStandAttachments.conversationDoesNotExist("intro"));
        assertEquals(
                "<RED>Error: That armor stand already has the Conversation <highlight>intro</highlight> attached to it!",
                ArmorStandAttachments.alreadyAttached("intro"));
        assertEquals(
                "<GREEN>Conversation with the name <highlight>intro</highlight> was added to this poor little armorstand!",
                ArmorStandAttachments.added("intro"));
        assertEquals(
                "<GREEN>All conversations were removed from this armorStand!",
                ArmorStandAttachments.removedAll());
        assertEquals(
                "<RED>This armorstand doesn't have the conversation attached to it.",
                ArmorStandAttachments.noneAttached());
        assertEquals(
                "<success>Conversation <highlight>intro</highlight> attached to <highlight2>armorstand:abc</highlight2>.",
                ArmorStandAttachments.conversationAttachedToNpc("intro", "armorstand:abc"));
        assertEquals(
                "<success>Removed attached conversations from <highlight2>armorstand:abc</highlight2>.",
                ArmorStandAttachments.conversationsRemovedFromNpc("armorstand:abc"));
    }
}
