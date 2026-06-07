/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

/**
 * A minimal {@link CommandSourceStack} backed by a plain {@link CommandSender}.
 *
 * <p>The modern {@link org.incendo.cloud.paper.PaperCommandManager} keeps NotQuests on the
 * {@code CommandSender} sender type via a {@code SenderMapper}. Mapping a source stack to a sender is
 * trivial ({@link CommandSourceStack#getSender()}); the reverse (sender → source stack) has no Paper
 * factory, so we wrap the sender here. NotQuests commands don't use relative coordinates, so the
 * location/executor are best-effort and only matter if cloud needs to round-trip a sender.
 */
final class CommandSenderSourceStack implements CommandSourceStack {

  private final CommandSender sender;

  CommandSenderSourceStack(final CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public CommandSender getSender() {
    return sender;
  }

  @Override
  public Location getLocation() {
    if (sender instanceof final Entity entity) {
      return entity.getLocation();
    }
    if (sender instanceof final BlockCommandSender blockSender) {
      return blockSender.getBlock().getLocation();
    }
    final World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    return world != null ? world.getSpawnLocation() : new Location(null, 0, 0, 0);
  }

  @Override
  public Entity getExecutor() {
    return sender instanceof final Entity entity ? entity : null;
  }

  @Override
  public CommandSourceStack withLocation(final Location location) {
    return this;
  }

  @Override
  public CommandSourceStack withExecutor(final Entity executor) {
    return this;
  }
}
