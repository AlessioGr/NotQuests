package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

/** Reads one command value up to whitespace while preserving characters such as commas and colons. */
public final class WhitespaceStringArgument implements CustomArgumentType.Converted<String, String> {
    public static WhitespaceStringArgument whitespaceString() {
        return new WhitespaceStringArgument();
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

    @Override
    public <S> String parse(final StringReader reader, final S source) throws CommandSyntaxException {
        return parse(reader);
    }
}
