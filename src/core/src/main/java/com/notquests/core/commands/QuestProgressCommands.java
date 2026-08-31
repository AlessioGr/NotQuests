package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer.CompletedObjective;
import com.notquests.core.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class QuestProgressCommands {
    private QuestProgressCommands() {}

    static NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            adminCommand(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter) {
        return root.literal("progress", NQDescription.of("Shows progress for one active quest of another player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .required(
                        "activeQuest",
                        NQArgumentType.activeQuest(),
                        NQDescription.of("Active quest on the selected player."))
                .commandDescription(NQDescription.of("Shows completed and active objective progress for a player's active quest."))
                .handler(context -> questProgress(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("activeQuest")))
                .registration();
    }

    static List<CommandMessage> questProgress(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String questName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final QuestPlayer playerData = target.questPlayer();
        if (playerData == null || !playerData.hasActiveQuest(questName)) {
            final ArrayList<CommandMessage> messages = new ArrayList<>();
            messages.add(CommandMessage.error("<error>Quest was not found or active!"));
            messages.addAll(QuestLifecycleCommands.activeQuests(plugin, adapter, playerName));
            return List.copyOf(messages);
        }
        final List<ActiveObjective> progress = plugin.activeObjectives(target.identifier()).stream()
                .filter(objective -> objective.getQuestIdentifier().equalsIgnoreCase(questName))
                .toList();
        final Quest quest = plugin.questManager().getQuest(questName);
        final String questDisplayName = quest == null ? questName : quest.getDisplayNameOrIdentifier();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<green>Completed Objectives for Quest <highlight>"
                + questDisplayName + "</highlight> of player " + highlight2(target.displayName())
                + " " + target.onlineStatus() + "<yellow>:"));
        renderCompletedObjectives(plugin, messages, playerData, questName, quest, target.platformPlayer());
        messages.add(CommandMessage.success("<green>Active Objectives for Quest <highlight>"
                + questDisplayName + "</highlight> of player " + highlight2(target.displayName())
                + " " + target.onlineStatus() + "<yellow>:"));
        renderActiveObjectives(plugin, messages, progress);
        return List.copyOf(messages);
    }

    static List<CommandMessage> userActiveQuests(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of(CommandMessage.error("<error>This command can only be used by a Player."));
        }
        final QuestPlayer playerData = plugin.activeQuestPlayer(questPlayer.playerIdentifier());
        final List<String> names = playerData.getActiveQuestIdentifiers().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (names.isEmpty()) {
            return List.of(CommandMessage.error(plugin.translate(questPlayer,
                    "chat.no-quests-accepted",
                    Map.of(),
                    "<error>Seems like you don't have any active quests!")));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success(plugin.translate(questPlayer,
                "chat.active-quests-label",
                Map.of(),
                "<highlight>Active quests:")));
        int counter = 1;
        for (final String name : names) {
            final Quest quest = plugin.questManager().getQuest(name);
            messages.add(CommandMessage.success("<green>" + counter + ". <yellow>"
                    + (quest == null ? name : quest.getDisplayNameOrIdentifier())));
            counter++;
        }
        return List.copyOf(messages);
    }

    static List<CommandMessage> userQuestProgress(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String questName) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of(CommandMessage.error("<error>This command can only be used by a Player."));
        }
        final QuestPlayer playerData = plugin.questPlayerIfLoaded(questPlayer.playerIdentifier());
        if (playerData == null || !playerData.hasActiveQuest(questName)) {
            final ArrayList<CommandMessage> messages = new ArrayList<>();
            messages.add(CommandMessage.error("<error>Quest was not found or active!"));
            messages.addAll(userActiveQuests(plugin, questPlayer));
            return List.copyOf(messages);
        }
        final List<ActiveObjective> progress = plugin.activeObjectives(questPlayer.playerIdentifier()).stream()
                .filter(objective -> objective.getQuestIdentifier().equalsIgnoreCase(questName))
                .toList();
        final Quest quest = plugin.questManager().getQuest(questName);
        final String questDisplayName = quest == null ? questName : quest.getDisplayNameOrIdentifier();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<green>Completed Objectives for Quest <highlight>"
                + questDisplayName + "</highlight><yellow>:"));
        renderCompletedObjectives(plugin, messages, playerData, questName, quest, questPlayer);
        messages.add(CommandMessage.success("<green>Active Objectives for Quest <highlight>"
                + questDisplayName + "</highlight><yellow>:"));
        renderActiveObjectives(plugin, messages, progress);
        return List.copyOf(messages);
    }

    private static void renderCompletedObjectives(
            final NotQuestsPlugin plugin,
            final ArrayList<CommandMessage> messages,
            final QuestPlayer playerData,
            final String questName,
            final Quest quest,
            final PlatformPlayer questPlayer) {
        final List<QuestPlayer.CompletedObjective> completedObjectives = playerData.getCompletedObjectives(questName);
        final List<String> completedObjectiveIds = playerData.getCompletedObjectiveIDs(questName);
        if (completedObjectives.isEmpty() && completedObjectiveIds.isEmpty()) {
            messages.add(CommandMessage.success("<unimportant>No completed objective progress is currently tracked.</unimportant>"));
            return;
        }
        for (final QuestPlayer.CompletedObjective completedObjective : completedObjectives) {
            renderCompletedObjective(plugin, messages, quest, completedObjective, questPlayer);
        }
        for (final String objectiveId : completedObjectiveIds) {
            if (playerData.getCompletedObjective(questName, objectiveId) == null) {
                renderCompletedObjective(
                        plugin,
                        messages,
                        quest,
                        completedObjectiveFallback(plugin, questName, quest, objectiveId),
                        questPlayer);
            }
        }
    }

    private static void renderActiveObjectives(
            final NotQuestsPlugin plugin,
            final ArrayList<CommandMessage> messages,
            final List<ActiveObjective> progress) {
        if (progress.isEmpty()) {
            messages.add(CommandMessage.success("<unimportant>No active objective progress is currently tracked.</unimportant>"));
            return;
        }
        for (final ActiveObjective objective : rootObjectives(progress)) {
            renderActiveObjective(plugin, messages, progress, objective, 0);
        }
    }

    private static void renderCompletedObjective(
            final NotQuestsPlugin plugin,
            final ArrayList<CommandMessage> messages,
            final Quest quest,
            final QuestPlayer.CompletedObjective completedObjective,
            final PlatformPlayer target) {
        if (completedObjective == null) {
            return;
        }
        final Objective entry = objectiveAt(
                plugin,
                quest == null ? completedObjective.questIdentifier() : quest.getIdentifier(),
                parseObjectivePath(completedObjective.objectivePath()));
        if (entry == null) {
            messages.add(CommandMessage.success("<highlight>"
                    + completedObjective.objectivePath()
                    + ".</highlight> <success>completed"));
            return;
        }
        final Objectives.Type type = objectiveType(plugin, entry.typeId());
        messages.add(CommandMessage.success("<strikethrough><gray>"
                + completedObjective.objectivePath()
                + ". "
                + objectiveDisplayNameOrIdentifier(entry, type)
                + ":</strikethrough>"));
        if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
            messages.add(CommandMessage.success("    <strikethrough><gray>Description: <white>"
                    + entry.getDescription()
                    + "</strikethrough>"));
        }
        final String taskDescription = target == null
                ? ""
                : objectiveTaskDescription(entry, type, target, null, true);
        if (taskDescription != null && !taskDescription.isBlank()) {
            messages.add(CommandMessage.success(taskDescription));
        }
        final double currentProgress = completedObjective.currentProgress() > 0
                ? completedObjective.currentProgress()
                : completedObjective.progressNeeded();
        final double progressNeeded = completedObjective.progressNeeded() > 0
                ? completedObjective.progressNeeded()
                : progressNeeded(plugin, entry);
        messages.add(CommandMessage.success("   <strikethrough><gray>Progress: <white>"
                + formatProgress(currentProgress)
                + " / "
                + formatProgress(progressNeeded)
                + "</strikethrough>"));
    }

    private static QuestPlayer.CompletedObjective completedObjectiveFallback(
            final NotQuestsPlugin plugin,
            final String questName,
            final Quest quest,
            final String objectivePath) {
        final int[] path = parseObjectivePath(objectivePath);
        final Objective entry = objectiveAt(plugin, questName, path);
        final double progressNeeded = progressNeeded(plugin, entry);
        return new QuestPlayer.CompletedObjective(
                quest == null ? questName : quest.getIdentifier(),
                objectivePath,
                holderPath(quest == null ? questName : quest.getIdentifier(), path),
                entry == null ? "" : entry.typeId(),
                progressNeeded,
                progressNeeded);
    }

    private static void renderActiveObjective(
            final NotQuestsPlugin plugin,
            final ArrayList<CommandMessage> messages,
            final List<ActiveObjective> allObjectives,
            final ActiveObjective objective,
            final int level) {
        if (objective == null || objective.isComplete()) {
            return;
        }
        final String prefix = "    ".repeat(Math.max(0, level));
        final Objectives.Type type = objectiveType(plugin, objective.getObjectiveTypeID());
        if (!objective.isUnlocked()) {
            messages.add(CommandMessage.success(prefix
                    + "<highlight>"
                    + objective.getObjectivePathKey()
                    + ".</highlight> <gray>Hidden objective"));
            return;
        }
        messages.add(CommandMessage.success(prefix
                + "<highlight>"
                + objective.getObjectivePathKey()
                + ".</highlight> <main>"
                + objectiveDisplayNameOrIdentifier(objective.getObjective(), type)
                + ":"));
        if (objective.getObjective().getDescription() != null && !objective.getObjective().getDescription().isBlank()) {
            messages.add(CommandMessage.success(prefix
                    + "    <veryUnimportant>└─ <unimportant>Description: <main>"
                    + objective.getObjective().getDescription()));
        }
        final String taskDescription = objectiveTaskDescription(objective.getObjective(), type, objective.getQuestPlayer(), objective, false);
        if (taskDescription != null && !taskDescription.isBlank()) {
            for (final String line : taskDescription.split("\\R", -1)) {
                messages.add(CommandMessage.success(prefix + line));
            }
        }
        messages.add(CommandMessage.success(prefix
                + "    <veryUnimportant>└─ <unimportant>Progress: <main>"
                + formatProgress(objective.getCurrentProgress())
                + " / "
                + formatProgress(objective.getProgressNeeded())
                + (objective.isComplete() ? " <success>complete" : "")));
        for (final ActiveObjective child : childObjectives(allObjectives, objective)) {
            renderActiveObjective(plugin, messages, allObjectives, child, level + 1);
        }
    }

    private static List<ActiveObjective> rootObjectives(final List<ActiveObjective> objectives) {
        return objectives.stream()
                .filter(objective -> objective.getObjectivePath().length == 1)
                .sorted(QuestProgressCommands::compareObjectivePaths)
                .toList();
    }

    private static List<ActiveObjective> childObjectives(
            final List<ActiveObjective> objectives,
            final ActiveObjective parent) {
        return objectives.stream()
                .filter(objective -> objective.isDirectChildOf(parent))
                .sorted(QuestProgressCommands::compareObjectivePaths)
                .toList();
    }

    private static int compareObjectivePaths(
            final ActiveObjective first,
            final ActiveObjective second) {
        final int[] firstPath = first.getObjectivePath();
        final int[] secondPath = second.getObjectivePath();
        final int length = Math.min(firstPath.length, secondPath.length);
        for (int i = 0; i < length; i++) {
            final int comparison = Integer.compare(firstPath[i], secondPath[i]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(firstPath.length, secondPath.length);
    }

    private static String playerDisplayName(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return "";
        }
        return questPlayer.playerName() == null || questPlayer.playerName().isBlank()
                ? questPlayer.playerIdentifier()
                : questPlayer.playerName();
    }

    private static Objective objectiveAt(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        if (objectivePath == null || objectivePath.length == 0) {
            return null;
        }
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return null;
        }
        Objective current = quest.getObjectiveFromID(objectivePath[0]);
        for (int i = 1; i < objectivePath.length && current != null; i++) {
            current = current.getObjectiveFromID(objectivePath[i]);
        }
        return current;
    }

    private static int[] parseObjectivePath(final String objectivePath) {
        if (objectivePath == null || objectivePath.isBlank()) {
            return new int[0];
        }
        final String[] tokens = objectivePath.split("\\.");
        final int[] parsed = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                parsed[i] = Integer.parseInt(tokens[i]);
            } catch (final NumberFormatException ignored) {
                return new int[0];
            }
        }
        return parsed;
    }

    private static String holderPath(final String questName, final int[] objectivePath) {
        if (questName == null || questName.isBlank() || objectivePath == null || objectivePath.length <= 1) {
            return questName == null ? "" : questName;
        }
        final StringBuilder builder = new StringBuilder(questName);
        for (int i = 0; i < objectivePath.length - 1; i++) {
            builder.append('.').append(objectivePath[i]);
        }
        return builder.toString();
    }

    private static Objectives.Type objectiveType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().objectives().stream()
                .filter(objective -> objective.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String objectiveDisplayNameOrIdentifier(
            final Objective objective,
            final Objectives.Type type) {
        if (objective == null) {
            return "unknown";
        }
        return objective.getDisplayName() == null || objective.getDisplayName().isBlank()
                ? (type == null || type.displayName() == null || type.displayName().isBlank()
                        ? objective.typeId()
                        : type.displayName())
                : objective.getDisplayName();
    }

    private static String objectiveTaskDescription(
            final Objective objective,
            final Objectives.Type type,
            final PlatformPlayer questPlayer,
            final ActiveObjective activeObjective,
            final boolean completed) {
        if (objective == null) {
            return "";
        }
        final String taskDescription;
        if (objective.getTaskDescription() != null && !objective.getTaskDescription().isBlank()) {
            taskDescription = objective.getTaskDescription();
        } else if (type != null && type.taskDescriptionRenderer() != null) {
            taskDescription = type.taskDescriptionRenderer().render(objective.data(), questPlayer, activeObjective);
        } else {
            taskDescription = "";
        }
        String rendered = wrapObjectiveTaskDescription(taskDescription);
        if (objective.getCompletionNPC() != null && !objective.getCompletionNPC().isBlank()) {
            rendered += "\n    <gray>To complete: Talk to NPC with ID <highlight>"
                    + objective.getCompletionNPC()
                    + " <red>[Currently not available]";
        }
        if (completed && !rendered.isBlank()) {
            return "<strikethrough>" + rendered + "</strikethrough>";
        }
        return rendered;
    }

    private static String wrapObjectiveTaskDescription(final String taskDescription) {
        if (taskDescription == null || taskDescription.isBlank()) {
            return "";
        }
        final String prefix = "    <veryUnimportant>└─ <unimportant>";
        if (taskDescription.startsWith(prefix)) {
            return taskDescription;
        }
        return prefix + taskDescription;
    }

    private static double progressNeeded(
            final NotQuestsPlugin plugin,
            final Objective objective) {
        if (objective == null) {
            return 1;
        }
        final Objectives.Type type = objectiveType(plugin, objective.typeId());
        if (type != null) {
            for (final RegistryField.Definition field : type.fields()) {
                if (field.progressNeeded()) {
                    final double fieldValue = progressNeededValue(objective.data().value(field.name()));
                    if (fieldValue > 0) {
                        return fieldValue;
                    }
                }
            }
        }
        return 1;
    }

    private static double progressNeededValue(final Object amount) {
        if (amount instanceof Number number) {
            return number.doubleValue();
        }
        if (amount != null) {
            try {
                return Double.parseDouble(amount.toString());
            } catch (final NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static String formatProgress(final double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
