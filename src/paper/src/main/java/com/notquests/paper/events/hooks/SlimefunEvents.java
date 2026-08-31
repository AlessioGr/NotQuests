package com.notquests.paper.events.hooks;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerPreResearchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.paper.NotQuests;

public class SlimefunEvents implements Listener {
    private final NotQuests main;

    public SlimefunEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerResearch(final PlayerPreResearchEvent e) {
        main.getCorePlugin().slimefunResearchCompleted(
                e.getPlayer().getUniqueId().toString(),
                e.getResearch().getCost());
    }
}
