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

package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class WorldEnterTrigger extends Trigger {

    private final NotQuests main;
    private final String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main, Action action, int applyOn, String worldName, long amountNeeded, String worldToEnterName) {
        super(main, action, TriggerType.WORLDENTER, applyOn, worldName, amountNeeded);
        this.main = main;
        this.worldToEnterName = worldToEnterName;

    }

    public final String getWorldToEnterName() {
        return worldToEnterName;
    }


}
