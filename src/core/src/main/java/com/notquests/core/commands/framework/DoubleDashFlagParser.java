package com.notquests.core.commands.framework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DoubleDashFlagParser {
    private DoubleDashFlagParser() {}

    public static <F> ParseResult parse(
            final String flagString,
            final List<F> flags,
            final Function<F, String> name,
            final Predicate<F> presence,
            final BiFunction<F, String, Object> valueConverter) {
        final Map<String, Object> values = new HashMap<>();
        final Set<String> present = new HashSet<>();
        if (flagString == null || flagString.isBlank() || flags == null || flags.isEmpty()) {
            return new ParseResult(values, present);
        }

        final String[] tokens = flagString.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i];
            if (!token.startsWith("--")) {
                continue;
            }
            final F flag = find(flags, name, token.substring(2));
            if (flag == null) {
                continue;
            }
            final String flagName = name.apply(flag);
            present.add(flagName);
            if (presence.test(flag) || i + 1 >= tokens.length || tokens[i + 1].startsWith("--")) {
                continue;
            }

            final StringBuilder rawBuilder = new StringBuilder();
            int valueIndex = i + 1;
            while (valueIndex < tokens.length && !tokens[valueIndex].startsWith("--")) {
                if (!rawBuilder.isEmpty()) {
                    rawBuilder.append(' ');
                }
                rawBuilder.append(tokens[valueIndex]);
                valueIndex++;
            }
            i = valueIndex - 1;
            final Object converted = valueConverter.apply(flag, rawBuilder.toString());
            if (converted != null) {
                values.put(flagName, converted);
            }
        }
        return new ParseResult(values, present);
    }

    public static <F> F awaitingValue(
            final String input,
            final List<F> flags,
            final Function<F, String> name,
            final Predicate<F> presence) {
        final String raw = input == null ? "" : input;
        final int tokenStart = currentTokenStart(raw);
        final String beforeCurrentToken = raw.substring(0, tokenStart).stripTrailing();
        if (beforeCurrentToken.isEmpty()) {
            return null;
        }
        final String[] tokens = beforeCurrentToken.split("\\s+");
        final String last = tokens[tokens.length - 1];
        if (!last.startsWith("--")) {
            return null;
        }
        final F flag = find(flags, name, last.substring(2));
        return flag != null && !presence.test(flag) ? flag : null;
    }

    public static int currentTokenStart(final String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        if (Character.isWhitespace(input.charAt(input.length() - 1))) {
            return input.length();
        }
        for (int index = input.length() - 1; index >= 0; index--) {
            if (Character.isWhitespace(input.charAt(index))) {
                return index + 1;
            }
        }
        return 0;
    }

    public static <F> F find(
            final List<F> flags,
            final Function<F, String> name,
            final String requestedName) {
        if (flags == null || name == null || requestedName == null) {
            return null;
        }
        for (final F flag : flags) {
            if (name.apply(flag).equalsIgnoreCase(requestedName)) {
                return flag;
            }
        }
        return null;
    }

    public record ParseResult(Map<String, Object> values, Set<String> present) {}
}
