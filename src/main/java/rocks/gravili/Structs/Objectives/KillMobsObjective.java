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

package rocks.gravili.Structs.Objectives;

import org.bukkit.entity.Player;
import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;

public class KillMobsObjective extends Objective {

    private final NotQuests main;
    private final String mobToKillType;
    private String nameTagContainsAny = "";
    private String nameTagEquals = "";

    public KillMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String mobToKill, int amountToKill) {
        super(main, quest, objectiveID, amountToKill);
        this.main = main;
        this.mobToKillType = mobToKill;
    }

    public KillMobsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        mobToKillType = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.mobToKill");

        //Extras
        final String nameTagContains = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".extras.nameTagContainsAny", "");
        if (!nameTagContains.isBlank()) {
            setNameTagContainsAny(nameTagContains);
        }

        final String nameTagEquals = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".extras.nameTagEquals", "");
        if (!nameTagEquals.isBlank()) {
            setNameTagEquals(nameTagEquals);
        }
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return "    §7" + eventualColor + "Mob to kill: §f" + eventualColor + getMobToKill();
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.mobToKill", getMobToKill());

        //Extra args
        if (!getNameTagContainsAny().isBlank()) {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".extras.nameTagContainsAny", getNameTagContainsAny());
        }
        if (!getNameTagEquals().isBlank()) {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".extras.nameTagEquals", getNameTagEquals());
        }
    }

    public final String getMobToKill() {
        return mobToKillType;
    }

    public final long getAmountToKill() {
        return super.getProgressNeeded();
    }


    //Extra args
    public final String getNameTagContainsAny() {
        return nameTagContainsAny;
    }

    public void setNameTagContainsAny(final String nameTagContainsAny) {
        this.nameTagContainsAny = nameTagContainsAny;
    }

    public final String getNameTagEquals() {
        return nameTagEquals;
    }

    public void setNameTagEquals(final String nameTagEquals) {
        this.nameTagEquals = nameTagEquals;
    }

}
