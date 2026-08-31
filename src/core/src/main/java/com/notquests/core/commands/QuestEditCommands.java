package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.Quest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QuestEditCommands {
    private QuestEditCommands() {}

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
                            editQuest,
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                acceptCooldownComplete = editQuest
                        .literal("acceptCooldown", NQDescription.of("Configures the cooldown before this quest can be accepted again."))
                        .literal("complete", NQDescription.of("Applies this cooldown after quest completion."));
        commands.add(acceptCooldownComplete.literal("set", NQDescription.of("Sets the cooldown players must wait after completing this quest before accepting it again."))
                .required(
                        "duration",
                        NQArgumentType.duration(),
                        NQDescription.of("New cooldown duration, such as 30s, 500ms, 10m, or 2h."))
                .commandDescription(NQDescription.of("Sets the wait time before players can accept this quest again after completing it."))
                .handler(context -> List.of(setQuestAcceptCooldownComplete(
                        plugin,
                        context.argument("quest"),
                        duration(context.rawArgument("duration")))))
                .registration());
        commands.add(acceptCooldownComplete.literal("disable", NQDescription.of("Disables the complete-accept cooldown."))
                .commandDescription(NQDescription.of("Disables the wait time before players can accept this quest again."))
                .handler(context -> List.of(setQuestAcceptCooldownComplete(plugin, context.argument("quest"), null)))
                .registration());

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                questDescription = editQuest.literal(
                        "description",
                        NQDescription.of("Quest description shown in quest previews, GUIs, and chat. Supports MiniMessage formatting."));
        commands.add(questDescription.literal("show", NQDescription.of("Shows the selected quest's current description text."))
                .commandDescription(NQDescription.of("Shows the selected quest's description."))
                .handler(context -> List.of(questDescription(plugin, context.argument("quest"))))
                .registration());
        commands.add(questDescription.literal("remove", NQDescription.of("Removes the selected quest's custom description text."))
                .commandDescription(NQDescription.of("Removes the selected quest's description."))
                .handler(context -> List.of(removeQuestDescription(plugin, context.argument("quest"))))
                .registration());
        commands.add(questDescription.literal("set", NQDescription.of("Sets the selected quest's description text shown to players."))
                .required(
                        "description",
                        NQArgumentType.greedyString("quest description"),
                        NQDescription.of("New quest description. Supports spaces and MiniMessage formatting."))
                .commandDescription(NQDescription.of("Sets the selected quest's description."))
                .handler(context -> List.of(setQuestDescription(
                        plugin,
                        context.argument("quest"),
                        context.argument("description"))))
                .registration());

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                questDisplayName = editQuest.literal(
                        "displayName",
                        NQDescription.of("Quest display name shown in quest lists, quest previews, GUIs, and chat messages. Supports MiniMessage formatting."));
        commands.add(questDisplayName.literal("show", NQDescription.of("Shows the selected quest's current display name."))
                .commandDescription(NQDescription.of("Shows the selected quest's display name."))
                .handler(context -> List.of(questDisplayName(plugin, context.argument("quest"))))
                .registration());
        commands.add(questDisplayName.literal("remove", NQDescription.of("Removes the selected quest's custom display name."))
                .commandDescription(NQDescription.of("Removes the selected quest's display name."))
                .handler(context -> List.of(removeQuestDisplayName(plugin, context.argument("quest"))))
                .registration());
        commands.add(questDisplayName.literal("set", NQDescription.of("Sets the selected quest's display name shown in GUIs, previews, and chat."))
                .required(
                        "display-name",
                        NQArgumentType.greedyString("quest display name"),
                        NQDescription.of("New quest display name. Supports spaces and MiniMessage formatting."))
                .commandDescription(NQDescription.of("Sets the selected quest's display name."))
                .handler(context -> List.of(setQuestDisplayName(
                        plugin,
                        context.argument("quest"),
                        context.argument("display-name"))))
                .registration());

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                limits = editQuest.literal(
                        "limits",
                        NQDescription.of("Configures quest accept, completion, and fail limits."));
        commands.add(limits.literal("completions", NQDescription.of("Configures how many times players may complete this quest."))
                .required(
                        "max-completions",
                        NQArgumentType.integer("maximum completions"),
                        NQDescription.of("Maximum completions allowed. Use -1 for unlimited."))
                .commandDescription(NQDescription.of("Sets the maximum number of times this quest can be completed."))
                .handler(context -> List.of(setQuestMaxCompletions(
                        plugin,
                        context.argument("quest"),
                        integer(context.argument("max-completions")))))
                .registration());
        commands.add(limits.literal("accepts", NQDescription.of("Configures how many times players may accept this quest."))
                .required(
                        "max-accepts",
                        NQArgumentType.integer("maximum accepts"),
                        NQDescription.of("Maximum accepts allowed. Use -1 for unlimited."))
                .commandDescription(NQDescription.of("Sets the maximum number of times this quest can be accepted."))
                .handler(context -> List.of(setQuestMaxAccepts(
                        plugin,
                        context.argument("quest"),
                        integer(context.argument("max-accepts")))))
                .registration());
        commands.add(limits.literal("fails", NQDescription.of("Configures how many times players may fail this quest."))
                .required(
                        "max-fails",
                        NQArgumentType.integer("maximum fails"),
                        NQDescription.of("Maximum failures allowed. Use -1 for unlimited."))
                .commandDescription(NQDescription.of("Sets the maximum number of times this quest can be failed."))
                .handler(context -> List.of(setQuestMaxFails(
                        plugin,
                        context.argument("quest"),
                        integer(context.argument("max-fails")))))
                .registration());

        commands.add(editQuest.literal("takeEnabled", NQDescription.of("Controls whether players may take this quest."))
                .required(
                        "take-enabled",
                        NQArgumentType.bool("take enabled"),
                        NQDescription.of("Whether players can accept this quest with /notquests take."))
                .commandDescription(NQDescription.of("Sets whether players can accept this quest with /notquests take."))
                .handler(context -> List.of(setQuestTakeEnabled(
                        plugin,
                        context.argument("quest"),
                        bool(context.argument("take-enabled")))))
                .registration());
        commands.add(editQuest.literal("abortEnabled", NQDescription.of("Controls whether players may abort this quest."))
                .required(
                        "abort-enabled",
                        NQArgumentType.bool("abort enabled"),
                        NQDescription.of("Whether players can abort this quest with /notquests abort."))
                .commandDescription(NQDescription.of("Sets whether players can abort this quest with /notquests abort."))
                .handler(context -> List.of(setQuestAbortEnabled(
                        plugin,
                        context.argument("quest"),
                        bool(context.argument("abort-enabled")))))
                .registration());
        commands.add(editQuest.literal("guiItem", NQDescription.of("Shows or changes the item displayed for this quest in GUIs."))
                .required(
                        "material",
                        NQArgumentType.itemSelection(),
                        NQDescription.of("Material, custom NotQuests item, hand item, any, or comma-separated material list for this quest's GUI item."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.GUI_ITEM_GLOW.name(), NQFlags.GUI_ITEM_GLOW.description()))
                .commandDescription(NQDescription.of("Sets the item displayed for this quest in GUIs."))
                .handler(context -> List.of(setQuestGuiItem(
                        plugin,
                        adapter,
                        context.argument("quest"),
                        context.rawArgument("material"),
                        context.questPlayer(),
                        context.flagPresent(NQFlags.GUI_ITEM_GLOW.name()))))
                .registration());

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                questCategory = editQuest.literal(
                        "category",
                        NQDescription.of("Shows or changes the category assigned to the selected quest."));
        commands.add(questCategory.literal("show", NQDescription.of("Shows the category currently assigned to the selected quest."))
                .commandDescription(NQDescription.of("Shows the selected quest's category."))
                .handler(context -> List.of(questCategory(plugin, context.argument("quest"))))
                .registration());
        commands.add(questCategory.literal("set", NQDescription.of("Moves the selected quest into another category."))
                .required(
                        "category",
                        NQArgumentType.category(),
                        NQDescription.of("New category for this quest."))
                .commandDescription(NQDescription.of("Changes the selected quest's category."))
                .handler(context -> List.of(setQuestCategory(
                        plugin,
                        context.argument("quest"),
                        context.argument("category"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage setQuestDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final String description) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setDescription(description);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Description successfully added to quest "
                + highlight(quest.getIdentifier()) + "! New description: " + highlight2(quest.getDescription()));
    }

    static CommandMessage setQuestDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final String displayName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setDisplayName(displayName);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Display name successfully added to quest "
                + highlight(quest.getIdentifier()) + "! New display name: " + highlight2(quest.getDisplayName()));
    }

    static CommandMessage setQuestCategory(
            final NotQuestsPlugin plugin,
            final String questName,
            final String categoryName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        if (plugin.questManager().getCategory(categoryName) == null) {
            return missingCategory(categoryName);
        }
        final String oldCategory = blankDefault(quest.getCategory(), "default");
        if (oldCategory.equalsIgnoreCase(categoryName)) {
            return CommandMessage.error("<error>The quest " + highlight(quest.getIdentifier())
                    + " already has the category " + highlight2(oldCategory) + ".");
        }
        quest.setCategory(categoryName);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Category for Quest " + highlight(quest.getIdentifier())
                + " has successfully been changed from " + highlight2(oldCategory)
                + " to " + highlight2(categoryName) + "!");
    }

    static CommandMessage questDescription(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return CommandMessage.error("Quest " + highlight(questName) + " does not exist.");
        }
        return CommandMessage.success("<main>Current description of Quest " + highlight(quest.getIdentifier())
                + ": " + highlight2(blankDefault(quest.getDescription(), "none")));
    }

    static CommandMessage removeQuestDescription(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return CommandMessage.error("Quest " + highlight(questName) + " does not exist.");
        }
        quest.clearDescription();
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Description successfully removed from quest "
                + highlight(quest.getIdentifier()) + "!");
    }

    static CommandMessage questDisplayName(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return CommandMessage.error("Quest " + highlight(questName) + " does not exist.");
        }
        return CommandMessage.success("<main>Current display name of Quest " + highlight(quest.getIdentifier())
                + ": " + highlight2(blankDefault(quest.getDisplayName(), "none")));
    }

    static CommandMessage removeQuestDisplayName(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return CommandMessage.error("Quest " + highlight(questName) + " does not exist.");
        }
        quest.clearDisplayName();
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Display name successfully removed from quest "
                + highlight(quest.getIdentifier()) + "!");
    }

    static CommandMessage questCategory(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return CommandMessage.error("Quest " + highlight(questName) + " does not exist.");
        }
        return CommandMessage.success("<main>Category for Quest " + highlight(quest.getIdentifier())
                + ": " + highlight2(blankDefault(quest.getCategory(), "default")) + ".");
    }

    static CommandMessage setQuestMaxCompletions(
            final NotQuestsPlugin plugin,
            final String questName,
            final int maxCompletions) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setMaxCompletions(maxCompletions > 0 ? maxCompletions : -1);
        plugin.saveConfiguredData();
        return questLimitMessage(quest, "completions", quest.getMaxCompletions());
    }

    static CommandMessage setQuestMaxAccepts(
            final NotQuestsPlugin plugin,
            final String questName,
            final int maxAccepts) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setMaxAccepts(maxAccepts > 0 ? maxAccepts : -1);
        plugin.saveConfiguredData();
        return questLimitMessage(quest, "accepts", quest.getMaxAccepts());
    }

    static CommandMessage setQuestMaxFails(
            final NotQuestsPlugin plugin,
            final String questName,
            final int maxFails) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setMaxFails(maxFails > 0 ? maxFails : -1);
        plugin.saveConfiguredData();
        return questLimitMessage(quest, "fails", quest.getMaxFails());
    }

    static CommandMessage setQuestTakeEnabled(
            final NotQuestsPlugin plugin,
            final String questName,
            final boolean takeEnabled) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setTakeEnabled(takeEnabled);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Quest taking (/notquests take) for the Quest "
                + highlight(quest.getIdentifier()) + " has been set to "
                + highlight2(takeEnabled ? "enabled" : "disabled") + "!");
    }

    static CommandMessage setQuestAbortEnabled(
            final NotQuestsPlugin plugin,
            final String questName,
            final boolean abortEnabled) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setAbortEnabled(abortEnabled);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Quest aborting (/notquests abort) for the Quest "
                + highlight(quest.getIdentifier()) + " has been set to "
                + highlight2(abortEnabled ? "enabled" : "disabled") + "!");
    }

    static CommandMessage setQuestAcceptCooldownComplete(
            final NotQuestsPlugin plugin,
            final String questName,
            final Duration cooldown) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final long minutes = cooldown == null ? -1L : cooldown.toMinutes();
        quest.setAcceptCooldownComplete(minutes);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Complete acceptCooldown for Quest "
                + highlight(quest.getIdentifier()) + " has been set to "
                + highlight2(minutes < 0 ? "disabled" : formatOldCooldownDuration(cooldown)) + "!");
    }

    static CommandMessage setQuestGuiItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String rawItemSelection,
            final boolean glow) {
        return setQuestGuiItem(plugin, adapter, questName, rawItemSelection, null, glow);
    }

    static CommandMessage setQuestGuiItem(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final Object rawItemSelection,
            final PlatformPlayer questPlayer,
            final boolean glow) {
        ItemSelection itemSelection = adapter.parseItemSelection(rawItemSelection, questPlayer);
        if (itemSelection == null) {
            itemSelection = ItemStackSelection.parse("book");
        }
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.setGuiItem(itemSelection);
        quest.setGuiItemGlow(glow);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Take Item Material for Quest " + highlight(questName)
                + " has been set to " + highlight2(firstListedMaterial(itemSelection)) + "!");
    }

    private static CommandMessage questLimitMessage(
            final Quest quest,
            final String kind,
            final int storedValue) {
        final String oldLabel = switch (kind) {
            case "completions" -> "Maximum amount of completions";
            case "accepts" -> "Maximum amount of accepts";
            case "fails" -> "Maximum amount of fails";
            default -> "Maximum amount of " + kind;
        };
        return CommandMessage.success("<success>" + oldLabel + " for Quest " + highlight(quest.getIdentifier())
                + " has been set to " + highlight2(storedValue > 0 ? storedValue : "unlimited (default)") + "!");
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

    private static Duration duration(final Object value) {
        if (value instanceof final Duration duration) {
            return duration;
        }
        if (!(value instanceof final String raw) || raw.isBlank()) {
            return null;
        }
        final String input = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (input.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(input.substring(0, input.length() - 2)));
            }
            if (input.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            return Duration.ofSeconds(Long.parseLong(input));
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static String formatOldCooldownDuration(final Duration duration) {
        if (duration == null) {
            return "disabled";
        }
        return duration.toDaysPart()
                + " days, "
                + duration.toHoursPart()
                + " hours, "
                + duration.toMinutesPart()
                + " minutes";
    }

    private static int integer(final String input) {
        try {
            return Integer.parseInt(input);
        } catch (final NumberFormatException exception) {
            return 0;
        }
    }

    private static boolean bool(final String input) {
        return Boolean.parseBoolean(input);
    }

    private static CommandMessage missingQuest(final String questName) {
        return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist!");
    }

    private static CommandMessage missingCategory(final String categoryName) {
        return CommandMessage.error("<error>No Category found: " + categoryName);
    }

    private static String highlight(final Object value) {
        return CommandSupport.highlight(value);
    }

    private static String highlight2(final Object value) {
        return CommandSupport.highlight2(value);
    }

    private static String blankDefault(final String value, final String fallback) {
        return CommandSupport.blankDefault(value, fallback);
    }
}
