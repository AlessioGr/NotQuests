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

/**
 * A short human-readable description of a command or argument. Mirrors Cloud's {@code Description}
 * so registration sites can swap with minimal changes. Used by the help menu and the action-bar
 * command-hint to label arguments.
 */
public final class NQDescription {
    public static final NQDescription EMPTY = new NQDescription("");

    private final String text;

    private NQDescription(final String text) {
        this.text = text == null ? "" : text;
    }

    public static NQDescription of(final String text) {
        return new NQDescription(text);
    }

    public String textDescription() {
        return text;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}
