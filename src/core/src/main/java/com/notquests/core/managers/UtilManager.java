package com.notquests.core.managers;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.text.DefaultFontInfo;
import com.notquests.core.variables.NumberExpression;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UtilManager {
    private static final int CENTER_PX = 154;
    private static final char BOLD_MARKER = '\u2615';
    private static final char RESET_MARKER = '\u2617';
    private static final Pattern BOLD_TAG = Pattern.compile("(?i)<bold>");
    private static final Pattern BOLD_END_OR_RESET_TAG = Pattern.compile("(?i)</bold>|<reset>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern DURATION = Pattern.compile(
            "(\\d+)\\s*("
                    + "ms|msec|msecs|millis|millisecond|milliseconds|milisecond|miliseconds|"
                    + "s|sec|secs|second|seconds|"
                    + "m|min|mins|minute|minutes|"
                    + "h|hr|hrs|hour|hours|"
                    + "d|day|days"
                    + ")?",
            Pattern.CASE_INSENSITIVE);

    private UtilManager() {}

    public static Duration parseDuration(final String input) {
        final Matcher matcher = DURATION.matcher(input == null ? "" : input.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + input + "' is not a valid duration (e.g. 500ms, 30s, 5m, 2h, 1d)");
        }
        final long amount = Long.parseLong(matcher.group(1));
        final String unit = matcher.group(2) == null
                ? "ms"
                : matcher.group(2).toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "s", "sec", "secs", "second", "seconds" -> Duration.ofSeconds(amount);
            case "m", "min", "mins", "minute", "minutes" -> Duration.ofMinutes(amount);
            case "h", "hr", "hrs", "hour", "hours" -> Duration.ofHours(amount);
            case "d", "day", "days" -> Duration.ofDays(amount);
            default -> Duration.ofMillis(amount);
        };
    }

    public static String replaceFromMap(final String string, final Map<String, Supplier<String>> replacements) {
        final StringBuilder builder = new StringBuilder(string);
        for (final Map.Entry<String, Supplier<String>> entry : replacements.entrySet()) {
            final String key = entry.getKey();
            int start = builder.indexOf(key, 0);
            if (start < 0) {
                continue;
            }

            final String value = entry.getValue().get();
            while (start > -1) {
                final int end = start + key.length();
                final int nextSearchStart = start + value.length();
                builder.replace(start, end, value);
                start = builder.indexOf(key, nextSearchStart);
            }
        }
        return builder.toString();
    }

    public static String applyNotQuestsPlaceholders(
            final String message,
            final String questIdentifier,
            final PlatformPlayer questPlayer,
            final NotQuestsAdapter adapter) {
        String result = message == null ? "" : message;

        if (questIdentifier != null && !questIdentifier.isBlank()) {
            result = result.replace("{QUEST}", questIdentifier);
        }

        if (questPlayer == null) {
            return result;
        }

        result = result.replace("{PLAYER}", questPlayer.playerName())
                .replace("{PLAYERUUID}", questPlayer.playerIdentifier())
                .replace("{PLAYERX}", String.valueOf(questPlayer.positionX()))
                .replace("{PLAYERY}", String.valueOf(questPlayer.positionY()))
                .replace("{PLAYERZ}", String.valueOf(questPlayer.positionZ()))
                .replace("{WORLD}", questPlayer.worldName());

        if (result.contains("}}")) {
            result = replaceExpressions(result, questPlayer, adapter);
        }

        return result;
    }

    private static String replaceExpressions(
            String result,
            final PlatformPlayer questPlayer,
            final NotQuestsAdapter adapter) {
        for (final String split : result.split("}}")) {
            if (!split.contains("{{")) {
                continue;
            }
            if (!split.contains("{{~")) {
                final int opening = split.indexOf("{{");
                final String expression = split.substring(opening + 2);
                final double value = new NumberExpression(adapter, expression).calculateValue(questPlayer);
                result = result.replace("{{" + expression + "}}", String.valueOf(value));
            } else {
                final int opening = split.indexOf("{{~");
                final String expression = split.substring(opening + 3);
                final double value = new NumberExpression(adapter, expression).calculateValue(questPlayer);
                result = result.replace("{{~" + expression + "}}", String.valueOf((int) Math.round(value)));
            }
        }
        return result;
    }

    public static String centered(final String message) {
        final String text = message == null ? "" : message;
        final String[] lines = text.split("\n", -1);
        final StringBuilder centered = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            centered.append(centerLine(lines[index]));
            if (index < lines.length - 1) {
                centered.append('\n');
            }
        }
        return centered.toString();
    }

    public static String centerLine(final String miniMessageLine) {
        final String line = miniMessageLine == null ? "" : miniMessageLine;
        final int messagePxSize = pixelWidth(line);
        final int toCompensate = CENTER_PX - messagePxSize / 2;
        final int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        final StringBuilder spaces = new StringBuilder();
        int compensated = 0;
        while (compensated < toCompensate) {
            spaces.append(' ');
            compensated += spaceLength;
        }
        return spaces.append(line).toString();
    }

    public static String wrap(final String text, final int maxLineLength, final boolean wrapLongWords) {
        final String input = text == null ? "" : text.replace("\\n", "\n");
        if (maxLineLength <= 0 || input.isBlank()) {
            return input;
        }
        final String[] sourceLines = input.split("\n", -1);
        final StringBuilder wrapped = new StringBuilder();
        for (int lineIndex = 0; lineIndex < sourceLines.length; lineIndex++) {
            if (lineIndex > 0) {
                wrapped.append('\n');
            }
            wrapped.append(wrapLine(sourceLines[lineIndex], maxLineLength, wrapLongWords));
        }
        return wrapped.toString();
    }

    public static List<String> wrapToList(final String text, final int maxLineLength, final boolean wrapLongWords) {
        return List.of(wrap(text, maxLineLength, wrapLongWords).split("\n", -1));
    }

    private static String wrapLine(final String line, final int maxLineLength, final boolean wrapLongWords) {
        if (line.length() <= maxLineLength) {
            return line;
        }
        final List<String> output = new ArrayList<>();
        String remaining = line;
        while (remaining.length() > maxLineLength) {
            int split = remaining.lastIndexOf(' ', maxLineLength);
            if (split <= 0) {
                if (!wrapLongWords) {
                    output.add(remaining);
                    return String.join("\n", output);
                }
                split = maxLineLength;
            }
            output.add(remaining.substring(0, split).stripTrailing());
            remaining = remaining.substring(split).stripLeading();
        }
        output.add(remaining);
        return String.join("\n", output);
    }

    private static int pixelWidth(final String miniMessageLine) {
        final String widthText = ANY_TAG.matcher(BOLD_END_OR_RESET_TAG
                        .matcher(BOLD_TAG.matcher(miniMessageLine).replaceAll(String.valueOf(BOLD_MARKER)))
                        .replaceAll(String.valueOf(RESET_MARKER)))
                .replaceAll("");
        int messagePxSize = 0;
        boolean isBold = false;
        for (final char character : widthText.toCharArray()) {
            if (character == BOLD_MARKER) {
                isBold = true;
                continue;
            }
            if (character == RESET_MARKER) {
                isBold = false;
                continue;
            }
            final DefaultFontInfo fontInfo = DefaultFontInfo.getDefaultFontInfo(character);
            messagePxSize += isBold ? fontInfo.getBoldLength() : fontInfo.getLength();
            messagePxSize++;
        }
        return messagePxSize;
    }
}
