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

package notquests.notquests.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class UtilManager {
    private final NotQuests main;

    public UtilManager(NotQuests main) {
        this.main = main;
    }

    public final OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }


    public Component getFancyActionBarTabCompletion(final String[] args, final String hintCurrentArg, String hintNextArgs) {
        final int maxPreviousArgs = main.getDataManager().getConfiguration().getActionBarCommandCompletionMaxPreviousArgumentsDisplayed();

        final StringBuilder argsTogether = new StringBuilder();


        final int initialCutoff = (args.length) - maxPreviousArgs;
        int cutoff = (args.length) - maxPreviousArgs;


        for (int i = -1; i < args.length - 1; i++) {

            if (cutoff == 0) {
                if (i == -1) {
                    argsTogether.append("/qa ");
                } else {
                    argsTogether.append(args[i]).append(" ");
                }

            } else {
                if (cutoff > 0) {
                    cutoff -= 1;
                } else { //Just 1 arg
                    argsTogether.append("/qa ");
                }

            }
        }

        if (initialCutoff > 0) {
            argsTogether.insert(0, "[...] ");
        }


        Component currentCompletion;
        if (args[args.length - 1].isBlank()) {
            currentCompletion = Component.text("" + hintCurrentArg, NamedTextColor.GREEN);
        } else {
            currentCompletion = Component.text("" + args[args.length - 1], NamedTextColor.YELLOW);
        }

        if (!hintNextArgs.isBlank()) {
            //Chop off if too long
            if (hintNextArgs.length() > 15) {
                hintNextArgs = hintNextArgs.substring(0, 14) + "...";
            }

            return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"))
                    .append(currentCompletion)
                    .append(Component.text(" " + hintNextArgs, NamedTextColor.GRAY));
        } else {
            if (!args[args.length - 1].isBlank()) { //Command finished
                return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"))
                        .append(currentCompletion)
                        .append(Component.text(" ✓", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"))
                        .append(currentCompletion);
            }

        }

    }


    public void sendFancyActionBar(final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        if (main.getDataManager().getConfiguration().isActionBarCommandCompletionEnabled()) {
            audience.sendActionBar(main.getUtilManager().getFancyActionBarTabCompletion(args, hintCurrentArg, hintNextArgs));
        }

    }

}
