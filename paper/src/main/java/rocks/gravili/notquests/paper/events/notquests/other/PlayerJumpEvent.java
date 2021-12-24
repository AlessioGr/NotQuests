/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.events.notquests.other;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.util.Vector;

public class PlayerJumpEvent extends PlayerEvent implements Cancellable {
    public static final Listener listener = new PlayerJumpEventListener();
    private static final HandlerList handlers = new HandlerList();
    private final PlayerStatisticIncrementEvent playerStatisticIncrementEvent;
    private boolean isCancelled = false;


    public PlayerJumpEvent(Player player, PlayerStatisticIncrementEvent playerStatisticIncrementEvent) {
        super(player);
        this.playerStatisticIncrementEvent = playerStatisticIncrementEvent;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
        if (cancel) {
            player.setVelocity(new Vector());
            playerStatisticIncrementEvent.setCancelled(cancel);
        }
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private static class PlayerJumpEventListener implements Listener {
        @EventHandler
        public void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
            if (event.getStatistic() == Statistic.JUMP) {
                Bukkit.getServer().getPluginManager().callEvent(new PlayerJumpEvent(event.getPlayer(), event));
            }
        }
    }
}