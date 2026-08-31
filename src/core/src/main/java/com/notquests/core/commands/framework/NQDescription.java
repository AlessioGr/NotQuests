package com.notquests.core.commands.framework;

import java.util.Objects;

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

    public static NQDescription required(final String text, final String owner) {
        final NQDescription description = of(Objects.requireNonNull(text, owner + " description"));
        if (description.textDescription().isBlank()) {
            throw new IllegalArgumentException(owner + " description is required");
        }
        return description;
    }

    public String textDescription() {
        return text;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}
