/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.commands.framework;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Base class for NotQuests custom command arguments built on Paper's native Brigadier API. This is
 * our replacement for Cloud's {@code ArgumentParser} / {@code ParserDescriptor}.
 *
 * <p>An {@code NQArgumentType<T>} reads a single string token off the input and {@link #convert
 * converts} it into a {@code T}. Subclasses implement {@link #convert} (throwing
 * {@link CommandSyntaxException} to reject bad input with a helpful message) and may override
 * {@link #suggest} to provide tab-completions.
 *
 * <p>The argument is backed by {@link StringArgumentType#string()} — a <b>non-greedy</b>, quotable
 * string. It reads exactly one token (or a quoted phrase) and never swallows the rest of the line
 * into itself, so it is safe to place before other arguments (e.g. {@code <tagName> <op> <amount>}).
 * Override {@link #getNativeType()} if a different native base is required.
 *
 * @param <T> the resolved argument type returned by {@code CommandContext#getArgument}
 */
public abstract class NQArgumentType<T> implements CustomArgumentType.Converted<T, String> {

    /**
     * Convert the raw token into the resolved value.
     *
     * @throws CommandSyntaxException to reject the input (the message is shown to the player)
     */
    @Override
    public abstract T convert(final String nativeType) throws CommandSyntaxException;

    @Override
    public ArgumentType<String> getNativeType() {
        // Non-greedy, quotable: reads one token (or a quoted phrase) and does NOT consume the rest of
        // the line. A greedy string here would break any command shaped like "<this> <more args...>".
        return StringArgumentType.string();
    }

    /**
     * Provide completions for the token currently being typed. Returned values are filtered by prefix
     * automatically. Defaults to no suggestions.
     *
     * @param context   the in-progress Brigadier command context
     * @param remaining the partial token the user has typed so far
     */
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return List.of();
    }

    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^>]+>");

    /**
     * Build a {@link CommandSyntaxException} to reject input from {@link #convert}. MiniMessage tags
     * in the message are stripped, since Brigadier renders the failure as plain text.
     */
    protected static CommandSyntaxException fail(final String message) {
        final String plain = message == null ? "" : MINIMESSAGE_TAG.matcher(message).replaceAll("");
        return new SimpleCommandExceptionType(new LiteralMessage(plain)).create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<S> context, final SuggestionsBuilder builder) {
        // Suggestions run on every keystroke of every player; regionMatches(true, ...) filters
        // case-insensitively without allocating a lowercase copy of each candidate.
        final String remaining = builder.getRemaining();
        for (final String suggestion : suggest(context, remaining)) {
            if (suggestion != null && suggestion.regionMatches(true, 0, remaining, 0, remaining.length())) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }
}
