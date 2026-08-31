package com.notquests.builtin.actions;

import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_BOOLEANS;
import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_NUMBERS;
import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_STRINGS;
import static com.notquests.builtin.actions.VariableActionFields.EXPRESSION;
import static com.notquests.builtin.actions.VariableActionFields.OPERATOR;
import static com.notquests.builtin.actions.VariableActionFields.VARIABLE_NAME;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.variables.VariableDataType;

import java.util.List;

public final class NumberAction {
    private NumberAction() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("Number")
                .displayName("Number Variable Action")
                .description("Changes a settable number variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, adapter.fields().text().config("specifics.variableName"), "Number variable to change.")
                .field(OPERATOR, adapter.fields().text().config("specifics.operator"), "How to change the variable value.")
                .field(
                        EXPRESSION,
                        adapter.fields().numberExpression().config("specifics.expression"),
                        "Number expression used as the input value for this variable action.")
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
                .variableCommands(
                        VariableDataType.NUMBER,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("set", "add", "deduct", "multiply", "divide"),
                        EXPRESSION,
                        adapter.fields().numberExpression(),
                        "How to change the number variable: set, add, deduct, multiply, or divide.",
                        "Number expression used as the input value for this variable action.")
                .singleLine((action, arguments) -> plugin.parseVariableAction(action, arguments, VariableDataType.NUMBER))
                .execute((action, questPlayer, objects) ->
                        plugin.executeVariableAction(VariableDataType.NUMBER, action, questPlayer, objects))
                .actionDescription((action, questPlayer, objects) ->
                        plugin.variableActionDescription(VariableDataType.NUMBER, action, questPlayer, objects))
                .register();
    }
}
