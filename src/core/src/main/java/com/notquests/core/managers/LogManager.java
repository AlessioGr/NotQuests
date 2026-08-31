package com.notquests.core.managers;

import com.notquests.core.text.NotQuestsMiniMessage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Owns NotQuests log filtering, formatting, category colors, console delivery, and history. */
public final class LogManager {
    private final ConfigurationManager configuration;
    private final CopyOnWriteArrayList<String> errorLogs = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> warningLogs = new CopyOnWriteArrayList<>();
    private volatile Consumer<ConsoleLine> console = ignored -> {};

    public LogManager(final ConfigurationManager configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    /** Supplies the platform's atomic console-emission leaf. */
    public void console(final Consumer<ConsoleLine> console) {
        this.console = console == null ? ignored -> {} : console;
    }

    public void info(final String message, final Object... values) {
        log(Level.INFO, LogCategory.DEFAULT, message, values);
    }

    public void info(final LogCategory category, final String message, final Object... values) {
        log(Level.INFO, category, message, values);
    }

    public void warn(final String message, final Object... values) {
        log(Level.WARNING, LogCategory.DEFAULT, message, values);
    }

    public void warn(final LogCategory category, final String message, final Object... values) {
        log(Level.WARNING, category, message, values);
    }

    public void severe(final String message, final Object... values) {
        log(Level.SEVERE, LogCategory.DEFAULT, message, values);
    }

    public void severe(final LogCategory category, final String message, final Object... values) {
        log(Level.SEVERE, category, message, values);
    }

    public void debug(final String message, final Object... values) {
        log(Level.FINE, LogCategory.DEFAULT, message, values);
    }

    public void debug(final LogCategory category, final String message, final Object... values) {
        log(Level.FINE, category, message, values);
    }

    private void log(
            final Level level,
            final LogCategory category,
            final String message,
            final Object... values) {
        final ConsoleLine line = consoleLine(level, category, message, values);
        if (line.enabled()) {
            console.accept(line);
        }
    }

    public ConsoleLine consoleLine(
            final Level level,
            final LogCategory category,
            String message,
            final Object... values) {
        final Level finalLevel = level == null ? Level.INFO : level;
        if (finalLevel == Level.FINE && !configuration.debugEnabled()) {
            return ConsoleLine.disabled();
        }
        message = message == null ? "" : message;
        if (values != null && values.length > 0) {
            message = message.formatted((Object[]) values);
        }
        if (finalLevel == Level.SEVERE) {
            recordError(message);
        } else if (finalLevel == Level.WARNING) {
            recordWarning(message);
        }
        if (!configuration.consoleColorsEnabled()) {
            return new ConsoleLine(
                    true,
                    finalLevel,
                    "[NotQuests]: ",
                    "",
                    NotQuestsMiniMessage.stripMiniMessage(message),
                    false,
                    false);
        }
        final boolean downsampled = configuration.consoleColorsDownsampled();
        final String prefix = downsampled
                ? "<gray>[<aqua>NotQuests</aqua>]</gray><dark_gray>: "
                : configuration.consolePrefixPrefix() + "NotQuests" + configuration.consolePrefixSuffix();
        return new ConsoleLine(
                true,
                finalLevel,
                prefix,
                color(finalLevel, category),
                message,
                true,
                downsampled);
    }

    private String color(final Level level, final LogCategory category) {
        if (level == Level.SEVERE) {
            return configuration.consoleSevereColor();
        }
        if (level == Level.WARNING) {
            return configuration.consoleWarnColor();
        }
        if (level == Level.FINE) {
            return configuration.consoleDebugColor();
        }
        return configuration.consoleInfoColor(category);
    }

    public void recordError(final String message) {
        if (message != null && !message.isBlank()) {
            errorLogs.add(message);
        }
    }

    public void recordWarning(final String message) {
        if (message != null && !message.isBlank()) {
            warningLogs.add(message);
        }
    }

    public List<String> errorLogs() {
        return List.copyOf(errorLogs);
    }

    public List<String> warningLogs() {
        return List.copyOf(warningLogs);
    }

    public void clear() {
        errorLogs.clear();
        warningLogs.clear();
    }

    public enum LogCategory {
        DEFAULT,
        DATA,
        LANGUAGE
    }

    public record ConsoleLine(
            boolean enabled,
            Level level,
            String prefix,
            String color,
            String message,
            boolean miniMessage,
            boolean downsampleColors) {
        public ConsoleLine {
            level = level == null ? Level.INFO : level;
            prefix = prefix == null ? "" : prefix;
            color = color == null ? "" : color;
            message = message == null ? "" : message;
        }

        public static ConsoleLine disabled() {
            return new ConsoleLine(false, Level.INFO, "", "", "", false, false);
        }
    }
}
