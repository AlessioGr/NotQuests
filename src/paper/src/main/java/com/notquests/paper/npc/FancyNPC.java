package com.notquests.paper.npc;

import de.oliver.fancynpcs.api.Npc;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.npc.NQNPCID;
import com.notquests.paper.NotQuests;

/**
 * A {@link NQNPC} backed by a FancyNPCs NPC (identified by its String id). FancyNPCs NPCs are
 * packet-based and have no Citizens-style trait system, so quest-giver behaviour is driven entirely
 * by the global {@code NpcInteractEvent} listener (see FancyNPCsEvents) rather than by attaching a
 * trait; interaction behavior is provided by the native FancyNPCs event listener.
 */
public class FancyNPC extends NQNPC {
  private final NQNPCID npcID;

  public FancyNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "fancynpcs");
    this.npcID = npcID;
  }

  private @Nullable Npc resolve() {
    if (main.integrations().fancyNpcs() == null) {
      return null;
    }
    return main.integrations().fancyNpcs().getNpc(npcID.getStringID());
  }

  @Nullable
  @Override
  public String getName() {
    final Npc npc = resolve();
    return npc != null ? npc.getData().getName() : null;
  }

  @Override
  public NQNPCID getID() {
    return npcID;
  }

}
