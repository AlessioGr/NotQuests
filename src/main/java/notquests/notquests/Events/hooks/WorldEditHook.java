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

package notquests.notquests.Events.hooks;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.ReachLocationObjective;
import notquests.notquests.Structs.Quest;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldEditHook {
    private final NotQuests main;

    public WorldEditHook(final NotQuests main) {
        this.main = main;
    }

    public void handleReachLocationObjectiveCreation(final Player player, final Quest quest, final String locationName) {
        BukkitPlayer actor = BukkitAdapter.adapt(player); // WorldEdit's native Player class extends Actor
        SessionManager manager = main.getWorldEdit().getWorldEdit().getSessionManager();
        LocalSession localSession = manager.get(actor);


        Region region;
        com.sk89q.worldedit.world.World selectionWorld = localSession.getSelectionWorld();
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
            final Location min = new Location(BukkitAdapter.adapt(selectionWorld), region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());
            final Location max = new Location(BukkitAdapter.adapt(selectionWorld), region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());

            //Create Objective
            ReachLocationObjective reachLocationObjective = new ReachLocationObjective(main, quest, quest.getObjectives().size() + 1, min, max, locationName);
            quest.addObjective(reachLocationObjective, true);
            player.sendMessage("§aReach Location Objective successfully added to quest §b" + quest.getQuestName() + "§a!");

        } catch (IncompleteRegionException ex) {
            player.sendMessage("§cPlease make a region selection using WorldEdit first.");
        }
    }
}
