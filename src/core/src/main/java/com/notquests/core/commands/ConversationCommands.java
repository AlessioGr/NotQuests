package com.notquests.core.commands;

import net.kyori.adventure.text.format.NamedTextColor;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.conversation.Speaker;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.npc.ArmorStandAttachments;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class ConversationCommands {
    private ConversationCommands() {}

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            userCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        commands.add(root.literal("continueConversation", NQDescription.of("Continues a conversation with the chosen option."))
                .required(
                        "optionID",
                        NQArgumentType.integer("conversation option"),
                        NQDescription.of("Conversation option number to select."),
                        (context, input) -> conversationOptionIds(plugin, context.questPlayer()))
                .commandDescription(NQDescription.of("Selects an answer for the currently open conversation."))
                .handler(context -> playerOnly(context, () -> continueConversation(
                        plugin,
                        context.questPlayer(),
                        integer(context.argument("optionID")))))
                .registration());
        return List.copyOf(commands);
    }

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            rootCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin) {
        final ArrayList<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        commands.add(root.literal("conversations", NQDescription.of("Lists and saves NotQuests conversations."))
                .handler(context -> listConversations(plugin))
                .registration());
        commands.add(root.literal("conversations", NQDescription.of("Lists and saves NotQuests conversations."))
                .literal("save", NQDescription.of("Saves a simple conversation line under a conversation name."))
                .required("name", NQArgumentType.word("text"), NQDescription.of("Conversation name to create or replace."))
                .required("line", NQArgumentType.greedyString("arguments"), NQDescription.of("First conversation line to save."))
                .handler(NQCommandHandler.commandMessage(context ->
                        saveConversation(plugin, context.argument("name"), context.argument("line"))))
                .registration());
        return List.copyOf(commands);
    }

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
                    final NotQuestsAdapter adapter,
                    final Supplier<Path> dataFolder) {
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
                conversations = root.literal(
                        "conversations",
                        NQDescription.of("Manages conversations and their NPC attachments."));
        commands.add(conversations.literal("create", NQDescription.of("Creates a new conversation file."))
                .required(
                        "conversation-name",
                        NQArgumentType.word("conversation name"),
                        NQDescription.of("Unique file/name for the new conversation."),
                        (context, input) -> List.of("<Enter new conversation-name>"))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.CONVERSATION_DEMO.name(), NQFlags.CONVERSATION_DEMO.description()))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.CATEGORY.name(), NQFlags.CATEGORY.description())
                        .withArgument(NQArgumentType.category())
                        .build())
                .commandDescription(NQDescription.of("Creates a new conversation file."))
                .handler(context -> List.of(createConversation(
                        plugin,
                        dataFolder,
                        context.argument("conversation-name"),
                        context.flagPresent(NQFlags.CONVERSATION_DEMO.name()),
                        context.flag(NQFlags.CATEGORY.name()))))
                .registration());
        if (plugin.configuration().debugEnabled()) {
            commands.add(conversations.literal("test", NQDescription.of("Runs a conversation test."))
                    .commandDescription(NQDescription.of("Starts a test conversation."))
                    .handler(context -> playerOnly(
                            context,
                            () -> startTestConversation(plugin, context.questPlayer())))
                    .registration());
        }
        commands.add(conversations.literal("list", NQDescription.of("Lists every saved conversation."))
                .commandDescription(NQDescription.of("Lists all conversations."))
                .handler(ignored -> listConversations(plugin))
                .registration());
        commands.add(conversations.literal("analyze", NQDescription.of("Runs conversation analysis checks."))
                .required(
                        "conversation",
                        NQArgumentType.conversation(),
                        NQDescription.of("Name of the conversation."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.PRINT_TO_CONSOLE.name(), NQFlags.PRINT_TO_CONSOLE.description()))
                .commandDescription(NQDescription.of("Analyzes a saved conversation for setup issues."))
                .handler(context -> analyzeConversation(
                        plugin,
                        adapter,
                        context.argument("conversation"),
                        context.flagPresent(NQFlags.PRINT_TO_CONSOLE.name())))
                .registration());
        commands.add(conversations.literal("start", NQDescription.of("Starts the selected conversation."))
                .required(
                        "conversation",
                        NQArgumentType.conversation(),
                        NQDescription.of("Name of the conversation."))
                .commandDescription(NQDescription.of("Starts a conversation."))
                .handler(context -> playerOnly(context, () -> List.of(startConversation(
                        plugin,
                        context.questPlayer(),
                        context.argument("conversation")))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conversationEdit = conversations.literal(
                        "edit",
                        NQDescription.of("Opens subcommands for editing a saved conversation."))
                        .required(
                                "conversation",
                                NQArgumentType.conversation(),
                                NQDescription.of("Name of the conversation."));
        if (adapter.supportsNpcAttachments()) {
            commands.add(conversationEdit.literal("npcs", NQDescription.of("Manages NPCs attached to this quest or conversation."))
                    .literal("add", NQDescription.of("Attaches the selected conversation to an NPC or armor stand."))
                    .required(
                            "NPC",
                            NQArgumentType.npcSelector(),
                            NQDescription.of("NPC, armor stand, or right-click selector that should start the conversation."))
                    .commandDescription(NQDescription.of("Attaches the selected conversation to an NPC or armor stand."))
                    .handler(context -> List.of(addConversationNpc(
                            plugin,
                            adapter,
                            context.questPlayer(),
                            context.argument("conversation"),
                            context.argument("NPC"))))
                    .registration());
        }
        if (adapter.supportsArmorStandAttachmentTools()) {
            commands.add(conversationEdit.literal("armorstand", NQDescription.of("Uses an armor stand selector/removal tool."))
                    .literal("remove", NQDescription.of("Gives a player the tool used to remove conversations from an armor stand."))
                    .commandDescription(NQDescription.of("Gives a player an item to remove all conversations from an armor stand."))
                    .handler(context -> List.of(conversationArmorStandRemoveToolMessage(adapter, context.questPlayer())))
                    .registration());
        }
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conversationSpeakers = conversationEdit.literal(
                        "speakers",
                        NQDescription.of("Manages speakers used by conversation lines."));
        commands.add(conversationSpeakers.literal("add", NQDescription.of("Creates a new speaker for the selected conversation."))
                .required(
                        "speaker-name",
                        NQArgumentType.word("speaker name"),
                        NQDescription.of("Unique speaker identifier used by conversation lines and speaker options."),
                        (context, input) -> List.of("<Enter new Speaker Name>"))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.SPEAKER_COLOR.name(), NQFlags.SPEAKER_COLOR.description())
                        .withArgument(NQArgumentType.word("speaker color"))
                        .withSuggestions((context, input) -> namedTextColorSuggestions())
                        .build())
                .commandDescription(NQDescription.of("Adds a new speaker to the selected conversation."))
                .handler(context -> List.of(addConversationSpeaker(
                        plugin,
                        context.argument("conversation"),
                        context.argument("speaker-name"),
                        context.flag(NQFlags.SPEAKER_COLOR.name()))))
                .registration());
        commands.add(conversationSpeakers.literal("list", NQDescription.of("Lists every speaker in the selected conversation."))
                .commandDescription(NQDescription.of("Lists every speaker in the selected conversation."))
                .handler(context -> conversationSpeakers(plugin, context.argument("conversation")))
                .registration());
        commands.add(conversationSpeakers.literal("remove", NQDescription.of("Removes a speaker from the selected conversation."))
                .required(
                        "speaker",
                        NQArgumentType.speaker(),
                        NQDescription.of("Speaker identifier to remove from this conversation."))
                .commandDescription(NQDescription.of("Removes a speaker from the selected conversation."))
                .handler(context -> List.of(removeConversationSpeaker(
                        plugin,
                        context.argument("conversation"),
                        context.argument("speaker"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conversationCategory = conversationEdit.literal(
                        "category",
                        NQDescription.of("Shows or changes the category assigned to the selected conversation."));
        commands.add(conversationCategory.literal("show", NQDescription.of("Shows the category currently assigned to the selected conversation."))
                .commandDescription(NQDescription.of("Shows the selected conversation's current category."))
                .handler(context -> List.of(conversationCategory(plugin, context.argument("conversation"))))
                .registration());
        commands.add(conversationCategory.literal("set", NQDescription.of("Moves the selected conversation into another category."))
                .required(
                        "category",
                        NQArgumentType.category(),
                        NQDescription.of("New category for this conversation."))
                .commandDescription(NQDescription.of("Changes the selected conversation's category."))
                .handler(context -> List.of(setConversationCategory(
                        plugin,
                        context.argument("conversation"),
                        context.argument("category"))))
                .registration());
        return List.copyOf(commands);
    }

    static List<String> conversationOptionIds(final NotQuestsPlugin plugin, final PlatformPlayer questPlayer) {
        return plugin.conversationManager().optionIds(questPlayer);
    }

    private static CommandMessage startConversation(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String conversationName) {
        return plugin.startConversation(questPlayer, conversationName, true, ignored -> {})
                ? CommandMessage.success("<main>Playing " + highlight(conversationName) + " conversation...")
                : CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist or cannot be started.");
    }

    private static List<CommandMessage> startTestConversation(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer) {
        if (!plugin.conversationManager().exists("__test__")) {
            plugin.conversationManager().save("__test__", List.of("<main>This is a NotQuests test conversation."));
            plugin.saveConfiguredData();
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<main>Playing test conversation..."));
        if (!plugin.startConversation(questPlayer, "__test__", true, ignored -> {})) {
            messages.add(CommandMessage.error("Conversation " + highlight("__test__") + " does not exist or cannot be started."));
        }
        return List.copyOf(messages);
    }

    static List<CommandMessage> continueConversation(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final int optionId) {
        plugin.chooseConversationOption(questPlayer, optionId);
        return List.of();
    }

    static CommandMessage createConversation(
            final NotQuestsPlugin plugin,
            final Supplier<Path> dataFolder,
            final String conversationName,
            final boolean demo,
            final String categoryName) {
        final String safeName = sanitizeFileName(conversationName);
        if (safeName.isBlank()) {
            return CommandMessage.error("<error>Error: Conversation name cannot be blank.");
        }
        if (plugin.conversationManager().exists(safeName)) {
            return CommandMessage.error("<error>Error: the conversation " + highlight(safeName) + " already exists!");
        }
        final String finalCategory = blankDefault(categoryName, "default");
        if (plugin.questManager().getCategory(finalCategory) == null) {
            return CommandMessage.error("<error>Error: Category for conversation is null.");
        }
        try {
            final Path created = ConversationManager.create(dataFolder.get(), safeName, demo, finalCategory);
            plugin.reloadData(ReloadTarget.CONVERSATIONS);
            return saveAndReturn(plugin, CommandMessage.success("<success>The conversation has been created successfully! "
                    + "There are currently no commands to edit them - you have to edit "
                    + "the conversation file. You can find it at <highlight>"
                    + created.toString().replace("\\", "/")));
        } catch (final FileAlreadyExistsException exception) {
            return CommandMessage.error("<error>Error: the conversation " + highlight(safeName) + " already exists!");
        } catch (final java.io.IOException exception) {
            return CommandMessage.error("<error>Error: couldn't create conversation file. There was an exception.");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("<error>Error: couldn't create conversation file. There was an exception. (2)");
        }
    }

    static CommandMessage saveConversation(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final String line) {
        return saveConversation(plugin, conversationName, line, "");
    }

    static CommandMessage saveConversation(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final String line,
            final String categoryName) {
        if (categoryName != null && !categoryName.isBlank() && plugin.questManager().getCategory(categoryName) == null) {
            return missingCategory(categoryName);
        }
        plugin.conversationManager().save(conversationName, List.of(line), categoryName);
        return saveAndReturn(plugin, CommandMessage.success("<success>The conversation has been saved successfully!"));
    }

    static List<CommandMessage> listConversations(final NotQuestsPlugin plugin) {
        final List<String> names = plugin.conversationNames();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All conversations:"));
        int counter = 1;
        for (final String name : names) {
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>" + name));
            messages.add(CommandMessage.success("<unimportant>--- Attached to NPC:</unimportant> <main>"
                    + attachedNpcLabels(plugin, name)));
            messages.add(CommandMessage.success("<unimportant>--- Amount of starting conversation lines:</unimportant> <main>"
                    + plugin.conversationManager().lineCount(name)));
            counter++;
        }
        return List.copyOf(messages);
    }

    private static CommandMessage addConversationNpc(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor,
            final String conversationName,
            final String npcSelector) {
        if (conversationName == null || conversationName.isBlank()) {
            return CommandMessage.error("<error>Conversation name is missing.");
        }
        if (npcSelector == null || npcSelector.isBlank()) {
            return CommandMessage.error("<error>NPC selector is missing.");
        }
        if (plugin.conversationNames().stream().noneMatch(conversationName::equalsIgnoreCase)) {
            return CommandMessage.error("Conversation <highlight>" + conversationName + "</highlight> does not exist.");
        }
        if ("rightClickSelect".equalsIgnoreCase(npcSelector)) {
            if (actor == null || !actor.hasPlayer()) {
                return CommandMessage.error("<error>rightClickSelect must be run in-game by a player.");
            }
            final boolean started = plugin.startNpcSelection(
                    adapter,
                    actor,
                    "<success>You received an NPC selector item. Right-click the NPC or armor stand that should start <highlight>"
                            + conversationName + "</highlight>.",
                    "<LIGHT_PURPLE>Attach conversation to NPC",
                    List.of(
                            "<GRAY>Conversation: <YELLOW>" + conversationName,
                            "<GRAY>Right-click a Citizens NPC, FancyNPC, or armor stand."),
                    selection -> {
                        attachConversationNpc(plugin, conversationName, selection);
                        actor.sendMessage("Conversation <highlight>" + conversationName
                                + "</highlight> attached to <highlight2>" + npcLabel(selection) + "</highlight2>.");
                    });
            return started
                    ? CommandMessage.success("<success>NPC selector item given for conversation <highlight>"
                            + conversationName + "</highlight>.")
                    : CommandMessage.error("Could not create an NPC selector item.");
        }
        final NotQuestsAdapter.NpcSelection selection = adapter.npcSelection(npcSelector);
        if (selection == null || selection.npcId() == null) {
            return CommandMessage.error("NPC selector <highlight>" + npcSelector + "</highlight> does not exist.");
        }
        attachConversationNpc(plugin, conversationName, selection);
        return CommandMessage.success("Conversation <highlight>" + conversationName
                + "</highlight> attached to <highlight2>" + npcLabel(selection) + "</highlight2>.");
    }

    private static CommandMessage conversationArmorStandRemoveToolMessage(
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor) {
        if (actor == null || !actor.hasPlayer()) {
            return CommandMessage.error("<error>This command can only be used by a player.");
        }
        if (adapter.giveArmorStandTool(actor, ArmorStandAttachments.conversationRemoveToolItem())) {
            return CommandMessage.success("<success>Armor-stand conversation removal tool given.");
        }
        return CommandMessage.error("Could not create an armor-stand conversation tool.");
    }

    private static CommandMessage addConversationSpeaker(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final String speakerName,
            final String speakerColor) {
        return saveAndReturn(plugin, plugin.conversationManager().addSpeaker(conversationName, speakerName, speakerColor)
                ? CommandMessage.success("<success>Speaker " + highlight(speakerName)
                        + " was successfully added to conversation " + highlight2(conversationName) + "!")
                : CommandMessage.error("<error>Speaker " + highlight(speakerName)
                        + " could not be added to " + highlight2(conversationName) + "! Does it already exist?"));
    }

    private static List<CommandMessage> conversationSpeakers(
            final NotQuestsPlugin plugin,
            final String conversationName) {
        if (!plugin.conversationManager().exists(conversationName)) {
            return List.of(CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist."));
        }
        final List<Speaker> speakers = plugin.conversationManager().speakers(conversationName);
        if (speakers.isEmpty()) {
            return List.of(CommandMessage.success("<success>This conversation has no speakers."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Speakers of conversation "
                + highlight2(conversationName) + ":"));
        int counter = 1;
        for (final Speaker speaker : speakers) {
            messages.add(CommandMessage.success("<highlight>" + counter
                    + ".</highlight> <main>Name:</main> <highlight2>"
                    + speaker.getSpeakerName()
                    + "</highlight2> Color: <highlight2>"
                    + speaker.getColor()
                    + speaker.getColor().replace("<", "").replace(">", "")
                    + "</highlight2>"));
            counter++;
        }
        return List.copyOf(messages);
    }

    private static CommandMessage removeConversationSpeaker(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final String speakerName) {
        if (!plugin.conversationManager().exists(conversationName)) {
            return CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist.");
        }
        return saveAndReturn(plugin, plugin.conversationManager().removeSpeaker(conversationName, speakerName)
                ? CommandMessage.success("<success>Speaker " + highlight(speakerName)
                        + " was successfully removed from conversation " + highlight2(conversationName) + "!")
                : CommandMessage.error("<error>Speaker " + highlight(speakerName)
                        + " could not be removed from " + highlight2(conversationName) + "! Does it exist?"));
    }

    private static CommandMessage conversationCategory(
            final NotQuestsPlugin plugin,
            final String conversationName) {
        if (!plugin.conversationManager().exists(conversationName)) {
            return CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist.");
        }
        return CommandMessage.success("<main>Category for conversation " + highlight(conversationName)
                + ": " + highlight2(blankDefault(plugin.conversationManager().category(conversationName), "default")) + ".");
    }

    private static CommandMessage setConversationCategory(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final String categoryName) {
        if (!plugin.conversationManager().exists(conversationName)) {
            return CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist.");
        }
        if (plugin.questManager().getCategory(categoryName) == null) {
            return missingCategory(categoryName);
        }
        final String oldCategory = blankDefault(plugin.conversationManager().category(conversationName), "default");
        if (oldCategory.equalsIgnoreCase(categoryName)) {
            return CommandMessage.error("<error> Error: The conversation " + highlight(conversationName)
                    + " already has the category " + highlight2(oldCategory) + ".");
        }
        plugin.conversationManager().category(conversationName, categoryName);
        return saveAndReturn(plugin, CommandMessage.success("<success>Category for conversation " + highlight(conversationName)
                + " has successfully been changed from " + highlight2(oldCategory)
                + " to " + highlight2(categoryName) + "!"));
    }

    private static List<CommandMessage> analyzeConversation(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String conversationName,
            final boolean printToConsole) {
        if (!plugin.conversationManager().exists(conversationName)) {
            return List.of(CommandMessage.error("Conversation " + highlight(conversationName) + " does not exist."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        final List<String> lines = plugin.conversationManager().lines(conversationName);
        for (int i = 0; i < lines.size(); i++) {
            final String analyzed = analyzeConversationLine(i + 1, lines.get(i), "  ");
            if (printToConsole) {
                adapter.logInfo("\n" + analyzed);
                messages.add(CommandMessage.success("<success>Analyze Output has been printed to your console!"));
            } else {
                messages.add(CommandMessage.success(analyzed));
            }
        }
        return List.copyOf(messages);
    }

    private static void attachConversationNpc(
            final NotQuestsPlugin plugin,
            final String conversationName,
            final NotQuestsAdapter.NpcSelection selection) {
        plugin.attachConversationNpc(conversationName, selection);
    }

    private static String attachedNpcLabels(final NotQuestsPlugin plugin, final String conversationName) {
        final List<String> labels = plugin.conversationNpcAttachments(conversationName).stream()
                .map(attachment -> NpcAttachments.formatAttachedNPC(
                        attachment.npcType(), attachment.npcId(), attachment.npcName()))
                .toList();
        return labels.isEmpty() ? "none" : String.join(", ", labels);
    }

    private static String analyzeConversationLine(final int lineNumber, final String line, final String beginningSpaces) {
        return beginningSpaces
                + " <unimportant>└<highlight>"
                + lineNumber
                + ":\n"
                + beginningSpaces
                + "  <unimportant>Speaker:</unimportant> <main>default\n"
                + beginningSpaces
                + "  <unimportant>Message:</unimportant> <main>["
                + line
                + "]<RESET>\n";
    }

    private static List<String> namedTextColorSuggestions() {
        return NamedTextColor.NAMES.values().stream()
                .map(color -> "<" + color + ">")
                .toList();
    }

    private static CommandMessage saveAndReturn(final NotQuestsPlugin plugin, final CommandMessage message) {
        if (message != null && message.success()) {
            plugin.saveConfiguredData();
        }
        return message;
    }

    private static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<CommandMessage> playerOnly(
            final NQCommandContext context,
            final Supplier<List<CommandMessage>> messages) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return List.of(CommandMessage.error("<error>This command can only be used by a Player."));
        }
        return messages.get();
    }

    private static int integer(final String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static String sanitizeFileName(final String value) {
        return value == null ? "" : value.replace("/", "").replace("\\", "").trim();
    }

    private static String highlight(final Object value) {
        return "<highlight>" + String.valueOf(value) + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + String.valueOf(value) + "</highlight2>";
    }

    private static CommandMessage missingCategory(final String categoryName) {
        return CommandMessage.error("Category " + highlight(categoryName) + " does not exist.");
    }

    private static String npcLabel(final NotQuestsAdapter.NpcSelection selection) {
        return selection == null || selection.label() == null || selection.label().isBlank()
                ? "selected NPC"
                : selection.label();
    }

}
