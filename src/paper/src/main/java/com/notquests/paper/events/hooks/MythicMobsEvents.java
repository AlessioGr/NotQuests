package com.notquests.paper.events.hooks;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.paper.NotQuests;

public class MythicMobsEvents implements Listener {
    private final NotQuests main;

    public MythicMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onMythicMobDeath(final MythicMobDeathEvent event) {
        //KillMobs objectives
        if (event.getKiller() instanceof final Player player) {
            final MythicMob killedMob = event.getMobType();
            main.getCorePlugin().mythicMobDied(
                    player.getUniqueId().toString(),
                    killedMob.getInternalName(),
                    killedMob.getFaction(),
                    event.getEntity() == event.getKiller());
        }
    }

}
