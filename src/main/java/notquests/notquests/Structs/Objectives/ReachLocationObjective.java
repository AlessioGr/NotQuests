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
import org.bukkit.Location;

public class ReachLocationObjective extends Objective {
    private final Location min, max;
    private final NotQuests main;
    private final String locationName;

    public ReachLocationObjective(NotQuests main, final Quest quest, final int objectiveID, final Location minLocation, final Location maxLocation, final String locationName) {
        super(main, quest, objectiveID, ObjectiveType.ReachLocation, 1);
        this.main = main;
        this.min = minLocation;
        this.max = maxLocation;
        this.locationName = locationName;
    }

    public final Location getMinLocation() {
        return min;
    }

    public final Location getMaxLocation() {
        return max;
    }

    public final String getLocationName() {
        return locationName;
    }

}
