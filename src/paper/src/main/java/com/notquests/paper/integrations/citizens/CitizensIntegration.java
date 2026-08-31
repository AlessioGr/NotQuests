package com.notquests.paper.integrations.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.core.platform.NQLocation;
import com.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Atomic Citizens API operations used by the core NPC and escort owners. */
public class CitizensIntegration {
  private final NotQuests main;

  public CitizensIntegration(final NotQuests main) {
    this.main = main;
  }

  public ArrayList<Integer> getAllNPCIDs() {
    final ArrayList<Integer> npcIds = new ArrayList<>();
    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
      npcIds.add(npc.getId());
    }
    return npcIds;
  }

  public void registerQuestGiverTrait() {
    deregisterQuestGiverTrait();
    CitizensAPI.getTraitFactory().registerTrait(
        TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));
    main.getMain().getServer().getPluginManager().registerEvents(
        new QuestGiverNPCTrait.NPCTPListener(), main.getMain());
  }

  public void onDisable() {
    deregisterQuestGiverTrait();
  }

  private static void deregisterQuestGiverTrait() {
    final ArrayList<TraitInfo> traits = new ArrayList<>();
    for (final TraitInfo trait : CitizensAPI.getTraitFactory().getRegisteredTraits()) {
      if ("nquestgiver".equals(trait.getTraitName())) {
        traits.add(trait);
      }
    }
    for (final TraitInfo trait : traits) {
      CitizensAPI.getTraitFactory().deregisterTrait(trait);
    }
  }

  public void stopFollowing(final int npcId) {
    final NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
    if (npc == null) {
      return;
    }
    final FollowTrait follow = followTrait(npc);
    if (follow != null) {
      npc.removeTrait(follow.getClass());
    }
    npc.despawn();
  }

  public boolean startFollowing(
      final int escortNpcId,
      final String playerIdentifier,
      final NQLocation spawnLocation) {
    final NPC escortNpc = CitizensAPI.getNPCRegistry().getById(escortNpcId);
    final Player player;
    try {
      player = Bukkit.getPlayer(UUID.fromString(playerIdentifier));
    } catch (final IllegalArgumentException exception) {
      return false;
    }
    final Location nativeSpawnLocation = bukkitLocation(spawnLocation);
    if (escortNpc == null || player == null || nativeSpawnLocation == null) {
      return false;
    }

    final Runnable start = () -> {
      FollowTrait follow = followTrait(escortNpc);
      if (follow == null) {
        follow = new FollowTrait();
        escortNpc.addTrait(follow);
      }
      if (!escortNpc.isSpawned()) {
        escortNpc.spawn(nativeSpawnLocation);
      }
      if (follow.getFollowing() == null || !follow.getFollowing().equals(player)) {
        follow.setProtect(false);
        follow.follow(player);
      }
    };
    if (Bukkit.isPrimaryThread()) {
      start.run();
    } else {
      Bukkit.getScheduler().runTask(main.getMain(), start);
    }
    return true;
  }

  public String npcName(final int npcId) {
    final NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
    return npc == null ? null : miniMessageName(npc.getName());
  }

  public void renderIndicator(
      final NQNPCID npcId,
      final Indicator indicator,
      final Set<String> visiblePlayerIdentifiers) {
    if (npcId == null || indicator == null) {
      return;
    }
    final NPC nativeNpc = CitizensAPI.getNPCRegistry().getById(npcId.getIntegerID());
    if (nativeNpc == null || !nativeNpc.isSpawned() || nativeNpc.getEntity() == null) {
      return;
    }
    final Entity entity = nativeNpc.getEntity();
    if (indicator.showParticle()) {
      final Location location = entity.getLocation();
      entity.getWorld().spawnParticle(
          main.particle(indicator.particleType()),
          location.getX() - 0.25 + Math.random() / 2,
          location.getY() + 1.75 + Math.random() / 2,
          location.getZ() - 0.25 + Math.random() / 2,
          indicator.particleCount());
    }
    if (!indicator.showText()) {
      return;
    }
    final ArmorStand hologram = hologram(entity, indicator.text());
    if (hologram == null) {
      return;
    }
    for (final Entity nearby : entity.getNearbyEntities(16, 16, 16)) {
      if (!(nearby instanceof final Player player)) {
        continue;
      }
      setHologramVisible(
          player,
          hologram,
          visiblePlayerIdentifiers != null
              && visiblePlayerIdentifiers.contains(player.getUniqueId().toString()));
    }
  }

  private void setHologramVisible(
      final Player player,
      final ArmorStand hologram,
      final boolean visible) {
    hologram.setCustomNameVisible(true);
    if (visible) {
      player.showEntity(main.getMain(), hologram);
    } else {
      player.hideEntity(main.getMain(), hologram);
    }
  }

  public List<String> nearbyPlayerIdentifiers(
      final NQNPCID npcId,
      final double radius) {
    if (npcId == null) {
      return List.of();
    }
    final NPC nativeNpc = CitizensAPI.getNPCRegistry().getById(npcId.getIntegerID());
    if (nativeNpc == null || !nativeNpc.isSpawned() || nativeNpc.getEntity() == null) {
      return List.of();
    }
    final double checkedRadius = Math.max(0.0d, radius);
    return nativeNpc.getEntity().getNearbyEntities(checkedRadius, checkedRadius, checkedRadius).stream()
        .filter(Player.class::isInstance)
        .map(Player.class::cast)
        .map(player -> player.getUniqueId().toString())
        .toList();
  }

  private ArmorStand hologram(final Entity npcEntity, final String text) {
    if (!npcEntity.getPassengers().isEmpty()) {
      if (npcEntity.getPassengers().getFirst() instanceof final ArmorStand hologram) {
        hologram.customName(main.parse(text));
        return hologram;
      }
      return null;
    }
    final ArmorStand hologram = npcEntity.getWorld().spawn(npcEntity.getLocation(), ArmorStand.class);
    hologram.setVisible(false);
    hologram.setSmall(true);
    hologram.setCustomNameVisible(false);
    hologram.customName(main.parse(text));
    npcEntity.addPassenger(hologram);
    return hologram;
  }

  private static Location bukkitLocation(final NQLocation location) {
    if (location == null || location.worldName().isBlank()) {
      return null;
    }
    final World world = Bukkit.getWorld(location.worldName());
    return world == null
        ? null
        : new Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
  }

  private static FollowTrait followTrait(final NPC npc) {
    for (final Trait trait : npc.getTraits()) {
      if (trait instanceof final FollowTrait follow) {
        return follow;
      }
    }
    return null;
  }

  private String miniMessageName(final String legacyName) {
    return main.getMiniMessage().serialize(
        LegacyComponentSerializer.legacyAmpersand().deserialize(legacyName.replace('§', '&')));
  }
}
