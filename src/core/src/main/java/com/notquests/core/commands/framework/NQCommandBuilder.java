package com.notquests.core.commands.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Immutable command-type builder. Platform modules provide their own small typed facade over
 * this class when they need platform-specific argument, suggestion, and handler types.
 */
public final class NQCommandBuilder<A extends NQCommandSchema.Argument, F extends NQCommandSchema.Flag, S, H> {
    private final List<NQCommandStep<A, S>> steps;
    private final List<F> flags;
    private final NQDescription commandDescription;
    private final String permission;
    private final Class<?> senderType;
    private final H handler;

    private NQCommandBuilder(
            final List<NQCommandStep<A, S>> steps,
            final List<F> flags,
            final NQDescription commandDescription,
            final String permission,
            final Class<?> senderType,
            final H handler) {
        this.steps = List.copyOf(steps);
        this.flags = List.copyOf(flags);
        this.commandDescription = commandDescription == null ? NQDescription.EMPTY : commandDescription;
        this.permission = permission;
        this.senderType = senderType;
        this.handler = handler;
    }

    public static <A extends NQCommandSchema.Argument, F extends NQCommandSchema.Flag, S, H> NQCommandBuilder<A, F, S, H> root(
            final String name, final NQDescription description, final String... aliases) {
        return new NQCommandBuilder<>(
                List.of(new NQCommandStep<>(
                        NQCommandKind.LITERAL,
                        name,
                        List.of(aliases),
                        null,
                        Objects.requireNonNull(description, "description"),
                        null)),
                List.of(),
                NQDescription.EMPTY,
                null,
                null,
                null);
    }

    private NQCommandBuilder<A, F, S, H> copy(
            final List<NQCommandStep<A, S>> steps,
            final List<F> flags,
            final NQDescription commandDescription,
            final String permission,
            final Class<?> senderType,
            final H handler) {
        return new NQCommandBuilder<>(steps, flags, commandDescription, permission, senderType, handler);
    }

    private NQCommandBuilder<A, F, S, H> withStep(final NQCommandStep<A, S> step) {
        final List<NQCommandStep<A, S>> next = new ArrayList<>(steps);
        next.add(step);
        return copy(next, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder<A, F, S, H> literal(
            final String name, final NQDescription description, final String... aliases) {
        return withStep(new NQCommandStep<>(
                NQCommandKind.LITERAL,
                name,
                List.of(aliases),
                null,
                Objects.requireNonNull(description, "description"),
                null));
    }

    public NQCommandBuilder<A, F, S, H> required(
            final String name, final A argument, final NQDescription description) {
        return withStep(new NQCommandStep<>(
                NQCommandKind.REQUIRED,
                name,
                List.of(),
                argument,
                Objects.requireNonNull(description, "description"),
                null));
    }

    public NQCommandBuilder<A, F, S, H> required(
            final String name,
            final A argument,
            final NQDescription description,
            final S suggestionOverride) {
        return withStep(new NQCommandStep<>(
                NQCommandKind.REQUIRED,
                name,
                List.of(),
                argument,
                Objects.requireNonNull(description, "description"),
                suggestionOverride));
    }

    public NQCommandBuilder<A, F, S, H> optional(
            final String name, final A argument, final NQDescription description) {
        return withStep(new NQCommandStep<>(
                NQCommandKind.OPTIONAL,
                name,
                List.of(),
                argument,
                Objects.requireNonNull(description, "description"),
                null));
    }

    public NQCommandBuilder<A, F, S, H> flag(final F flag) {
        final List<F> next = new ArrayList<>(flags);
        next.add(flag);
        return copy(steps, next, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder<A, F, S, H> commandDescription(final NQDescription description) {
        return copy(steps, flags, description, permission, senderType, handler);
    }

    public NQCommandBuilder<A, F, S, H> permission(final String permission) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder<A, F, S, H> senderType(final Class<?> senderType) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder<A, F, S, H> handler(final H handler) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    public List<NQCommandStep<A, S>> steps() {
        return steps;
    }

    public List<F> flags() {
        return flags;
    }

    public NQDescription commandDescription() {
        return commandDescription;
    }

    public String permission() {
        return permission;
    }

    public Class<?> senderType() {
        return senderType;
    }

    public H handler() {
        return handler;
    }

    public NQCommandRegistration<A, F, S, H> registration() {
        return new NQCommandRegistration<>(steps, flags, commandDescription, permission, senderType, handler);
    }

    public static <C> List<String> resolveSuggestions(
            final NQArgumentType argument,
            final NQSuggestionProvider<C> override,
            final C context,
            final String input,
            final Function<NQArgumentType, List<String>> fallback) {
        if (argument == null) {
            return List.of();
        }
        if (override == null) {
            return fallback.apply(argument);
        }
        final List<String> suggestions = override.suggest(context, input);
        return suggestions == null ? List.of() : suggestions;
    }

    public static List<String> matchingSuggestions(
            final NQArgumentType argument,
            final List<String> candidates,
            final String input,
            final String argumentName) {
        final List<String> available = candidates == null || candidates.isEmpty()
                ? placeholderSuggestion(argumentName)
                : candidates;
        return argument != null && argument.commaSeparated()
                ? commaSeparatedSuggestions(available, input)
                : prefixMatches(available, input);
    }

    public static List<String> prefixMatches(final List<String> candidates, final String input) {
        final String remaining = input == null ? "" : input;
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.regionMatches(true, 0, remaining, 0, remaining.length()))
                .toList();
    }

    public static List<String> placeholderSuggestion(final String argumentName) {
        if (argumentName == null || argumentName.isBlank()) {
            return List.of();
        }
        return List.of("<" + argumentName.strip().replaceAll("\\s+", "-") + ">");
    }

    private static List<String> commaSeparatedSuggestions(
            final List<String> candidates,
            final String input) {
        final String remaining = input == null ? "" : input;
        final int comma = remaining.lastIndexOf(',');
        if (comma < 0) {
            final ArrayList<String> values = new ArrayList<>();
            for (final String candidate : prefixMatches(candidates, remaining)) {
                values.add(candidate);
                if (!candidate.endsWith(",") && !candidate.startsWith("<")) {
                    values.add(candidate + ",");
                }
            }
            return List.copyOf(values);
        }
        final String prefix = remaining.substring(0, comma + 1);
        final String partial = remaining.substring(comma + 1).toLowerCase(Locale.ROOT);
        final ArrayList<String> values = new ArrayList<>();
        for (final String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (partial.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith(partial)) {
                values.add(prefix + candidate);
            }
        }
        return List.copyOf(values);
    }
}
