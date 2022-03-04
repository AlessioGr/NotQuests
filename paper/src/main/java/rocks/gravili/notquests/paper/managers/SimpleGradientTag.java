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
package rocks.gravili.notquests.paper.managers;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.internal.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tree.Node;
import net.kyori.adventure.util.ShadyPines;
import net.kyori.examination.Examinable;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.*;
import java.util.stream.Stream;

/**
 * A transformation that applies a colour gradient. Source code has been copied from net.kyori.adventure.text.minimessage.transformation.inbuild.GradientTransformation
 *
 * @since 4.10.0
 */
public final class SimpleGradientTag implements Modifying, Examinable {
    public static final String GRADIENT = "gradient";


    private boolean visited;
    private int size = 0;
    private int disableApplyingColorDepth = -1;

    private int index = 0;
    private int colorIndex = 0;

    private float factorStep = 0;
    private final TextColor[] colors;
    private float phase;
    private final boolean negativePhase;

    private final static List<String> fallbackColors = Arrays.asList("#1985ff", "#2bc7ff");

    /**
     * Create a new gradient transformation from a tag.
     *
     * @return a new transformation
     * @since 4.10.0
     */

    public static Tag main(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsMain() != null ? NotQuests.getInstance().getConfiguration().getColorsMain() : fallbackColors);
    }
    public static Tag highlight(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsHighlight() != null ? NotQuests.getInstance().getConfiguration().getColorsHighlight() : fallbackColors);
    }
    public static Tag highlight2(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsHighlight2() != null ? NotQuests.getInstance().getConfiguration().getColorsHighlight2() : fallbackColors);
    }
    public static Tag error(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsError() != null ? NotQuests.getInstance().getConfiguration().getColorsError() : fallbackColors);
    }
    public static Tag success(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsSuccess() != null ? NotQuests.getInstance().getConfiguration().getColorsSuccess() : fallbackColors);
    }

    public static Tag unimportant(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsUnimportant() != null ? NotQuests.getInstance().getConfiguration().getColorsUnimportant() : fallbackColors);
    }

    public static Tag veryUnimportant(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsVeryUnimportant() != null ? NotQuests.getInstance().getConfiguration().getColorsVeryUnimportant() : fallbackColors);
    }

    public static Tag warn(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null &&NotQuests.getInstance().getConfiguration().getColorsWarn() != null ? NotQuests.getInstance().getConfiguration().getColorsWarn() : fallbackColors);
    }

    public static Tag positive(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsPositive() != null ? NotQuests.getInstance().getConfiguration().getColorsPositive() : fallbackColors);
    }

    public static Tag negative(final ArgumentQueue args, final Context ctx) {
        return create(NotQuests.getInstance() != null && NotQuests.getInstance().getConfiguration().getColorsNegative() != null ? NotQuests.getInstance().getConfiguration().getColorsNegative() : fallbackColors);

    }

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
            this.colors = new TextColor[]{TextColor.color(0xffffff), TextColor.color(0x000000)};
        } else {
            this.colors = colors.toArray(new TextColor[0]);
        }
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

    private void init() {
        int sectorLength = this.size() / (this.colors.length - 1);
        if (sectorLength < 1) {
            sectorLength = 1;
        }
        this.factorStep = 1.0f / (sectorLength + this.index);
        this.phase = this.phase * sectorLength;
        this.index = 0;
    }

    private void advanceColor() {
        // color switch needed?
        this.index++;
        if (this.factorStep * this.index > 1) {
            this.colorIndex++;
            this.index = 0;
        }
    }

    protected final int size() {
        return this.size;
    }

    @Override
    public final void visit(final @NotNull Node current, final int depth) {
        if (this.visited) {
            throw new IllegalStateException("Color changing tag instances cannot be re-used, return a new one for each resolve");
        }

        if (current instanceof ValueNode) {
            final String value = ((ValueNode) current).value();
            this.size += value.codePointCount(0, value.length());
        } else if (current instanceof TagNode) {
            final TagNode tag = (TagNode) current;
            if (tag.tag() instanceof Inserting) {
                // ComponentTransformation.apply() returns the value of the component placeholder
                ComponentFlattener.textOnly().flatten(((Inserting) tag.tag()).value(), s -> this.size += s.codePointCount(0, s.length()));
            }
        }
    }

    @Override
    public final void postVisit() {
        // init
        this.visited = true;
        this.init();
    }

    @Override
    public final Component apply(final @NotNull Component current, final int depth) {
        if ((this.disableApplyingColorDepth != -1 && depth > this.disableApplyingColorDepth) || current.style().color() != null) {
            if (this.disableApplyingColorDepth == -1 || depth < this.disableApplyingColorDepth) {
                this.disableApplyingColorDepth = depth;
            }
            // This component has its own color applied, which overrides ours
            // We still want to keep track of where we are though if this is text
            if (current instanceof TextComponent) {
                final String content = ((TextComponent) current).content();
                final int len = content.codePointCount(0, content.length());
                for (int i = 0; i < len; i++) {
                    // increment our color index
                    this.advanceColor();
                }
            }
            return current.children(Collections.emptyList());
        }

        this.disableApplyingColorDepth = -1;
        if (current instanceof TextComponent && ((TextComponent) current).content().length() > 0) {
            final TextComponent textComponent = (TextComponent) current;
            final String content = textComponent.content();

            final TextComponent.Builder parent = Component.text();

            // apply
            final int[] holder = new int[1];
            for (final PrimitiveIterator.OfInt it = content.codePoints().iterator(); it.hasNext();) {
                holder[0] = it.nextInt();
                final Component comp = Component.text(new String(holder, 0, 1), this.color());
                this.advanceColor();
                parent.append(comp);
            }

            return parent.build();
        }

        return Component.empty().mergeStyle(current);
    }
    // The lifecycle


    private TextColor color() {
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
                ExaminableProperty.of("phase", this.phase),
                ExaminableProperty.of("colors", this.colors)
        );
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        final SimpleGradientTag that = (SimpleGradientTag) other;
        return this.index == that.index
                && this.colorIndex == that.colorIndex
                && ShadyPines.equals(that.factorStep, this.factorStep)
                && this.phase == that.phase && Arrays.equals(this.colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.index, this.colorIndex, this.factorStep, this.phase);
        result = 31 * result + Arrays.hashCode(this.colors);
        return result;
    }
}
