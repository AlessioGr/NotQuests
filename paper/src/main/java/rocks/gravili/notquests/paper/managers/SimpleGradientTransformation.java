package rocks.gravili.notquests.paper.managers;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.parser.ParsingException;
import net.kyori.adventure.text.minimessage.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.parser.node.TagPart;
import net.kyori.adventure.text.minimessage.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.transformation.Modifying;
import net.kyori.adventure.text.minimessage.transformation.Transformation;
import net.kyori.adventure.text.minimessage.transformation.inbuild.ComponentTransformation;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;

/**
 * A transformation that applies a colour gradient.
 *
 * @since 4.10.0
 */
public final class SimpleGradientTransformation extends Transformation implements Modifying {
    public static final String GRADIENT = "gradient";

    private int size = 0;
    private int disableApplyingColorDepth = -1;

    private int index = 0;
    private int colorIndex = 0;

    private float factorStep = 0;
    private final TextColor[] colors;
    private float phase;
    private final boolean negativePhase;

    /**
     * Create a new gradient transformation from a tag.
     *
     * @param name the tag name
     * @return a new transformation
     * @since 4.10.0
     */

    public static SimpleGradientTransformation main(final String name, final List<TagPart> args2) {
        final List<String> args = new ArrayList<>();
        args.add("#1985ff");
        args.add("#2bc7ff");

        return create(args);
    }
    public static SimpleGradientTransformation highlight(final String name, final List<TagPart> args2) {
        final List<String> args = new ArrayList<>();
        args.add("#00fffb");
        args.add("#00ffc3");

        return create(args);
    }
    public static SimpleGradientTransformation highlight2(final String name, final List<TagPart> args2) {
        final List<String> args = new ArrayList<>();
        args.add("#ff2465");
        args.add("#ff24a0");

        return create(args);
    }
    public static SimpleGradientTransformation error(final String name, final List<TagPart> args2) {
        final List<String> args = new ArrayList<>();
        args.add("#ff004c");
        args.add("#a80000");

        return create(args);
    }
    public static SimpleGradientTransformation success(final String name, final List<TagPart> args2) {
        final List<String> args = new ArrayList<>();
        args.add("#54b2ff");
        args.add("#ff5ecc");

        return create(args);
    }

    public static SimpleGradientTransformation create(final List<String> args) {
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

        return new SimpleGradientTransformation(phase, textColors);
    }

    private SimpleGradientTransformation(final float phase, final List<TextColor> colors) {
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

    @Override
    public void visit(final ElementNode curr) {
        if (curr instanceof ValueNode) {
            final String value = ((ValueNode) curr).value();
            this.size += value.codePointCount(0, value.length());
        } else if (curr instanceof TagNode) {
            final TagNode tag = (TagNode) curr;
            if (tag.transformation() instanceof ComponentTransformation) {
                // ComponentTransformation.apply() returns the value of the component placeholder
                ComponentFlattener.textOnly().flatten(tag.transformation().apply(), s -> this.size += s.codePointCount(0, s.length()));
            }
        }
    }

    @Override
    public Component apply() {
        // init
        int sectorLength = this.size / (this.colors.length - 1);
        if (sectorLength < 1) {
            sectorLength = 1;
        }
        this.factorStep = 1.0f / (sectorLength + this.index);
        this.phase = this.phase * sectorLength;
        this.index = 0;

        return Component.empty();
    }

    @Override
    public Component apply(final Component current, final int depth) {
        if ((this.disableApplyingColorDepth != -1 && depth > this.disableApplyingColorDepth) || current.style().color() != null) {
            if (this.disableApplyingColorDepth == -1) {
                this.disableApplyingColorDepth = depth;
            }
            // This component has its own color applied, which overrides ours
            // We still want to keep track of where we are though if this is text
            if (current instanceof TextComponent) {
                final String content = ((TextComponent) current).content();
                final int len = content.codePointCount(0, content.length());
                for (int i = 0; i < len; i++) {
                    // increment our color index
                    this.color();
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
                parent.append(comp);
            }

            return parent.build();
        }

        return Component.empty().mergeStyle(current);
    }

    private TextColor color() {
        // color switch needed?
        if (this.factorStep * this.index > 1) {
            this.colorIndex++;
            this.index = 0;
        }

        float factor = this.factorStep * (this.index++ + this.phase);
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
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        final SimpleGradientTransformation that = (SimpleGradientTransformation) other;
        return this.index == that.index
                && this.colorIndex == that.colorIndex
                && Float.compare(that.factorStep, this.factorStep) == 0
                && this.phase == that.phase && Arrays.equals(this.colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.index, this.colorIndex, this.factorStep, this.phase);
        result = 31 * result + Arrays.hashCode(this.colors);
        return result;
    }
}
