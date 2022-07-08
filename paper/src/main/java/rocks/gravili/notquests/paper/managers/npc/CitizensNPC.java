package rocks.gravili.notquests.paper.managers.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.managers.integrations.citizens.QuestGiverNPCTrait;

public class CitizensNPC extends NQNPC {
  private NPC cachedNPC;
  private final int npcID;

  public CitizensNPC(final NotQuests main, final int npcID){
    super(main, "Citizens");
    this.npcID = npcID;
    this.cachedNPC = CitizensAPI.getNPCRegistry()
        .getById(npcID);
  }

  private boolean updateCachedNPC(){
    if(cachedNPC == null) {
      cachedNPC = CitizensAPI.getNPCRegistry()
          .getById(npcID);
    }
    return cachedNPC != null;
  }

  public String getName() {
    if(!updateCachedNPC()) {
      return null;
    }
      return cachedNPC.getName();
  }
  public int getID() {
    if(!updateCachedNPC()) {
      return npcID;
    }
    return cachedNPC.getId();
  }

  @Override
  public void bindToConversation(Conversation conversation) {
    if(!updateCachedNPC()) {
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
  public void removeQuestGiverNPCTrait() {
    if(!updateCachedNPC()) {
      return;
    }
    cachedNPC.removeTrait(QuestGiverNPCTrait.class);
  }

  @Override
  public void addQuestGiverNPCTrait() {
    if(!updateCachedNPC()) {
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
      // System.out.println("§2NPC doesnt have trait. giving him trait... Cur traits: " +
      // npc.getTraits().toString());
      cachedNPC.addTrait(QuestGiverNPCTrait.class);
    }
  }
}
