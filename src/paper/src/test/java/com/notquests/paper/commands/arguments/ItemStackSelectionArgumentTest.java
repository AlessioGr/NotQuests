package com.notquests.paper.commands.arguments;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class ItemStackSelectionArgumentTest {

    @Test
    void serverParserStopsBeforeFollowingAmount() throws Exception {
        final ItemStackSelectionArgument argument = ItemStackSelectionArgument.itemStackSelectionArgument();
        final StringReader reader = new StringReader("acacia_boat,acacia_boat 4");

        argument.parse(reader);

        assertEquals(" 4", reader.getRemaining(), "item selection must not swallow the following amount argument");
    }

    @Test
    void followingAmountRemainsAvailableToBrigadier() throws Exception {
        final ItemStackSelectionArgument itemSelection = ItemStackSelectionArgument.itemStackSelectionArgument();
        final AtomicInteger amount = new AtomicInteger();
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("execute")
                .then(literal("GiveItem")
                        .then(argument("material", itemSelection)
                                .then(argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            amount.set(IntegerArgumentType.getInteger(context, "amount"));
                                            return 1;
                                        })))));

        dispatcher.execute("execute GiveItem acacia_boat 4", new Object());

        assertEquals(4, amount.get());
    }
}
