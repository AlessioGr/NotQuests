/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.spigot.managers.integrations;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Location;
import rocks.gravili.notquests.spigot.NotQuests;

public class MythicMobsManager {
    private final NotQuests main;
    private MythicMobs mythicMobs;

    public MythicMobsManager(final NotQuests main) {
        this.main = main;
        this.mythicMobs = MythicMobs.inst();
    }

    public MythicMobs getMythicMobs() {
        return mythicMobs;
    }

    public void spawnMob(String mobToSpawnType, Location location, int amount) {
        MythicMob foundMythicMob = mythicMobs.getMobManager().getMythicMob(mobToSpawnType);
        if (foundMythicMob == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the mythic mob " + mobToSpawnType + " was not found.");
            return;
        }
        if (location == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location is invalid.");
            return;
        }
        if (location.getWorld() == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location world is invalid.");
            return;
        }


        try {
            for (int i = 0; i < amount; i++) {
                mythicMobs.getAPIHelper().spawnMythicMob(foundMythicMob, location, 1);
            }
        } catch (InvalidMobTypeException e) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the mythic mob " + mobToSpawnType + " is invalid.");
        }


    }
}
