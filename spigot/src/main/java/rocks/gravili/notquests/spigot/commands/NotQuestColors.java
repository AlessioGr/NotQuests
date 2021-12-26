/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.spigot.commands;

import net.kyori.adventure.text.format.TextColor;

public class NotQuestColors {
    public static TextColor main = TextColor.color(0x1985ff);
    public static String mainMM = "<#1985ff>";
    public static TextColor highlight = TextColor.color(0xff45ae);
    public static String highlightMM = "<#ff45ae>";
    public static TextColor highlight2 = TextColor.color(0xff004c);
    public static TextColor lightHighlight = TextColor.color(0x4da0ff);
    public static String lightHighlightMM = "<#4da0ff>";

    public static String successGradient = "<gradient:#54b2ff:#ff5ecc>";
    public static String errorGradient = "<gradient:#ff004c:#a80000>";
    public static String mainGradient = "<gradient:#1985ff:#2bc7ff>";
    public static String highlightGradient = "<gradient:#00fffb:#00ffc3>";
    public static String highlight2Gradient = "<gradient:#ff2465:#ff24a0>";
    public static String unimportant = "<GRAY>";
    public static String veryUnimportant = "<DARK_GRAY>";

    public static String unimportantClose = "</GRAY>";

    public static String warningGradient = "<gradient:#fff700:#ffa629>";

    public static String debugTitleGradient = "<gradient:#abfff5:#c7fff8>";
    public static String debugGradient = "<gradient:#8a98c2:#8ac2bb>";
    public static String debugHighlightGradient = "<gradient:#009dff:#00e5ff>";
}
