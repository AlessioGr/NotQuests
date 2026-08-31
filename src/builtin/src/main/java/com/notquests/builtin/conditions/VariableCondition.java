package com.notquests.builtin.conditions;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.variables.NumberExpression;
import com.notquests.core.variables.VariableDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VariableCondition {
    private static final String VARIABLE_NAME = "variableName";
    private static final String OPERATOR = "operator";
    private static final String EXPRESSION = "expression";
    private static final String AMOUNT = "amount";
    private static final String ADDITIONAL_STRINGS = "additionalStrings";
    private static final String ADDITIONAL_NUMBERS = "additionalNumbers";
    private static final String ADDITIONAL_BOOLEANS = "additionalBooleans";

    private VariableCondition() {}

    public static void register(final NotQuestsAdapter adapter) {
        registerNumber(adapter);
        registerBoolean(adapter);
        registerString(adapter);
        registerList(adapter);
        registerItemStackList(adapter);
    }

    private static void registerNumber(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("Number")
                .displayName("Number Variable")
                .description("Compares a number variable with a number value.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text(() -> adapter.variableNames(VariableDataType.NUMBER)).config("specifics.variableName"),
                        "Number variable to check.")
                .field(
                        OPERATOR,
                        adapter.fields()
                                .text(() -> List.of("equals", "lessThan", "moreThan", "moreOrEqualThan", "lessOrEqualThan"))
                                .config("specifics.operator"),
                        "How to compare the variable value with the required number.")
                .field(
                        EXPRESSION,
                        adapter.fields().numberExpression().config("specifics.expression"),
                        "Required number value.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Text arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Number-expression arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Boolean-expression arguments passed to the selected variable.")
                .variableCommands(
                        VariableDataType.NUMBER,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("equals", "lessThan", "moreThan", "moreOrEqualThan", "lessOrEqualThan"),
                        EXPRESSION,
                        adapter.fields().numberExpression(),
                        "How to compare the selected number variable against the expression.",
                        "Number expression to compare with the selected variable value.")
                .singleLine((condition, arguments) -> setValues(condition, arguments))
                .check((condition, questPlayer) -> checkNumber(adapter, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition))
                .register();
    }

    private static void registerBoolean(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("Boolean")
                .displayName("Boolean Variable")
                .description("Compares a boolean variable with true or false.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text(() -> adapter.variableNames(VariableDataType.BOOLEAN)).config("specifics.variableName"),
                        "Boolean variable to check.")
                .field(
                        OPERATOR,
                        adapter.fields().text(() -> List.of("equals", "and", "or")).config("specifics.operator"),
                        "How to compare the variable value with the required boolean.")
                .field(
                        EXPRESSION,
                        adapter.fields().text(() -> List.of("true", "false")).config("specifics.expression"),
                        "Required boolean value.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Text arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Number-expression arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Boolean-expression arguments passed to the selected variable.")
                .variableCommands(
                        VariableDataType.BOOLEAN,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("and", "equals", "or"),
                        EXPRESSION,
                        adapter.fields().text(() -> List.of("true", "false")),
                        "How to compare the selected boolean variable: equals, and, or.",
                        "Boolean value or expression to compare with the selected variable value.")
                .singleLine((condition, arguments) -> setValues(condition, arguments))
                .check((condition, questPlayer) -> checkBoolean(adapter, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition))
                .register();
    }

    private static void registerString(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("String")
                .displayName("String Variable")
                .description("Compares a string variable with text.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text(() -> adapter.variableNames(VariableDataType.STRING)).config("specifics.variableName"),
                        "String variable to check.")
                .field(
                        OPERATOR,
                        adapter.fields()
                                .text(() -> List.of("equals", "equalsIgnoreCase", "contains", "startsWith", "endsWith", "isEmpty"))
                                .config("specifics.operator"),
                        "How to compare the variable value with the required text.")
                .field(
                        EXPRESSION,
                        adapter.fields().greedyText().config("specifics.string"),
                        "Required text value.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Text arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Number-expression arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Boolean-expression arguments passed to the selected variable.")
                .variableCommands(
                        VariableDataType.STRING,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("equals", "equalsIgnoreCase", "contains", "startsWith", "endsWith", "isEmpty"),
                        EXPRESSION,
                        adapter.fields().greedyText(),
                        "How to compare the selected text variable against the supplied text.",
                        "Text value to compare with the selected variable value.")
                .singleLine((condition, arguments) -> setValues(condition, arguments))
                .check((condition, questPlayer) -> checkString(adapter, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition))
                .register();
    }

    private static void registerList(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("List")
                .displayName("List Variable")
                .description("Compares a list variable with comma-separated text values.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text(() -> adapter.variableNames(VariableDataType.LIST)).config("specifics.variableName"),
                        "List variable to check.")
                .field(
                        OPERATOR,
                        adapter.fields()
                                .text(() -> List.of("equals", "equalsIgnoreCase", "contains", "containsIgnoreCase"))
                                .config("specifics.operator"),
                        "How to compare the variable list with the required values.")
                .field(
                        EXPRESSION,
                        adapter.fields().greedyText().config("specifics.expression"),
                        "Comma-separated values to compare with the variable list.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Text arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Number-expression arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Boolean-expression arguments passed to the selected variable.")
                .variableCommands(
                        VariableDataType.LIST,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("equals", "equalsIgnoreCase", "contains", "containsIgnoreCase"),
                        EXPRESSION,
                        adapter.fields().greedyText(),
                        "How to compare the selected list variable against the supplied values.",
                        "Comma-separated values to compare with the selected variable list.")
                .singleLine((condition, arguments) -> setValues(condition, arguments))
                .check((condition, questPlayer) -> checkList(adapter, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition))
                .register();
    }

    private static void registerItemStackList(final NotQuestsAdapter adapter) {
        adapter.conditions().condition("ItemStackList")
                .displayName("Item Stack List Variable")
                .description("Compares an item-stack-list variable with a required material or item selection.")
                .field(
                        VARIABLE_NAME,
                        adapter.fields().text(() -> adapter.variableNames(VariableDataType.ITEMSTACKLIST)).config("specifics.variableName"),
                        "Item-stack-list variable to check.")
                .field(
                        OPERATOR,
                        adapter.fields().text(() -> List.of("equals", "contains")).config("specifics.operator"),
                        "How to compare the variable item list with the required item selection.")
                .field(
                        EXPRESSION,
                        adapter.fields().itemSelection().config("specifics.itemStack"),
                        "Required material, custom item, hand item, any, or comma-separated item selection.")
                .field(
                        AMOUNT,
                        adapter.fields().integer(1).config("specifics.amount"),
                        "Required stack amount for each item in this item-list condition.")
                .field(
                        ADDITIONAL_STRINGS,
                        adapter.fields().stringMap().config("specifics.additionalStrings"),
                        "Text arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"),
                        "Number-expression arguments passed to the selected variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"),
                        "Boolean-expression arguments passed to the selected variable.")
                .variableCommands(
                        VariableDataType.ITEMSTACKLIST,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("equals", "contains"),
                        EXPRESSION,
                        adapter.fields().itemSelection(),
                        "How to compare the selected item-stack-list variable against the supplied item selection.",
                        "Material, custom item, hand item, any, or comma-separated item selection to compare.")
                .singleLine((condition, arguments) -> setItemStackListValues(adapter, condition, arguments))
                .check((condition, questPlayer) -> checkItemStackList(adapter, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition))
                .register();
    }

    private static void setValues(
            final Conditions.Draft condition,
            final List<String> arguments) {
        condition.setValue(VARIABLE_NAME, arguments.get(0));
        condition.setValue(OPERATOR, arguments.get(1));
        condition.setValue(EXPRESSION, arguments.size() <= 2 ? "" : String.join(" ", arguments.subList(2, arguments.size())));
        condition.setValue(ADDITIONAL_STRINGS, Map.of());
        condition.setValue(ADDITIONAL_NUMBERS, Map.of());
        condition.setValue(ADDITIONAL_BOOLEANS, Map.of());
    }

    private static void setItemStackListValues(
            final NotQuestsAdapter adapter,
            final Conditions.Draft condition,
            final List<String> arguments) {
        condition.setValue(VARIABLE_NAME, arguments.get(0));
        condition.setValue(OPERATOR, arguments.get(1));
        final int amount = arguments.size() <= 3 ? 1 : parseInteger(arguments.get(3), 1);
        condition.setValue(EXPRESSION, adapter.parseItemSelection(arguments.size() <= 2 ? "any" : arguments.get(2)).withAmount(amount));
        condition.setValue(AMOUNT, amount);
        condition.setValue(ADDITIONAL_STRINGS, Map.of());
        condition.setValue(ADDITIONAL_NUMBERS, Map.of());
        condition.setValue(ADDITIONAL_BOOLEANS, Map.of());
    }

    private static String checkNumber(
            final NotQuestsAdapter adapter,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final String variableName = condition.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                questPlayer,
                stringMap(condition, ADDITIONAL_STRINGS),
                rawMap(condition, ADDITIONAL_NUMBERS),
                rawMap(condition, ADDITIONAL_BOOLEANS));
        if (!(value instanceof Number current)) {
            return "<error>Variable <highlight>" + variableName + "</highlight> is not a number.";
        }
        final double required = new NumberExpression(adapter, condition.text(EXPRESSION))
                .calculateValue(questPlayer);
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "moreThan" -> current.doubleValue() > required;
            case "moreOrEqualThan" -> current.doubleValue() >= required;
            case "lessThan" -> current.doubleValue() < required;
            case "lessOrEqualThan" -> current.doubleValue() <= required;
            case "equals" -> Double.compare(current.doubleValue(), required) == 0;
            default -> false;
        };
        return fulfilled ? "" : "<yellow>" + adapter.variablePlural(variableName) + " must match " + condition.text(OPERATOR) + " " + required + ".";
    }

    private static String checkBoolean(
            final NotQuestsAdapter adapter,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final String variableName = condition.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                questPlayer,
                stringMap(condition, ADDITIONAL_STRINGS),
                rawMap(condition, ADDITIONAL_NUMBERS),
                rawMap(condition, ADDITIONAL_BOOLEANS));
        if (!(value instanceof Boolean current)) {
            return "<error>Variable <highlight>" + variableName + "</highlight> is not true/false.";
        }
        final boolean required = new NumberExpression(adapter, condition.text(EXPRESSION))
                .calculateBooleanValue(questPlayer);
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "equals" -> current == required;
            case "or" -> current || required;
            case "and" -> current && required;
            default -> false;
        };
        return fulfilled ? "" : "<yellow>" + adapter.variableSingular(variableName) + " must be " + required + ".";
    }

    private static String checkString(
            final NotQuestsAdapter adapter,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final String variableName = condition.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                questPlayer,
                stringMap(condition, ADDITIONAL_STRINGS),
                rawMap(condition, ADDITIONAL_NUMBERS),
                rawMap(condition, ADDITIONAL_BOOLEANS));
        if (!(value instanceof String current)) {
            return "<error>Variable <highlight>" + variableName + "</highlight> is not text.";
        }
        final String required = condition.text(EXPRESSION);
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "equals" -> current.equals(required);
            case "equalsIgnoreCase" -> current.equalsIgnoreCase(required);
            case "contains" -> current.contains(required);
            case "startsWith" -> current.startsWith(required);
            case "endsWith" -> current.endsWith(required);
            case "isEmpty" -> current.isBlank();
            default -> false;
        };
        return fulfilled ? "" : "<yellow>" + adapter.variableSingular(variableName) + " must satisfy " + condition.text(OPERATOR) + ".";
    }

    private static String checkList(
            final NotQuestsAdapter adapter,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final String variableName = condition.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                questPlayer,
                stringMap(condition, ADDITIONAL_STRINGS),
                rawMap(condition, ADDITIONAL_NUMBERS),
                rawMap(condition, ADDITIONAL_BOOLEANS));
        if (!(value instanceof List<?> current)) {
            return "<error>Variable <highlight>" + variableName + "</highlight> is not a list.";
        }
        final List<String> currentValues = current.stream().map(String::valueOf).toList();
        final List<String> requiredValues = splitList(condition.text(EXPRESSION));
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "equals" -> currentValues.equals(requiredValues);
            case "equalsIgnoreCase" -> lower(currentValues).equals(lower(requiredValues));
            case "contains" -> currentValues.containsAll(requiredValues);
            case "containsIgnoreCase" -> lower(currentValues).containsAll(lower(requiredValues));
            default -> false;
        };
        return fulfilled ? "" : "<yellow>" + adapter.variablePlural(variableName) + " must satisfy " + condition.text(OPERATOR) + ".";
    }

    private static String checkItemStackList(
            final NotQuestsAdapter adapter,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final String variableName = condition.text(VARIABLE_NAME);
        final Object value = adapter.variableValue(
                variableName,
                questPlayer,
                stringMap(condition, ADDITIONAL_STRINGS),
                rawMap(condition, ADDITIONAL_NUMBERS),
                rawMap(condition, ADDITIONAL_BOOLEANS));
        if (!(value instanceof List<?> current)) {
            return "<error>Variable <highlight>" + variableName + "</highlight> is not an item-stack list.";
        }
        final ItemSelection required = itemSelection(condition);
        if (required == null) {
            return "<error>Item-stack-list condition has no required item selection.";
        }
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "equals" -> equalsRequired(adapter, current, required);
            case "contains" -> containsRequired(adapter, current, required);
            default -> false;
        };
        return fulfilled ? "" : "<yellow>" + adapter.variablePlural(variableName) + " must satisfy " + condition.text(OPERATOR) + ".";
    }

    private static String description(final Conditions.Data condition) {
        return "<gray>-- " + condition.text(VARIABLE_NAME) + " " + condition.text(OPERATOR) + " " + condition.text(EXPRESSION) + "</gray>";
    }

    private static int parseInteger(final String value, final int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException exception) {
            return fallback;
        }
    }

    private static List<String> splitList(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private static List<String> lower(final List<String> values) {
        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }

    private static ItemSelection itemSelection(final Conditions.Data condition) {
        final Object value = condition.value(EXPRESSION);
        if (value instanceof final ItemSelection itemSelection) {
            return itemSelection;
        }
        return null;
    }

    private static boolean equalsRequired(
            final NotQuestsAdapter adapter,
            final List<?> values,
            final ItemSelection required) {
        int amount = 0;
        for (final Object value : values) {
            if (value == null) {
                continue;
            }
            if (!(value instanceof final ItemSelection actual)
                    || !adapter.itemSelectionsAreSimilar(required, actual)) {
                return false;
            }
            amount += Math.max(0, actual.amount());
        }
        return amount == required.amount();
    }

    private static boolean containsRequired(
            final NotQuestsAdapter adapter,
            final List<?> values,
            final ItemSelection required) {
        int amount = 0;
        for (final Object value : values) {
            if (value instanceof final ItemSelection actual
                    && adapter.itemSelectionsAreSimilar(required, actual)) {
                amount += Math.max(0, actual.amount());
                if (amount >= required.amount()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringMap(final Conditions.Data condition, final String key) {
        final Object value = condition.value(key);
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

    private static Map<String, ?> rawMap(final Conditions.Data condition, final String key) {
        final Object value = condition.value(key);
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
}
