package com.notquests.paper.events.hooks;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.core.NotQuestsPlugin.EliteMobCredit;
import com.notquests.paper.NotQuests;

public class EliteMobsEvents implements Listener {
    private final NotQuests main;

    public EliteMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onEliteMobDeath(EliteMobDeathEvent event) {
        final EliteEntity eliteMob = event.getEliteEntity();

        main.getCorePlugin().eliteMobDied(
                eliteMob.getDamagers().entrySet().stream()
                        .map(entry -> new EliteMobCredit(
                                entry.getKey().getUniqueId().toString(),
                                entry.getValue()))
                        .toList(),
                eliteMob.getName(),
                eliteMob.getLevel(),
                eliteMob.getMaxHealth(),
                eliteMob.getSpawnReason().toString());
    }

}
