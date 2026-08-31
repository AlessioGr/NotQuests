package com.notquests.core.commands.framework;

public record CommandMessage(boolean success, String message, boolean renderEmptyLine) {
    public static CommandMessage none() {
        return new CommandMessage(true, "", false);
    }

    public static CommandMessage emptyLine() {
        return new CommandMessage(true, "", true);
    }

    public static CommandMessage success(final String message) {
        return new CommandMessage(true, message, false);
    }

    public static CommandMessage error(final String message) {
        return new CommandMessage(false, message, false);
    }

    public String formattedMessage() {
        if (message == null || message.isBlank()) {
            return message;
        }
        if (containsMiniMessageTag(message)) {
            return startsWithMiniMessageTag(message)
                    ? message
                    : (success ? "<success>" : "<error>") + message;
        }
        return (success ? "<success>" : "<error>") + message;
    }

    private static boolean containsMiniMessageTag(final String message) {
        final int open = message.indexOf('<');
        return open >= 0 && message.indexOf('>', open + 1) > open;
    }

    private static boolean startsWithMiniMessageTag(final String message) {
        return message.stripLeading().startsWith("<");
    }
}
