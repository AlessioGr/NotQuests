package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.SavedItem;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.util.ArrayList;
import java.util.List;

final class ItemCommands {
    private ItemCommands() {}

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            items,
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        commands.add(items.literal("create", NQDescription.of("Creates a new saved NotQuests item."))
                .required(
                        "name",
                        NQArgumentType.word("item name"),
                        NQDescription.of("Unique saved NotQuests item name."))
                .required(
                        "material",
                        NQArgumentType.itemSelection(),
                        NQDescription.of("Material or held item used as the saved item's base."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.CATEGORY.name(), NQFlags.CATEGORY.description())
                        .withArgument(NQArgumentType.category())
                        .build())
                .commandDescription(NQDescription.of("Creates a new saved NotQuests item."))
                .handler(context -> List.of(createSavedItem(
                        plugin,
                        adapter,
                        context.argument("name"),
                        context.rawArgument("material"),
                        context.questPlayer(),
                        context.flag(NQFlags.CATEGORY.name()))))
                .registration());
        commands.add(items.literal("list", NQDescription.of("Lists every saved NotQuests item."))
                .commandDescription(NQDescription.of("Lists all saved NotQuests items."))
                .handler(ignored -> savedItemMessages(plugin))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                itemEdit = items.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a saved NotQuests item."))
                        .required(
                                "item",
                                NQArgumentType.itemName(),
                                NQDescription.of("Saved NotQuests item to edit."));
        commands.add(itemEdit.literal("give", NQDescription.of("Gives the selected NotQuests item to a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player who should receive the item."))
                .required(
                        "amount",
                        NQArgumentType.integer("item amount"),
                        NQDescription.of("Amount of items the player should receive."))
                .commandDescription(NQDescription.of("Gives the selected saved NotQuests item to a player."))
                .handler(context -> List.of(giveSavedItem(
                        plugin,
                        adapter,
                        context.argument("item"),
                        context.argument("player"),
                        integer(context.argument("amount")))))
                .registration());
        commands.add(itemEdit.literal(
                        "remove",
                        NQDescription.of("Deletes the selected saved NotQuests item."))
                .commandDescription(NQDescription.of("Deletes the selected saved NotQuests item."))
                .handler(context -> List.of(deleteSavedItem(plugin, context.argument("item"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                itemDisplayName = itemEdit.literal(
                        "displayName",
                        NQDescription.of("Custom item display name shown on the saved NotQuests item. Supports MiniMessage formatting."));
        commands.add(itemDisplayName.literal(
                        "set",
                        NQDescription.of("Sets the selected item's display name."))
                .required(
                        "display-name",
                        NQArgumentType.greedyString("display name"),
                        NQDescription.of("New item display name. Supports spaces and MiniMessage formatting."))
                .commandDescription(NQDescription.of("Sets an item's display name."))
                .handler(context -> List.of(setSavedItemDisplayName(
                        plugin,
                        context.argument("item"),
                        context.argument("display-name"))))
                .registration());
        commands.add(itemDisplayName.literal(
                        "remove",
                        NQDescription.of("Removes the selected item's custom display name."))
                .commandDescription(NQDescription.of("Removes an item's display name."))
                .handler(context -> List.of(removeSavedItemDisplayName(plugin, context.argument("item"))))
                .registration());
        commands.add(itemDisplayName.literal(
                        "show",
                        NQDescription.of("Shows the selected item's current display name."))
                .commandDescription(NQDescription.of("Shows an item's current display name."))
                .handler(context -> List.of(showSavedItemDisplayName(plugin, context.argument("item"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage createSavedItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String itemName,
            final String rawItemSelection,
            final String categoryName) {
        return createSavedItem(plugin, adapter, itemName, rawItemSelection, null, categoryName);
    }

    static CommandMessage createSavedItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String itemName,
            final Object rawItemSelection,
            final PlatformPlayer questPlayer,
            final String categoryName) {
        if (plugin.savedItem(itemName) != null) {
            return CommandMessage.error("<error>Error: The item "
                    + CommandSupport.highlight(itemName) + " already exists!");
        }
        if (adapter.itemSelectionOptions().stream().anyMatch(option -> option.equalsIgnoreCase(itemName))) {
            return CommandMessage.error("<error>Error: The item " + CommandSupport.highlight(itemName)
                    + " already exists! You cannot use item names identical to vanilla Minecraft item names.");
        }
        final ItemSelection selection = adapter.parseItemSelection(rawItemSelection, questPlayer);
        if (selection == null) {
            return CommandMessage.error("Invalid item material: " + CommandSupport.highlight(rawItemSelection) + ".");
        }
        if ("any".equalsIgnoreCase(selection.listedMaterials(""))) {
            return CommandMessage.error("<error>You cannot use <highlight>'any'</highlight> here!");
        }
        if (!plugin.putSavedItem(itemName, selection, categoryName, "")) {
            return CommandMessage.error("Could not create NotQuests item "
                    + CommandSupport.highlight(itemName) + ".");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>The item "
                + CommandSupport.highlight(itemName) + " has been added successfully!");
    }

    static CommandMessage deleteSavedItem(final NotQuestsPlugin plugin, final String itemName) {
        if (!plugin.deleteSavedItem(itemName)) {
            return CommandMessage.error("Item " + CommandSupport.highlight(itemName) + " does not exist.");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>The item "
                + CommandSupport.highlight(itemName) + " has been deleted successfully!");
    }

    static CommandMessage giveSavedItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String itemName,
            final String playerName,
            final int amount) {
        final PlatformPlayer target = CommandSupport.targetPlatformPlayer(adapter, playerName);
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("Player " + CommandSupport.highlight(playerName) + " is not online.");
        }
        final SavedItem item = plugin.savedItem(itemName);
        if (item == null) {
            return CommandMessage.error("Item " + CommandSupport.highlight(itemName) + " does not exist.");
        }
        return target.giveItems(plugin.resolveItems(item.getItemSelection().withAmount(amount)))
                ? CommandMessage.success("<success>The item " + CommandSupport.highlight(itemName)
                        + " has been given to player " + CommandSupport.highlight2(target.playerName()) + "!")
                : CommandMessage.error("Could not give NotQuests item " + CommandSupport.highlight(itemName)
                        + " to " + CommandSupport.highlight2(target.playerName()) + ".");
    }

    static CommandMessage setSavedItemDisplayName(
            final NotQuestsPlugin plugin,
            final String itemName,
            final String displayName) {
        if (!plugin.setSavedItemDisplayName(itemName, displayName)) {
            return CommandMessage.error("Item " + CommandSupport.highlight(itemName) + " does not exist.");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>The display name of item "
                + CommandSupport.highlight(itemName) + " has been set to: <white><reset>" + displayName);
    }

    static CommandMessage removeSavedItemDisplayName(final NotQuestsPlugin plugin, final String itemName) {
        if (!plugin.setSavedItemDisplayName(itemName, null)) {
            return CommandMessage.error("Item " + CommandSupport.highlight(itemName) + " does not exist.");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>The display name of item "
                + CommandSupport.highlight(itemName) + " has been removed!");
    }

    static CommandMessage showSavedItemDisplayName(final NotQuestsPlugin plugin, final String itemName) {
        final SavedItem item = plugin.savedItem(itemName);
        if (item == null) {
            return CommandMessage.error("Item " + CommandSupport.highlight(itemName) + " does not exist.");
        }
        final String displayName = item.getDisplayName();
        return CommandMessage.success("<success>The display name of item "
                + CommandSupport.highlight(itemName)
                + " is: \n<white><reset>"
                + CommandSupport.blankDefault(displayName, itemName));
    }

    private static List<CommandMessage> savedItemMessages(final NotQuestsPlugin plugin) {
        final List<SavedItem> items = plugin.savedItems();
        if (items.isEmpty()) {
            return List.of(CommandMessage.success("<main>No NotQuests items saved."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All Items:"));
        int index = 1;
        for (final SavedItem item : items) {
            messages.add(CommandMessage.success(
                    "<highlight>" + index++ + ".</highlight> <main>" + item.getName()
                            + "</main> <highlight2>Type: <main>" + item.getItemSelection().listedMaterials("")
                            + " <highlight2>Display Name:</highlight2> <white><reset>" + item.getDisplayName()));
        }
        return List.copyOf(messages);
    }

    private static int integer(final String input) {
        try {
            return Integer.parseInt(input);
        } catch (final NumberFormatException exception) {
            return 0;
        }
    }
}
