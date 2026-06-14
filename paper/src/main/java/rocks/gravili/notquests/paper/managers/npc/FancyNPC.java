/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers.npc;

import de.oliver.fancynpcs.api.Npc;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.Quest;

/**
 * A {@link NQNPC} backed by a FancyNPCs NPC (identified by its String id). FancyNPCs NPCs are
 * packet-based and have no Citizens-style trait system, so quest-giver behaviour is driven entirely
 * by the global {@code NpcInteractEvent} listener (see FancyNPCsEvents) rather than by attaching a
 * trait; the trait/conversation-binding methods are therefore no-ops here.
 */
public class FancyNPC extends NQNPC {
  private final NQNPCID npcID;

  public FancyNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "fancynpcs");
    this.npcID = npcID;
  }

  private @Nullable Npc resolve() {
    if (!main.getIntegrationsManager().isFancyNPCsEnabled()) {
      return null;
    }
    return main.getIntegrationsManager().getFancyNPCsManager().getNpc(npcID.getStringID());
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

  @Override
  public void bindToConversation(final Conversation conversation) {
    // No-op: FancyNPCs has no trait system; clicks are handled globally via NpcInteractEvent.
  }

  @Override
  public String removeQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    return ""; // handled globally; nothing to detach
  }

  @Override
  public String addQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    return ""; // handled globally; nothing to attach
  }

  @Override
  public @Nullable Entity getEntity() {
    // FancyNPCs NPCs are packet-based and do not expose a real Bukkit entity.
    return null;
  }
}
