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

package rocks.gravili.notquests.paper.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.logging.Level;

public class LogManager {
    private final NotQuests main;
    private ConsoleCommandSender consoleSender;
    private final Component prefix;
    private String prefixText;



    private final ArrayList<String> severeLogs, warnLogs;

    public LogManager(final NotQuests main) {
        this.main = main;
        severeLogs = new ArrayList<>();
        warnLogs = new ArrayList<>();

        consoleSender = Bukkit.getConsoleSender();

        prefixText = "<#393e46>[<gradient:#E0EAFC:#CFDEF3>NotQuests<#393e46>]<#636c73>: ";
        prefix = main.parse(prefixText);
    }

    public void lateInit() {
        prefixText = main.getConfiguration().getColorsConsolePrefixPrefix() + "NotQuests" + main.getConfiguration().getColorsConsolePrefixSuffix();
    }

    public final ArrayList<String> getErrorLogs() {
        return severeLogs;
    }
    public final ArrayList<String> getWarnLogs() {
        return warnLogs;
    }

    private void log(final Level level, final String color, final String message) {
        log(level, LogCategory.DEFAULT, color, message);
    }

    private void log(final Level level, final LogCategory logCategory, final String color, final String message) {
        consoleSender.sendMessage(main.parse(prefixText + color + message));
    }


    public void info(final LogCategory logCategory, final String message) {
        if (logCategory == LogCategory.DEFAULT) {
            log(Level.INFO, logCategory, main.getConfiguration().getColorsConsoleInfoDefault(), message);
        } else if (logCategory == LogCategory.DATA) {
            log(Level.INFO, logCategory, main.getConfiguration().getColorsConsoleInfoData(), message);
        } else if (logCategory == LogCategory.LANGUAGE) {
            log(Level.INFO, logCategory, main.getConfiguration().getColorsConsoleInfoLanguage(), message);
        }
    }

    public void info(final String message) {
        info(LogCategory.DEFAULT, message);
    }

    public void warn(final LogCategory logCategory, final String message) {
        log(Level.WARNING, logCategory, main.getConfiguration().getColorsConsoleWarnDefault(), message);
        warnLogs.add(message);
    }

    public void warn(final String message) {
        warn(LogCategory.DEFAULT, message);
    }

    public void severe(final LogCategory logCategory, final String message) {
        log(Level.SEVERE, logCategory, main.getConfiguration().getColorsConsoleSevereDefault(), message);
        severeLogs.add(message);
    }

    public void severe(final String message) {
        severe(LogCategory.DEFAULT, message);
    }

    public void debug(final LogCategory logCategory, final String message) {
        if (main.getConfiguration().debug) {
            log(Level.FINE, logCategory, main.getConfiguration().getColorsConsoleDebugDefault(), message);
        }
    }

    public void debug(final String message) {
        debug(LogCategory.DEFAULT, message);
    }
}

