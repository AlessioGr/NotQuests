package rocks.gravili.notquests.paper.managers.npc;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;

public class ArmorstandNPC extends NQNPC {
  private ArmorStand cachedArmorstand;
  private final NQNPCID npcID;

  public ArmorstandNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "armorstand");
    this.npcID = npcID;
    this.cachedArmorstand = (ArmorStand) main.getMain().getServer().getEntity(npcID.getUUIDID());
    }

  private boolean updateCachedNPC() {
    if (cachedArmorstand == null) {
      cachedArmorstand = (ArmorStand) main.getMain().getServer().getEntity(npcID.getUUIDID());
    }
    return cachedArmorstand != null;
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

  @Override
  public void bindToConversation(Conversation conversation) { //TODO: needed?

  }

  @Override
  public void removeQuestGiverNPCTrait() { //TODO: needed?

  }

  @Override
  public void addQuestGiverNPCTrait() { //TODO: needed?

  }

  @Override
  public final Entity getEntity() {
    if (!updateCachedNPC()) {
      return null;
    }
    return cachedArmorstand;
  }
}
