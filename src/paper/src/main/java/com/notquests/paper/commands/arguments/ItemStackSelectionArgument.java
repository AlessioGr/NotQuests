package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

/** Paper's native single-token parser does not accept commas, so item/action lists use this bridge. */
public final class ItemStackSelectionArgument implements CustomArgumentType.Converted<String, String> {
    public static ItemStackSelectionArgument itemStackSelectionArgument() {
        return new ItemStackSelectionArgument();
    }

    @Override
    public String convert(final String input) {
        return input;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readString();
        }
        final int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }
}
