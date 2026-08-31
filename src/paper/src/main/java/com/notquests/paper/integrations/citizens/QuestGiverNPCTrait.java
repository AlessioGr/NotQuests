package com.notquests.paper.integrations.citizens;

import net.citizensnpcs.api.event.NPCTeleportEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.core.npc.NQNPCID;
import com.notquests.paper.NotQuests;

import java.util.List;

/** Marks a Citizens NPC as a NotQuests quest giver and bridges native trait lifecycle events. */
public class QuestGiverNPCTrait extends Trait {

  final NotQuests main;

  public QuestGiverNPCTrait() {
    super("nquestgiver");
    this.main = NotQuests.getInstance();
  }

  @Override
  public void onAttach() {
    main.getCorePlugin().npcTraitAttached(npc.getId(), npc.getName());
  }

  @Override
  public void onDespawn() {
    if(getNPC().getEntity() != null){
      getNPC().getEntity().getPassengers().forEach(Entity::remove);
    }
  }

  @Override
  public void onRemove() {
    if (getNPC() == null) {
      return;
    }
    if(getNPC().getEntity() != null){
      getNPC().getEntity().getPassengers().forEach(Entity::remove);
    }
    main.getCorePlugin().npcRemoved(
        "citizens", NQNPCID.fromInteger(getNPC().getId()), getNPC().getName());
  }

  public static class NPCTPListener implements Listener{
    @EventHandler
    public void onNPCTp(NPCTeleportEvent npcTp){
      if(npcTp.getNPC().getEntity() == null){
        return;
      }
      final List<Entity> entityList=npcTp.getNPC().getEntity().getPassengers();
      if(entityList.size()==0)return;
      NotQuests.getInstance().getCorePlugin().nativeNpcTeleported(
          () -> {
            if (npcTp.getNPC().getEntity() != null) {
              npcTp.getNPC().getEntity().addPassenger(entityList.getFirst());
            }
          });

    }
  }
}
