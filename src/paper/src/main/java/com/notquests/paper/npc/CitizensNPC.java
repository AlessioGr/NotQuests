package com.notquests.paper.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.npc.NQNPCID;
import com.notquests.paper.NotQuests;
import com.notquests.paper.integrations.citizens.QuestGiverNPCTrait;

public class CitizensNPC extends NQNPC {
  private NPC cachedNPC;
  private final NQNPCID npcID;

  public CitizensNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "citizens");
    this.npcID = npcID;
    this.cachedNPC = CitizensAPI.getNPCRegistry().getById(npcID.getIntegerID());
  }

  private boolean updateCachedNPC() {
    if (cachedNPC == null) {
      cachedNPC = CitizensAPI.getNPCRegistry().getById(npcID.getIntegerID());
    }
    return cachedNPC != null;
  }

  @Nullable
  @Override
  public String getName() {
    if (!updateCachedNPC()) {
      return null;
    }
    return main.getMiniMessage()
        .serialize(
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(cachedNPC.getName().replace("§", "&")));
  }

  @Override
  public NQNPCID getID() {
    if (!updateCachedNPC()) {
      return npcID;
    }
    return NQNPCID.fromInteger(cachedNPC.getId());
  }

  public boolean setQuestGiverTrait(final boolean enabled) {
    if (!updateCachedNPC()) {
      return false;
    }
    if (enabled && !cachedNPC.hasTrait(QuestGiverNPCTrait.class)) {
      cachedNPC.addTrait(QuestGiverNPCTrait.class);
    } else if (!enabled && cachedNPC.hasTrait(QuestGiverNPCTrait.class)) {
      cachedNPC.removeTrait(QuestGiverNPCTrait.class);
    }
    return true;
  }

}
