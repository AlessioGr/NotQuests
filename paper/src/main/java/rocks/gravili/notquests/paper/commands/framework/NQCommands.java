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

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * NotQuests' command registrar, built on Paper's native Brigadier command API instead of Cloud.
 *
 * <p>Usage: construct once, call {@link #hook()} during {@code onEnable} (before Paper fires its
 * {@code COMMANDS} lifecycle event), then queue commands with {@link #register}. Every queued
 * registration runs when the event fires (including on {@code /reload}). Our commands register onto
 * the same Brigadier dispatcher Cloud uses, so the two coexist during the migration off Cloud.
 */
public final class NQCommands {
    private final NotQuests main;
    private final List<Consumer<Commands>> registrations = new ArrayList<>();
    private boolean hooked = false;

    public NQCommands(final NotQuests main) {
        this.main = main;
    }

    /**
     * Registers the Paper {@code COMMANDS} lifecycle handler. Idempotent. Must be called during
     * {@code onEnable} (before the event fires); commands queued via {@link #register} any time before
     * the event fires are picked up.
     */
    public void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        main.getMain()
                .getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS,
                        event -> {
                            final Commands commands = event.registrar();
                            for (final Consumer<Commands> registration : registrations) {
                                try {
                                    registration.accept(commands);
                                } catch (final Throwable t) {
                                    main.getLogManager()
                                            .warn("Failed to register a native command: " + t.getMessage());
                                }
                            }
                        });
    }

    /** Queue a registration callback, run with the live {@link Commands} registrar when the event fires. */
    public void register(final Consumer<Commands> registration) {
        registrations.add(registration);
    }

    /** Convenience: queue a fully-built command node with a description and optional aliases. */
    public void register(
            final LiteralCommandNode<CommandSourceStack> node,
            final String description,
            final String... aliases) {
        register(commands -> commands.register(node, description, Arrays.asList(aliases)));
    }
}
