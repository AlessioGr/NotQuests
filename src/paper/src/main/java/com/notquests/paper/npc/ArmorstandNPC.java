package com.notquests.paper.npc;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.npc.NQNPCID;
import com.notquests.paper.NotQuests;

public class ArmorstandNPC extends NQNPC {
  private ArmorStand cachedArmorstand;
  private final NQNPCID npcID;

  public ArmorstandNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "armorstand");
    this.npcID = npcID;
    this.cachedArmorstand = armorStand();
    }

  private boolean updateCachedNPC() {
    if (cachedArmorstand == null) {
      cachedArmorstand = armorStand();
    }
    return cachedArmorstand != null;
  }

  private ArmorStand armorStand() {
    final Entity entity = main.getMain().getServer().getEntity(npcID.getUUIDID());
    return entity instanceof final ArmorStand armorStand ? armorStand : null;
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
                .deserialize(cachedArmorstand.getName().replace("§", "&")));
  }

  @Override
  public NQNPCID getID() {
    if (!updateCachedNPC()) {
      return npcID;
    }
    return NQNPCID.fromUUID(cachedArmorstand.getUniqueId());
  }

}
