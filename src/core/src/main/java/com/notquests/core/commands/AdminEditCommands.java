package com.notquests.core.commands;

import com.notquests.core.commands.framework.*;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class AdminEditCommands {
    public enum ConditionGroup {
        UNLOCK,
        PROGRESS,
        COMPLETE
    }

    public enum RewardTarget {
        QUEST,
        OBJECTIVE
    }

    public static final String ORDER = "order";
    public static final String WORLD = "world";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    public static final String COMPLETION_NPC = "Completion NPC";
    public static final String OBJECTIVE_DESCRIPTION = "Objective Description";
    public static final String TASK_DESCRIPTION = "Task Description";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String REQUIREMENT_ID = "Requirement ID";
    public static final String CONDITION_ID = "Condition ID";
    public static final String DESCRIPTION = "description";
    public static final String HIDDEN_STATUS_EXPRESSION = "hiddenStatusExpression";
    public static final String REWARD_ID = "reward-id";
    public static final String TRIGGER_ID = "trigger-id";

    private AdminEditCommands() {}

    public static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            commands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            editQuest,
                    final CommandManager commandManager) {
        final Builder builder = new Builder(commandManager);
        builder.objectiveLevel(editQuest.literal(
                "objectives",
                NQDescription.of("Manages objectives on this quest or parent objective."), "o"), 0);

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                level1Objectives = editQuest.literal(
                                "objectives",
                                NQDescription.of("Manages objectives on this quest or parent objective."), "o")
                        .literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific objective on the selected quest."))
                        .required(
                                "objectiveId",
                                NQArgumentType.integer("objective id"),
                                NQDescription.of("Objective ID shown by this quest's objectives list."),
                                (context, input) -> builder.objectiveIds(context, 0))
                        .literal(
                                "objectives",
                                NQDescription.of("Manages child objectives inside the selected objective."), "o");
        builder.objectiveLevel(level1Objectives, 1);

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                level2Objectives = level1Objectives
                        .literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a child objective inside the selected objective."))
                        .required(
                                "objectiveId2",
                                NQArgumentType.integer("child objective id"),
                                NQDescription.of("Child objective ID shown inside the selected parent objective."),
                                (context, input) -> builder.objectiveIds(context, 1))
                        .literal(
                                "objectives",
                                NQDescription.of("Manages child objectives inside the selected nested objective."), "o");
        builder.objectiveLevel(level2Objectives, 2);

        builder.requirements(editQuest.literal(
                "requirements",
                NQDescription.of("Manages requirements that must pass before the selected quest can be taken."), "req"));
        builder.questRewards(editQuest.literal(
                "rewards",
                NQDescription.of("Manages rewards granted by the selected quest."), "rew"));
        builder.triggers(editQuest.literal(
                "triggers",
                NQDescription.of("Manages triggers attached to this quest."), "t"));
        return List.copyOf(builder.commands);
    }

    private static final class Builder {
        private final CommandManager commandManager;
        private final List<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();

        private Builder(final CommandManager commandManager) {
            this.commandManager = commandManager;
        }

        private void objectiveLevel(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final int level) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    progressOrder = base.literal(
                    "predefinedProgressOrder",
                    NQDescription.of("Configures the required progress order for objectives in this branch."));
            add(progressOrder.literal("show", NQDescription.of("Shows the current objective progress order.")),
                    context -> blankThen(QuestObjectiveCommands.objectiveProgressOrder(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level))),
                    NQDescription.of("Shows the current predefined objective progress order."));
            add(progressOrder.literal("set", NQDescription.of("Changes the objective progress order."))
                            .literal("none", NQDescription.of("Clears this objective progress order.")),
                    context -> blankThen(QuestObjectiveCommands.clearObjectiveProgressOrder(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level))),
                    NQDescription.of("Removes the predefined objective progress order."));
            add(progressOrder.literal("set", NQDescription.of("Changes the objective progress order."))
                            .literal("firstToLast", NQDescription.of("Requires objectives to progress from first to last.")),
                    context -> blankThen(QuestObjectiveCommands.setObjectiveProgressOrder(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level), "firstToLast")),
                    NQDescription.of("Sets objective progress order to first-to-last."));
            add(progressOrder.literal("set", NQDescription.of("Changes the objective progress order."))
                            .literal("lastToFirst", NQDescription.of("Requires objectives to progress from last to first.")),
                    context -> blankThen(QuestObjectiveCommands.setObjectiveProgressOrder(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level), "lastToFirst")),
                    NQDescription.of("Sets objective progress order to last-to-first."));
            add(progressOrder.literal("set", NQDescription.of("Changes the objective progress order."))
                            .literal("custom", NQDescription.of("Uses a custom objective ID order."))
                            .required(ORDER, NQArgumentType.greedyString("objective order"),
                                    NQDescription.of("Objective IDs in the exact order players must progress them."),
                                    (context, input) -> objectiveIds(context, level)),
                    context -> blankThen(QuestObjectiveCommands.setObjectiveProgressOrder(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level), context.argument(ORDER))),
                    NQDescription.of("Sets a custom objective progress order."));

            add(base.literal("clear", NQDescription.of(level == 0
                            ? "Removes every objective from the selected quest."
                            : "Removes every child objective from the selected objective.")),
                    context -> blankThen(QuestObjectiveCommands.clearQuestObjectives(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level))),
                    NQDescription.of("Removes all objectives in this branch."));
            add(base.literal("list", NQDescription.of(level == 0
                            ? "Lists every objective on the selected quest."
                            : "Lists every child objective inside the selected objective."), "show"),
                    context -> QuestObjectiveCommands.listQuestObjectives(
                            commandManager.plugin(), questName(context), objectiveParentPath(context, level)),
                    NQDescription.of("Lists objectives in this branch."));

            final String objectiveIdName = level == 0
                    ? "objectiveId"
                    : "objectiveId" + (level + 1);
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    editObjective = base.literal("edit", level == 0
                                    ? NQDescription.of("Opens subcommands for editing a specific objective on the selected quest.")
                                    : NQDescription.of("Opens subcommands for editing a child objective inside the selected objective."))
                            .required(
                                    objectiveIdName,
                                    NQArgumentType.integer("objective id"),
                                    level == 0
                                            ? NQDescription.of("Objective ID shown by this quest's objectives list.")
                                            : NQDescription.of("Objective ID shown inside the selected parent objective."),
                                    (context, input) -> objectiveIds(context, level));
            editObjective(editObjective, level);
        }

        private void editObjective(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        objective,
                final int level) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    location = objective.literal("location", NQDescription.of("Manages the guiding marker shown while players track this objective."));
            add(location.literal("enable", NQDescription.of("Turns on this objective's saved guiding marker.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveLocationEnabled(
                            commandManager.plugin(), questName(context), objectivePath(context, level), true)),
                    NQDescription.of("Shows this objective's saved marker to players tracking the objective."));
            add(location.literal("disable", NQDescription.of("Stops showing this objective's saved guiding marker.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveLocationEnabled(
                            commandManager.plugin(), questName(context), objectivePath(context, level), false)),
                    NQDescription.of("Turns off the objective marker without deleting its saved coordinates."));
            add(location.literal("disables", NQDescription.of("Stops showing this objective's saved guiding marker.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveLocationEnabled(
                            commandManager.plugin(), questName(context), objectivePath(context, level), false)),
                    NQDescription.of("Turns off the objective marker without deleting its saved coordinates."));
            add(location.literal("set", NQDescription.of("Sets this objective's guiding marker location and turns it on."))
                            .literal("here", NQDescription.of("Uses your current in-game block position.")),
                    context -> currentLocationMarkerMessages(context, level),
                    NQDescription.of("Sets this objective's marker to your current location."));
            add(location.literal("set", NQDescription.of("Sets this objective's guiding marker location and turns it on."))
                            .literal("looking", NQDescription.of("Uses the block you are looking at.")),
                    context -> setObjectiveLocationLooking(context, level),
                    NQDescription.of("Sets this objective's marker to the targeted block."));
            add(location.literal("set", NQDescription.of("Sets this objective's guiding marker location and turns it on."))
                            .required(WORLD, NQArgumentType.world(), NQDescription.of("World where this objective marker should point."))
                            .required(X, NQArgumentType.integer("x coordinate"), NQDescription.of("X coordinate where this objective marker should point."))
                            .required(Y, NQArgumentType.integer("y coordinate"), NQDescription.of("Y coordinate where this objective marker should point."))
                            .required(Z, NQArgumentType.integer("z coordinate"), NQDescription.of("Z coordinate where this objective marker should point.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveLocation(
                            commandManager.plugin(),
                            questName(context),
                            objectivePath(context, level),
                            context.argument(WORLD),
                            parseDouble(context.argument(X), 0),
                            parseDouble(context.argument(Y), 0),
                            parseDouble(context.argument(Z), 0),
                            "exact coordinates")),
                    NQDescription.of("Sets this objective marker to exact coordinates."));
            add(location.literal("clear", NQDescription.of("Deletes this objective's saved guiding marker location.")),
                    context -> messages(QuestObjectiveCommands.clearObjectiveLocation(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Removes the saved objective marker location."));
            add(location.literal("status", NQDescription.of("Shows whether this objective has a saved guiding marker.")),
                    context -> messages(QuestObjectiveCommands.objectiveLocationStatus(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Shows the selected objective's marker status."));
            add(location.literal("preview", NQDescription.of("Shows this objective's saved guiding marker to you briefly.")),
                    context -> previewObjectiveLocation(context, level),
                    NQDescription.of("Previews this objective marker for the command sender."));

            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    completionNpc = objective.literal("completionNPC", NQDescription.of("Manages the NPC used to complete this objective."));
            add(completionNpc.literal("show", NQDescription.of("Shows the NPC players must click to complete this objective."), "view"),
                    context -> messages(QuestObjectiveCommands.objectiveCompletionNpc(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Shows the selected objective's completion NPC."));
            add(completionNpc.literal("set", NQDescription.of("Sets the NPC players must click to complete this objective."))
                            .required(COMPLETION_NPC, NQArgumentType.npcSelectorOrNone(),
                                    NQDescription.of("NPC players must click to complete this objective, or none.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveCompletionNpc(
                            commandManager.plugin(), commandManager.adapter(), context.questPlayer(),
                            questName(context), objectivePath(context, level), context.argument(COMPLETION_NPC))),
                    NQDescription.of("Sets the selected objective's completion NPC."));

            textProperty(objective, "description", "Objective description shown in lists, progress output, and GUIs.",
                    OBJECTIVE_DESCRIPTION, "New objective description. Supports spaces and MiniMessage formatting.",
                    context -> messages(QuestObjectiveCommands.objectiveDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.removeObjectiveDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.setObjectiveDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level),
                            context.argument(OBJECTIVE_DESCRIPTION))));
            textProperty(objective, "taskDescription", "Task text shown to players for this objective.",
                    TASK_DESCRIPTION, "New objective task text. Supports spaces and MiniMessage formatting.",
                    context -> messages(QuestObjectiveCommands.objectiveTaskDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.removeObjectiveTaskDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.setObjectiveTaskDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level),
                            context.argument(TASK_DESCRIPTION))));
            textProperty(objective, "displayName", "Objective display name shown in quest progress, objective lists, GUIs, and task messages.",
                    DISPLAY_NAME, "New objective display name. Supports spaces and MiniMessage formatting.",
                    context -> messages(QuestObjectiveCommands.objectiveDisplayName(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.removeObjectiveDisplayName(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    context -> messages(QuestObjectiveCommands.setObjectiveDisplayName(
                            commandManager.plugin(), questName(context), objectivePath(context, level),
                            context.argument(DISPLAY_NAME))));

            add(objective.literal("info", NQDescription.of("Shows detailed information about the selected objective.")),
                    context -> messages(QuestObjectiveCommands.questObjectiveInfo(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Shows detailed information about this objective."));
            add(objective.literal("remove", NQDescription.of("Removes the selected objective from its quest or parent objective."), "delete"),
                    context -> messages(QuestObjectiveCommands.removeQuestObjective(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Removes the selected objective."));

            conditions(objective.literal("conditions", NQDescription.of("Manages conditions attached to the selected objective.")), level);
            objectiveRewards(objective.literal("rewards", NQDescription.of("Manages rewards granted by the selected objective.")), level);
        }

        private void conditions(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final int level) {
            conditionGroup(base.literal("unlock", NQDescription.of("Configures conditions required before the objective can unlock.")),
                    AdminEditCommands.ConditionGroup.UNLOCK, level);
            conditionGroup(base.literal("progress", NQDescription.of("Configures conditions required while the objective is progressing.")),
                    AdminEditCommands.ConditionGroup.PROGRESS, level);
            conditionGroup(base.literal("complete", NQDescription.of("Configures conditions required before the objective can complete.")),
                    AdminEditCommands.ConditionGroup.COMPLETE, level);
        }

        private void conditionGroup(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final AdminEditCommands.ConditionGroup group,
                final int level) {
            add(base.literal("clear", NQDescription.of("Removes every condition in this condition group.")),
                    context -> messages(QuestObjectiveCommands.clearObjectiveConditions(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group))),
                    NQDescription.of("Removes all conditions in this group."));
            add(base.literal("list", NQDescription.of("Lists every condition in this condition group."), "show"),
                    context -> QuestObjectiveCommands.listObjectiveConditions(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group)),
                    NQDescription.of("Lists conditions in this group."));
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    edit = base.literal("edit", NQDescription.of("Opens subcommands for editing a specific condition in this group."))
                            .required(CONDITION_ID, NQArgumentType.integer("condition id"),
                                    NQDescription.of("Condition ID shown by this condition group's list."),
                                    (context, input) -> objectiveConditionIds(context, group, level));
            add(edit.literal("delete", NQDescription.of("Removes the selected condition from this objective."), "remove"),
                    context -> messages(QuestObjectiveCommands.removeObjectiveCondition(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group), conditionId(context))),
                    NQDescription.of("Removes the selected condition."));
            textProperty(edit, "description", "Description shown when listing this objective condition.",
                    DESCRIPTION, "New condition description. Supports spaces and MiniMessage formatting.",
                    context -> messages(QuestObjectiveCommands.objectiveConditionDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group), conditionId(context))),
                    context -> messages(QuestObjectiveCommands.removeObjectiveConditionDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group), conditionId(context))),
                    context -> messages(QuestObjectiveCommands.setObjectiveConditionDescription(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group), conditionId(context),
                            context.argument(DESCRIPTION))));
            add(edit.literal("hidden", NQDescription.of("Controls whether this condition is hidden from players."))
                            .literal("set", NQDescription.of("Changes whether this condition is hidden from players."))
                            .required(HIDDEN_STATUS_EXPRESSION, NQArgumentType.word("hidden status expression"),
                                    NQDescription.of("Boolean expression that decides whether this condition is hidden.")),
                    context -> messages(QuestObjectiveCommands.setObjectiveConditionHidden(
                            commandManager.plugin(), questName(context), objectivePath(context, level), conditionGroup(group), conditionId(context),
                            context.argument(HIDDEN_STATUS_EXPRESSION))),
                    NQDescription.of("Sets the selected condition's hidden status."));
        }

        private void requirements(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base) {
            add(base.literal("list", NQDescription.of("Lists every requirement on the selected quest."), "show"),
                    context -> QuestRequirementCommands.listQuestRequirements(commandManager.plugin(), questName(context)),
                    NQDescription.of("Lists all requirements on this quest."));
            add(base.literal("clear", NQDescription.of("Removes every requirement from the selected quest.")),
                    context -> messages(QuestRequirementCommands.clearQuestRequirements(
                            commandManager.plugin(), questName(context))),
                    NQDescription.of("Clears all requirements on this quest."));
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    edit = base.literal("edit", NQDescription.of("Opens subcommands for editing a specific quest requirement."))
                            .required(REQUIREMENT_ID, NQArgumentType.integer("requirement id"),
                                    NQDescription.of("Requirement ID shown by this quest's requirements list."),
                                    (context, input) -> requirementIds(context));
            add(edit.literal("delete", NQDescription.of("Removes the selected requirement from the quest."), "remove"),
                    context -> messages(QuestRequirementCommands.removeQuestRequirement(
                            commandManager.plugin(), questName(context), requirementId(context))),
                    NQDescription.of("Removes the selected requirement."));
            textProperty(edit, "description", "Requirement description shown when listing this quest's requirements.",
                    DESCRIPTION, "New requirement description. Supports spaces and MiniMessage formatting.",
                    context -> messages(QuestRequirementCommands.requirementDescription(
                            commandManager.plugin(), questName(context), requirementId(context))),
                    context -> messages(QuestRequirementCommands.removeRequirementDescription(
                            commandManager.plugin(), questName(context), requirementId(context))),
                    context -> messages(QuestRequirementCommands.setRequirementDescription(
                            commandManager.plugin(), questName(context), requirementId(context), context.argument(DESCRIPTION))));
            add(edit.literal("hidden", NQDescription.of("Controls whether the selected requirement is hidden from players."))
                            .literal("set", NQDescription.of("Changes whether the selected requirement is hidden from players."))
                            .required(HIDDEN_STATUS_EXPRESSION, NQArgumentType.word("hidden status expression"),
                                    NQDescription.of("Boolean expression that decides whether this requirement is hidden.")),
                    context -> messages(QuestRequirementCommands.setRequirementHidden(
                            commandManager.plugin(), questName(context), requirementId(context),
                            context.argument(HIDDEN_STATUS_EXPRESSION))),
                    NQDescription.of("Sets the selected requirement's hidden status."));
        }

        private void questRewards(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base) {
            add(base.literal("list", NQDescription.of("Lists every reward granted by the selected quest."), "show"),
                    context -> QuestRewardCommands.listQuestRewards(commandManager.plugin(), questName(context)),
                    NQDescription.of("Lists all rewards granted by this quest."));
            add(base.literal("clear", NQDescription.of("Removes every reward from the selected quest.")),
                    context -> messages(QuestRewardCommands.clearQuestRewards(commandManager.plugin(), questName(context))),
                    NQDescription.of("Clears all rewards granted by this quest."));
            rewardEdit(base, AdminEditCommands.RewardTarget.QUEST, 0);
        }

        private void objectiveRewards(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final int level) {
            add(base.literal("list", NQDescription.of("Lists every reward granted by the selected objective."), "show"),
                    context -> QuestObjectiveCommands.listObjectiveRewards(
                            commandManager.plugin(), questName(context), objectivePath(context, level)),
                    NQDescription.of("Lists all rewards granted by this objective."));
            add(base.literal("clear", NQDescription.of("Removes every reward from the selected objective.")),
                    context -> messages(QuestObjectiveCommands.clearObjectiveRewards(
                            commandManager.plugin(), questName(context), objectivePath(context, level))),
                    NQDescription.of("Clears all rewards granted by this objective."));
            rewardEdit(base, AdminEditCommands.RewardTarget.OBJECTIVE, level);
        }

        private void rewardEdit(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    edit = base.literal("edit", NQDescription.of("Opens subcommands for editing a specific reward."))
                            .required(REWARD_ID, NQArgumentType.integer("reward id"),
                                    NQDescription.of("Reward ID shown by the reward list."),
                                    (context, input) -> rewardIds(context, target, level));
            add(edit.literal("info", NQDescription.of("Shows detailed information about the selected reward.")),
                    context -> blankThen(rewardInfoMessage(context, target, level)),
                    NQDescription.of("Shows detailed information about this reward."));
            add(edit.literal("remove", NQDescription.of("Removes the selected reward."), "delete"),
                    context -> blankThen(removeRewardMessage(context, target, level)),
                    NQDescription.of("Removes the selected reward."));
            textProperty(edit, "displayName", "Reward display name shown in reward previews and reward lists.",
                    DISPLAY_NAME, "New reward display name. Supports spaces and MiniMessage formatting.",
                    context -> blankThen(rewardDisplayNameMessage(context, target, level)),
                    context -> blankThen(removeRewardDisplayNameMessage(context, target, level)),
                    context -> blankThen(setRewardDisplayNameMessage(context, target, level)));
        }

        private CommandMessage rewardInfoMessage(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.objectiveRewardInfo(
                        commandManager.plugin(), questName(context), objectivePath(context, level), rewardId(context));
            }
            return QuestRewardCommands.questRewardInfo(commandManager.plugin(), questName(context), rewardId(context));
        }

        private CommandMessage removeRewardMessage(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.removeObjectiveReward(
                        commandManager.plugin(), questName(context), objectivePath(context, level), rewardId(context));
            }
            return QuestRewardCommands.removeQuestReward(commandManager.plugin(), questName(context), rewardId(context));
        }

        private CommandMessage rewardDisplayNameMessage(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.objectiveRewardDisplayName(
                        commandManager.plugin(), questName(context), objectivePath(context, level), rewardId(context));
            }
            return QuestRewardCommands.questRewardDisplayName(
                    commandManager.plugin(), questName(context), rewardId(context));
        }

        private CommandMessage removeRewardDisplayNameMessage(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.removeObjectiveRewardDisplayName(
                        commandManager.plugin(), questName(context), objectivePath(context, level), rewardId(context));
            }
            return QuestRewardCommands.removeQuestRewardDisplayName(
                    commandManager.plugin(), questName(context), rewardId(context));
        }

        private CommandMessage setRewardDisplayNameMessage(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.setObjectiveRewardDisplayName(
                        commandManager.plugin(), questName(context), objectivePath(context, level), rewardId(context),
                        context.argument(DISPLAY_NAME));
            }
            return QuestRewardCommands.setQuestRewardDisplayName(
                    commandManager.plugin(), questName(context), rewardId(context), context.argument(DISPLAY_NAME));
        }

        private void triggers(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base) {
            add(base.literal("clear", NQDescription.of("Removes every trigger from the selected quest.")),
                    context -> messages(TriggerCommands.clearQuestTriggers(commandManager.plugin(), questName(context))),
                    NQDescription.of("Clears all triggers on this quest."));
            add(base.literal("list", NQDescription.of("Lists every trigger attached to the selected quest."), "show"),
                    context -> TriggerCommands.listQuestTriggers(commandManager.plugin(), questName(context)),
                    NQDescription.of("Lists all triggers attached to this quest."));
            add(base.literal("remove", NQDescription.of("Removes the selected trigger from the quest."), "delete")
                            .required(TRIGGER_ID, NQArgumentType.integer("trigger id"),
                                    NQDescription.of("Trigger ID shown by this quest's trigger list."),
                                    (context, input) -> triggerIds(context)),
                    context -> messages(TriggerCommands.removeQuestTrigger(
                            commandManager.plugin(), questName(context), triggerId(context))),
                    NQDescription.of("Removes the selected trigger."));
        }

        private void textProperty(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        base,
                final String branch,
                final String branchDescription,
                final String argumentName,
                final String argumentDescription,
                final NQCommandHandler show,
                final NQCommandHandler remove,
                final NQCommandHandler set) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    property = base.literal(branch, NQDescription.of(branchDescription + " Supports MiniMessage formatting."));
            add(property.literal("show", NQDescription.of("Shows the current text."), "check"), show,
                    NQDescription.of("Shows the current text."));
            add(property.literal("remove", NQDescription.of("Removes the custom text."), "delete"), remove,
                    NQDescription.of("Removes the custom text."));
            add(property.literal("set", NQDescription.of("Sets the text."))
                            .required(argumentName, NQArgumentType.greedyString(argumentName), NQDescription.of(argumentDescription)),
                    set,
                    NQDescription.of("Sets the text."));
        }

        private void add(
                final NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        builder,
                final NQCommandHandler handler,
                final NQDescription commandDescription) {
            commands.add(builder.commandDescription(commandDescription)
                    .handler(handler)
                    .registration());
        }

        private static List<CommandMessage> messages(
                final CommandMessage message) {
            return List.of(message);
        }

        private static List<CommandMessage> blankThen(
                final CommandMessage message) {
            return List.of(CommandMessage.emptyLine(), message);
        }

        private static String questName(final NQCommandContext context) {
            return context.argument("quest");
        }

        private static int objectiveId(final NQCommandContext context, final int level) {
            final String argumentName = level == 0
                    ? "objectiveId"
                    : "objectiveId" + (level + 1);
            return parseInt(context.argument(argumentName), 1);
        }

        private static int[] objectiveParentPath(final NQCommandContext context, final int level) {
            if (level <= 0) {
                return new int[0];
            }
            final int[] path = new int[level];
            for (int i = 0; i < level; i++) {
                path[i] = objectiveId(context, i);
            }
            return path;
        }

        private static int[] objectivePath(final NQCommandContext context, final int level) {
            final int[] path = new int[level + 1];
            for (int i = 0; i <= level; i++) {
                path[i] = objectiveId(context, i);
            }
            return path;
        }

        private static String objectivePathDescription(final int[] objectivePath) {
            if (objectivePath == null || objectivePath.length == 0) {
                return "";
            }
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < objectivePath.length; i++) {
                if (i > 0) {
                    builder.append('/');
                }
                builder.append(objectivePath[i]);
            }
            return builder.toString();
        }

        private static int requirementId(final NQCommandContext context) {
            return parseInt(context.argument(REQUIREMENT_ID), 1);
        }

        private static int conditionId(final NQCommandContext context) {
            return parseInt(context.argument(CONDITION_ID), 1);
        }

        private static int rewardId(final NQCommandContext context) {
            return parseInt(context.argument(REWARD_ID), 1);
        }

        private static int triggerId(final NQCommandContext context) {
            return parseInt(context.argument(TRIGGER_ID), 1);
        }

        private static String conditionGroup(final AdminEditCommands.ConditionGroup group) {
            return switch (group) {
                case UNLOCK -> "unlock";
                case PROGRESS -> "progress";
                case COMPLETE -> "complete";
            };
        }

        private List<String> objectiveIds(final NQCommandContext context, final int level) {
            return commandManager.questObjectiveIds(questName(context), objectiveParentPath(context, level));
        }

        private List<String> requirementIds(final NQCommandContext context) {
            return commandManager.questRequirementIds(questName(context));
        }

        private List<String> objectiveConditionIds(
                final NQCommandContext context,
                final AdminEditCommands.ConditionGroup group,
                final int level) {
            return QuestObjectiveCommands.objectiveConditionIds(
                    commandManager.plugin(),
                    questName(context),
                    objectivePath(context, level),
                    conditionGroup(group));
        }

        private List<String> rewardIds(
                final NQCommandContext context,
                final AdminEditCommands.RewardTarget target,
                final int level) {
            if (target == AdminEditCommands.RewardTarget.OBJECTIVE) {
                return QuestObjectiveCommands.objectiveRewardIds(
                        commandManager.plugin(), questName(context), objectivePath(context, level));
            }
            return commandManager.questRewardIds(questName(context));
        }

        private List<String> triggerIds(final NQCommandContext context) {
            return commandManager.questTriggerIds(questName(context));
        }

        private List<CommandMessage> currentLocationMarkerMessages(
                final NQCommandContext context,
                final int level) {
            final PlatformPlayer player = player(context);
            if (player == null) {
                return playerOnly("location set here");
            }
            return messages(QuestObjectiveCommands.setObjectiveLocation(
                    commandManager.plugin(),
                    questName(context),
                    objectivePath(context, level),
                    player.worldName(),
                    Math.floor(player.positionX()),
                    Math.floor(player.positionY()),
                    Math.floor(player.positionZ()),
                    "your current location"));
        }

        private List<CommandMessage> setObjectiveLocationLooking(
                final NQCommandContext context,
                final int level) {
            final PlatformPlayer player = player(context);
            if (player == null) {
                return playerOnly("location set looking");
            }
            final NQLocation location = player.lookingAtBlock(120);
            if (location == null) {
                return messages(CommandMessage.error(
                        "<error>No block found in your line of sight. Move closer or use <highlight>location set here</highlight>."));
            }
            return messages(QuestObjectiveCommands.setObjectiveLocation(
                    commandManager.plugin(),
                    questName(context),
                    objectivePath(context, level),
                    location.worldName(),
                    location.x(),
                    location.y(),
                    location.z(),
                    "the block you are looking at"));
        }

        private List<CommandMessage> previewObjectiveLocation(
                final NQCommandContext context,
                final int level) {
            final int[] objectivePath = objectivePath(context, level);
            final NQLocation location = QuestObjectiveCommands.objectiveLocation(
                    commandManager.plugin(), questName(context), objectivePath);
            if (location == null) {
                return messages(CommandMessage.error(
                        "<error>This objective has no marker location yet. Use <highlight>location set here</highlight> first."));
            }
            final PlatformPlayer player = player(context);
            if (player == null) {
                return playerOnly("location preview");
            }
            final String path = objectivePathDescription(objectivePath);
            final String markerName = "objective-preview-" + questName(context) + "-" + path;
            if (!commandManager.plugin().showTemporaryObjectiveMarker(
                    player, markerName, location, Duration.ofSeconds(10))) {
                return messages(CommandMessage.error(
                        "Could not preview the objective marker for this player."));
            }
            return messages(CommandMessage.success(
                    "<success>Previewing objective <highlight>" + path
                            + "</highlight>'s guiding marker for 10 seconds."));
        }

        private static PlatformPlayer player(final NQCommandContext context) {
            final PlatformPlayer player = context.questPlayer();
            return player != null && player.hasPlayer() ? player : null;
        }

        private static List<CommandMessage> playerOnly(final String commandName) {
            return messages(CommandMessage.error(
                    "<error><highlight>" + commandName
                            + "</highlight> must be run in-game by a player. Console can use exact coordinates instead."));
        }

        private static int parseInt(final String value, final int fallback) {
            try {
                return Integer.parseInt(value);
            } catch (final Exception exception) {
                return fallback;
            }
        }

        private static double parseDouble(final String value, final double fallback) {
            try {
                return Double.parseDouble(value);
            } catch (final Exception exception) {
                return fallback;
            }
        }
    }
}
