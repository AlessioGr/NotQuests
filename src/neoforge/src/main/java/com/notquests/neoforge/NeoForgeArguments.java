package com.notquests.neoforge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

final class NeoForgeArguments {
    private NeoForgeArguments() {}

    static WhitespaceTerminatedStringArgument commaToken() {
        return new WhitespaceTerminatedStringArgument();
    }

    static class WhitespaceTerminatedStringArgument implements ArgumentType<String> {
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
        public Collection<String> getExamples() {
            return List.of("grass_block", "grass_block,dirt", "minecraft:stone");
        }
    }

}
