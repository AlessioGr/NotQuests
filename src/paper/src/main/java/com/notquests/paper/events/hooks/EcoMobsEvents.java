package com.notquests.paper.events.hooks;

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.impl.ConfigDrivenEcoMobKt;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

public class EcoMobsEvents implements Listener {

  private final NotQuests main;

  public EcoMobsEvents(NotQuests main) {
    this.main = main;
  }

  // We listen to the vanilla EntityDeathEvent (which always fires) and ask EcoMobs whether the dead
  // entity is one of its mobs, rather than relying on EcoMobs' own kill event (which does not fire
  // reliably for plain combat kills).
  @EventHandler
  public void onEcoMobDeath(final EntityDeathEvent event) {
    if (!(event.getEntity() instanceof final Mob mob)) {
      return;
    }
    final Player killer = mob.getKiller();
    if (killer == null) {
      return;
    }
    final EcoMob ecoMob = ConfigDrivenEcoMobKt.getEcoMob(mob);
    if (ecoMob == null) {
      return; // not an EcoMobs mob
    }

    final PaperPlayer questPlayer =
        main.getRegistryAdapter().activePaperPlayer(killer.getUniqueId());
    if (questPlayer == null) {
      return;
    }

    main.getCorePlugin().playerKilledEntity(
        questPlayer,
        new EcoMobKillEvent(
            ecoMob.getID(),
            mob.customName() == null
                ? ""
                : PlainTextComponentSerializer.plainText().serialize(mob.customName())));
  }

  private record EcoMobKillEvent(String entityTypeId, String plainCustomName)
      implements Objectives.EntityEvent {
    @Override
    public boolean selfAttributedDeath() {
      return false;
    }
  }
}
