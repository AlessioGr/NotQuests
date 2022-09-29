package rocks.gravili.notquests.paper.managers.integrations.znpcs;

import io.github.znetworkw.znpcservers.ServersNPC;
import io.github.znetworkw.znpcservers.npc.NPC;
import java.util.ArrayList;
import rocks.gravili.notquests.paper.NotQuests;

public class ZNPCsManager {
  private final NotQuests main;
  private final ServersNPC serversNPC;

  public ZNPCsManager(final NotQuests main) {
    this.main = main;
    serversNPC = (ServersNPC) main.getMain().getServer().getPluginManager().getPlugin("ServersNPC");
  }


  public final ArrayList<Integer> getAllNPCIDs(){
    final ArrayList<Integer> npcIDs = new ArrayList<>();

    for (final NPC npc : NPC.all()) {
      npcIDs.add(npc.getEntityID());
    }
    return npcIDs;
  }
}
