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

package rocks.gravili.notquests.paper.managers.integrations;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.SpawnMobAction;

import java.util.Collection;

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

    public final Collection<String> getMobNames() {
        return mythicMobs.getMobManager().getMobNames();
    }

    public void spawnMob(String mobToSpawnType, Location location, int amount, final SpawnMobAction spawnMobAction) {
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
                mythicMobs.getAPIHelper().spawnMythicMob(foundMythicMob, spawnMobAction.getRandomLocationWithRadius(location), 1);
            }
        } catch (InvalidMobTypeException e) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the mythic mob " + mobToSpawnType + " is invalid.");
        }


    }

    public final boolean isMythicMob(final String mobToSpawnType) {
        return mythicMobs.getMobManager().getMythicMob(mobToSpawnType) != null;
    }
}
