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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.BukkitCaptionKeys;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.items.NQItem;

/**
 * cloud argument type that parses Bukkit {@link Material materials}
 *
 * @param <C> Command sender type
 */
public class ItemStackSelectionArgument<C> extends CommandArgument<C, ItemStackSelection> {

    //TODO: Add way of setting optional amount per itemstack (ex. stone:5,dirt,diorite:71), then the amount in giveitemaction can be removed or made an optional flag

    protected ItemStackSelectionArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(required, name, new ItemStackSelectionArgument.MaterialParser<>(main), defaultValue, ItemStackSelection.class, suggestionsProvider);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> ItemStackSelectionArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new ItemStackSelectionArgument.Builder<>(name, main);
    }

    /**
     * Create a new required argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, ItemStackSelection> of(final @NonNull String name, final NotQuests main) {
        return ItemStackSelectionArgument.<C>newBuilder(name, main).asRequired().build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, ItemStackSelection> optional(final @NonNull String name, final NotQuests main) {
        return ItemStackSelectionArgument.<C>newBuilder(name, main).asOptional().build();
    }

    /**
     * Create a new optional argument with a default value
     *
     * @param name     Argument name
     * @param <C>      Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, ItemStackSelection> optional(
            final @NonNull String name,
            final @NonNull ItemStackSelection itemStackSelection,
            final NotQuests main
    ) {
        return ItemStackSelectionArgument.<C>newBuilder(name, main).asOptionalWithDefault(itemStackSelection.toFirstItemStack().getType().name().toLowerCase()).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, ItemStackSelection> {
        private final NotQuests main;

        private Builder(final @NonNull String name, NotQuests main) {
            super(ItemStackSelection.class, name);
            this.main = main;
        }

        @Override
        public @NonNull CommandArgument<C, ItemStackSelection> build() {
            return new ItemStackSelectionArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main
            );
        }

    }

    public static final class MaterialParser<C> implements ArgumentParser<C, ItemStackSelection> {

        private final NotQuests main;

        /**
         * Constructs a new MaterialParser.
         */
        public MaterialParser(
                NotQuests main
        ) {
            this.main = main;
        }

        @Override
        public @NonNull ArgumentParseResult<ItemStackSelection> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                        ItemStackSelectionArgument.MaterialParser.class,
                        commandContext
                ));
            }

            final CommandSender sender = ((CommandSender)commandContext.getSender());
            try {
                final ItemStackSelection itemStackSelection = new ItemStackSelection(main);

                for(final String inputPart : input.split(",")){
                    if (inputPart.equalsIgnoreCase("hand")) {
                        if (commandContext.getSender() instanceof final Player player) {
                            itemStackSelection.addItemStack(player.getInventory().getItemInMainHand());
                        } else {
                            return ArgumentParseResult.failure(new ItemStackSelectionArgument.MaterialParseException(inputPart, commandContext));
                        }
                    } else if (inputPart.equalsIgnoreCase("any")) {
                        itemStackSelection.setAny(true);
                    } else {
                        try{
                            itemStackSelection.addMaterial(Material.valueOf(inputPart.toUpperCase(Locale.ROOT)));
                        }catch (Exception ignored){
                            final NQItem nqItem = main.getItemsManager().getItem(inputPart);
                            if(nqItem != null){
                                itemStackSelection.addNqItem(nqItem);
                            }else{
                                return ArgumentParseResult.failure(new ItemStackSelectionArgument.MaterialParseException(inputPart, commandContext));
                            }
                        }
                    }
                }

                inputQueue.remove();
                return ArgumentParseResult.success(itemStackSelection);

            } catch (final IllegalArgumentException exception) {
                return ArgumentParseResult.failure(new ItemStackSelectionArgument.MaterialParseException(input, commandContext));
            }
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(
                final @NonNull CommandContext<C> context,
                final @NonNull String input
        ) {
            final CommandSender sender = ((CommandSender)context.getSender());

            final List<String> possibleMaterials = new ArrayList<>();
            for (final Material value : Material.values()) {
                possibleMaterials.add(value.name().toLowerCase());
                possibleMaterials.add(value.name().toLowerCase() + ",");
            }

            for(final NQItem nqItem : main.getItemsManager().getItems()){
                possibleMaterials.add(nqItem.getItemName());
                possibleMaterials.add(nqItem.getItemName() + ",");
            }

            possibleMaterials.add("hand");
            possibleMaterials.add("hand,");
            possibleMaterials.add("any");
            possibleMaterials.add("any,");


            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Item Name / 'hand' / 'any']. Can be separated by comma", "[...]");


            if(!input.contains(",")){
                return possibleMaterials;
            } else {
                final List<String> completions = new ArrayList<>();
                final String partAfterLastCommaInInput = input.substring( (input.lastIndexOf(",") > input.length()-1) ? (input.lastIndexOf(",")) : (input.lastIndexOf(",") + 1));
                for(final String possibleMaterial : possibleMaterials){
                    final String string = input.substring(0, input.length()-1-partAfterLastCommaInInput.length()) + "," + possibleMaterial;
                    completions.add(string);
                }
                return completions;
            }
        }

    }


    public static final class MaterialParseException extends ParserException {

        private static final long serialVersionUID = 1615554107385965610L;
        private final String input;

        /**
         * Construct a new MaterialParseException
         *
         * @param input   Input
         * @param context Command context
         */
        public MaterialParseException(
                final @NonNull String input,
                final @NonNull CommandContext<?> context
        ) {
            super(
                    ItemStackSelectionArgument.MaterialParser.class,
                    context,
                    BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_MATERIAL,
                    CaptionVariable.of("input", input)
            );
            this.input = input;
        }

        /**
         * Get the input
         *
         * @return Input
         */
        public @NonNull String getInput() {
            return this.input;
        }

    }

}
