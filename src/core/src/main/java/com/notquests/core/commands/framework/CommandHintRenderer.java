package com.notquests.core.commands.framework;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import com.notquests.core.platform.PlatformPlayer;

/** Renders the shared "what to type next" command hint component. */
public final class CommandHintRenderer {
    private static final TextColor HINT_COLOR = TextColor.color(0x00FFFB);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private CommandHintRenderer() {}

    public static String hintLabel(final NQCommandSchema.Node node) {
        if (node == null) {
            return "[]";
        }
        final String description = node.description() == null ? "" : node.description().textDescription();
        return hintLabel(description, node.name());
    }

    public static String hintLabel(final NQDescription description, final String fallback) {
        return hintLabel(description == null ? "" : description.textDescription(), fallback);
    }

    public static String hintLabel(final String description, final String fallback) {
        final String label = description != null && !description.isBlank() ? description : fallback;
        return "[" + (label == null ? "" : label) + "]";
    }

    public static Component render(final String fullInput, final String hint, final int maxPreviousArguments) {
        final String input = fullInput == null ? "" : fullInput;
        final boolean trailingSpace = input.endsWith(" ");
        String trimmed = input.strip();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        final String[] tokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        final int completed = trailingSpace ? tokens.length : Math.max(0, tokens.length - 1);
        final int start = Math.max(0, completed - Math.max(0, maxPreviousArguments));

        final StringBuilder path = new StringBuilder(start == 0 ? "/" : "\u2026 ");
        for (int i = start; i < completed; i++) {
            path.append(tokens[i]).append(' ');
        }

        final Component base = Component.text(path.toString(), NamedTextColor.GRAY);
        if (!trailingSpace && tokens.length > 0) {
            return base.append(Component.text(tokens[tokens.length - 1], HINT_COLOR, TextDecoration.BOLD));
        }
        return base.append(Component.text(hint == null ? "" : hint, HINT_COLOR, TextDecoration.BOLD));
    }

    public static String renderMiniMessage(final String fullInput, final String hint, final int maxPreviousArguments) {
        return MINI_MESSAGE.serialize(render(fullInput, hint, maxPreviousArguments));
    }

    public static boolean sendActionBarHint(
            final PlatformPlayer questPlayer,
            final String fullInput,
            final String hint,
            final int maxPreviousArguments) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return false;
        }
        questPlayer.sendActionBar(renderMiniMessage(fullInput, hint, maxPreviousArguments));
        return true;
    }

    public static boolean sendActionBarHint(
            final PlatformPlayer questPlayer,
            final String fullInput,
            final NQCommandSchema.Node node,
            final int maxPreviousArguments) {
        return sendActionBarHint(questPlayer, fullInput, hintLabel(node), maxPreviousArguments);
    }
}
