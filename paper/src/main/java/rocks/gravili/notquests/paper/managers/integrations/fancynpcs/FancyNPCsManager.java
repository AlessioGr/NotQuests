/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers.integrations.fancynpcs;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.Quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Thin wrapper around the FancyNPCs API (de.oliver.fancynpcs.api). */
public class FancyNPCsManager {
  private final NotQuests main;

  public FancyNPCsManager(final NotQuests main) {
    this.main = main;
    main.getLogManager().info("Initialized FancyNPCs integration");

    // FancyNPCs are packet-based and expose no Bukkit entity, so the entity-based particle
    // schedulers used for Citizens / armor stands skip them entirely. Run our own here, reusing the
    // Citizens NPC particle config (FancyNPCs are NPCs too).
    if (main.getConfiguration().isCitizensNPCQuestGiverIndicatorParticleEnabled()) {
      startQuestGiverIndicatorParticleRunnable();
    }
  }

  /**
   * Spawns the quest-giver indicator particle above FancyNPCs that have a (showing) quest attached,
   * matching the Citizens / armor-stand behaviour. Must be a SYNC timer — {@code World#spawnParticle}
   * is main-thread-only on Paper.
   */
  private void startQuestGiverIndicatorParticleRunnable() {
    final int interval = main.getConfiguration().getCitizensNPCQuestGiverIndicatorParticleSpawnInterval();
    Bukkit.getServer().getScheduler().runTaskTimer(main.getMain(), () -> {
      final double minimumTPS = main.getConfiguration().getCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow();
      if (minimumTPS >= 0 && main.getPerformanceManager().getTPS() < minimumTPS) {
        return;
      }

      // Collect the distinct FancyNPCs that have a quest attached & showing.
      final Set<String> fancyNpcIds = new HashSet<>();
      for (final Quest quest : main.getQuestManager().getAllQuests()) {
        for (final NQNPC nqnpc : quest.getAttachedNPCsWithQuestShowing()) {
          if ("fancynpcs".equalsIgnoreCase(nqnpc.getNPCType())) {
            final String id = nqnpc.getID().getStringID();
            if (id != null) {
              fancyNpcIds.add(id);
            }
          }
        }
      }

      for (final String id : fancyNpcIds) {
        final Npc npc = getNpc(id);
        if (npc == null) {
          continue;
        }
        final Location location = npc.getData().getLocation();
        if (location == null || location.getWorld() == null) {
          continue;
        }
        location.getWorld().spawnParticle(
            main.getConfiguration().getCitizensNPCQuestGiverIndicatorParticleType(),
            location.getX() - 0.25 + (Math.random() / 2),
            location.getY() + 1.75 + (Math.random() / 2),
            location.getZ() - 0.25 + (Math.random() / 2),
            main.getConfiguration().getCitizensNPCQuestGiverIndicatorParticleCount());
      }
    }, interval, interval);
  }

  /** Returns the FancyNPCs NPC for the given (String) id, or null if it doesn't exist. */
  public @Nullable Npc getNpc(final String id) {
    if (id == null) {
      return null;
    }
    return FancyNpcsPlugin.get().getNpcManager().getNpcById(id);
  }

  public Collection<Npc> getAllNpcs() {
    return FancyNpcsPlugin.get().getNpcManager().getAllNpcs();
  }

  /** All FancyNPCs NPC ids (used to list selectable NPCs). */
  public List<String> getAllNPCIds() {
    final List<String> ids = new ArrayList<>();
    for (final Npc npc : getAllNpcs()) {
      ids.add(npc.getData().getId());
    }
    return ids;
  }
}
