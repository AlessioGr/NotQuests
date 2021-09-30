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

import io.papermc.lib.PaperLib;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class LogManager {
    private final NotQuests main;
    private final Audience consoleSender;
    private final Component prefix;
    private final String prefixText;

    public LogManager(final NotQuests main) {
        this.main = main;
        consoleSender = main.adventure().sender(Bukkit.getConsoleSender());


        if (PaperLib.isPaper()) {
            prefixText = "<#393e46>[<gradient:#E0EAFC:#CFDEF3>NotQuests<#393e46>]<#636c73>: ";
            prefix = MiniMessage.miniMessage().parse(prefixText);
        } else {
            prefixText = "<DARK_GRAY>[<WHITE>NotQuests<DARK_GRAY>]<GRAY>: ";
            prefix = MiniMessage.miniMessage().parse(prefixText);
        }
    }


    public void log(final Level level, final String message) {
        /*Component afterPrefixSuffix = Component.text("<WHITE>");
        if(level == Level.INFO) {
            afterPrefixSuffix = MiniMessage.miniMessage().parse("<GREEN>");
        }
        consoleSender.sendMessage(Component.text("Log level: " + level.getName()));
        consoleSender.sendMessage(prefix.append(afterPrefixSuffix).append(Component.text(message)));*/

        String afterPrefixSuffix = "<WHITE>";
        if (level == Level.INFO) {
            if (PaperLib.isPaper()) {
                afterPrefixSuffix = "<gradient:#37a659:#56B4D3>";
            } else {
                afterPrefixSuffix = "<GREEN>";
            }

        } else if (level == Level.WARNING) {
            afterPrefixSuffix = "<YELLOW>";
        } else if (level == Level.SEVERE) {
            afterPrefixSuffix = "<RED>";
        }

        consoleSender.sendMessage(MiniMessage.miniMessage().parse(prefixText + afterPrefixSuffix + message));

    }


    public void log(final Level level, final LogCategory logCategory, final String message) {
        String afterPrefixSuffix = "<WHITE>";
        if (level == Level.INFO) {
            if (logCategory == LogCategory.DEFAULT) {
                if (PaperLib.isPaper()) {
                    afterPrefixSuffix = "<gradient:#37a659:#56B4D3>";

                } else {
                    afterPrefixSuffix = "<GREEN>";
                }

            } else if (logCategory == LogCategory.DATA) {
                if (PaperLib.isPaper()) {
                    afterPrefixSuffix = "<gradient:#1FA2FF:#12D8FA:#A6FFCB>";

                } else {
                    afterPrefixSuffix = "<BLUE>";
                }

            } else if (logCategory == LogCategory.LANGUAGE) {
                if (PaperLib.isPaper()) {
                    afterPrefixSuffix = "<gradient:#AA076B:#61045F>";

                } else {
                    afterPrefixSuffix = "<DARK_PURPLE>";
                }

            }

        } else if (level == Level.WARNING) {
            afterPrefixSuffix = "<YELLOW>";
        } else if (level == Level.SEVERE) {
            afterPrefixSuffix = "<RED>";
        }

        consoleSender.sendMessage(MiniMessage.miniMessage().parse(prefixText + afterPrefixSuffix + message));

    }


    public void info(final String message) {
        log(Level.INFO, message);
    }

    public void warn(final String message) {
        log(Level.WARNING, message);
    }

    public void severe(final String message) {
        log(Level.SEVERE, message);
    }

}

