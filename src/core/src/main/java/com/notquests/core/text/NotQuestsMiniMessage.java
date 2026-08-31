package com.notquests.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import com.notquests.core.managers.UtilManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared NotQuests MiniMessage factory and completion-token source. */
public final class NotQuestsMiniMessage {
    private static final Pattern TAG_NAME = Pattern.compile("<(/?)([!?#]?[A-Za-z0-9_-]+)(?=[:>\\s])");
    private static final Pattern CENTER_TAG = Pattern.compile("(?i)</?center>");
    private static final Pattern EMPTY_TAG = Pattern.compile("(?i)<empty>");

    public static final List<String> CUSTOM_TAGS = List.of(
            "main",
            "highlight",
            "highlight2",
            "error",
            "success",
            "unimportant",
            "warn",
            "veryunimportant",
            "negative",
            "positive");

    private NotQuestsMiniMessage() {}

    public static MiniMessage create(final NotQuestsColors.Palette palette) {
        return MiniMessage.builder()
                .preProcessor(NotQuestsMiniMessage::normalizeTags)
                .tags(tagResolver(palette))
                .build();
    }

    public static TagResolver tagResolver(final NotQuestsColors.Palette palette) {
        final TagResolver.Builder builder = TagResolver.builder().resolver(TagResolver.standard());
        for (final String tag : CUSTOM_TAGS) {
            builder.resolver(customTagResolver(palette, tag, tag));
        }
        return builder.build();
    }

    public static String normalizeTags(final String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return miniMessage;
        }
        return TAG_NAME.matcher(applySpecialTags(miniMessage))
                .replaceAll(match -> "<" + match.group(1) + match.group(2).toLowerCase(Locale.ROOT));
    }

    public static String stripMiniMessage(final String miniMessage) {
        final String normalized = normalizeTags(miniMessage);
        return normalized == null ? "" : normalized.replaceAll("<[^>]+>", "");
    }

    public static Component deserialize(final MiniMessage parser, final String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return Component.empty();
        }
        try {
            return parser.deserialize(miniMessage);
        } catch (final RuntimeException exception) {
            return Component.text(stripMiniMessage(miniMessage));
        }
    }

    public static String applySpecialTags(final String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return miniMessage;
        }
        final String withEmptySpaces = EMPTY_TAG.matcher(miniMessage).replaceAll(" ");
        final String[] lines = withEmptySpaces.split("\n", -1);
        final StringBuilder centered = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            final String line = lines[index];
            if (CENTER_TAG.matcher(line).find()) {
                centered.append(centerLine(CENTER_TAG.matcher(line).replaceAll("")));
            } else {
                centered.append(line);
            }
            if (index < lines.length - 1) {
                centered.append('\n');
            }
        }
        return centered.toString();
    }

    private static String centerLine(final String miniMessageLine) {
        return UtilManager.centerLine(miniMessageLine);
    }

    private static TagResolver customTagResolver(
            final NotQuestsColors.Palette palette,
            final String tagName,
            final String paletteKey) {
        return TagResolver.resolver(
                tagName,
                (args, ctx) -> SimpleGradientTag.create(colorsFor(palette, paletteKey)));
    }

    public static List<String> completionTokens() {
        final List<String> tokens = new ArrayList<>();
        for (final NamedTextColor color : NamedTextColor.NAMES.values()) {
            tokens.add(color.toString().toLowerCase(Locale.ROOT));
        }
        for (final String customTag : CUSTOM_TAGS) {
            tokens.add(customTag.equals("veryunimportant") ? "veryUnimportant" : customTag);
        }
        tokens.add("bold");
        tokens.add("strikethrough");
        tokens.add("italic");
        tokens.add("underlined");
        tokens.add("obfuscated");
        tokens.add("gradient");
        tokens.add("rainbow");
        tokens.add("center");
        return List.copyOf(tokens);
    }

    private static List<String> colorsFor(final NotQuestsColors.Palette palette, final String tag) {
        final List<String> configured = palette == null ? null : palette.colors(tag);
        return configured == null || configured.isEmpty() ? SimpleGradientTag.FALLBACK_COLORS : configured;
    }
}
