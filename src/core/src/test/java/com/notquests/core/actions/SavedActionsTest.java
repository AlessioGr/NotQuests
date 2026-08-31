package com.notquests.core.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.Map;
import java.util.Set;

class SavedActionsTest {
    @Test
    void savedActionConditionIdsReuseFirstFreeGap() {
        final SavedActions registry = new SavedActions();
        final Actions.Type actionType = new Actions.Type(
                "TestAction", "Test Action", "Runs a test action.",
                java.util.List.of(), java.util.List.of(), null, true, null,
                (data, player, objects) -> {}, null);
        final Conditions.Type conditionType = new Conditions.Type(
                "TestCondition", "Test Condition", "Checks a test condition.",
                java.util.List.of(), java.util.List.of(), Set.of(Conditions.Target.QUEST), null, null,
                (data, player) -> "", null);

        registry.save("daily", actionType, new com.notquests.core.TestData(Map.of()));
        final SavedActions.SavedAction action = registry.action("daily");
        action.addCondition(1, conditionType, new com.notquests.core.TestData(Map.of()));
        action.addCondition(3, conditionType, new com.notquests.core.TestData(Map.of()));

        assertEquals(2, action.getFreeConditionID());
        assertEquals(2, action.addCondition(conditionType, new com.notquests.core.TestData(Map.of())));
    }
}
