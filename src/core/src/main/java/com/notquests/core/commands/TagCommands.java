package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.managers.tags.TagManager.Tag;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TagCommands {
    private TagCommands() {}

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
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tags = root.literal(
                        "tags",
                        NQDescription.of("Manages player tags."));
        commands.add(tags.literal("create", NQDescription.of("Creates a new player tag with the selected value type."))
                .required("type", NQArgumentType.tagType(), NQDescription.of("Value type for the new tag: BOOLEAN, INTEGER, FLOAT, DOUBLE, or STRING."))
                .required("name", NQArgumentType.word("tag name"), NQDescription.of("Unique name for the tag to create."))
                .commandDescription(NQDescription.of("Creates a new tag of the selected type."))
                .handler(context -> List.of(createTag(
                        plugin,
                        context.argument("type"),
                        context.argument("name"),
                        "")))
                .registration());
        commands.add(tags.literal("list", NQDescription.of("Lists every configured player tag."))
                .commandDescription(NQDescription.of("Lists all configured tags."))
                .handler(ignored -> listTags(plugin))
                .registration());
        commands.add(tags.literal("delete", NQDescription.of("Deletes the selected player tag."))
                .required("tag-name", NQArgumentType.tagName(), NQDescription.of("Name of the tag."))
                .commandDescription(NQDescription.of("Deletes an existing tag."))
                .handler(context -> List.of(deleteTag(plugin, context.argument("tag-name"))))
                .registration());
        commands.add(tags.literal("check", NQDescription.of("Displays the selected tag's stored value for a player."))
                .required("tag-name", NQArgumentType.tagName(), NQDescription.of("Name of the tag."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.PLAYER.name(), NQFlags.PLAYER.description())
                        .withArgument(NQArgumentType.player())
                        .build())
                .commandDescription(NQDescription.of("Displays a player's stored value for a tag."))
                .handler(context -> List.of(checkTag(
                        plugin,
                        adapter,
                        context.argument("tag-name"),
                        playerNameOrFlag(context, NQFlags.PLAYER.name()))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage createTag(
            final NotQuestsPlugin plugin,
            final String type,
            final String name,
            final String categoryName) {
        final TagType tagType;
        try {
            tagType = TagType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return CommandMessage.error("Unknown tag type: " + highlight(type) + ".");
        }
        if (!plugin.createTag(tagType, name, categoryName)) {
            return CommandMessage.error("<error>Error: The tag " + highlight(name) + " already exists!");
        }
        plugin.saveData();
        return CommandMessage.success("<success>The "
                + tagType.name().toLowerCase(Locale.ROOT)
                + " tag " + highlight(name) + " has been added successfully!");
    }

    static List<CommandMessage> listTags(final NotQuestsPlugin plugin) {
        final List<String> names = plugin.tagNames();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All tags:"));
        int counter = 1;
        for (final String name : names) {
            final Tag tag = plugin.tag(name);
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>"
                    + name + "</main> <highlight2>Type: <main>"
                    + (tag == null ? "UNKNOWN" : tag.tagType().name())));
            counter++;
        }
        return List.copyOf(messages);
    }

    static CommandMessage deleteTag(final NotQuestsPlugin plugin, final String tagName) {
        if (!plugin.deleteTag(tagName)) {
            return CommandMessage.error("<error>Error: The tag " + highlight(tagName) + " doesn't exist!");
        }
        plugin.saveData();
        return CommandMessage.success("<success>The tag "
                + highlight(tagName) + " has been deleted successfully!");
    }

    static CommandMessage checkTag(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String tagName,
            final String playerName) {
        final Tag tag = plugin.tag(tagName);
        if (tag == null) {
            return CommandMessage.error("<error>Error: The tag " + highlight(tagName) + " doesn't exists!");
        }
        if (playerName == null || playerName.isBlank()) {
            return CommandMessage.error("<error>Error: Run this in-game, or specify a player with "
                    + "<highlight>--player</highlight> from console.");
        }
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final Object value = plugin.activeQuestPlayer(target.identifier())
                .getTagValue(tag.tagName());
        return CommandMessage.success("<main>" + tag.tagType().name().toLowerCase(Locale.ROOT)
                + " tag " + highlight(tag.tagName())
                + " for " + highlight2(target.displayName())
                + ":</main> <highlight>" + (value == null ? "not set" : value));
    }

    private static String playerNameOrFlag(final NQCommandContext context, final String flagName) {
        final String value = context.flag(flagName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return context.questPlayer() == null ? "" : context.questPlayer().playerName();
    }

    private static String highlight(final Object value) {
        return CommandSupport.highlight(value);
    }

    private static String highlight2(final Object value) {
        return CommandSupport.highlight2(value);
    }
}
