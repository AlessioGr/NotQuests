package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class QuestObjectiveCommands {
    private QuestObjectiveCommands() {}

    static CommandMessage addQuestObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String objectiveTypeId,
            final String rawArguments,
            final String taskDescription) {
        return addQuestObjective(
                plugin, adapter, questName, new int[0], objectiveTypeId, rawArguments, taskDescription, null);
    }

    static CommandMessage addQuestObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String objectiveTypeId,
            final String rawArguments,
            final String taskDescription,
            final PlatformPlayer questPlayer) {
        return addQuestObjective(
                plugin, adapter, questName, new int[0], objectiveTypeId, rawArguments, taskDescription, questPlayer);
    }

    static CommandMessage addQuestObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] parentPath,
            final String objectiveTypeId,
            final String rawArguments,
            final String taskDescription) {
        return addQuestObjective(
                plugin,
                adapter,
                questName,
                parentPath,
                objectiveTypeId,
                rawArguments,
                taskDescription,
                null);
    }

    static CommandMessage addQuestObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] parentPath,
            final String objectiveTypeId,
            final String rawArguments,
            final String taskDescription,
            final PlatformPlayer questPlayer) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Objectives.Type type = objectiveType(plugin, objectiveTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests objective type: " + highlight(objectiveTypeId) + ".");
        }
        final Objective parent = objectiveAt(plugin, questName, parentPath);
        if (parentPath != null && parentPath.length > 0 && parent == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(parentPath)) + " was not found!");
        }
        try {
            final Objective data = Objectives.parse(
                    adapter, type, rawArguments, questPlayer);
            final Objective entry = parent == null
                    ? quest.addObjective(type.id(), data, taskDescription)
                    : parent.addChildObjective(type.id(), data, taskDescription);
            plugin.saveData();
            return CommandMessage.success("<success>" + type.id() + " Objective successfully added to "
                    + (parent == null
                            ? "Quest " + highlight(questName)
                            : "Quest " + highlight(objectiveDisplayNameOrIdentifier(parent)))
                    + "!");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add NotQuests objective: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listQuestObjectives(
            final NotQuestsPlugin plugin,
            final String questName) {
        return listQuestObjectives(plugin, questName, new int[0]);
    }

    static List<CommandMessage> listQuestObjectives(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] parentPath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(missingQuest(questName));
        }
        final Objective parent = objectiveAt(plugin, questName, parentPath);
        if (parentPath != null && parentPath.length > 0 && parent == null) {
            return List.of(CommandMessage.error("Objective with the path " + highlight(objectivePath(parentPath)) + " was not found!"));
        }
        final List<Objective> objectives = parent == null
                ? quest.getObjectives()
                : parent.getObjectives();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.emptyLine());
        messages.add(CommandMessage.success("<highlight>Objectives for "
                + (parent == null ? "Quest " + highlight2(questName) : "objective " + highlight2(objectivePath(parentPath)))
                + ":</highlight>"));
        for (final Objective objective : objectives) {
            messages.add(CommandMessage.success("<highlight>" + objective.id() + ".</highlight> <main>"
                    + objectiveDisplayNameOrIdentifier(objective)));
            if (objective.getDescription() != null && !objective.getDescription().isBlank()) {
                messages.add(CommandMessage.success("   <highlight>Description:</highlight> <main>"
                        + objective.getDescription()));
            }
            addObjectiveListConditions(plugin, messages, "Unlock", objective.getConditions("unlock"), null);
            addObjectiveListConditions(plugin, messages, "Progress", objective.getConditions("progress"), null);
            addObjectiveListConditions(plugin, messages, "Complete", objective.getConditions("complete"), null);
            messages.add(CommandMessage.success(objectiveContentDescription(
                    objective, objectiveType(plugin, objective.typeId()))));
        }
        return List.copyOf(messages);
    }

    static CommandMessage clearQuestObjectives(final NotQuestsPlugin plugin, final String questName) {
        return clearQuestObjectives(plugin, questName, new int[0]);
    }

    static CommandMessage clearQuestObjectives(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] parentPath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Objective parent = objectiveAt(plugin, questName, parentPath);
        if (parentPath != null && parentPath.length > 0 && parent == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(parentPath)) + " was not found!");
        }
        if (parent == null) {
            quest.clearObjectives();
        } else {
            parent.clearChildObjectives();
        }
        plugin.saveData();
        return CommandMessage.success("<success>All objectives of "
                + (parent == null ? "Quest " + highlight(questName) : "objective " + highlight(objectivePath(parentPath)))
                + " have been removed!");
    }

    static CommandMessage removeQuestObjective(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId) {
        return removeQuestObjective(plugin, questName, new int[] {objectiveId});
    }

    static CommandMessage removeQuestObjective(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final boolean removed;
        if (objectivePath == null || objectivePath.length <= 1) {
            removed = quest.removeObjective(objectivePath == null || objectivePath.length == 0 ? 0 : objectivePath[0]);
        } else {
            final int[] parentPath = Arrays.copyOf(objectivePath, objectivePath.length - 1);
            final Objective parent = objectiveAt(plugin, questName, parentPath);
            removed = parent != null && parent.removeChildObjective(objectivePath[objectivePath.length - 1]);
        }
        if (!removed) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        plugin.saveData();
        return CommandMessage.success("<success>Objective with the ID "
                + highlight(objectiveLabel(objectivePath))
                + " has been successfully removed from Quest " + highlight2(questName) + "!");
    }

    static CommandMessage questObjectiveInfo(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId) {
        return questObjectiveInfo(plugin, questName, new int[] {objectiveId});
    }

    static CommandMessage questObjectiveInfo(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        final Objectives.Type type = objectiveType(plugin, objective.typeId());
        return CommandMessage.success("<highlight>Information of objective with the ID "
                + highlight2(objectiveLabel(objectivePath)) + " from Quest " + highlight2(questName) + ":\n"
                + "<highlight>Objective Type: <main>" + objective.typeId() + "\n"
                + "<highlight>Objective Content:</highlight>\n" + objectiveContentDescription(objective, type) + "\n"
                + "<highlight>Objective DisplayName: <main>" + blankDefault(objective.getDisplayName(), "none") + "\n"
                + "<highlight>Objective Description: <main>" + blankDefault(objective.getDescription(), "none") + "\n"
                + "<highlight>Task Description: <main>" + blankDefault(objective.getTaskDescription(), "none") + "\n"
                + "<highlight>Unlock Conditions: <main>" + objective.getConditions("unlock").size() + "\n"
                + "<highlight>Progress Conditions: <main>" + objective.getConditions("progress").size() + "\n"
                + "<highlight>Complete Conditions: <main>" + objective.getConditions("complete").size());
    }

    static CommandMessage objectiveDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return objectiveText(plugin, questName, objectivePath, ObjectiveTextField.DESCRIPTION);
    }

    static CommandMessage setObjectiveDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String description) {
        return setObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.DESCRIPTION, description);
    }

    static CommandMessage removeObjectiveDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return removeObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.DESCRIPTION);
    }

    static CommandMessage objectiveTaskDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return objectiveText(plugin, questName, objectivePath, ObjectiveTextField.TASK_DESCRIPTION);
    }

    static CommandMessage setObjectiveTaskDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String taskDescription) {
        return setObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.TASK_DESCRIPTION, taskDescription);
    }

    static CommandMessage removeObjectiveTaskDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return removeObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.TASK_DESCRIPTION);
    }

    static CommandMessage objectiveDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return objectiveText(plugin, questName, objectivePath, ObjectiveTextField.DISPLAY_NAME);
    }

    static CommandMessage setObjectiveDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String displayName) {
        return setObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.DISPLAY_NAME, displayName);
    }

    static CommandMessage removeObjectiveDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        return removeObjectiveText(plugin, questName, objectivePath, ObjectiveTextField.DISPLAY_NAME);
    }

    static CommandMessage objectiveCompletionNpc(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        return CommandMessage.success("<main>The completionNPCID of the objective with the ID "
                + highlight(objectivePath(objectivePath)) + " is " + highlight2(blankDefault(objective.getCompletionNPC(), "none")));
    }

    static CommandMessage setObjectiveCompletionNpc(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor,
            final String questName,
            final int[] objectivePath,
            final String completionNpc) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        if ("none".equalsIgnoreCase(completionNpc)) {
            objective.setCompletionNpc("");
            plugin.saveData();
            return CommandMessage.success("<success>The completionNPC of the objective with the ID "
                    + highlight(objectivePath(objectivePath)) + " has been removed!");
        }
        if ("rightClickSelect".equalsIgnoreCase(completionNpc)) {
            if (actor == null || !actor.hasPlayer()) {
                return CommandMessage.error("<error>Error: this command can only be run as a player.");
            }
            final boolean started = plugin.startNpcSelection(
                    adapter,
                    actor,
                    "<success>You have been given an item with which you can add the completionNPC of this Objective to an NPC. Check your inventory!",
                    "<LIGHT_PURPLE>Set completionNPC of Quest <highlight>" + questName + "</highlight> to this NPC",
                    List.of("<WHITE>Right-click an NPC to set it as the completionNPC of Quest <highlight>"
                            + questName + "</highlight> and ObjectiveID <highlight>"
                            + objectiveLabel(objectivePath) + "</highlight>."),
                    selection -> {
                        objective.setCompletionNpc(selection.selector());
                        plugin.saveData();
                        actor.sendMessage("<success>The completionArmorStandUUID of the objective with the ID <highlight>"
                                + objectiveLabel(objectivePath)
                                + "</highlight> has been set to the NPC with the ID <highlight2>"
                                + blankDefault(selection.selector(), "unknown")
                                + "</highlight2> and name <highlight2>"
                                + blankDefault(selection.label(), "todo")
                                + "</highlight2>!");
                    });
            if (!started) {
                return CommandMessage.error(
                        "Could not start NPC selection for objective " + highlight(objectiveLabel(objectivePath)) + ".");
            }
            return CommandMessage.none();
        }
        objective.setCompletionNpc(completionNpc);
        plugin.saveData();
        return CommandMessage.success("<success>The completionNPC of the objective with the ID "
                + highlight(objectivePath(objectivePath)) + " has been set to "
                + highlight2(blankDefault(completionNpc, "none")) + "!");
    }

    static CommandMessage objectiveProgressOrder(final NotQuestsPlugin plugin, final String questName) {
        return objectiveProgressOrder(plugin, questName, new int[0]);
    }

    static CommandMessage objectiveProgressOrder(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Objective holder = objectiveAt(plugin, questName, objectivePath);
        if (objectivePath != null && objectivePath.length > 0 && holder == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        final String holderIdentifier = holder == null ? questName : objectiveDisplayNameOrIdentifier(holder);
        final String order = holder == null ? quest.getObjectiveProgressOrder() : holder.getChildObjectiveProgressOrder();
        return CommandMessage.success("<success>Current predefined progress order of Quest "
                + highlight(holderIdentifier) + ": " + highlight2(blankDefault(order, "None")));
    }

    static CommandMessage setObjectiveProgressOrder(
            final NotQuestsPlugin plugin,
            final String questName,
            final String order) {
        return setObjectiveProgressOrder(plugin, questName, new int[0], order);
    }

    static CommandMessage setObjectiveProgressOrder(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String order) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Objective holder = objectiveAt(plugin, questName, objectivePath);
        if (objectivePath != null && objectivePath.length > 0 && holder == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        final String holderIdentifier = holder == null ? questName : objectiveDisplayNameOrIdentifier(holder);
        if (holder == null) {
            quest.setObjectiveProgressOrder(order);
        } else {
            holder.setChildObjectiveProgressOrder(order);
        }
        final String message = switch (order == null ? "" : order.toLowerCase(Locale.ROOT)) {
            case "firsttolast" -> "<success>Predefined progress order of Quest "
                    + highlight(holderIdentifier) + " have been set to first to last!";
            case "lasttofirst" -> "<success>Predefined progress order of Quest "
                    + highlight(holderIdentifier) + " have been set to last to first!";
            default -> "<success>Predefined progress order of Quest " + highlight(holderIdentifier)
                    + " have been set to custom with this order: " + blankDefault(order, "none");
        };
        plugin.saveData();
        return CommandMessage.success(message);
    }

    static CommandMessage clearObjectiveProgressOrder(final NotQuestsPlugin plugin, final String questName) {
        return clearObjectiveProgressOrder(plugin, questName, new int[0]);
    }

    static CommandMessage clearObjectiveProgressOrder(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Objective holder = objectiveAt(plugin, questName, objectivePath);
        if (objectivePath != null && objectivePath.length > 0 && holder == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        final String holderIdentifier = holder == null ? questName : objectiveDisplayNameOrIdentifier(holder);
        if (holder == null) {
            quest.clearObjectiveProgressOrder();
        } else {
            holder.clearChildObjectiveProgressOrder();
        }
        plugin.saveData();
        return CommandMessage.success("<success>Predefined progress order of Quest "
                + highlight(holderIdentifier) + " have been removed!");
    }

    static CommandMessage setObjectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId,
            final String world,
            final double x,
            final double y,
            final double z) {
        return setObjectiveLocation(plugin, questName, new int[] {objectiveId}, world, x, y, z);
    }

    static CommandMessage setObjectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String world,
            final double x,
            final double y,
            final double z) {
        return setObjectiveLocation(plugin, questName, objectivePath, world, x, y, z, null);
    }

    static CommandMessage setObjectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String world,
            final double x,
            final double y,
            final double z,
            final String sourceDescription) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        objective.setLocation(NQLocation.at(world, x, y, z));
        objective.setLocationEnabled(true);
        final String sourceText = sourceDescription == null || sourceDescription.isBlank()
                ? ""
                : " using " + highlight(sourceDescription);
        plugin.saveData();
        return CommandMessage.success("<success>Objective " + highlight(objectiveLabel(objectivePath))
                + " now points to " + highlight2(formatLocation(world, x, y, z)) + sourceText + ".");
    }

    static NQLocation objectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId) {
        return objectiveLocation(plugin, questName, new int[] {objectiveId});
    }

    static NQLocation objectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        return objective == null ? null : objective.getLocation();
    }

    static CommandMessage objectiveLocationStatus(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId) {
        return objectiveLocationStatus(plugin, questName, new int[] {objectiveId});
    }

    static CommandMessage objectiveLocationStatus(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        if (objective.getLocation() == null) {
            return CommandMessage.success("<main>Objective " + highlight(objectiveLabel(objectivePath))
                    + " has no saved guiding marker location.");
        }
        final String markerState = objective.isLocationEnabled() ? "<success>enabled</success>" : "<warn>disabled</warn>";
        return CommandMessage.success("<main>Objective " + highlight(objectiveLabel(objectivePath))
                + " marker is " + markerState + "<main> at " + highlight2(formatLocation(objective.getLocation())) + ".");
    }

    static CommandMessage setObjectiveLocationEnabled(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId,
            final boolean enabled) {
        return setObjectiveLocationEnabled(plugin, questName, new int[] {objectiveId}, enabled);
    }

    static CommandMessage setObjectiveLocationEnabled(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final boolean enabled) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        if (objective.getLocation() == null) {
            if (enabled) {
                return CommandMessage.error("<error>This objective has no marker location yet. Use <highlight>location set here</highlight> in-game while editing this objective, or set exact coordinates from console.");
            }
            return CommandMessage.success("<success>Objective " + highlight(objectiveLabel(objectivePath))
                    + " has no saved guiding marker location, so there is nothing to show.");
        }
        objective.setLocationEnabled(enabled);
        plugin.saveData();
        return CommandMessage.success(enabled
                ? "<success>Objective " + highlight(objectiveLabel(objectivePath)) + " now shows its guiding marker at "
                        + highlight2(formatLocation(objective.getLocation())) + "."
                : "<success>Objective " + highlight(objectiveLabel(objectivePath))
                        + " keeps its saved marker location, but no longer shows it to players.");
    }

    static CommandMessage clearObjectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int objectiveId) {
        return clearObjectiveLocation(plugin, questName, new int[] {objectiveId});
    }

    static CommandMessage clearObjectiveLocation(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
        }
        objective.setLocation(null);
        objective.setLocationEnabled(false);
        plugin.saveData();
        return CommandMessage.success("<success>Objective " + highlight(objectiveLabel(objectivePath))
                + " no longer has a saved guiding marker location.");
    }

    static CommandMessage addObjectiveCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] objectivePath,
            final String group,
            final String conditionTypeId,
            final String rawArguments) {
        return addObjectiveCondition(
                plugin, adapter, questName, objectivePath, group, conditionTypeId, rawArguments, null);
    }

    static CommandMessage addObjectiveCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] objectivePath,
            final String group,
            final String conditionTypeId,
            final String rawArguments,
            final PlatformPlayer questPlayer) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        final Conditions.Type type = conditionType(plugin, conditionTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests condition type: " + highlight(conditionTypeId) + ".");
        }
        try {
            final Condition data = Conditions.parse(
                    adapter, type, rawArguments, questPlayer);
            applySharedConditionFlags(data, rawArguments);
            final CommandMessage validation = validateConditionData(type, data, objectivePath);
            if (validation != null) {
                return validation;
            }
            objective.addCondition(group, type.id(), data);
            plugin.saveData();
            return CommandMessage.success("<success>" + type.id() + " " + objectiveConditionAddLabel(group)
                    + " successfully added to Objective " + highlight(objectiveDisplayNameOrIdentifier(objective)) + "!");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add objective condition: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listObjectiveConditions(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return List.of(missingObjective(objectivePath));
        }
        final List<Condition> conditions = objective.getConditions(group);
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        final String label = objectiveConditionLabel(group);
        messages.add(CommandMessage.success("<highlight>" + label + " of objective with ID "
                + highlight2(objectivePath(objectivePath)) + ":"));
        if (conditions.isEmpty()) {
            messages.add(CommandMessage.success("<warn>This objective has no "
                    + label.toLowerCase(Locale.ROOT) + "!"));
            return List.copyOf(messages);
        }
        for (final Condition condition : conditions) {
            final Conditions.Type type = conditionType(plugin, condition.typeId());
            messages.add(CommandMessage.success("<highlight>" + condition.id() + ".</highlight> <main>"
                    + condition.typeId() + "</main>"));
            messages.add(CommandMessage.success("<main>" + conditionDescription(condition, type, null)));
        }
        return List.copyOf(messages);
    }

    static List<String> objectiveConditionIds(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return List.of();
        }
        return objective.getConditions(group).stream().map(entry -> String.valueOf(entry.id())).toList();
    }

    static CommandMessage clearObjectiveConditions(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        objective.clearConditions(group);
        plugin.saveData();
        return CommandMessage.success("<success>All "
                + objectiveConditionLabel(group).toLowerCase(Locale.ROOT)
                + " of " + objectiveContext(objectivePath, questName)
                + " have been removed!");
    }

    static CommandMessage removeObjectiveCondition(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        if (!objective.removeCondition(group, conditionId)) {
            return CommandMessage.error("Condition with the ID " + highlight(conditionId) + " was not found!");
        }
        plugin.saveData();
        return CommandMessage.success("<main>The condition with the ID "
                + highlight(conditionId) + " of " + objectiveContext(objectivePath, questName) + " has been removed!");
    }

    static CommandMessage objectiveConditionDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId) {
        final Condition entry = objectiveCondition(plugin, questName, objectivePath, group, conditionId);
        if (entry == null) {
            return CommandMessage.error("Condition with the ID " + highlight(conditionId) + " was not found!");
        }
        return CommandMessage.success("<main>Description of condition with ID "
                + highlight(conditionId) + " of " + objectiveContext(objectivePath, questName)
                + ": " + highlight2(entry.getDescription()));
    }

    static CommandMessage setObjectiveConditionDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId,
            final String description) {
        final Condition entry = objectiveCondition(plugin, questName, objectivePath, group, conditionId);
        if (entry == null) {
            return CommandMessage.error("Condition with the ID " + highlight(conditionId) + " was not found!");
        }
        entry.setDescription(description);
        plugin.saveData();
        return CommandMessage.success("<success>Description successfully added to condition with ID "
                + highlight(conditionId) + " of " + objectiveContext(objectivePath, questName)
                + "! New description: " + highlight2(entry.getDescription()));
    }

    static CommandMessage removeObjectiveConditionDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId) {
        final Condition entry = objectiveCondition(plugin, questName, objectivePath, group, conditionId);
        if (entry == null) {
            return CommandMessage.error("Condition with the ID " + highlight(conditionId) + " was not found!");
        }
        entry.setDescription("");
        plugin.saveData();
        return CommandMessage.success("<success>Description successfully removed from condition with ID "
                + highlight(conditionId) + " of " + objectiveContext(objectivePath, questName) + "!");
    }

    static CommandMessage setObjectiveConditionHidden(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId,
            final String hiddenExpression) {
        final Condition entry = objectiveCondition(plugin, questName, objectivePath, group, conditionId);
        if (entry == null) {
            return CommandMessage.error("Condition with the ID " + highlight(conditionId) + " was not found!");
        }
        entry.setHiddenExpression(hiddenExpression);
        plugin.saveData();
        return CommandMessage.success("<success>Hidden status successfully added to condition with ID "
                + highlight(conditionId) + " of " + objectiveContext(objectivePath, questName)
                + "! New hidden status: " + highlight2(entry.getHiddenExpression()));
    }

    static CommandMessage addObjectiveReward(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] objectivePath,
            final String actionTypeId,
            final String rawArguments) {
        return addObjectiveReward(
                plugin, adapter, questName, objectivePath, actionTypeId, rawArguments, null);
    }

    static CommandMessage addObjectiveReward(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final int[] objectivePath,
            final String actionTypeId,
            final String rawArguments,
            final PlatformPlayer questPlayer) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        final Actions.Type type = actionType(plugin, actionTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests action type: " + highlight(actionTypeId) + ".");
        }
        try {
            objective.addReward(type.id(), Actions.parse(
                    adapter, type, rawArguments, questPlayer));
            plugin.saveData();
            return CommandMessage.success("<success>" + type.id() + " Reward successfully added to Objective "
                    + highlight(objectiveDisplayNameOrIdentifier(objective)) + "!");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add objective reward: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listObjectiveRewards(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return List.of(missingObjective(objectivePath));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Rewards for " + objectiveContext(objectivePath, questName) + ":"));
        if (objective.getRewards().isEmpty()) {
            messages.add(CommandMessage.success("<warn>This objective has no rewards!"));
            return List.copyOf(messages);
        }
        for (final Action reward : objective.getRewards()) {
            final Actions.Type type = actionType(plugin, reward.typeId());
            messages.add(CommandMessage.success("<highlight>" + reward.id() + ".</highlight> <main>" + reward.typeId()));
            messages.add(CommandMessage.success("<unimportant>--</unimportant> <main>"
                    + actionDescription(reward, type, null)));
        }
        return List.copyOf(messages);
    }

    static List<String> objectiveRewardIds(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return List.of();
        }
        return objective.getRewards().stream().map(entry -> String.valueOf(entry.id())).toList();
    }

    static CommandMessage clearObjectiveRewards(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        objective.clearRewards();
        plugin.saveData();
        return CommandMessage.success("<success>All rewards of objective with ID "
                + highlight(objectivePath(objectivePath)) + " have been removed!");
    }

    static CommandMessage removeObjectiveReward(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        if (!objective.removeReward(rewardId)) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        plugin.saveData();
        return CommandMessage.success("<main>The reward with the ID "
                + highlight(rewardId) + " of " + objectiveContext(objectivePath, questName) + " has been removed!");
    }

    static CommandMessage objectiveRewardInfo(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId) {
        final Action reward = objectiveReward(plugin, questName, objectivePath, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        final Actions.Type type = actionType(plugin, reward.typeId());
        return CommandMessage.success("<main>Reward " + highlight(rewardId) + " for Objective with ID "
                + highlight2(objectivePath(objectivePath)) + " of Quest " + highlight2(questName) + ":\n"
                + "<unimportant>--</unimportant> <main>" + actionDescription(reward, type, null));
    }

    static CommandMessage objectiveRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId) {
        final Action reward = objectiveReward(plugin, questName, objectivePath, rewardId);
        if (reward == null) {
            return CommandMessage.error("<error>Invalid reward.");
        }
        if (reward.getDisplayName() == null || reward.getDisplayName().isBlank()) {
            return CommandMessage.success("<main>This reward has no display name set.");
        }
        return CommandMessage.success("<main>Reward display name: <highlight>" + reward.getDisplayName() + "</highlight>");
    }

    static CommandMessage setObjectiveRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId,
            final String displayName) {
        final Action reward = objectiveReward(plugin, questName, objectivePath, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        reward.setDisplayName(displayName);
        plugin.saveData();
        return CommandMessage.success("<success>Display name successfully added to reward with ID "
                + highlight(rewardId) + " of " + objectiveContext(objectivePath, questName)
                + "! New display name: " + highlight2(reward.getDisplayName()));
    }

    static CommandMessage removeObjectiveRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId) {
        final Action reward = objectiveReward(plugin, questName, objectivePath, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        reward.setDisplayName("");
        plugin.saveData();
        return CommandMessage.success("<success>Display name successfully removed from reward with ID "
                + highlight(rewardId) + " of " + objectiveContext(objectivePath, questName) + "!");
    }

    private static Objective objectiveAt(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null || objectivePath == null || objectivePath.length == 0) {
            return null;
        }
        Objective current = quest.getObjectiveFromID(objectivePath[0]);
        for (int i = 1; i < objectivePath.length && current != null; i++) {
            current = current.getObjectiveFromID(objectivePath[i]);
        }
        return current;
    }

    private static Objectives.Type objectiveType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().objectives().stream()
                .filter(objective -> objective.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Conditions.Type conditionType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Actions.Type actionType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static CommandMessage objectiveText(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final ObjectiveTextField textField) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        return CommandMessage.success("<main>Current " + textField.label()
                + " of objective with ID " + highlight(objectiveLabel(objectivePath))
                + ": " + highlight2(textField.value(objective)));
    }

    private static CommandMessage setObjectiveText(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final ObjectiveTextField textField,
            final String value) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        textField.set(objective, value);
        plugin.saveData();
        return CommandMessage.success("<main>" + textField.actionLabel()
                + " successfully added to objective with ID " + highlight(objectiveLabel(objectivePath)) + "! New "
                + textField.label()
                + ": " + highlight2(textField.value(objective)));
    }

    private static CommandMessage removeObjectiveText(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final ObjectiveTextField textField) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        if (objective == null) {
            return missingObjective(objectivePath);
        }
        textField.set(objective, "");
        plugin.saveData();
        return CommandMessage.success("<main>" + textField.actionLabel()
                + " successfully removed from objective with ID " + highlight(objectiveLabel(objectivePath))
                + "! New " + textField.label() + ": "
                + highlight2(textField.value(objective)));
    }

    private static String objectiveContentDescription(
            final Objective objective,
            final Objectives.Type type) {
        if (type == null) {
            return objective.typeId();
        }
        if (type.taskDescriptionRenderer() == null) {
            return type.description();
        }
        try {
            final String rendered = type.taskDescriptionRenderer().render(objective.data(), null, null);
            return rendered == null || rendered.isBlank() ? type.description() : rendered;
        } catch (final RuntimeException exception) {
            return type.description();
        }
    }

    private static String objectiveDisplayNameOrIdentifier(final Objective objective) {
        if (objective == null) {
            return "unknown";
        }
        return objective.getDisplayName() == null || objective.getDisplayName().isBlank()
                ? objective.typeId()
                : objective.getDisplayName();
    }

    private static void addObjectiveListConditions(
            final NotQuestsPlugin plugin,
            final ArrayList<CommandMessage> messages,
            final String label,
            final List<Condition> conditions,
            final PlatformPlayer target) {
        messages.add(CommandMessage.success("   <highlight>" + label + " Conditions:"));
        if (conditions == null || conditions.isEmpty()) {
            messages.add(CommandMessage.success("      <unimportant>No "
                    + label.toLowerCase(Locale.ROOT)
                    + " conditions found!"));
            return;
        }
        for (final Condition condition : conditions) {
            messages.add(CommandMessage.success("         <highlight>" + condition.id()
                    + ".</highlight> <main>Condition:</main> <highlight2>"
                    + conditionDescription(condition, conditionType(plugin, condition.typeId()), target)));
        }
    }

    private static String conditionDescription(
            final Condition condition,
            final Conditions.Type type,
            final PlatformPlayer target) {
        if (condition.getDescription() != null && !condition.getDescription().isBlank()) {
            return condition.getDescription();
        }
        if (type == null) {
            return condition.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.description();
        }
        try {
            return type.descriptionRenderer().render(condition.data(), target);
        } catch (final RuntimeException exception) {
            return type.description();
        }
    }

    private static String actionDescription(
            final Action action,
            final Actions.Type type,
            final PlatformPlayer target) {
        if (action.getDescription() != null && !action.getDescription().isBlank()) {
            return action.getDescription();
        }
        if (type == null) {
            return action.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
        try {
            return type.descriptionRenderer().render(action.data(), target);
        } catch (final RuntimeException exception) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
    }

    private static String objectiveConditionAddLabel(final String group) {
        if ("complete".equalsIgnoreCase(group)) {
            return "Complete Condition";
        }
        if ("progress".equalsIgnoreCase(group)) {
            return "Condition";
        }
        return "Unlock Condition";
    }

    private static String objectiveConditionLabel(final String group) {
        if ("progress".equalsIgnoreCase(group)) {
            return "Progress conditions";
        }
        if ("complete".equalsIgnoreCase(group)) {
            return "Complete conditions";
        }
        return "Unlock conditions";
    }

    private static Condition objectiveCondition(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final String group,
            final int conditionId) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        return objective == null ? null : objective.getConditionFromID(group, conditionId);
    }

    private static Action objectiveReward(
            final NotQuestsPlugin plugin,
            final String questName,
            final int[] objectivePath,
            final int rewardId) {
        final Objective objective = objectiveAt(plugin, questName, objectivePath);
        return objective == null ? null : objective.getRewardFromID(rewardId);
    }

    private static CommandMessage validateConditionData(
            final Conditions.Type type,
            final Condition data,
            final int[] objectivePath) {
        if ("Date".equalsIgnoreCase(type.id())) {
            final String operation = data.text("Date operation").toLowerCase(Locale.ROOT);
            if (!operation.equals("after") && !operation.equals("before")) {
                return CommandMessage.error(
                        "<error>Error: The date operation can only be <highlight>after</highlight> or <highlight>before</highlight>.");
            }
            data.setValue("Date operation", operation);
        }
        if ("CompletedObjective".equalsIgnoreCase(type.id())
                && objectivePath != null
                && objectivePath.length > 0
                && data.integer("dependingObjectiveId", -1) == objectivePath[objectivePath.length - 1]) {
            return CommandMessage.error("<error>Error: You cannot set an objective to depend on itself!");
        }
        return null;
    }

    private static void applySharedConditionFlags(
            final Condition data,
            final String rawArguments) {
        final List<String> tokens = Actions.tokenize(rawArguments);
        if (tokens.contains("--" + NQFlags.NEGATE.name())) {
            data.setValue("negated", true);
        }
        if (tokens.contains("--" + NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.name())) {
            data.setValue("allowProgressDecreaseIfNotFulfilled", true);
        }
    }

    private static String objectivePath(final int[] objectivePath) {
        if (objectivePath == null || objectivePath.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objectivePath.length; i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append(objectivePath[i]);
        }
        return builder.toString();
    }

    private static String objectiveLabel(final int[] objectivePath) {
        if (objectivePath == null || objectivePath.length == 0) {
            return "";
        }
        return objectivePath.length == 1 ? String.valueOf(objectivePath[0]) : objectivePath(objectivePath);
    }

    private static String objectiveContext(final int[] objectivePath, final String questName) {
        if (objectivePath == null || objectivePath.length == 0) {
            return "Quest " + highlight2(questName);
        }
        if (objectivePath.length == 1) {
            return "Objective with ID " + highlight2(objectivePath[0]) + " of Quest " + highlight2(questName);
        }
        return "Objective with path " + highlight2(objectivePath(objectivePath)) + " of Quest " + highlight2(questName);
    }

    private static String formatLocation(
            final String world,
            final double x,
            final double y,
            final double z) {
        return world + " " + formatCoordinate(x) + " " + formatCoordinate(y) + " " + formatCoordinate(z);
    }

    private static String formatLocation(final NQLocation location) {
        return location == null
                ? "none"
                : formatLocation(location.worldName(), location.x(), location.y(), location.z());
    }

    private static String formatCoordinate(final double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static CommandMessage missingQuest(final String questName) {
        return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist!");
    }

    private static CommandMessage missingObjective(final int[] objectivePath) {
        return CommandMessage.error("Objective with the path " + highlight(objectivePath(objectivePath)) + " was not found!");
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }

    private enum ObjectiveTextField {
        DESCRIPTION("description", "Description") {
            @Override
            String value(final Objective objective) {
                return objective.getDescription();
            }

            @Override
            void set(final Objective objective, final String value) {
                objective.setDescription(value);
            }
        },
        TASK_DESCRIPTION("task description", "Task Description") {
            @Override
            String value(final Objective objective) {
                return objective.getTaskDescription();
            }

            @Override
            void set(final Objective objective, final String value) {
                objective.setTaskDescription(value);
            }
        },
        DISPLAY_NAME("displayname", "Displayname") {
            @Override
            String value(final Objective objective) {
                return objective.getDisplayName();
            }

            @Override
            void set(final Objective objective, final String value) {
                objective.setDisplayName(value);
            }
        };

        private final String label;
        private final String actionLabel;

        ObjectiveTextField(final String label, final String actionLabel) {
            this.label = label;
            this.actionLabel = actionLabel;
        }

        String label() {
            return label;
        }

        String actionLabel() {
            return actionLabel;
        }

        abstract String value(Objective objective);

        abstract void set(Objective objective, String value);
    }
}
