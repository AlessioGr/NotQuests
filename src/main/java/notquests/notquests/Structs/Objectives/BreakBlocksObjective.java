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

package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;

public class BreakBlocksObjective extends Objective {

    private final NotQuests main;
    private final Material blockToBreak;
    private final int amountToBreak;
    private final boolean deductIfBlockIsPlaced;

    public BreakBlocksObjective(NotQuests main, final Quest quest, final int objectiveID, Material blockToBreak, int amountToBreak, boolean deductIfBlockIsPlaced) {
        super(main, quest, objectiveID, ObjectiveType.BreakBlocks, amountToBreak);
        this.main = main;
        this.blockToBreak = blockToBreak;
        this.amountToBreak = amountToBreak;
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final int getAmountToBreak() {
        return amountToBreak;
    }

    public final boolean willDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }
}
