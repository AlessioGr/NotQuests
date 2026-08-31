package com.notquests.neoforge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class NeoForgeArguments {
    private NeoForgeArguments() {}

    static WhitespaceTerminatedStringArgument commaToken() {
        return new WhitespaceTerminatedStringArgument();
    }

    static <S> LiteralCommandNode<S> hiddenLiteralAlias(final LiteralCommandNode<S> alias) {
        final LiteralCommandNode<S> hidden = new LiteralCommandNode<>(
                alias.getLiteral(),
                alias.getCommand(),
                alias.getRequirement(),
                alias.getRedirect(),
                alias.getRedirectModifier(),
                alias.isFork()) {
            @Override
            public CompletableFuture<Suggestions> listSuggestions(
                    final CommandContext<S> context,
                    final SuggestionsBuilder builder) {
                return Suggestions.empty();
            }
        };
        alias.getChildren().forEach(hidden::addChild);
        return hidden;
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
