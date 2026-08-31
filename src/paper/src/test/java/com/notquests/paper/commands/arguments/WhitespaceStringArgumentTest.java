package com.notquests.paper.commands.arguments;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class WhitespaceStringArgumentTest {

    @Test
    void stopsBeforeTheFollowingArgument() throws Exception {
        final WhitespaceStringArgument argument = WhitespaceStringArgument.whitespaceString();
        final StringReader reader = new StringReader("acacia_boat,acacia_boat 4");

        argument.parse(reader);

        assertEquals(" 4", reader.getRemaining());
    }

    @Test
    void preservesColonsAndCommasWhenUsedByBrigadier() throws Exception {
        final WhitespaceStringArgument itemSelection = WhitespaceStringArgument.whitespaceString();
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

        dispatcher.execute("execute GiveItem minecraft:acacia_boat,custom:item 4", new Object());

        assertEquals(4, amount.get());
    }
}
