package com.notquests.core.variables;

import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NumberExpression {
    private final NotQuestsAdapter adapter;
    private final String expression;
    private final EvaluationEnvironment evaluationEnvironment = new EvaluationEnvironment();
    private final CompiledExpression compiledExpression;
    private int variableCounter;
    private PlatformPlayer questPlayerToEvaluate;
    private double cachedStaticResult;
    private boolean resultStatic;

    public NumberExpression(final NotQuestsAdapter adapter, final String expression) {
        this.adapter = adapter;
        this.expression = expression == null || expression.isBlank() ? "0" : expression;

        final String normalized = normalizeBooleanLiteral(this.expression);
        final String modifiedExpression = getExpressionAndGenerateEnv(normalized);
        compiledExpression = Crunch.compileExpression(modifiedExpression, evaluationEnvironment);

        if (variableCounter == 0) {
            cachedStaticResult = compiledExpression.evaluate();
            resultStatic = true;
        }
    }

    private NumberExpression(final NotQuestsAdapter adapter, final double staticValue) {
        this.adapter = adapter;
        this.expression = String.valueOf(staticValue);
        cachedStaticResult = staticValue;
        resultStatic = true;
        compiledExpression = null;
    }

    public static NumberExpression ofStatic(final NotQuestsAdapter adapter, final double staticValue) {
        return new NumberExpression(adapter, staticValue);
    }

    public static NumberExpression ofStatic(final double staticValue) {
        return new NumberExpression(null, staticValue);
    }

    public double calculateValue(final PlatformPlayer questPlayer) {
        if (resultStatic) {
            return cachedStaticResult;
        }
        questPlayerToEvaluate = questPlayer;
        return compiledExpression.evaluate();
    }

    public boolean calculateBooleanValue(final PlatformPlayer questPlayer) {
        return calculateValue(questPlayer) >= 0.98d;
    }

    public String getRawExpression() {
        return expression;
    }

    private String getExpressionAndGenerateEnv(String expressions) {
        if (adapter == null) {
            return expressions;
        }
        boolean foundVariable = false;

        for (String variableName : expressionVariableNames()) {
            if (!expressions.contains(variableName)) {
                continue;
            }
            if (adapter.variableType(variableName) != VariableDataType.NUMBER
                    && adapter.variableType(variableName) != VariableDataType.BOOLEAN) {
                continue;
            }

            final ParsedVariableArguments arguments;
            String replaceTarget = variableName;
            if (expressions.contains(variableName + "(")) {
                final int open = expressions.indexOf(variableName + "(") + variableName.length() + 1;
                final int close = expressions.indexOf(")", open);
                if (close > open) {
                    arguments = parseArguments(variableName, expressions.substring(open, close));
                    replaceTarget = variableName + "(" + expressions.substring(open, close) + ")";
                } else {
                    arguments = ParsedVariableArguments.EMPTY;
                }
            } else {
                arguments = ParsedVariableArguments.EMPTY;
            }

            foundVariable = true;
            final String crunchVariableName = "var" + ++variableCounter;
            expressions = expressions.replace(replaceTarget, crunchVariableName);
            final String resolvedVariableName = variableName;
            evaluationEnvironment.addLazyVariable(crunchVariableName, () -> variableAsDouble(
                    adapter.variableValue(
                            resolvedVariableName,
                            questPlayerToEvaluate,
                            arguments.strings(),
                            arguments.numberValues(questPlayerToEvaluate),
                            arguments.booleanValues(questPlayerToEvaluate))));
        }

        return foundVariable ? getExpressionAndGenerateEnv(expressions) : expressions;
    }

    private List<String> expressionVariableNames() {
        final ArrayList<String> names = new ArrayList<>();
        names.addAll(adapter.variableNames(VariableDataType.NUMBER));
        names.addAll(adapter.variableNames(VariableDataType.BOOLEAN));
        names.sort(Comparator.comparingInt(String::length).reversed().thenComparing(String.CASE_INSENSITIVE_ORDER));
        return names;
    }

    private ParsedVariableArguments parseArguments(final String variableName, final String rawArguments) {
        final HashMap<String, String> strings = new HashMap<>();
        final HashMap<String, NumberExpression> numbers = new HashMap<>();
        final HashMap<String, NumberExpression> booleans = new HashMap<>();
        if (rawArguments == null || rawArguments.isBlank()) {
            return new ParsedVariableArguments(strings, numbers, booleans);
        }
        for (final String rawPart : rawArguments.split(",")) {
            final String part = rawPart.trim();
            if (part.isBlank()) {
                continue;
            }
            if (part.startsWith("--")) {
                booleans.put(part.substring(2), NumberExpression.ofStatic(adapter, 1));
                continue;
            }
            final String[] keyValue = part.split(":", 2);
            if (keyValue.length != 2) {
                continue;
            }
            putTypedArgument(variableName, keyValue[0].trim(), keyValue[1].trim(), strings, numbers, booleans);
        }
        return new ParsedVariableArguments(strings, numbers, booleans);
    }

    private void putTypedArgument(
            final String variableName,
            final String name,
            final String value,
            final Map<String, String> strings,
            final Map<String, NumberExpression> numbers,
            final Map<String, NumberExpression> booleans) {
        final String valueType = adapter.variableFields(variableName).stream()
                .filter(field -> field.name().equalsIgnoreCase(name))
                .findFirst()
                .map(RegistryField.Definition::valueType)
                .orElse("text")
                .toLowerCase(Locale.ROOT);
        if (valueType.contains("boolean") || valueType.contains("bool")) {
            booleans.put(name, new NumberExpression(adapter, value));
        } else if (valueType.contains("number") || valueType.contains("integer") || valueType.contains("double")) {
            numbers.put(name, new NumberExpression(adapter, value));
        } else {
            strings.put(name, value);
        }
    }

    private static double variableAsDouble(final Object valueObject) {
        if (valueObject instanceof final Number number) {
            return number.doubleValue();
        }
        if (valueObject instanceof final Boolean bool) {
            return bool ? 1 : 0;
        }
        if (valueObject instanceof final String text) {
            if ("true".equalsIgnoreCase(text)) {
                return 1;
            }
            if ("false".equalsIgnoreCase(text)) {
                return 0;
            }
            try {
                return Double.parseDouble(text);
            } catch (final NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String normalizeBooleanLiteral(final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return "1";
        }
        if ("false".equalsIgnoreCase(value)) {
            return "0";
        }
        return value;
    }

    private record ParsedVariableArguments(
            Map<String, String> strings,
            Map<String, NumberExpression> numbers,
            Map<String, NumberExpression> booleans) {
        private static final ParsedVariableArguments EMPTY =
                new ParsedVariableArguments(Map.of(), Map.of(), Map.of());

        private Map<String, Object> numberValues(final PlatformPlayer questPlayer) {
            final HashMap<String, Object> values = new HashMap<>();
            numbers.forEach((name, expression) -> values.put(name, expression.calculateValue(questPlayer)));
            return values;
        }

        private Map<String, Object> booleanValues(final PlatformPlayer questPlayer) {
            final HashMap<String, Object> values = new HashMap<>();
            booleans.forEach((name, expression) -> values.put(name, expression.calculateBooleanValue(questPlayer)));
            return values;
        }
    }
}
