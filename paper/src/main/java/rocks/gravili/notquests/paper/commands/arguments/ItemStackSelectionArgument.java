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

package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.managers.items.NQItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native-framework port of {@code ItemStackSelectionParser}: resolves a comma-separated selection of
 * materials, NotQuests items, {@code hand} (the sender's main-hand item) and {@code any} into an
 * {@link ItemStackSelection}.
 *
 * <p>The {@code hand} keyword resolves the command sender's main-hand item, so this argument reads
 * the source via {@link #convert(String, Object)}.
 */
public final class ItemStackSelectionArgument extends NQArgumentType<ItemStackSelection> {
    private final NotQuests main;

    public ItemStackSelectionArgument(final NotQuests main) {
        this.main = main;
    }

    public static ItemStackSelectionArgument itemStackSelectionArgument(final NotQuests main) {
        return new ItemStackSelectionArgument(main);
    }

    // An item selection is a comma-separated run of materials/NotQuests-items (e.g.
    // "grass_block,acacia_boat", plus the "hand"/"any" keywords). A comma is NOT a legal character
    // in an unquoted Brigadier string, so the default string() tokenizer stops at the first comma and
    // leaves the rest as "trailing data". We therefore read the token ourselves: the whole run up to
    // the next space (commas included), or a quoted phrase for NotQuests item names that contain
    // spaces. This does NOT swallow following arguments (it stops at the space before e.g. <amount>).
    // getNativeType() stays string() only as the client-facing argument shape.
    @Override
    public <S> ItemStackSelection parse(final StringReader reader, final S source) throws CommandSyntaxException {
        return convert(readSelectionToken(reader), source);
    }

    @Override
    public ItemStackSelection parse(final StringReader reader) throws CommandSyntaxException {
        return convert(readSelectionToken(reader), null);
    }

    private static String readSelectionToken(final StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
            return reader.readQuotedString();
        }
        final int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public ItemStackSelection convert(final String input) throws CommandSyntaxException {
        // The "hand" keyword needs the command sender; resolution happens in convert(String, source).
        return convert(input, null);
    }

    @Override
    public <S> ItemStackSelection convert(final String input, final S source) throws CommandSyntaxException {
        final CommandSender sender =
                source instanceof CommandSourceStack stack ? stack.getSender() : null;

        if (input == null || input.isEmpty()) {
            throw fail("No input provided");
        }

        try {
            final ItemStackSelection itemStackSelection = new ItemStackSelection(main);

            for (final String inputPart : input.split(",")) {
                if (inputPart.equalsIgnoreCase("hand")) {
                    if (sender instanceof final Player player) {
                        itemStackSelection.addItemStack(player.getInventory().getItemInMainHand());
                    } else {
                        throw fail("Cannot parse item argument '" + inputPart + "'");
                    }
                } else if (inputPart.equalsIgnoreCase("any")) {
                    itemStackSelection.setAny(true);
                } else {
                    try {
                        itemStackSelection.addMaterial(Material.valueOf(inputPart.toUpperCase(Locale.ROOT)));
                    } catch (Exception ignored) {
                        final NQItem nqItem = main.getItemsManager().getItem(inputPart);
                        if (nqItem != null) {
                            itemStackSelection.addNqItem(nqItem);
                        } else {
                            throw fail("Cannot parse item argument '" + inputPart + "'");
                        }
                    }
                }
            }
            return itemStackSelection;

        } catch (final IllegalArgumentException exception) {
            throw fail("Cannot parse item argument 'invalid input'");
        }
    }

    // Material is a static enum, but suggestions run on every keystroke of every player — build the
    // ~2800 material entries (plain + trailing-comma variants for chaining) once and reuse them.
    // NQItems are appended per call because admins can create them at runtime.
    private static volatile List<String> cachedMaterialSuggestions;

    private static List<String> materialSuggestions() {
        List<String> cached = cachedMaterialSuggestions;
        if (cached == null) {
            final List<String> list = new ArrayList<>(Material.values().length * 2 + 4);
            for (final Material value : Material.values()) {
                final String name = value.name().toLowerCase(Locale.ROOT);
                list.add(name);
                list.add(name + ",");
            }
            list.add("hand");
            list.add("hand,");
            list.add("any");
            list.add("any,");
            cached = List.copyOf(list);
            cachedMaterialSuggestions = cached; // benign race: computation is idempotent
        }
        return cached;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> possibleMaterials = new ArrayList<>(materialSuggestions());
        for (final NQItem nqItem : main.getItemsManager().getItems()) {
            possibleMaterials.add(nqItem.getItemName());
            possibleMaterials.add(nqItem.getItemName() + ",");
        }

        if (!remaining.contains(",")) {
            return possibleMaterials;
        }
        // Mid-list (after a comma): suggest the already-typed selection up to the last comma with
        // each candidate appended, so accepting a suggestion extends the list instead of replacing it.
        final String prefix = remaining.substring(0, remaining.lastIndexOf(','));
        final List<String> completions = new ArrayList<>(possibleMaterials.size());
        for (final String possibleMaterial : possibleMaterials) {
            completions.add(prefix + "," + possibleMaterial);
        }
        return completions;
    }
}
