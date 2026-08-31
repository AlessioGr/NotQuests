package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.ConfigurationManager;

import java.util.List;

class QuestVisibilityTest {
    @Test
    void hidesAlreadyAcceptedQuestWhenConfigured() {
        final Quest quest = new Quest("daily");
        final QuestPlayer player = new QuestPlayer("player", "default");
        player.addActiveQuest("daily");

        assertEquals(List.of(), Quest.visibleQuestIdentifiers(
                List.of(quest), player, settings(true, false, false, false), 1000, ignored -> true));
    }

    @Test
    void hidesQuestWhenCompletionLimitReached() {
        final Quest quest = new Quest("daily");
        quest.setMaxCompletions(1);
        final QuestPlayer player = new QuestPlayer("player", "default");
        player.addCompletedQuest(new QuestPlayer.CompletedQuest("daily", "player", 500));

        assertEquals(List.of(), Quest.visibleQuestIdentifiers(
                List.of(quest), player, settings(false, true, false, false), 1000, ignored -> true));
    }

    @Test
    void hidesQuestOnAcceptCooldown() {
        final Quest quest = new Quest("daily");
        quest.setAcceptCooldownComplete(10_000);
        final QuestPlayer player = new QuestPlayer("player", "default");
        player.addCompletedQuest(new QuestPlayer.CompletedQuest("daily", "player", 500));

        assertEquals(List.of(), Quest.visibleQuestIdentifiers(
                List.of(quest), player, settings(false, false, true, false), 1000, ignored -> true));
    }

    @Test
    void delegatesRequirementVisibilityToCallback() {
        final Quest quest = new Quest("daily");

        assertEquals(List.of(), Quest.visibleQuestIdentifiers(
                List.of(quest), null, settings(false, false, false, true), 1000, ignored -> false));
        assertEquals(List.of("daily"), Quest.visibleQuestIdentifiers(
                List.of(quest), null, settings(false, false, false, true), 1000, ignored -> true));
    }

    private static ConfigurationManager settings(
            final boolean accepted,
            final boolean limits,
            final boolean cooldown,
            final boolean conditions) {
        final YamlConfig yaml = YamlConfig.empty();
        yaml.set("gui.quest-visibility-evaluations.already-accepted.enabled", accepted);
        yaml.set("gui.quest-visibility-evaluations.max-accepts.enabled", limits);
        yaml.set("gui.quest-visibility-evaluations.accept-cooldown.enabled", cooldown);
        yaml.set("gui.quest-visibility-evaluations.conditions.enabled", conditions);
        final ConfigurationManager settings = new ConfigurationManager();
        settings.loadFrom(yaml);
        return settings;
    }
}
