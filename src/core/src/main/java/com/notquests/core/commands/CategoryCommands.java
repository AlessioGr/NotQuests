package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CategoryCommands {
    private CategoryCommands() {}

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
                            root,
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        commands.add(root.literal("categories", NQDescription.of("Manages quest categories."))
                .literal("create", NQDescription.of("Creates a new quest category."))
                .required(
                        "categoryName",
                        NQArgumentType.word("category name"),
                        NQDescription.of("Category identifier to create."),
                        (context, input) -> withPlaceholder(plugin.questManager().getCategoryNames(), "<Enter new category name>"))
                .commandDescription(NQDescription.of("Creates a new quest category."))
                .handler(context -> List.of(createCategory(plugin, context.argument("categoryName"))))
                .registration());
        commands.add(root.literal("categories", NQDescription.of("Manages quest categories."))
                .literal("list", NQDescription.of("Lists every quest category."))
                .commandDescription(NQDescription.of("Lists all quest categories."))
                .handler(ignored -> listCategories(plugin))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                categoryEdit = root.literal("categories", NQDescription.of("Manages quest categories."))
                        .literal("edit", NQDescription.of("Opens subcommands for editing a quest category."))
                        .required(
                                "category",
                                NQArgumentType.category(),
                                NQDescription.of("Category to edit."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                categoryProgressOrder = categoryEdit.literal(
                        "predefinedProgressOrder",
                        NQDescription.of("Configures the required progress order for quests inside this category."));
        commands.add(categoryProgressOrder.literal(
                        "show",
                        NQDescription.of("Shows the selected category's current predefined progress order."))
                .commandDescription(NQDescription.of("Shows the selected category's predefined progress order."))
                .handler(context -> List.of(categoryProgressOrder(plugin, context.argument("category"))))
                .registration());
        commands.add(categoryProgressOrder.literal(
                        "set",
                        NQDescription.of("Changes the selected category's predefined progress order."))
                .literal(
                        "none",
                        NQDescription.of("Removes the selected category's predefined progress order."))
                .commandDescription(NQDescription.of("Removes the selected category's predefined progress order."))
                .handler(context -> List.of(clearCategoryProgressOrder(plugin, context.argument("category"))))
                .registration());
        commands.add(categoryProgressOrder.literal(
                        "set",
                        NQDescription.of("Changes the selected category's predefined progress order."))
                .literal(
                        "firstToLast",
                        NQDescription.of("Requires quests in this category to be progressed from first to last."))
                .commandDescription(NQDescription.of("Sets the selected category's predefined progress order to first-to-last."))
                .handler(context -> List.of(setCategoryProgressOrder(
                        plugin,
                        context.argument("category"),
                        "firstToLast",
                        "")))
                .registration());
        commands.add(categoryProgressOrder.literal(
                        "set",
                        NQDescription.of("Changes the selected category's predefined progress order."))
                .literal(
                        "lastToFirst",
                        NQDescription.of("Requires quests in this category to be progressed from last to first."))
                .commandDescription(NQDescription.of("Sets the selected category's predefined progress order to last-to-first."))
                .handler(context -> List.of(setCategoryProgressOrder(
                        plugin,
                        context.argument("category"),
                        "lastToFirst",
                        "")))
                .registration());
        commands.add(categoryProgressOrder.literal(
                        "set",
                        NQDescription.of("Changes the selected category's predefined progress order."))
                .literal(
                        "custom",
                        NQDescription.of("Sets a custom quest order for this category."))
                .required(
                        "order",
                        NQArgumentType.greedyString("quest order"),
                        NQDescription.of("Custom quest order, separated by spaces."),
                        (context, input) -> plugin.questManager().getQuestNamesInCategory(context.argument("category")))
                .commandDescription(NQDescription.of("Sets the selected category's predefined progress order to a custom quest order."))
                .handler(context -> List.of(setCategoryProgressOrder(
                        plugin,
                        context.argument("category"),
                        "custom",
                        context.argument("order"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                categoryDisplayName = categoryEdit.literal(
                        "displayName",
                        NQDescription.of("Category display name shown in category GUIs and category lists. Supports MiniMessage formatting."));
        commands.add(categoryDisplayName.literal(
                        "show",
                        NQDescription.of("Shows the selected category's current display name."))
                .commandDescription(NQDescription.of("Shows the selected category's display name."))
                .handler(context -> List.of(categoryDisplayName(plugin, context.argument("category"))))
                .registration());
        commands.add(categoryDisplayName.literal(
                        "remove",
                        NQDescription.of("Removes the selected category's custom display name."))
                .commandDescription(NQDescription.of("Removes the selected category's display name."))
                .handler(context -> List.of(removeCategoryDisplayName(plugin, context.argument("category"))))
                .registration());
        commands.add(categoryDisplayName.literal(
                        "set",
                        NQDescription.of("Sets the selected category's display name."))
                .required(
                        "display-name",
                        NQArgumentType.greedyString("display name"),
                        NQDescription.of("New category display name. Supports spaces and MiniMessage formatting."))
                .commandDescription(NQDescription.of("Sets the selected category's display name."))
                .handler(context -> List.of(setCategoryDisplayName(
                        plugin,
                        context.argument("category"),
                        context.argument("display-name"))))
                .registration());
        commands.add(categoryEdit.literal("guiItem", NQDescription.of("Shows or changes the item displayed for this category in GUIs."))
                .required(
                        "material",
                        NQArgumentType.itemSelection(),
                        NQDescription.of("Material or NotQuests item displayed in the category GUI."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.GUI_ITEM_GLOW.name(), NQFlags.GUI_ITEM_GLOW.description()))
                .commandDescription(NQDescription.of("Sets the item displayed in the category GUI."))
                .handler(context -> List.of(setCategoryGuiItem(
                        plugin,
                        adapter,
                        context.argument("category"),
                        context.rawArgument("material"),
                        context.questPlayer(),
                        context.flagPresent(NQFlags.GUI_ITEM_GLOW.name()))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage createCategory(final NotQuestsPlugin plugin, final String categoryName) {
        final String categoryIdentifier = categoryName == null ? "" : categoryName.replaceAll("[^0-9a-zA-Z-._]", "_");
        if (categoryIdentifier.isBlank()) {
            return CommandMessage.error("<error>Error: Category name cannot be blank.");
        }
        if (plugin.questManager().getCategory(categoryIdentifier) != null) {
            return CommandMessage.error("<error>Error: The category "
                    + CommandSupport.highlight(categoryIdentifier) + " already exists!");
        }
        if (categoryIdentifier.endsWith(".") || categoryIdentifier.startsWith(".")) {
            return CommandMessage.error("<error>Error: The category " + CommandSupport.highlight(categoryIdentifier)
                    + " is invalid. It cannot contain a dot at the beginning or the end of the category. "
                    + "Dots are used to create a sub-category of an already existing category.");
        }
        if (categoryIdentifier.contains(".")) {
            final String parentCategory = categoryIdentifier.substring(0, categoryIdentifier.lastIndexOf("."));
            if (plugin.questManager().getCategory(parentCategory) == null) {
                return CommandMessage.error("<error>Error: The parent company "
                        + CommandSupport.highlight(parentCategory) + " does not exist.");
            }
        }
        plugin.questManager().createCategory(categoryIdentifier);
        plugin.saveData();
        return CommandMessage.success("<success>Category "
                + CommandSupport.highlight(categoryIdentifier) + " has successfully been created!");
    }

    static List<CommandMessage> listCategories(final NotQuestsPlugin plugin) {
        final List<String> names = plugin.questManager().getCategoryNames();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All categories:"));
        int counter = 1;
        for (final String name : names) {
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>" + name));
            counter++;
        }
        return List.copyOf(messages);
    }

    static CommandMessage setCategoryDisplayName(
            final NotQuestsPlugin plugin,
            final String categoryName,
            final String displayName) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return missingCategory(categoryName);
        }
        plugin.setCategoryDisplayName(categoryName, displayName);
        plugin.saveData();
        return CommandMessage.success("<success>Display name successfully added to category "
                + CommandSupport.highlight(category.getIdentifier())
                + "! New display name: "
                + CommandSupport.highlight2(plugin.questManager().getCategory(categoryName).getDisplayName()));
    }

    static CommandMessage categoryDisplayName(final NotQuestsPlugin plugin, final String categoryName) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return CommandMessage.error("Category " + CommandSupport.highlight(categoryName) + " does not exist.");
        }
        return CommandMessage.success("<main>Current display name of Category "
                + CommandSupport.highlight(category.getIdentifier())
                + ": "
                + CommandSupport.highlight2(CommandSupport.blankDefault(category.getDisplayName(), "none")));
    }

    static CommandMessage removeCategoryDisplayName(final NotQuestsPlugin plugin, final String categoryName) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return CommandMessage.error("Category " + CommandSupport.highlight(categoryName) + " does not exist.");
        }
        plugin.clearCategoryDisplayName(categoryName);
        plugin.saveData();
        return CommandMessage.success("<success>Display name successfully removed from Category "
                + CommandSupport.highlight(category.getIdentifier()) + "!");
    }

    static CommandMessage categoryProgressOrder(final NotQuestsPlugin plugin, final String categoryName) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return CommandMessage.error("Category " + CommandSupport.highlight(categoryName) + " does not exist.");
        }
        return CommandMessage.success("<success>Current predefined progress order of category "
                + CommandSupport.highlight(category.getIdentifier())
                + ": "
                + CommandSupport.highlight2(CommandSupport.blankDefault(category.getProgressOrder(), "None")));
    }

    static CommandMessage clearCategoryProgressOrder(final NotQuestsPlugin plugin, final String categoryName) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return CommandMessage.error("Category " + CommandSupport.highlight(categoryName) + " does not exist.");
        }
        plugin.clearCategoryProgressOrder(categoryName);
        plugin.saveData();
        return CommandMessage.success("<success>Predefined progress order of category "
                + CommandSupport.highlight(category.getIdentifier()) + " have been removed!");
    }

    static CommandMessage setCategoryProgressOrder(
            final NotQuestsPlugin plugin,
            final String categoryName,
            final String orderType,
            final String customOrder) {
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return CommandMessage.error("Category " + CommandSupport.highlight(categoryName) + " does not exist.");
        }
        final String order = "custom".equalsIgnoreCase(orderType)
                ? (customOrder == null ? "" : customOrder).trim()
                : orderType;
        plugin.setCategoryProgressOrder(categoryName, ("custom".equalsIgnoreCase(orderType) ? "custom " : "") + order.trim());
        plugin.saveData();
        final String message = switch (orderType.toLowerCase(Locale.ROOT)) {
            case "firsttolast" -> "<success>Predefined progress order of category "
                    + CommandSupport.highlight(category.getIdentifier()) + " have been set to first to last!";
            case "lasttofirst" -> "<success>Predefined progress order of category "
                    + CommandSupport.highlight(category.getIdentifier()) + " have been set to last to first!";
            case "custom" -> "<success>Predefined progress order of category "
                    + CommandSupport.highlight(category.getIdentifier()) + " have been set to custom with this order: " + order;
            default -> "<success>Predefined progress order of category "
                    + CommandSupport.highlight(category.getIdentifier()) + " have been set to " + order + "!";
        };
        return CommandMessage.success(message);
    }

    static CommandMessage setCategoryGuiItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String categoryName,
            final String rawItemSelection,
            final boolean glow) {
        return setCategoryGuiItem(plugin, adapter, categoryName, rawItemSelection, null, glow);
    }

    static CommandMessage setCategoryGuiItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String categoryName,
            final Object rawItemSelection,
            final PlatformPlayer questPlayer,
            final boolean glow) {
        ItemSelection itemSelection = adapter.parseItemSelection(rawItemSelection, questPlayer);
        if (itemSelection == null) {
            itemSelection = ItemStackSelection.parse("book");
        }
        final Category category = plugin.questManager().getCategory(categoryName);
        if (category == null) {
            return missingCategory(categoryName);
        }
        plugin.setCategoryGuiItem(categoryName, itemSelection, glow);
        plugin.saveData();
        return CommandMessage.success("<success>GUI Item for Category " + CommandSupport.highlight(categoryName)
                + " has been set to " + CommandSupport.highlight2(firstListedMaterial(itemSelection)) + "!");
    }

    private static CommandMessage missingCategory(final String categoryName) {
        return CommandMessage.error("<error>No Category found: " + categoryName);
    }

    private static String firstListedMaterial(final ItemSelection itemSelection) {
        if (itemSelection == null) {
            return "BOOK";
        }
        final String listed = itemSelection.listedMaterials("");
        if (listed == null || listed.isBlank()) {
            return "BOOK";
        }
        final String first = listed.split(",", 2)[0];
        return first.isBlank() ? "BOOK" : first.toUpperCase(Locale.ROOT);
    }

    private static List<String> withPlaceholder(final List<String> suggestions, final String placeholder) {
        final ArrayList<String> values = new ArrayList<>(suggestions == null ? List.of() : suggestions);
        if (placeholder != null && !placeholder.isBlank()) {
            values.add(placeholder);
        }
        return List.copyOf(values);
    }
}
