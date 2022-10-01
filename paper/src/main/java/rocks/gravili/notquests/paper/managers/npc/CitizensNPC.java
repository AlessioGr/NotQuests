package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.managers.integrations.citizens.QuestGiverNPCTrait;
import rocks.gravili.notquests.paper.structs.Quest;

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

  @Override
  public void bindToConversation(Conversation conversation) {
    if (!updateCachedNPC()) {
      return;
    }

    boolean hasTrait = false;
    for (Trait trait : cachedNPC.getTraits()) {
      if (trait.getName().contains("questgiver")) {
        hasTrait = true;
        break;
      }
    }
    if (!cachedNPC.hasTrait(QuestGiverNPCTrait.class) && !hasTrait) {
      main.getLogManager()
          .info(
              "Trying to add Conversation <highlight>"
                  + conversation.getIdentifier()
                  + "</highlight> to NPC with ID <highlight>"
                  + cachedNPC.getId()
                  + "</highlight>...");

      cachedNPC.addTrait(QuestGiverNPCTrait.class);
    }
  }

  @Override
  public String removeQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    if (!updateCachedNPC()) {
      return "NPC not found!";
    }

    final ArrayList<Trait> npcTraitsToRemove = new ArrayList<>();
    for (final Trait trait : cachedNPC.getTraits()) {
      if (trait.getName().equalsIgnoreCase("nquestgiver")) {
        npcTraitsToRemove.add(trait);
      }
    }
    for (final Trait trait : npcTraitsToRemove) {
      cachedNPC.removeTrait(trait.getClass());
    }
    npcTraitsToRemove.clear();

    // cachedNPC.removeTrait(QuestGiverNPCTrait.class); //This is not enough to ensure compatibility
    // with ServerUtils
    return "";
  }

  @Override
  public String addQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) {
    if (!updateCachedNPC()) {
      return "NPC not found";
    }
    boolean hasTrait = false;
    for (Trait trait : cachedNPC.getTraits()) {
      if (trait.getName().contains("questgiver")) {
        hasTrait = true;
        break;
      }
    }
    if (!cachedNPC.hasTrait(QuestGiverNPCTrait.class) && !hasTrait) {
      // System.out.println("§2NPC doesnt have trait. giving him trait... Cur traits: " +
      // npc.getTraits().toString());
      cachedNPC.addTrait(QuestGiverNPCTrait.class);
    }
    return "";
  }

  @Override
  public final Entity getEntity() {
    if (!updateCachedNPC()) {
      return null;
    }
    return cachedNPC.getEntity();
  }
}
