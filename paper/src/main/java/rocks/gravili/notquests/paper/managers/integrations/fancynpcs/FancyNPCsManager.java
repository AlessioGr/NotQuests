/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers.integrations.fancynpcs;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Thin wrapper around the FancyNPCs API (de.oliver.fancynpcs.api). */
public class FancyNPCsManager {
  private final NotQuests main;

  public FancyNPCsManager(final NotQuests main) {
    this.main = main;
    main.getLogManager().info("Initialized FancyNPCs integration");
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
