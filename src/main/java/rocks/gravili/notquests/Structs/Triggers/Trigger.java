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

package rocks.gravili.notquests.Structs.Triggers;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveQuest;
import rocks.gravili.notquests.Structs.Quest;

import java.util.logging.Level;

public abstract class Trigger {
    private final NotQuests main;
    private final Quest quest;
    private final int triggerID;
    private final Action action;
    private final int applyOn; // 0 is for the whole quest. Positive numbers = objectives (JUST INTERNALLY HERE, NOT IN THE ADMIN COMMAND)
    private final String worldName;
    private final long amountNeeded; // 0 or 1 means every trigger() triggers it


    public Trigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded) {
        this.main = main;
        this.quest = quest;
        this.triggerID = triggerID;
        this.action = action;
        this.applyOn = applyOn;
        this.amountNeeded = amountNeeded;
        this.worldName = worldName;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final int getTriggerID() {
        return triggerID;
    }

    public final String getTriggerType() {
        return main.getTriggerManager().getTriggerType(this.getClass());
    }

    public final String getWorldName() {
        return worldName;
    }

    public final Action getTriggerAction() {
        return action;
    }

    public final int getApplyOn() {
        return applyOn;
    }

    public final long getAmountNeeded() {
        return amountNeeded;
    }

    public void trigger(ActiveQuest activeQuest) { //or void completeTrigger() or finishTrigger()
        //execute action here
        final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());

        if (player != null) {
            action.execute(player, activeQuest);
        } else {
            main.getLogManager().log(Level.WARNING, "Tried to execute trigger for offline player - ABORTED!");

        }
    }

    public abstract void save();

    public abstract String getTriggerDescription();

}
