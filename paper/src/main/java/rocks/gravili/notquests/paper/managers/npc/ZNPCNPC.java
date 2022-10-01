package rocks.gravili.notquests.paper.managers.npc;

import io.github.znetworkw.znpcservers.npc.NPC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.Quest;

public class ZNPCNPC extends NQNPC {
  private NPC cachedNPC;
  private final NQNPCID npcID;

  public ZNPCNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "znpcs");
    this.npcID = npcID;
    this.cachedNPC = NPC.find(npcID.getIntegerID());
  }

  private boolean updateCachedNPC() {
    if (cachedNPC == null) {
      cachedNPC = NPC.find(npcID.getIntegerID());
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
                .deserialize(""+cachedNPC.getEntityID()));
  }

  @Override
  public NQNPCID getID() {
    if (!updateCachedNPC()) {
      return npcID;
    }
    return npcID;
  }

  @Override
  public void bindToConversation(Conversation conversation) {
    if (!updateCachedNPC()) {
      return;
    }

    return;
  }

  @Override
  public String removeQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    return "";
  }

  @Override
  public String addQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    return "";
  }

  @Override
  public final Entity getEntity() {
    if (!updateCachedNPC()) {
      return null;
    }
    return (Entity) cachedNPC.getBukkitEntity();
  }
}
