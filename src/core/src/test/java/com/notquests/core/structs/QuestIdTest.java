package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry;

import java.util.Map;

class QuestIdTest {
    @Test
    void firstFreeIdsReuseGaps() {
        final Quest quest = new Quest("story");
        quest.addRequirement(1, "ConditionA", new com.notquests.core.TestData(Map.of()));
        quest.addRequirement(3, "ConditionC", new com.notquests.core.TestData(Map.of()));
        quest.addReward(2, "RewardB", new com.notquests.core.TestData(Map.of()));
        quest.addTrigger(1, "TriggerA", new com.notquests.core.TestData(Map.of()));
        final com.notquests.core.objectives.Objective objective =
                quest.addObjective(1, "ObjectiveA", new com.notquests.core.TestData(Map.of()), "");
        objective.addChildObjective(1, "ChildA", new com.notquests.core.TestData(Map.of()), "");
        objective.addChildObjective(3, "ChildC", new com.notquests.core.TestData(Map.of()), "");
        objective.addReward(2, "ChildRewardB", new com.notquests.core.TestData(Map.of()));
        objective.addCondition("unlock", 1, "UnlockA", new com.notquests.core.TestData(Map.of()));
        objective.addCondition("unlock", 3, "UnlockC", new com.notquests.core.TestData(Map.of()));
        objective.addCondition("progress", 2, "ProgressB", new com.notquests.core.TestData(Map.of()));
        objective.addCondition("complete", 1, "CompleteA", new com.notquests.core.TestData(Map.of()));

        assertEquals(2, quest.getFreeRequirementID());
        assertEquals(1, quest.getFreeRewardID());
        assertEquals(2, quest.getFreeTriggerID());
        assertEquals(2, objective.getFreeObjectiveID());
        assertEquals(1, objective.getFreeRewardID());
        assertEquals(2, objective.getFreeConditionID("unlock"));
        assertEquals(1, objective.getFreeConditionID("progress"));
        assertEquals(2, objective.getFreeConditionID("complete"));
    }
}
