/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2017-2022 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.notquests.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.internal.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tree.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.PrimitiveIterator;

/** Code copied from net.kyori.adventure.text.minimessage.tag.standard. */
abstract class AbstractColorChangingTag implements Modifying {
    private static final ComponentFlattener LENGTH_CALCULATOR = ComponentFlattener.builder()
            .mapper(TextComponent.class, TextComponent::content)
            .unknownMapper(x -> "_")
            .build();

    private boolean visited;
    private int size = 0;
    private int disableApplyingColorDepth = -1;

    protected final int size() {
        return size;
    }

    @Override
    public final void visit(final @NotNull Node current, final int depth) {
        if (visited) {
            throw new IllegalStateException("Color changing tag instances cannot be re-used");
        }

        if (current instanceof final ValueNode valueNode) {
            final String value = valueNode.value();
            size += value.codePointCount(0, value.length());
        } else if (current instanceof final TagNode tagNode && tagNode.tag() instanceof final Inserting inserting) {
            LENGTH_CALCULATOR.flatten(
                    inserting.value(), value -> size += value.codePointCount(0, value.length()));
        }
    }

    @Override
    public final void postVisit() {
        visited = true;
        init();
    }

    @Override
    public final Component apply(final @NotNull Component current, final int depth) {
        if ((disableApplyingColorDepth != -1 && depth > disableApplyingColorDepth)
                || current.style().color() != null) {
            if (disableApplyingColorDepth == -1 || depth < disableApplyingColorDepth) {
                disableApplyingColorDepth = depth;
            }
            if (current instanceof final TextComponent textComponent) {
                final String content = textComponent.content();
                final int length = content.codePointCount(0, content.length());
                for (int i = 0; i < length; i++) {
                    advanceColor();
                }
            }
            return current.children(Collections.emptyList());
        }

        disableApplyingColorDepth = -1;
        if (current instanceof final TextComponent textComponent && !textComponent.content().isEmpty()) {
            final TextComponent.Builder parent = Component.text();
            final int[] holder = new int[1];
            for (final PrimitiveIterator.OfInt it = textComponent.content().codePoints().iterator(); it.hasNext(); ) {
                holder[0] = it.nextInt();
                parent.append(Component.text(new String(holder, 0, 1), current.style().color(color())));
                advanceColor();
            }
            return parent.build();
        }
        if (!(current instanceof TextComponent)) {
            final Component result = current.children(Collections.emptyList()).colorIfAbsent(color());
            advanceColor();
            return result;
        }

        return Component.empty().mergeStyle(current);
    }

    protected abstract void init();

    protected abstract void advanceColor();

    protected abstract TextColor color();

    @Override
    public abstract boolean equals(final @Nullable Object other);

    @Override
    public abstract int hashCode();
}
