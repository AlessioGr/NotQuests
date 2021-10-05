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

public class KillMobsObjective extends Objective {

    private final NotQuests main;
    private final String mobToKillType;
    private final int amountToKill;
    private String nameTagContains = "";
    private String nameTagEquals = "";

    public KillMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String mobToKill, int amountToKill) {
        super(main, quest, objectiveID, ObjectiveType.KillMobs, amountToKill);
        this.main = main;
        this.amountToKill = amountToKill;
        this.mobToKillType = mobToKill;
    }

    public final String getMobToKill() {
        return mobToKillType;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }


    //Extra args
    public final String getNameTagContains() {
        return nameTagContains;
    }

    public void setNameTagContains(final String nameTagContains) {
        this.nameTagContains = nameTagContains;
    }

    public final String getNameTagEquals() {
        return nameTagEquals;
    }

    public void setNameTagEquals(final String nameTagEquals) {
        this.nameTagEquals = nameTagEquals;
    }

}
