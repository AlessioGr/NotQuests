package com.notquests.core.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmorStandAttachmentsQuestTest {
    @Test
    void addsWrappedQuestTokensWithoutDuplicates() {
        assertEquals("°daily°", ArmorStandAttachments.add(null, "daily"));
        assertEquals("°daily°", ArmorStandAttachments.add("°daily°", "daily"));
        assertEquals("°daily°weekly°", ArmorStandAttachments.add("°daily°", "weekly"));
    }

    @Test
    void removesQuestTokensAndReportsWhenStorageIsEmpty() {
        final ArmorStandAttachments.Removal first =
                ArmorStandAttachments.remove("°daily°weekly°", "daily");
        assertTrue(first.removed());
        assertFalse(first.empty());
        assertEquals("°weekly°", first.storedQuests());

        final ArmorStandAttachments.Removal last =
                ArmorStandAttachments.remove("°daily°", "daily");
        assertTrue(last.removed());
        assertTrue(last.empty());
        assertEquals("", last.storedQuests());
    }

    @Test
    void listsOnlyRealQuestNames() {
        assertEquals(java.util.List.of("daily", "weekly"), ArmorStandAttachments.questNames("°daily°weekly°"));
    }

    @Test
    void plansAddAndRemoveMessages() {
        final ArmorStandAttachments.QuestAttachmentUpdate added =
                ArmorStandAttachments.addQuest(null, "daily", true);
        assertTrue(added.changed());
        assertFalse(added.removeStorage());
        assertEquals("°daily°", added.storedQuests());
        assertEquals(
                "<DARK_GREEN>Quest with the name <highlight>daily</highlight> was added to this armor stand (showing)!\n"
                        + "<DARK_GREEN>Attached Quests: <highlight>°daily°",
                added.message());

        final ArmorStandAttachments.QuestAttachmentUpdate duplicate =
                ArmorStandAttachments.addQuest("°daily°", "daily", false);
        assertFalse(duplicate.changed());
        assertEquals(
                "<RED>Error: That armor stand already has the Quest <highlight>daily</highlight> attached to it!",
                duplicate.message());

        final ArmorStandAttachments.QuestAttachmentUpdate removed =
                ArmorStandAttachments.removeQuest("°daily°", "daily");
        assertTrue(removed.changed());
        assertTrue(removed.removeStorage());
        assertEquals("", removed.storedQuests());
        assertEquals(
                "<DARK_GREEN>Quest with the name <highlight>daily</highlight> was removed from this armor stand!\n"
                        + "<DARK_GREEN>Attached Quests: <highlight>",
                removed.message());
    }

    @Test
    void formatsCheckMessages() {
        assertEquals(
                java.util.List.of(
                        "<GRAY>Armor Stand Entity ID: <WHITE>stand-1",
                        "<BLUE>All 2 attached showing Quests:",
                        "<GRAY>1. <YELLOW>daily",
                        "<GRAY>2. <YELLOW>weekly",
                        "<BLUE>All attached non-showing Quests: <GRAY>None"),
                ArmorStandAttachments.checkMessages(
                        "stand-1",
                        true,
                        "°daily°weekly°",
                        false,
                        null));
    }

    @Test
    void formatsPortableClickFeedback() {
        assertEquals(
                "<success>Quest <highlight>daily</highlight> attached to <highlight2>armorstand:abc</highlight2>.",
                ArmorStandAttachments.questAttachedToNpc("daily", "armorstand:abc"));
        assertEquals(
                "<success>Quest <highlight>daily</highlight> removed from <highlight2>armorstand:abc</highlight2>.",
                ArmorStandAttachments.questRemovedFromNpc("daily", "armorstand:abc"));
        assertEquals(
                "<main>No NotQuests quests are attached to <highlight>armorstand:abc</highlight>.",
                ArmorStandAttachments.attachedQuestsMessage("armorstand:abc", java.util.List.of()));
        assertEquals(
                "<main>Quests attached to <highlight>armorstand:abc</highlight>: <highlight2>daily, weekly</highlight2>",
                ArmorStandAttachments.attachedQuestsMessage("armorstand:abc", java.util.List.of("daily", "weekly")));
    }
}
