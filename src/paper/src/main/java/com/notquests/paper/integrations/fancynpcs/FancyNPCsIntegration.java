package com.notquests.paper.integrations.fancynpcs;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Thin wrapper around the FancyNPCs API (de.oliver.fancynpcs.api). */
public class FancyNPCsIntegration {
  private final NotQuests main;

  public FancyNPCsIntegration(final NotQuests main) {
    this.main = main;
  }

  public void renderIndicator(
      final NQNPCID npcId,
      final Indicator indicator) {
    if (indicator == null || !indicator.showParticle()) {
      return;
    }
    final Npc npc = getNpc(npcId.getStringID());
    if (npc == null) {
      return;
    }
    final Location location = npc.getData().getLocation();
    if (location == null || location.getWorld() == null) {
      return;
    }
    location.getWorld().spawnParticle(
        main.particle(indicator.particleType()),
        location.getX() - 0.25 + (Math.random() / 2),
        location.getY() + 1.75 + (Math.random() / 2),
        location.getZ() - 0.25 + (Math.random() / 2),
        indicator.particleCount());
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
