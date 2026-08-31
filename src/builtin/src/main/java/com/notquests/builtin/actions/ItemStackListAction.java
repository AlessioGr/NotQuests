package com.notquests.builtin.actions;

import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_BOOLEANS;
import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_NUMBERS;
import static com.notquests.builtin.actions.VariableActionFields.ADDITIONAL_STRINGS;
import static com.notquests.builtin.actions.VariableActionFields.EXPRESSION;
import static com.notquests.builtin.actions.VariableActionFields.ITEM_STACK;
import static com.notquests.builtin.actions.VariableActionFields.OPERATOR;
import static com.notquests.builtin.actions.VariableActionFields.VARIABLE_NAME;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.variables.VariableDataType;

import java.util.List;

public final class ItemStackListAction {
    private ItemStackListAction() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("ItemStackList")
                .displayName("Item Stack List Variable Action")
                .description("Changes a settable item-stack-list variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, adapter.fields().text().config("specifics.variableName"), "Item-stack-list variable to change.")
                .field(OPERATOR, adapter.fields().text().config("specifics.operator"), "How to change the variable value.")
                .field(ITEM_STACK, adapter.fields().storedItemStack().config("specifics.itemStack"), "Item stack used by this variable action.")
                .field(ADDITIONAL_STRINGS, adapter.fields().stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, adapter.fields().numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, adapter.fields().numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .variableCommands(
                        VariableDataType.ITEMSTACKLIST,
                        VARIABLE_NAME,
                        OPERATOR,
                        List.of("set", "add", "remove", "clear"),
                        EXPRESSION,
                        adapter.fields().itemSelection(),
                        "How to change the item-list variable: set, add, remove, or clear.",
                        "Material, custom item, hand, or any value used by this item-list action.")
                .singleLine((action, arguments) -> plugin.parseVariableAction(action, arguments, VariableDataType.ITEMSTACKLIST))
                .execute((action, questPlayer, objects) ->
                        plugin.executeVariableAction(VariableDataType.ITEMSTACKLIST, action, questPlayer, objects))
                .actionDescription((action, questPlayer, objects) ->
                        plugin.variableActionDescription(VariableDataType.ITEMSTACKLIST, action, questPlayer, objects))
                .register();
    }
}
