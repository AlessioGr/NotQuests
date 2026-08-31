package com.notquests.paper.integrations;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.paper.NotQuests;

public class WorldEditIntegration {
  private final NotQuests main;
  private final WorldEditPlugin worldEditPlugin;

  public WorldEditIntegration(final NotQuests main) {
    this.main = main;
    worldEditPlugin =
        (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
  }

  public LocationRegion getSelectionRegionOrNull(final Player player) {
    try {
      return getSelectionRegion(player);
    } catch (IncompleteRegionException ignored) {
      return null;
    }
  }

  private LocationRegion getSelectionRegion(final Player player) throws IncompleteRegionException {
    BukkitPlayer actor =
        BukkitAdapter.adapt(player); // WorldEdit's native Player class extends Actor
    SessionManager manager =
        main.integrations()
            .worldEdit()
            .getWorldEdit()
            .getWorldEdit()
            .getSessionManager();
    LocalSession localSession = manager.get(actor);

    com.sk89q.worldedit.world.World selectionWorld = localSession.getSelectionWorld();
    if (selectionWorld == null) {
      throw new IncompleteRegionException();
    }

    Region region = localSession.getSelection(selectionWorld);
    final String worldName = BukkitAdapter.adapt(selectionWorld).getName();
    final NQLocation min =
        NQLocation.at(
            worldName,
            region.getMinimumPoint().x(),
            region.getMinimumPoint().y(),
            region.getMinimumPoint().z());
    final NQLocation max =
        NQLocation.at(
            worldName,
            region.getMaximumPoint().x(),
            region.getMaximumPoint().y(),
            region.getMaximumPoint().z());
    return new LocationRegion(min, max);
  }

  public WorldEditPlugin getWorldEdit() {
    return worldEditPlugin;
  }
}
