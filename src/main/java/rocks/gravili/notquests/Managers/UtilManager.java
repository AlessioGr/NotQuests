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

package rocks.gravili.notquests.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;

import java.io.File;
import java.util.*;

public class UtilManager {
    private final NotQuests main;
    private final HashMap<Audience, BossBar> playersAndBossBars;

    private final static int CENTER_PX = 154;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public UtilManager(NotQuests main) {
        this.main = main;
        playersAndBossBars = new HashMap<>();
    }

    /**
     * Utility function: Returns the UUID of an online player. If the player is
     * offline, it will return null.
     *
     * @param playerName the name of the online player you want to get the UUID from
     * @return the UUID of the specified, online player
     */
    public final UUID getOnlineUUID(final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        } else {
            return null;
        }
    }

    /**
     * Utility function: Tries to return the UUID of an offline player (can also be online)
     * via some weird Bukkit function. This probably makes calls to the Minecraft API, I don't
     * know for sure. It's definitely slower.
     *
     * @param playerName the name of the player you want to get the UUID from
     * @return the UUID from the player based on his current username.
     */
    public final UUID getOfflineUUID(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }


    public final OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    /**
     * Replaces all occurrences of keys of the given map in the given string
     * with the associated value in that map.
     * <p>
     * This method is semantically the same as calling
     * {@link String#replace(CharSequence, CharSequence)} for each of the
     * entries in the map, but may be significantly faster for many replacements
     * performed on a short string, since
     * {@link String#replace(CharSequence, CharSequence)} uses regular
     * expressions internally and results in many String object allocations when
     * applied iteratively.
     * <p>
     * The order in which replacements are applied depends on the order of the
     * map's entry set.
     */
    public String replaceFromMap(String string,
                                 Map<String, String> replacements) {
        StringBuilder sb = new StringBuilder(string);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            int start = sb.indexOf(key, 0);
            while (start > -1) {
                int end = start + key.length();
                int nextSearchStart = start + value.length();
                sb.replace(start, end, value);
                start = sb.indexOf(key, nextSearchStart);
            }
        }
        return sb.toString();
    }

    private Component getFancyCommandTabCompletion(final String[] args, final String hintCurrentArg, String hintNextArgs) {
        final int maxPreviousArgs = main.getConfiguration().getFancyCommandCompletionMaxPreviousArgumentsDisplayed();

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
            currentCompletion = miniMessage.parse("<RESET>" + NotQuestColors.highlightMM + "<BOLD>" + hintCurrentArg);
        } else {
            currentCompletion = miniMessage.parse("<RESET><YELLOW><BOLD>" + args[args.length - 1]);

        }

        if (!hintNextArgs.isBlank()) {
            //Chop off if too long
            if (hintNextArgs.length() > 15) {
                hintNextArgs = hintNextArgs.substring(0, 14) + "...";
            }
            return miniMessage.parse(NotQuestColors.lightHighlightMM + "<ITALIC>" + argsTogether)
                    .append(currentCompletion)
                    .append(miniMessage.parse("<GRAY> " + hintNextArgs));
        } else {
            if (!args[args.length - 1].isBlank()) { //Command finished
                return miniMessage.parse(NotQuestColors.lightHighlightMM + "<ITALIC>" + argsTogether)
                        .append(currentCompletion)
                        .append(Component.text(" ✓", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                return miniMessage.parse(NotQuestColors.lightHighlightMM + "<ITALIC>" + argsTogether)
                        .append(currentCompletion);
            }

        }

    }


    public void sendFancyCommandCompletion(final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        if (!main.getConfiguration().isActionBarFancyCommandCompletionEnabled() && !main.getConfiguration().isTitleFancyCommandCompletionEnabled() && !main.getConfiguration().isBossBarFancyCommandCompletionEnabled()) {
            return;
        }

        final Component fancyTabCompletion = getFancyCommandTabCompletion(args, hintCurrentArg, hintNextArgs);
        if (main.getConfiguration().isActionBarFancyCommandCompletionEnabled()) {
            audience.sendActionBar(fancyTabCompletion);
        }
        if (main.getConfiguration().isTitleFancyCommandCompletionEnabled()) {
            audience.showTitle(Title.title(Component.text(""), fancyTabCompletion));
        }
        if (main.getConfiguration().isBossBarFancyCommandCompletionEnabled()) {

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
        String[] lines = miniMessageToLegacy(message).split("\n", 40);//TODO: Rethink with minimessage in mind
        StringBuilder returnMessage = new StringBuilder();

        int lineCounter = 0;
        for (String line : lines) {//TODO: Rethink with minimessage in mind
            lineCounter++;
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
            returnMessage.append(sb).append(line);
            if (lineCounter != lines.length) {
                returnMessage.append("\n");
            }
        }

        main.getLogManager().debug("Centered message! Old message:\n" + message + "\nCentered Message:\n" + returnMessage);

        return returnMessage.toString();
    }


    public final boolean isItemEmpty(final ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }


    public List<File> listFilesRecursively(File directory) {
        List<File> files = new ArrayList<>();
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    files.addAll(listFilesRecursively(file));
                }
            }

        return files;
    }

    public final String wrapText(final String unwrappedText, final int maxLineLength){
        /*final StringBuilder descriptionWithLineBreaks = new StringBuilder();
        int count = 0;
        for (char character : unwrappedText.toCharArray()) {
            count++;
            if (count > maxLineLength) {
                count = 0;
                descriptionWithLineBreaks.append("\n§8");
            } else {
                descriptionWithLineBreaks.append(character);
            }
        }*/
        //return descriptionWithLineBreaks.toString();
        return WordUtils.wrap(unwrappedText.replace("\\n", "\n"), maxLineLength, "\n<GRAY>", main.getConfiguration().wrapLongWords);

    }


    public final String replaceLegacyWithMiniMessage(String toReplace) {
        if (!toReplace.replace("& ", "").contains("&")) {
            return toReplace;
        }
        Component component = LegacyComponentSerializer.builder().hexColors().build().deserialize(ChatColor.translateAlternateColorCodes('&', toReplace));
        String finalS = miniMessage.serialize(component);

        main.getLogManager().debug("legacy => minimessage Converted <RESET>" + toReplace + "</RESET> to <RESET>" + finalS + "</RESET>");
        return finalS;
    }

    public final String miniMessageToLegacy(String miniMessageString) {
        String legacy = LegacyComponentSerializer.builder().hexColors().build().serialize(miniMessage.parse(miniMessageString));
        main.getLogManager().debug("mm => legacy: Converted <RESET>" + miniMessageString + "</RESET> to <RESET>" + legacy + "</RESET>");

        return legacy;
    }

    public String miniMessageToLegacyWithSpigotRGB(String miniMessageString) {
        String legacy = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build().serialize(miniMessage.parse(miniMessageString));
        main.getLogManager().debug("mm => legacy: Converted <RESET>" + miniMessageString + "</RESET> to <RESET>" + legacy + "</RESET>");

        return legacy;
    }
}
