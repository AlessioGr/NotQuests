package com.notquests.paper.builtin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

class PaperBuiltinConfigPathTest {
    @Test
    void paperIntegrationObjectivesKeepTheirV630YamlPaths() throws IOException {
        final Map<String, Set<String>> pathsBySource = Map.of(
                "EscortNPCObjective.java", Set.of(
                        "specifics.NPCToEscortID",
                        "specifics.destinationNPCID",
                        "specifics.spawnLocation"),
                "JobsRebornReachJobLevelObjective.java", Set.of(
                        "specifics.jobName",
                        "specifics.countPreviousLevels"),
                "KillEliteMobsObjective.java", Set.of(
                        "specifics.eliteMobToKill",
                        "specifics.minimumLevel",
                        "specifics.maximumLevel",
                        "specifics.spawnReason",
                        "specifics.minimumDamagePercentage"),
                "TownyNationReachTownCountObjective.java", Set.of("specifics.countPreviousTowns"),
                "TownyReachResidentCountObjective.java", Set.of("specifics.countPreviousResidents"));

        for (final Map.Entry<String, Set<String>> entry : pathsBySource.entrySet()) {
            final String source = Files.readString(objectiveSource(entry.getKey()));
            for (final String path : entry.getValue()) {
                assertTrue(source.contains("\"" + path + "\""),
                        () -> entry.getKey() + " must retain v6.3 path " + path);
            }
        }
    }

    @Test
    void negativePaperFlagsInvertTheOldPositiveBooleans() throws IOException {
        assertInverted("JobsRebornReachJobLevelObjective.java", "specifics.countPreviousLevels");
        assertInverted("TownyNationReachTownCountObjective.java", "specifics.countPreviousTowns");
        assertInverted("TownyReachResidentCountObjective.java", "specifics.countPreviousResidents");
    }

    private static void assertInverted(final String sourceName, final String path) throws IOException {
        final String source = Files.readString(objectiveSource(sourceName));
        assertTrue(source.contains("invertedBooleanConfig(\"" + path + "\")"),
                () -> sourceName + " must invert the old positive config value at " + path);
    }

    private static Path objectiveSource(final String sourceName) {
        final Path portable = Path.of(
                "../builtin/src/main/java/com/notquests/builtin/objectives", sourceName);
        return Files.exists(portable)
                ? portable
                : Path.of("src/main/java/com/notquests/paper/builtin/objectives", sourceName);
    }
}
