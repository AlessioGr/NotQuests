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

package rocks.gravili.notquests.Structs.Triggers.TriggerTypes;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Triggers.Action;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

public class NPCDeathTrigger extends Trigger {

    private final NotQuests main;
    private final int npcToDieID;


    public NPCDeathTrigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded) {
        super(main, quest, triggerID, action, applyOn, worldName, amountNeeded);
        this.main = main;

        this.npcToDieID = main.getDataManager().getQuestsData().getInt("quests." + getQuest().getQuestName() + ".triggers." + triggerID + ".specifics.npcToDie");
    }

    public NPCDeathTrigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded, int npcToDieID) {
        super(main, quest, triggerID, action, applyOn, worldName, amountNeeded);
        this.main = main;
        this.npcToDieID = npcToDieID;
    }

    public final int getNpcToDieID() {
        return npcToDieID;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {

    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".triggers." + getTriggerID() + ".specifics.npcToDie", getNpcToDieID());
    }

    @Override
    public String getTriggerDescription() {
        return "NPC to die ID: §f" + getNpcToDieID();
    }




    /*@Override
    public void isCompleted(){

    }*/


}
