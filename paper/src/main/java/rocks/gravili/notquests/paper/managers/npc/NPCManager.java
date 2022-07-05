package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import rocks.gravili.notquests.paper.NotQuests;

public class NPCManager {
  private final NotQuests main;

  private final ArrayList<NQNPC> npcs;
  public NPCManager(final NotQuests main){
    this.main = main;
    npcs = new ArrayList<>();
  }

  public final NQNPC getOrCreateNQNpc(final String type, final int npcID){
    for(final NQNPC nqnpc : npcs){
      if(nqnpc.getID() == npcID && nqnpc.getNPCType().equalsIgnoreCase(type)){
        return nqnpc;
      }
    }

    if(type.equalsIgnoreCase("Citizens")){
      return new CitizensNPC(main, npcID);
    }

    return null;
  }


}
