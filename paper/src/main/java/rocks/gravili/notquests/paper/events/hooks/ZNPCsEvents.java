package rocks.gravili.notquests.paper.events.hooks;

import io.github.znetworkw.znpcservers.npc.NPC;
import io.github.znetworkw.znpcservers.npc.event.NPCInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;

public class ZNPCsEvents implements Listener {
  private final NotQuests main;

  public ZNPCsEvents(final NotQuests main) {
    this.main = main;
    main.getLogManager().info("Initialized ZNPCsEvents");
  }

  @EventHandler
  private void onNPCInteract(NPCInteractEvent event) {
    final NPC npc = event.getNpc();
  }

}
