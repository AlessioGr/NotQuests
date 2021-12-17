/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.Managers.Registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Conditions.*;
import rocks.gravili.notquests.Structs.Conditions.hooks.Towny.TownyNationNameCondition;
import rocks.gravili.notquests.Structs.Conditions.hooks.Towny.TownyNationTownCountCondition;
import rocks.gravili.notquests.Structs.Conditions.hooks.Towny.TownyTownPlotCountCondition;
import rocks.gravili.notquests.Structs.Conditions.hooks.Towny.TownyTownResidentCountCondition;
import rocks.gravili.notquests.Structs.Conditions.hooks.UltimateClans.UltimateClansClanLevelCondition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class ConditionsManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Condition>> conditions;


    public ConditionsManager(final NotQuests main) {
        this.main = main;
        conditions = new HashMap<>();

        registerDefaultConditions();

    }

    public void registerDefaultConditions() {
        conditions.clear();
        registerCondition("OtherQuest", OtherQuestCondition.class);
        registerCondition("QuestPoints", QuestPointsCondition.class);
        registerCondition("Permission", PermissionCondition.class);
        registerCondition("Money", MoneyCondition.class);
        registerCondition("WorldTime", WorldTimeCondition.class);
        registerCondition("UltimateClansClanLevel", UltimateClansClanLevelCondition.class);
        registerCondition("ObjectiveCompleted", ObjectiveCompletedCondition.class);

        //Towny
        registerCondition("TownyNationName", TownyNationNameCondition.class);
        registerCondition("TownyNationTownCount", TownyNationTownCountCondition.class);
        registerCondition("TownyTownResidentCount", TownyTownResidentCountCondition.class);
        registerCondition("TownyTownPlotCount", TownyTownPlotCountCondition.class);

    }


    public void registerCondition(final String identifier, final Class<? extends Condition> condition) {
        main.getLogManager().info("Registering condition <AQUA>" + identifier);
        conditions.put(identifier, condition);

        try {
            Method commandHandler = condition.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ConditionFor.class);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), ConditionFor.QUEST);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder(), ConditionFor.OBJECTIVE);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Condition> getConditionClass(final String type) {
        return conditions.get(type);
    }

    public final String getConditionType(final Class<? extends Condition> condition) {
        for (final String conditionType : conditions.keySet()) {
            if (conditions.get(conditionType).equals(condition)) {
                return conditionType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Condition>> getConditionsAndIdentifiers() {
        return conditions;
    }

    public final Collection<Class<? extends Condition>> getConditions() {
        return conditions.values();
    }

    public final Collection<String> getConditionIdentifiers() {
        return conditions.keySet();
    }

    public void addCondition(Condition condition, CommandContext<CommandSender> context) {
        Audience audience = main.adventure().sender(context.getSender());

        Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.contains("Objective ID")) {
            int objectiveID = context.get("Objective ID");
            objectiveOfQuest = quest.getObjectiveFromID(objectiveID);
        }

        if (quest != null) {
            condition.setQuest(quest);
            if (objectiveOfQuest != null) {//Objective Condition
                condition.setObjective(objectiveOfQuest);

                objectiveOfQuest.addCondition(condition, true);

                audience.sendMessage(MiniMessage.miniMessage().parse(
                        NotQuestColors.successGradient + getConditionType(condition.getClass()) + " Condition successfully added to Objective " + NotQuestColors.highlightGradient
                                + objectiveOfQuest.getObjectiveFinalName() + "</gradient>!</gradient>"));
            }else{ //Quest Requirement
                quest.addRequirement(condition, true);

                audience.sendMessage(MiniMessage.miniMessage().parse(
                        NotQuestColors.successGradient + getConditionType(condition.getClass()) + " Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                + quest.getQuestName() + "</gradient>!</gradient>"
                ));
            }
        }
    }
}