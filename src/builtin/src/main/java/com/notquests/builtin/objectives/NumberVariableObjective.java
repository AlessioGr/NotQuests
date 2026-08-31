package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.variables.VariableDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NumberVariableObjective {
    private static final String VARIABLE_NAME = "variableName";
    private static final String OPERATOR = "operator";
    private static final String AMOUNT = "amount";
    private static final String ADDITIONAL_STRINGS = "additionalStrings";
    private static final String ADDITIONAL_NUMBERS = "additionalNumbers";
    private static final String ADDITIONAL_BOOLEANS = "additionalBooleans";

    private NumberVariableObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("NumberVariable")
                .displayName("Number Variable")
                .description("Tracks progress from a NotQuests number variable.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text().config("specifics.variableName"),
                        "Number variable that provides objective progress.")
                .field(
                        OPERATOR,
                        adapter.fields().text().config("specifics.operator"),
                        "Comparison operator used for the variable check.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Target variable value required to complete the objective.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Extra text arguments passed to the variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Extra number-expression arguments passed to the variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Extra boolean-expression arguments passed to the variable.")
                .flag(
                        "checkOnlyWhenCorrespondingVariableValueChanged",
                        adapter.fields().presenceFlag().config("specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                        "Only re-check this objective when the matching variable value changes.")
                .variableCommands(
                        VariableDataType.NUMBER,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("equals", "lessThan", "moreThan", "moreOrEqualThan", "lessOrEqualThan"),
                        AMOUNT,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "How to compare the number variable against the target expression.",
                        "Target number expression this objective must reach.")
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(objective, activeObjective))
                .onRefresh((refresh, objective) -> {
                    if (objective.flag("checkOnlyWhenCorrespondingVariableValueChanged")) {
                        if (!refresh.variableValueChanged() || !refresh.matchesVariable(objective.text(VARIABLE_NAME))) {
                            return;
                        }
                    }
                    updateProgress(adapter, objective);
                })
                .register();
    }

    private static void updateProgress(
            final NotQuestsAdapter adapter,
            final Objectives.Progress objective) {
        final String variableName = objective.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                objective.questPlayer(),
                stringMap(objective, ADDITIONAL_STRINGS),
                rawMap(objective, ADDITIONAL_NUMBERS),
                rawMap(objective, ADDITIONAL_BOOLEANS));
        if (!(value instanceof Number number)) {
            return;
        }
        final double current = number.doubleValue();
        final double target = objective.progressNeeded();
        switch (objective.text(OPERATOR).toLowerCase(Locale.ROOT)) {
            case "morethan", "moreorequalthan" -> objective.setProgress(current, false);
            case "lessthan" -> {
                if (current < target) {
                    objective.setProgress(target, false);
                }
            }
            case "lessorequalthan" -> {
                if (current <= target) {
                    objective.setProgress(target, false);
                }
            }
            case "equals" -> {
                if (Double.compare(current, target) == 0) {
                    objective.setProgress(target, false);
                } else if (current < target) {
                    objective.setProgress(current, false);
                }
            }
            default -> {
            }
        }
    }

    private static String taskDescription(
            final Objectives.Data objective,
            final ActiveObjective activeObjective) {
        final String variableName = objective.text(VARIABLE_NAME);
        if (variableName.isBlank()) {
            return "<YELLOW>Error: Variable not found.";
        }
        final double target = activeObjective == null
                ? parseNumber(objective.text(AMOUNT))
                : activeObjective.progressNeeded();
        return switch (objective.text(OPERATOR)) {
            case "moreThan" -> "<GRAY>-- " + variableName + " needed: More than " + (target - 1) + "</GRAY>";
            case "moreOrEqualThan" -> "<GRAY>-- " + variableName + " needed: More or equal than " + target + "</GRAY>";
            case "lessThan" -> "<GRAY>-- " + variableName + " needed: Less than " + target + "</GRAY>";
            case "lessOrEqualThan" -> "<GRAY>-- " + variableName + " needed: Less or equal than" + target + "</GRAY>";
            case "equals" -> "<GRAY>-- " + variableName + " needed: Exactly " + target + "</GRAY>";
            default -> "<GRAY>-- " + variableName + " needed: " + target + "</GRAY>";
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringMap(final Objectives.Data objective, final String key) {
        final Object value = objective.value(key);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        final HashMap<String, String> values = new HashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return values;
    }

    private static Map<String, ?> rawMap(final Objectives.Data objective, final String key) {
        final Object value = objective.value(key);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        final HashMap<String, Object> values = new HashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                values.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return values;
    }

    private static double parseNumber(final String value) {
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException ignored) {
            return 0;
        }
    }
}
