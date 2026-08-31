package com.notquests.core.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.actions.SavedActions.Chain;
import com.notquests.core.actions.SavedActions.ChainRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SavedActionsChainTest {
    @Test
    void executesResolvedActionsInOrderForEachRun() {
        final List<String> executed = new ArrayList<>();
        final ChainRunner<String> runner = ChainRunner.<String>builder()
                .resolveWith(Map.of("one", "one", "two", "two")::get)
                .executeWith((action, ignoreConditions, delay, objects) -> executed.add(action))
                .build();

        runner.run(Chain.builder()
                .actionNames("one,two")
                .amount(2)
                .delay(Duration.ZERO)
                .build());

        assertEquals(List.of("one", "two", "one", "two"), executed);
    }

    @Test
    void randomSelectionCanSkipActionsWhoseConditionsDoNotPass() {
        final List<String> executed = new ArrayList<>();
        final ChainRunner<String> runner = ChainRunner.<String>builder()
                .resolveWith(Map.of("one", "one", "two", "two", "three", "three")::get)
                .conditionsFulfilledBy(action -> false)
                .executeWith((action, ignoreConditions, delay, objects) -> executed.add(action))
                .build();

        runner.run(Chain.builder()
                .actionNames("one,two,three")
                .randomRange(2, 2)
                .onlyCountRandomIfConditionsFulfilled(true)
                .build());

        assertEquals(List.of(), executed);
    }

    @Test
    void randomSelectionContinuesUntilEnoughEligibleActionsRun() {
        final List<String> executed = new ArrayList<>();
        final ChainRunner<String> runner = ChainRunner.<String>builder()
                .resolveWith(Map.of("blocked", "blocked", "eligible", "eligible")::get)
                .conditionsFulfilledBy("eligible"::equals)
                .executeWith((action, ignoreConditions, delay, objects) -> executed.add(action))
                .build();

        runner.run(Chain.builder()
                .actionNames("blocked,eligible")
                .randomRange(1, 1)
                .onlyCountRandomIfConditionsFulfilled(true)
                .build());

        assertEquals(List.of("eligible"), executed);
    }

    @Test
    void reportsUnknownActionNames() {
        final List<String> warnings = new ArrayList<>();
        final ChainRunner<String> runner = ChainRunner.<String>builder()
                .resolveWith(name -> null)
                .executeWith((action, ignoreConditions, delay, objects) -> {})
                .warnWith(warnings::add)
                .build();

        runner.run(Chain.builder().actionNames("missing").build());

        assertEquals(List.of("Action chain references unknown action 'missing'."), warnings);
    }
}
