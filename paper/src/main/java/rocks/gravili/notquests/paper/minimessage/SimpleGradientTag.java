/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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
package rocks.gravili.notquests.paper.minimessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.util.ShadyPines;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;

/**
 * A transformation that applies a colour gradient. Source code has been copied from
 * net.kyori.adventure.text.minimessage.transformation.inbuild.GradientTransformation
 *
 * @since 4.10.0
 */
public final class SimpleGradientTag extends AbstractColorChangingTag {

  private static final List<String> fallbackColors = Arrays.asList("#1985ff", "#2bc7ff");
  private final TextColor[] colors;
  private final boolean negativePhase;
  private int index = 0;
  private int colorIndex = 0;
  private float factorStep = 0;
  private float phase;

  private SimpleGradientTag(final float phase, final List<TextColor> colors) {
    if (phase < 0) {
      this.negativePhase = true;
      this.phase = 1 + phase;
      Collections.reverse(colors);
    } else {
      this.negativePhase = false;
      this.phase = phase;
    }

    if (colors.isEmpty()) {
      this.colors = new TextColor[] {TextColor.color(0xffffff), TextColor.color(0x000000)};
    } else {
      this.colors = colors.toArray(new TextColor[0]);
    }
  }

  /**
   * Create a new gradient transformation from a tag.
   *
   * @return a new transformation
   * @since 4.10.0
   */
  public static Tag main(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsMain() != null
            ? NotQuests.getInstance().getConfiguration().getColorsMain()
            : fallbackColors);
  }

  public static Tag highlight(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsHighlight() != null
            ? NotQuests.getInstance().getConfiguration().getColorsHighlight()
            : fallbackColors);
  }

  public static Tag highlight2(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsHighlight2() != null
            ? NotQuests.getInstance().getConfiguration().getColorsHighlight2()
            : fallbackColors);
  }

  public static Tag error(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsError() != null
            ? NotQuests.getInstance().getConfiguration().getColorsError()
            : fallbackColors);
  }

  public static Tag success(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsSuccess() != null
            ? NotQuests.getInstance().getConfiguration().getColorsSuccess()
            : fallbackColors);
  }

  public static Tag unimportant(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsUnimportant() != null
            ? NotQuests.getInstance().getConfiguration().getColorsUnimportant()
            : fallbackColors);
  }

  public static Tag veryUnimportant(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsVeryUnimportant() != null
            ? NotQuests.getInstance().getConfiguration().getColorsVeryUnimportant()
            : fallbackColors);
  }

  public static Tag warn(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsWarn() != null
            ? NotQuests.getInstance().getConfiguration().getColorsWarn()
            : fallbackColors);
  }

  public static Tag positive(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsPositive() != null
            ? NotQuests.getInstance().getConfiguration().getColorsPositive()
            : fallbackColors);
  }

  public static Tag negative(final ArgumentQueue args, final Context ctx) {
    return create(
        NotQuests.getInstance() != null
                && NotQuests.getInstance().getConfiguration().getColorsNegative() != null
            ? NotQuests.getInstance().getConfiguration().getColorsNegative()
            : fallbackColors);
  }

  public static SimpleGradientTag create(final List<String> args) {
    float phase = 0;
    final List<TextColor> textColors;
    if (!args.isEmpty()) {
      textColors = new ArrayList<>();
      for (int i = 0; i < args.size(); i++) {
        final String arg = args.get(i);
        // last argument? maybe this is the phase?
        if (i == args.size() - 1) {
          try {
            phase = Float.parseFloat(arg);

            break;
          } catch (final NumberFormatException ignored) {
          }
        }

        final TextColor parsedColor;
        if (arg.charAt(0) == '#') {
          parsedColor = TextColor.fromHexString(arg);
        } else {
          parsedColor = NamedTextColor.NAMES.value(arg.toLowerCase(Locale.ROOT));
        }

        textColors.add(parsedColor);
      }

    } else {
      textColors = Collections.emptyList();
    }

    return new SimpleGradientTag(phase, textColors);
  }

  @Override
  protected void init() {
    int sectorLength = this.size() / (this.colors.length - 1);
    if (sectorLength < 1) {
      sectorLength = 1;
    }
    this.factorStep = 1.0f / (sectorLength + this.index);
    this.phase = this.phase * sectorLength;
    this.index = 0;
  }

  @Override
  protected void advanceColor() {
    // color switch needed?
    this.index++;
    if (this.factorStep * this.index > 1) {
      this.colorIndex++;
      this.index = 0;
    }
  }

  @Override
  protected TextColor color() {
    float factor = this.factorStep * (this.index + this.phase);
    // loop around if needed
    if (factor > 1) {
      factor = 1 - (factor - 1);
    }

    if (this.negativePhase && this.colors.length % 2 != 0) {
      // flip the gradient segment for to allow for looping phase -1 through 1
      return TextColor.lerp(factor, this.colors[this.colorIndex + 1], this.colors[this.colorIndex]);
    } else {
      return TextColor.lerp(factor, this.colors[this.colorIndex], this.colors[this.colorIndex + 1]);
    }
  }

  @Override
  public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
    return Stream.of(
        ExaminableProperty.of("phase", this.phase), ExaminableProperty.of("colors", this.colors));
  }

  @Override
  public boolean equals(final @Nullable Object other) {
    if (this == other) return true;
    if (other == null || this.getClass() != other.getClass()) return false;
    final SimpleGradientTag that = (SimpleGradientTag) other;
    return this.index == that.index
        && this.colorIndex == that.colorIndex
        && ShadyPines.equals(that.factorStep, this.factorStep)
        && this.phase == that.phase
        && Arrays.equals(this.colors, that.colors);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(this.index, this.colorIndex, this.factorStep, this.phase);
    result = 31 * result + Arrays.hashCode(this.colors);
    return result;
  }
}
