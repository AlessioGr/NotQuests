package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.managers.LanguageManager.Placeholders;

import java.util.List;
import java.util.Map;

class LanguageManagerPlaceholdersTest {
    @Test
    void appliesQuestObjectiveCategoryNpcAndProvidedPlaceholders() {
        final Placeholders placeholders = Placeholders.create()
                .quest(() -> "A Display Quest", () -> "quest_id", () -> "Quest description", () -> 3)
                .objective(() -> 2, () -> "Break Blocks")
                .category(() -> "Daily", () -> "daily")
                .npcId(() -> "armorstand:123")
                .putAll(Map.of("%CUSTOM%", "custom value"));

        assertEquals(
                "A Display Quest quest_id Quest description 3 2 Break Blocks Daily daily armorstand:123 custom value",
                placeholders.apply("%QUESTNAME% %QUESTID% %QUESTDESCRIPTION% %ALLOBJECTIVESCOUNT% "
                        + "%OBJECTIVEID% %OBJECTIVENAME% %CATEGORYNAME% %CATEGORYID% %NPCID% %CUSTOM%"));
    }

    @Test
    void formatsProgressLikeTheOldPaperPlaceholderPath() {
        final Placeholders placeholders = Placeholders.create()
                .activeObjective(
                        () -> 1,
                        () -> "Jump",
                        () -> 2.0d,
                        () -> 5.25d,
                        () -> "Task",
                        () -> "Done task",
                        () -> "Objective description");

        assertEquals(
                "2 5.25 38 Task Done task Objective description",
                placeholders.apply("%ACTIVEOBJECTIVEPROGRESS% %OBJECTIVEPROGRESSNEEDED% "
                        + "%OBJECTIVEPROGRESSPERCENTAGE% %OBJECTIVETASKDESCRIPTION% "
                        + "%COMPLETEDOBJECTIVETASKDESCRIPTION% %OBJECTIVEDESCRIPTION%"));
    }

    @Test
    void appliesLists() {
        final Placeholders placeholders = Placeholders.create().questPlayer(() -> 12, () -> "main");

        assertEquals(
                List.of("Points: 12", "Profile: main"),
                placeholders.apply(List.of("Points: %QUESTPOINTS%", "Profile: %PROFILENAME%")));
    }
}
