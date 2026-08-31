package com.notquests.core.text;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.util.ShadyPines;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** MiniMessage gradient tag used by NotQuests' configurable color aliases. */
public final class SimpleGradientTag extends AbstractColorChangingTag {
    public static final List<String> FALLBACK_COLORS = List.of("#1985ff", "#2bc7ff");

    private final TextColor[] colors;
    private final boolean negativePhase;
    private int index = 0;
    private int colorIndex = 0;
    private float factorStep = 0;
    private float phase;

    private SimpleGradientTag(final float phase, final List<TextColor> colors) {
        if (phase < 0) {
            negativePhase = true;
            this.phase = 1 + phase;
            Collections.reverse(colors);
        } else {
            negativePhase = false;
            this.phase = phase;
        }

        this.colors = colors.isEmpty()
                ? new TextColor[] {TextColor.color(0xffffff), TextColor.color(0x000000)}
                : colors.toArray(new TextColor[0]);
    }

    public static Tag create(final List<String> colors) {
        float phase = 0;
        final List<TextColor> textColors = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            final String color = colors.get(i);
            if (i == colors.size() - 1) {
                try {
                    phase = Float.parseFloat(color);
                    break;
                } catch (final NumberFormatException ignored) {
                    // Last token is a normal color, not a phase value.
                }
            }

            final TextColor parsed = color.startsWith("#")
                    ? TextColor.fromHexString(color)
                    : NamedTextColor.NAMES.value(color.toLowerCase(Locale.ROOT));
            if (parsed != null) {
                textColors.add(parsed);
            }
        }

        return new SimpleGradientTag(phase, textColors);
    }

    @Override
    protected void init() {
        int sectorLength = size() / (colors.length - 1);
        if (sectorLength < 1) {
            sectorLength = 1;
        }
        factorStep = 1.0f / (sectorLength + index);
        phase = phase * sectorLength;
        index = 0;
    }

    @Override
    protected void advanceColor() {
        index++;
        if (factorStep * index > 1) {
            colorIndex++;
            index = 0;
        }
    }

    @Override
    protected TextColor color() {
        float factor = factorStep * (index + phase);
        if (factor > 1) {
            factor = 1 - (factor - 1);
        }

        return negativePhase && colors.length % 2 != 0
                ? TextColor.lerp(factor, colors[colorIndex + 1], colors[colorIndex])
                : TextColor.lerp(factor, colors[colorIndex], colors[colorIndex + 1]);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final SimpleGradientTag that = (SimpleGradientTag) other;
        return index == that.index
                && colorIndex == that.colorIndex
                && ShadyPines.equals(that.factorStep, factorStep)
                && phase == that.phase
                && Arrays.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index, colorIndex, factorStep, phase);
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }
}
