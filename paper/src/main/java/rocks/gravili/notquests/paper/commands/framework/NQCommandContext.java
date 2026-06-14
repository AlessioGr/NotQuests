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

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Execution context handed to command handlers. Our replacement for Cloud's {@code CommandContext}:
 * exposes positional arguments by name, the sender, the raw input, and parsed flags. Backed by the
 * native Brigadier {@link com.mojang.brigadier.context.CommandContext}.
 */
public final class NQCommandContext {
    private final CommandContext<CommandSourceStack> brigadier;
    private final Map<String, Object> flagValues;
    private final Set<String> presentFlags;
    private final String rawInput;

    public NQCommandContext(
            final CommandContext<CommandSourceStack> brigadier,
            final Map<String, Object> flagValues,
            final Set<String> presentFlags,
            final String rawInput) {
        this.brigadier = brigadier;
        this.flagValues = flagValues;
        this.presentFlags = presentFlags;
        this.rawInput = rawInput;
    }

    /** Returns a positional argument (or flag value) by name, or {@code null} if absent. */
    @SuppressWarnings("unchecked")
    public <T> T get(final String name) {
        try {
            return (T) brigadier.getArgument(name, Object.class);
        } catch (final IllegalArgumentException notAnArgument) {
            return (T) flagValues.get(name);
        }
    }

    public <T> T getOrDefault(final String name, final T fallback) {
        final T value = get(name);
        return value != null ? value : fallback;
    }

    public CommandSender sender() {
        return brigadier.getSource().getSender();
    }

    public CommandSourceStack source() {
        return brigadier.getSource();
    }

    public CommandContext<CommandSourceStack> brigadier() {
        return brigadier;
    }

    public RawInput rawInput() {
        return new RawInput(rawInput);
    }

    public Flags flags() {
        return new Flags();
    }

    /** Mirrors Cloud's {@code context.rawInput().input()}. */
    public record RawInput(String input) {}

    /** Flag accessor mirroring Cloud's {@code context.flags()}. */
    public final class Flags {
        public boolean isPresent(final String name) {
            return presentFlags.contains(name);
        }

        public boolean hasFlag(final String name) {
            return presentFlags.contains(name);
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getValue(final String name) {
            return Optional.ofNullable((T) flagValues.get(name));
        }

        @SuppressWarnings("unchecked")
        public <T> T getValue(final String name, final T fallback) {
            return flagValues.containsKey(name) ? (T) flagValues.get(name) : fallback;
        }

        // Flag-object overloads (mirror Cloud's context.flags().getValue(flag, ...)).
        public boolean isPresent(final NQFlag flag) {
            return presentFlags.contains(flag.name());
        }

        public boolean contains(final NQFlag flag) {
            return presentFlags.contains(flag.name());
        }

        public boolean contains(final String name) {
            return presentFlags.contains(name);
        }

        public <T> Optional<T> getValue(final NQFlag flag) {
            return getValue(flag.name());
        }

        public <T> T getValue(final NQFlag flag, final T fallback) {
            return getValue(flag.name(), fallback);
        }
    }
}
