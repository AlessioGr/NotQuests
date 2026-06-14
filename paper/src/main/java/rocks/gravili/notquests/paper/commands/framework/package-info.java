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

/**
 * NotQuests' own command framework, built directly on Paper's native Brigadier command API
 * ({@code io.papermc.paper.command.brigadier}) and registered through Paper's
 * {@code LifecycleEvents.COMMANDS} lifecycle event.
 *
 * <p>This package exists so NotQuests can depend on first-party, always-maintained Paper APIs
 * instead of the third-party Cloud command framework (whose Paper integration has only ever shipped
 * pre-release/beta builds). The guiding principle is to use Paper's {@code Commands} API directly and
 * build thin wrappers here only <b>where necessary</b>:
 *
 * <ul>
 *   <li>{@link rocks.gravili.notquests.paper.commands.framework.NQArgumentType} — base for custom
 *       argument types (replaces Cloud's {@code ArgumentParser}/{@code ParserDescriptor}).</li>
 *   <li>{@link rocks.gravili.notquests.paper.commands.framework.NQCommands} — registrar that hooks
 *       the Paper {@code COMMANDS} lifecycle event and registers our Brigadier command trees.</li>
 * </ul>
 *
 * <p><b>Migration status:</b> foundation + a self-test command ({@code /nqnative}) are live and
 * coexist with the Cloud-registered commands on the same Brigadier dispatcher. The Cloud command
 * framework is removed only once this package reaches feature parity (builder ergonomics, all 19
 * argument parsers ported, flags, help menu, exception/usage handling).
 */
package rocks.gravili.notquests.paper.commands.framework;
