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

package rocks.gravili.notquests.Structs.Objectives.hooks;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

public class KillEliteMobsObjective extends Objective {

    private final NotQuests main;
    private final String eliteMobToKillContainsName; //Blank: doesn't matter
    private final int minimumLevel, maximumLevel; //-1: doesn't matter
    private final String spawnReason; //Optional. If blank, any spawn reason will be used
    private final int minimumDamagePercentage; //How much damage the player has to do to the mob minimum. -1: Doesn't matter
    private final int amountToKill;

    public KillEliteMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String eliteMobToKillContainsName, int minimumLevel, int maximumLevel, String spawnReason, int minimumDamagePercentage, int amountToKill) {
        super(main, quest, objectiveID, amountToKill);
        this.main = main;
        this.eliteMobToKillContainsName = eliteMobToKillContainsName;
        this.minimumLevel = minimumLevel;
        this.maximumLevel = maximumLevel;
        this.spawnReason = spawnReason;
        this.minimumDamagePercentage = minimumDamagePercentage;
        this.amountToKill = amountToKill;
    }

    public KillEliteMobsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        eliteMobToKillContainsName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.eliteMobToKill");
        minimumLevel = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.minimumLevel");
        maximumLevel = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.maximumLevel");
        spawnReason = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.spawnReason");
        minimumDamagePercentage = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.minimumDamagePercentage");
        amountToKill = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.amountToKill");

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (!getEliteMobToKillContainsName().isBlank()) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.killEliteMobs.base", player)
                    .replaceAll("%EVENTUALCOLOR%", eventualColor)
                    .replaceAll("%ELITEMOBNAME%", "" + getEliteMobToKillContainsName());
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.killEliteMobs.any", player)
                    .replaceAll("%EVENTUALCOLOR%", eventualColor);
        }
        if (getMinimumLevel() != -1) {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        §7" + eventualColor + "Level: §f" + eventualColor + getMinimumLevel() + "-" + getMaximumLevel();
            } else {
                toReturn += "\n        §7" + eventualColor + "Minimum Level: §f" + eventualColor + getMinimumLevel();
            }
        } else {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        §7" + eventualColor + "Maximum Level: §f" + eventualColor + getMaximumLevel();
            }
        }

        if (!getSpawnReason().isBlank()) {
            toReturn += "\n        §7" + eventualColor + "Spawned from: §f" + eventualColor + getSpawnReason();
        }

        if (getMinimumDamagePercentage() != -1) {
            toReturn += "\n        §7" + eventualColor + "Inflict minimum damage: §f" + eventualColor + getMinimumDamagePercentage() + "%";
        }
        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.eliteMobToKill", getEliteMobToKillContainsName());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.minimumLevel", getMinimumLevel());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.maximumLevel", getMaximumLevel());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.spawnReason", getSpawnReason());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.minimumDamagePercentage", getMinimumDamagePercentage());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.amountToKill", getAmountToKill());

    }

    public final String getEliteMobToKillContainsName() {
        return eliteMobToKillContainsName;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }

    public final int getMinimumLevel() {
        return minimumLevel;
    }

    public final int getMaximumLevel() {
        return maximumLevel;
    }

    public final String getSpawnReason() {
        return spawnReason;
    }

    public final int getMinimumDamagePercentage() {
        return minimumDamagePercentage;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {

    }
}
