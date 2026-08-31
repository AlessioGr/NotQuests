package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestOrderRequirementsTest {
    private static final List<Quest.OrderEntry> QUESTS = List.of(
            new Quest.OrderEntry("a", "Quest A"),
            new Quest.OrderEntry("b", "Quest B"),
            new Quest.OrderEntry("c", "Quest C"));

    @Test
    void firstToLastRequiresEarlierIncompleteQuest() {
        final String missing = Quest.OrderRequirements.missingQuestDisplayName(
                PredefinedProgressOrder.firstToLast(),
                "c",
                2,
                QUESTS,
                Set.of("a")::contains);

        assertEquals("Quest B", missing);
    }

    @Test
    void lastToFirstRequiresLaterIncompleteQuest() {
        final String missing = Quest.OrderRequirements.missingQuestDisplayName(
                PredefinedProgressOrder.lastToFirst(),
                "a",
                0,
                QUESTS,
                Set.of("c")::contains);

        assertEquals("Quest B", missing);
    }

    @Test
    void customOrderUsesConfiguredQuestNameForMissingMessage() {
        final String missing = Quest.OrderRequirements.missingQuestDisplayName(
                PredefinedProgressOrder.custom(new ArrayList<>(List.of("intro", "boss"))),
                "boss",
                1,
                QUESTS,
                ignored -> false);

        assertEquals("intro", missing);
    }

    @Test
    void requirementMessageMatchesOldPaperText() {
        final String message = Quest.OrderRequirements.requirementMessage(
                PredefinedProgressOrder.firstToLast(),
                "c",
                2,
                QUESTS,
                Set.of("a")::contains);

        assertEquals("Quest Quest B needs to be completed first", message);
    }
}
