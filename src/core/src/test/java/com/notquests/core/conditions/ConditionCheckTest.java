package com.notquests.core.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConditionCheckTest {
    @Test
    void fulfilledNonNegatedConditionReturnsEmptySuccess() {
        final ConditionCheck.Result result = ConditionCheck.fromRawResult("", false, "", "fallback");

        assertEquals(true, result.fulfilled());
        assertEquals("", result.message());
    }

    @Test
    void unfulfilledNonNegatedConditionUsesRawMessageWithoutCustomDescription() {
        final ConditionCheck.Result result = ConditionCheck.fromRawResult("Need 5 apples", false, "", "fallback");

        assertEquals(false, result.fulfilled());
        assertEquals("Need 5 apples", result.message());
    }

    @Test
    void unfulfilledNonNegatedConditionUsesCustomDescription() {
        final ConditionCheck.Result result = ConditionCheck.fromRawResult("Need 5 apples", false, "Bring apples", "fallback");

        assertEquals(false, result.fulfilled());
        assertEquals("<YELLOW>Bring apples", result.message());
    }

    @Test
    void fulfilledNegatedConditionFailsWithFallbackDescription() {
        final ConditionCheck.Result result = ConditionCheck.fromRawResult("", true, "", "Quest must not be active");

        assertEquals(false, result.fulfilled());
        assertEquals(
                "<YELLOW>You cannot fulfill this condition: <unimportant>Quest must not be active",
                result.message());
    }

    @Test
    void descriptionUsesCustomDescriptionWhenPresent() {
        assertEquals("<GRAY>Custom text", ConditionCheck.description("Custom text", "fallback"));
    }
}
