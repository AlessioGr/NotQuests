package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.npc.ArmorStandAttachments;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.List;

final class QuestNpcCommands {
    private QuestNpcCommands() {}

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
        if (adapter.supportsNpcAttachments()) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    questNpcs = editQuest.literal(
                            "npcs",
                            NQDescription.of("Manages NPCs attached to this quest."));
            commands.add(questNpcs.literal("add", NQDescription.of("Attaches the selected quest to an NPC or armor stand quest giver."))
                    .required(
                            "npc",
                            NQArgumentType.npcSelector(),
                            NQDescription.of("Citizens NPC, FancyNPC, armor stand, or right-click selector to attach this quest to."))
                    .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                            NQFlags.HIDE_IN_NPC.name(), NQFlags.HIDE_IN_NPC.description()))
                    .commandDescription(NQDescription.of("Attaches the selected quest to an NPC or armor stand."))
                    .handler(context -> List.of(addQuestNpc(
                            plugin,
                            adapter,
                            context.questPlayer(),
                            context.argument("quest"),
                            context.argument("npc"),
                            !context.flagPresent(NQFlags.HIDE_IN_NPC.name()))))
                    .registration());
            commands.add(questNpcs.literal("clear", NQDescription.of("Detaches the selected quest from every NPC and armor stand quest giver."))
                    .commandDescription(NQDescription.of("Detaches the selected quest from all NPCs and armor stands."))
                    .handler(context -> List.of(clearQuestNpcs(plugin, adapter, context.argument("quest"))))
                    .registration());
            commands.add(questNpcs.literal("list", NQDescription.of("Lists every NPC and armor stand currently attached to the selected quest."))
                    .commandDescription(NQDescription.of("Lists all NPCs and armor stands attached to the selected quest."))
                    .handler(context -> questNpcs(plugin, context.argument("quest")))
                    .registration());
        }
        if (adapter.supportsArmorStandAttachmentTools()) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    armorStands = editQuest.literal(
                            "armorstands",
                            NQDescription.of("Manages armor stands attached as quest givers."));
            commands.add(armorStands.literal("check", NQDescription.of("Gives a player the armor-stand inspection tool for quest attachments."))
                    .commandDescription(NQDescription.of("Gives you an item for checking which quests are attached to an armor stand."))
                    .handler(context -> List.of(armorStandQuestToolMessage(
                            plugin,
                            adapter,
                            context.questPlayer(),
                            context.argument("quest"),
                            ArmorStandAttachments.QuestTool.CHECK,
                            false)))
                    .registration());
            commands.add(armorStands.literal("add", NQDescription.of("Gives a player the tool used to attach this quest to an armor stand."))
                    .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                            NQFlags.HIDE_IN_ARMOR_STAND.name(), NQFlags.HIDE_IN_ARMOR_STAND.description()))
                    .commandDescription(NQDescription.of("Gives you an item for attaching this quest to an armor stand."))
                    .handler(context -> List.of(armorStandQuestToolMessage(
                            plugin,
                            adapter,
                            context.questPlayer(),
                            context.argument("quest"),
                            ArmorStandAttachments.QuestTool.ADD,
                            context.flagPresent(NQFlags.HIDE_IN_ARMOR_STAND.name()))))
                    .registration());
            commands.add(armorStands.literal("clear", NQDescription.of("Clears all armor-stand quest-giver attachments for this quest."))
                    .commandDescription(NQDescription.of("Clears all armor stands attached to this quest."))
                    .handler(context -> List.of(clearQuestArmorStands(plugin, adapter, context.argument("quest"))))
                    .registration());
            commands.add(armorStands.literal("list", NQDescription.of("Lists armor stands attached to the selected quest."))
                    .commandDescription(NQDescription.of("Lists all armor stands attached to this quest."))
                    .handler(context -> questArmorStands(plugin, context.argument("quest")))
                    .registration());
            commands.add(armorStands.literal("remove", NQDescription.of("Gives a player the tool used to remove this quest from an armor stand."))
                    .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                            NQFlags.HIDE_IN_ARMOR_STAND.name(), NQFlags.HIDE_IN_ARMOR_STAND.description()))
                    .commandDescription(NQDescription.of("Gives you an item for removing this quest from an armor stand."))
                    .handler(context -> List.of(armorStandQuestToolMessage(
                            plugin,
                            adapter,
                            context.questPlayer(),
                            context.argument("quest"),
                            ArmorStandAttachments.QuestTool.REMOVE,
                            context.flagPresent(NQFlags.HIDE_IN_ARMOR_STAND.name()))))
                    .registration());
        }
        return List.copyOf(commands);
    }

    static CommandMessage addQuestNpc(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor,
            final String questName,
            final String npcSelector,
            final boolean showQuestInNpc) {
        if (plugin.questManager().getQuest(questName) == null) {
            return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist.");
        }
        if (npcSelector == null || npcSelector.isBlank()) {
            return CommandMessage.error("<error>NPC selector is missing.");
        }
        if ("rightClickSelect".equalsIgnoreCase(npcSelector)) {
            if (actor == null || !actor.hasPlayer()) {
                return CommandMessage.error("<error>rightClickSelect must be run in-game by a player.");
            }
            final boolean started = plugin.startNpcSelection(
                    adapter,
                    actor,
                    "<success>You received an NPC selector item. Right-click the NPC or armor stand for quest <highlight>"
                            + questName + "</highlight>.",
                    "<LIGHT_PURPLE>Attach quest to NPC",
                    List.of(
                            "<GRAY>Quest: <YELLOW>" + questName,
                            "<GRAY>Right-click a Citizens NPC, FancyNPC, or armor stand."),
                    selection -> {
                        final String error = attachQuestNpc(plugin, questName, selection, showQuestInNpc);
                        if (error != null && !error.isBlank()) {
                            actor.sendMessage("<error>" + error);
                            return;
                        }
                        actor.sendMessage("Quest <highlight>" + questName
                                + "</highlight> attached to <highlight2>" + npcLabel(selection) + "</highlight2>.");
                    });
            return started
                    ? CommandMessage.success("<success>NPC selector item given for quest <highlight>"
                            + questName + "</highlight>.")
                    : CommandMessage.error("Could not create an NPC selector item.");
        }
        final NotQuestsAdapter.NpcSelection selection = adapter.npcSelection(npcSelector);
        if (selection == null || selection.npcId() == null) {
            return CommandMessage.error("NPC selector <highlight>" + npcSelector + "</highlight> does not exist.");
        }
        final String error = attachQuestNpc(plugin, questName, selection, showQuestInNpc);
        if (error != null && !error.isBlank()) {
            return CommandMessage.error(error);
        }
        return CommandMessage.success("Quest <highlight>" + questName
                + "</highlight> attached to <highlight2>" + npcLabel(selection) + "</highlight2>.");
    }

    static CommandMessage clearQuestNpcs(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName) {
        if (plugin.questManager().getQuest(questName) == null) {
            return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist.");
        }
        final NpcAttachments.Detachments detachments =
                plugin.questNpcDetachments(questName);
        plugin.clearQuestNpcAttachments(questName);
        plugin.saveConfiguredData();
        plugin.applyNpcDetachments(detachments);
        return CommandMessage.success("<success>Cleared NPC and armor-stand attachments for quest <highlight>"
                + questName + "</highlight>.");
    }

    static CommandMessage clearQuestArmorStands(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName) {
        if (plugin.questManager().getQuest(questName) == null) {
            return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist.");
        }
        final NpcAttachments.Detachments detachments =
                plugin.questNpcDetachments(questName, "armorstand");
        final int removed = plugin.clearQuestNpcAttachments(questName, "armorstand");
        if (removed > 0) {
            plugin.saveConfiguredData();
        }
        plugin.applyNpcDetachments(detachments);
        return CommandMessage.success("<success>Cleared " + removed
                + " armor-stand attachment(s) for quest <highlight>" + questName + "</highlight>.");
    }

    static List<CommandMessage> questArmorStands(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist."));
        }
        final List<NpcAttachment> attachments = quest.getNpcAttachments().stream()
                .filter(attachment -> "armorstand".equalsIgnoreCase(attachment.npcType()))
                .toList();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Armor stands bound to quest <highlight2>"
                + questName
                + "</highlight2>:"));
        if (attachments.isEmpty()) {
            messages.add(CommandMessage.success("<unimportant>No armor stands are attached to this quest."));
            return List.copyOf(messages);
        }
        int counter = 1;
        for (final NpcAttachment attachment : attachments) {
            messages.add(CommandMessage.success("<highlight>" + counter
                    + ".</highlight> <main>ID:</main> <highlight2>"
                    + NpcAttachments.formatAttachedNPC(
                            attachment.npcType(),
                            attachment.npcId(),
                            attachment.npcName())
                    + "</highlight2> <main>showing:</main> <highlight>"
                    + attachment.questShowing()
                    + "</highlight>"));
            counter++;
        }
        return List.copyOf(messages);
    }

    static List<CommandMessage> questNpcs(final NotQuestsPlugin plugin, final String questName) {
        if (plugin.questManager().getQuest(questName) == null) {
            return List.of(CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist."));
        }
        final List<NpcAttachment> attachments = plugin.questManager().getQuest(questName).getNpcAttachments();
        final List<String> shown = npcAttachmentLabels(attachments, true);
        final List<String> hidden = npcAttachmentLabels(attachments, false);
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>NPCs bound to quest <highlight2>"
                + questName
                + "</highlight2> with Quest showing:"));
        addNpcAttachmentSection(messages, shown);
        messages.add(CommandMessage.success("<highlight>NPCs bound to quest <highlight2>"
                + questName
                + "</highlight2> without Quest showing:"));
        addNpcAttachmentSection(messages, hidden);
        return List.copyOf(messages);
    }

    static CommandMessage armorStandQuestToolMessage(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor,
            final String questName,
            final ArmorStandAttachments.QuestTool tool,
            final boolean hiddenAttachment) {
        if (actor == null || !actor.hasPlayer()) {
            return CommandMessage.error("<error>This command can only be used by a player.");
        }
        if (plugin.questManager().getQuest(questName) == null) {
            return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist.");
        }
        return adapter.giveArmorStandTool(
                actor,
                ArmorStandAttachments.questToolItem(questName, tool, hiddenAttachment))
                ? CommandMessage.success("<success>" + ArmorStandAttachments.questToolAction(tool)
                        + " tool given for quest <highlight>" + questName + "</highlight>.")
                : CommandMessage.error(
                        "Could not create an armor-stand quest tool for quest <highlight>" + questName + "</highlight>.");
    }

    private static void addNpcAttachmentSection(
            final ArrayList<CommandMessage> messages,
            final List<String> attachments) {
        int counter = 1;
        for (final String attachment : attachments) {
            messages.add(CommandMessage.success("<highlight>"
                    + counter
                    + ".</highlight> <main>ID:</main> <highlight2>"
                    + attachment
                    + "</highlight2>"));
            counter++;
        }
    }

    private static List<String> npcAttachmentLabels(
            final List<NpcAttachment> attachments,
            final boolean questShowing) {
        return attachments.stream()
                .filter(attachment -> attachment.questShowing() == questShowing)
                .map(attachment -> NpcAttachments.formatAttachedNPC(
                        attachment.npcType(),
                        attachment.npcId(),
                        attachment.npcName()))
                .toList();
    }

    private static String attachQuestNpc(
            final NotQuestsPlugin plugin,
            final String questName,
            final NotQuestsAdapter.NpcSelection selection,
            final boolean showQuestInNpc) {
        if (!plugin.attachQuestNpc(questName, selection, showQuestInNpc)) {
            return "NPC selector <highlight>" + selection.selector() + "</highlight> does not exist.";
        }
        return "";
    }

    private static String npcLabel(final NotQuestsAdapter.NpcSelection selection) {
        return selection == null || selection.label() == null || selection.label().isBlank()
                ? "selected NPC"
                : selection.label();
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }
}
