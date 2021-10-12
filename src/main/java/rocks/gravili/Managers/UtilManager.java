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

package rocks.gravili.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import rocks.gravili.NotQuests;

import java.util.HashMap;

public class UtilManager {
    private final NotQuests main;
    private final HashMap<Audience, BossBar> playersAndBossBars;

    private final static int CENTER_PX = 154;

    public UtilManager(NotQuests main) {
        this.main = main;
        playersAndBossBars = new HashMap<>();
    }

    public final OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }


    private Component getFancyCommandTabCompletion(final String[] args, final String hintCurrentArg, String hintNextArgs) {
        final int maxPreviousArgs = main.getDataManager().getConfiguration().getFancyCommandCompletionMaxPreviousArgumentsDisplayed();

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
            currentCompletion = Component.text("" + hintCurrentArg, NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
        } else {
            currentCompletion = Component.text("" + args[args.length - 1], NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
        }

        if (!hintNextArgs.isBlank()) {
            //Chop off if too long
            if (hintNextArgs.length() > 15) {
                hintNextArgs = hintNextArgs.substring(0, 14) + "...";
            }

            return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"), TextDecoration.ITALIC)
                    .append(currentCompletion)
                    .append(Component.text(" " + hintNextArgs, NamedTextColor.GRAY));
        } else {
            if (!args[args.length - 1].isBlank()) { //Command finished
                return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"), TextDecoration.ITALIC)
                        .append(currentCompletion)
                        .append(Component.text(" ✓", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                return Component.text(argsTogether.toString(), TextColor.fromHexString("#a5c7a6"), TextDecoration.ITALIC)
                        .append(currentCompletion);
            }

        }

    }


    public void sendFancyCommandCompletion(final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        if (!main.getDataManager().getConfiguration().isActionBarFancyCommandCompletionEnabled() && !main.getDataManager().getConfiguration().isTitleFancyCommandCompletionEnabled() && !main.getDataManager().getConfiguration().isBossBarFancyCommandCompletionEnabled()) {
            return;
        }

        final Component fancyTabCompletion = getFancyCommandTabCompletion(args, hintCurrentArg, hintNextArgs);
        if (main.getDataManager().getConfiguration().isActionBarFancyCommandCompletionEnabled()) {
            audience.sendActionBar(fancyTabCompletion);
        }
        if (main.getDataManager().getConfiguration().isTitleFancyCommandCompletionEnabled()) {
            audience.showTitle(Title.title(Component.text(""), fancyTabCompletion));
        }
        if (main.getDataManager().getConfiguration().isBossBarFancyCommandCompletionEnabled()) {

            final BossBar oldBossBar = playersAndBossBars.get(audience);
            if (oldBossBar != null) {
                oldBossBar.name(fancyTabCompletion);
                playersAndBossBars.replace(audience, oldBossBar);
                audience.showBossBar(oldBossBar);

            } else {
                BossBar bossBarToShow = BossBar.bossBar(fancyTabCompletion, 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                playersAndBossBars.put(audience, bossBarToShow);
                audience.showBossBar(bossBarToShow);
            }

        }

    }

    public final HashMap<String, String> getExtraArguments(final String argumentString) {
        return new HashMap<>() {{

            String currentIdentifier = "";
            String currentContent = "";

            boolean identifierMode = true;

            for (int i = 0; i < argumentString.length(); i++) {
                char curChar = argumentString.charAt(i);
                if (curChar == '-') {
                    identifierMode = true;
                    if (!currentIdentifier.isBlank() && !currentContent.isBlank()) {
                        put(currentIdentifier, currentContent);
                    }
                    currentIdentifier = "";
                    currentContent = "";
                } else if (curChar == ' ') {
                    if (!identifierMode) {
                        currentContent += ' ';
                    }

                    identifierMode = false;
                } else {
                    if (identifierMode) {
                        currentIdentifier += curChar;
                    } else {
                        currentContent += curChar;
                    }
                }
            }

            if (!currentIdentifier.isBlank() && !currentContent.isBlank()) {
                put(currentIdentifier, currentContent);
            }
        }};
    }

    public final HashMap<String, String> getExtraArguments(final String[] args, final int startAt) {
        StringBuilder extraArgsString = new StringBuilder();
        if (args.length > startAt) {
            for (int i = startAt; i < args.length; i++) {
                if (i > startAt) {
                    extraArgsString.append(" ");
                }
                extraArgsString.append(args[i]);
            }
        }
        return getExtraArguments(extraArgsString.toString());
    }


    public final String getCenteredMessage(final String message) {
        String[] lines = message.split("\n", 40);
        StringBuilder returnMessage = new StringBuilder();


        for (String line : lines) {
            int messagePxSize = 0;
            boolean previousCode = false;
            boolean isBold = false;

            for (char c : line.toCharArray()) {
                if (c == '§') {
                    previousCode = true;
                } else if (previousCode) {
                    previousCode = false;
                    isBold = c == 'l';
                } else {
                    DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                    messagePxSize = isBold ? messagePxSize + dFI.getBoldLength() : messagePxSize + dFI.getLength();
                    messagePxSize++;
                }
            }
            int toCompensate = CENTER_PX - messagePxSize / 2;
            int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
            int compensated = 0;
            final StringBuilder sb = new StringBuilder();
            while (compensated < toCompensate) {
                sb.append(" ");
                compensated += spaceLength;
            }
            returnMessage.append(sb).append(line).append("\n");
        }

        return returnMessage.toString();
    }

}
