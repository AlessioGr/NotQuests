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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Immutable, fluent command builder — our replacement for Cloud's {@code Command.Builder}. Every
 * method returns a NEW builder (so a base builder can be reused to derive many sub-commands, exactly
 * like Cloud). Build up a path with {@link #literal}/{@link #required}/{@link #optional}, attach
 * {@link #flag}s, set a {@link #handler}, then register it via
 * {@link NQCommandManager#command(NQCommandBuilder)}.
 */
public final class NQCommandBuilder {

    public enum Kind {
        LITERAL,
        REQUIRED,
        OPTIONAL
    }

    /** One node along the command path. */
    public record Step(
            Kind kind,
            String name,
            List<String> aliases,
            NQArgumentType<?> argument,
            NQDescription description,
            NQSuggestionProvider suggestionOverride) {}

    private final List<Step> steps;
    private final List<NQFlag> flags;
    private final NQDescription commandDescription;
    private final String permission;
    private final Class<?> senderType;
    private final Consumer<NQCommandContext> handler;

    private NQCommandBuilder(
            final List<Step> steps,
            final List<NQFlag> flags,
            final NQDescription commandDescription,
            final String permission,
            final Class<?> senderType,
            final Consumer<NQCommandContext> handler) {
        this.steps = List.copyOf(steps);
        this.flags = List.copyOf(flags);
        this.commandDescription = commandDescription;
        this.permission = permission;
        this.senderType = senderType;
        this.handler = handler;
    }

    static NQCommandBuilder root(final String name, final NQDescription description, final String... aliases) {
        return new NQCommandBuilder(
                List.of(new Step(Kind.LITERAL, name, List.of(aliases), null, description, null)),
                List.of(),
                description,
                null,
                null,
                null);
    }

    private NQCommandBuilder copy(
            final List<Step> steps,
            final List<NQFlag> flags,
            final NQDescription commandDescription,
            final String permission,
            final Class<?> senderType,
            final Consumer<NQCommandContext> handler) {
        return new NQCommandBuilder(steps, flags, commandDescription, permission, senderType, handler);
    }

    private NQCommandBuilder withStep(final Step step) {
        final List<Step> next = new ArrayList<>(steps);
        next.add(step);
        return copy(next, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder literal(final String name, final String... aliases) {
        return withStep(new Step(Kind.LITERAL, name, List.of(aliases), null, NQDescription.EMPTY, null));
    }

    public NQCommandBuilder literal(final String name, final NQDescription description, final String... aliases) {
        return withStep(new Step(Kind.LITERAL, name, List.of(aliases), null, description, null));
    }

    public NQCommandBuilder required(final String name, final NQArgumentType<?> argument, final NQDescription description) {
        return withStep(new Step(Kind.REQUIRED, name, List.of(), argument, description, null));
    }

    public NQCommandBuilder required(
            final String name,
            final NQArgumentType<?> argument,
            final NQDescription description,
            final NQSuggestionProvider suggestionOverride) {
        return withStep(new Step(Kind.REQUIRED, name, List.of(), argument, description, suggestionOverride));
    }

    public NQCommandBuilder required(final String name, final NQArgumentType<?> argument) {
        return required(name, argument, NQDescription.EMPTY);
    }

    public NQCommandBuilder optional(final String name, final NQArgumentType<?> argument, final NQDescription description) {
        return withStep(new Step(Kind.OPTIONAL, name, List.of(), argument, description, null));
    }

    public NQCommandBuilder optional(final String name, final NQArgumentType<?> argument) {
        return optional(name, argument, NQDescription.EMPTY);
    }

    public NQCommandBuilder flag(final NQFlag flag) {
        final List<NQFlag> next = new ArrayList<>(flags);
        next.add(flag);
        return copy(steps, next, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder commandDescription(final NQDescription description) {
        return copy(steps, flags, description, permission, senderType, handler);
    }

    public NQCommandBuilder permission(final String permission) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder senderType(final Class<?> senderType) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    public NQCommandBuilder handler(final Consumer<NQCommandContext> handler) {
        return copy(steps, flags, commandDescription, permission, senderType, handler);
    }

    // --- accessors used by NQCommandManager ---

    public List<Step> steps() {
        return steps;
    }

    public List<NQFlag> flags() {
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

    public Consumer<NQCommandContext> handler() {
        return handler;
    }
}
