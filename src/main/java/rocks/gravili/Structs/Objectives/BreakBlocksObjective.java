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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;

public class BreakBlocksObjective extends Objective {

    private final NotQuests main;
    private final Material blockToBreak;
    private final boolean deductIfBlockIsPlaced;

    public BreakBlocksObjective(NotQuests main, final Quest quest, final int objectiveID, Material blockToBreak, int amountToBreak, boolean deductIfBlockIsPlaced) {
        super(main, quest, objectiveID, amountToBreak);
        this.main = main;
        this.blockToBreak = blockToBreak;
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    public BreakBlocksObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        blockToBreak = Material.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToBreak.material"));
        deductIfBlockIsPlaced = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockPlaced");
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breakBlocks.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%BLOCKTOBREAK%", getBlockToBreak().toString());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.blockToBreak.material", getBlockToBreak().toString());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName()  + ".objectives." + getObjectiveID() + ".specifics.deductIfBlockPlaced", willDeductIfBlockPlaced());

    }

    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final long getAmountToBreak() {
        return super.getProgressNeeded();
    }

    public final boolean willDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }

}
