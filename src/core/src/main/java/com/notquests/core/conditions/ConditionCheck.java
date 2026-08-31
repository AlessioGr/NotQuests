package com.notquests.core.conditions;

import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;

public final class ConditionCheck {
    private ConditionCheck() {}

    public record Result(boolean fulfilled, String message) {}

    public static String check(
            final Conditions.Type condition,
            final Conditions.Data data,
            final PlatformPlayer questPlayer) {
        if (condition == null || condition.checker() == null) {
            return "Condition cannot be checked.";
        }
        final String result = condition.checker().check(data, questPlayer);
        final boolean fulfilled = result == null || result.isBlank();
        if (!data.flag("negated")) {
            return fulfilled ? "" : result;
        }
        if (!fulfilled) {
            return "";
        }
          final String description = condition.description() == null || condition.description().isBlank()
                ? condition.id()
                  : condition.description();
        return "<YELLOW>You cannot fulfill this condition: <unimportant>" + description;
    }

    public static Result fromRawResult(
            final String rawResult,
            final boolean negated,
            final String customDescription,
            final String fallbackDescription) {
        final String result = rawResult == null ? "" : rawResult;
        final String description = customDescription == null ? "" : customDescription;
        if (!negated) {
            if (result.isBlank()) {
                return new Result(true, "");
            }
            return new Result(false, description.isBlank() ? result : "<YELLOW>" + description);
        }
        if (result.isBlank()) {
            return new Result(
                    false,
                    "<YELLOW>You cannot fulfill this condition: <unimportant>" + nullToBlank(fallbackDescription));
        }
        return new Result(true, "");
    }

    public static String description(
            final String customDescription,
            final String fallbackDescription) {
        final String description = customDescription == null ? "" : customDescription;
        return description.isBlank() ? nullToBlank(fallbackDescription) : "<GRAY>" + description;
    }

    private static String nullToBlank(final String value) {
        return value == null ? "" : value;
    }
}
