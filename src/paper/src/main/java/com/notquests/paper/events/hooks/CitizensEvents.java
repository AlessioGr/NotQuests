package com.notquests.paper.events.hooks;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.paper.NotQuests;

public class CitizensEvents implements Listener {
    private static final PotionEffect FOCUS_SLOWNESS =
            new PotionEffect(PotionEffectType.SLOWNESS, 4, 2, false, false);

    private final NotQuests main;

    public CitizensEvents(final NotQuests main) {
        this.main = main;
    }

    @EventHandler
    private void onNPCDeathEvent(NPCDeathEvent event) {
        main.getCorePlugin().npcDied(String.valueOf(event.getNPC().getId()), "");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onNPCClickEvent(final NPCRightClickEvent event) {
        final NPC npc = event.getNPC();
        final Player player = event.getClicker();
        final NpcAttachments.ClickEffects effects = main.getCorePlugin().nativeNpcClicked(
                player.getUniqueId().toString(),
                "citizens",
                NQNPCID.fromInteger(npc.getId()),
                npcName(npc),
                selectionActionId(player),
                escortNpcId -> escortNpc(player, escortNpcId),
                () -> npc.getNavigator().setPaused(false),
                focus(player, npc));
        if (effects.pauseNavigation()) {
            npc.getNavigator().cancelNavigation();
            npc.getNavigator().setPaused(true);
        }
        if (effects.stopPlayerMovement()) {
            player.setVelocity(new Vector(0, 0, 0));
        }
    }

    private int selectionActionId(final Player player) {
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR || heldItem.getItemMeta() == null) {
            return -1;
        }
        final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();
        final NamespacedKey actionKey =
                new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action");
        final Integer actionId = container.get(actionKey, PersistentDataType.INTEGER);
        return actionId == null ? -1 : actionId;
    }

    private NpcAttachments.EscortNpc escortNpc(final Player player, final int npcId) {
        final NPC escort = CitizensAPI.getNPCRegistry().getById(npcId);
        if (escort == null) {
            return null;
        }
        final Entity entity = escort.getEntity();
        final boolean sameWorld = escort.isSpawned()
                && entity != null
                && entity.isValid()
                && entity.getWorld().equals(player.getWorld());
        return new NpcAttachments.EscortNpc(
                sameWorld,
                sameWorld
                        ? entity.getLocation().distanceSquared(player.getLocation())
                        : Double.POSITIVE_INFINITY,
                npcName(escort),
                () -> {
                    if (escort.hasTrait(FollowTrait.class)) {
                        escort.removeTrait(FollowTrait.class);
                    }
                    escort.despawn();
                });
    }

    private ConversationManager.Focus.Native focus(final Player player, final NPC npc) {
        return new ConversationManager.Focus.Native() {
            @Override
            public ConversationManager.Focus.Observation observe() {
                final Location playerLocation = player.getLocation();
                final Location playerEye = player.getEyeLocation();
                final Entity entity = npc.isSpawned() ? npc.getEntity() : null;
                final boolean npcValid = entity != null
                        && entity.isValid()
                        && entity.getWorld().equals(player.getWorld());
                final Location npcEye = npcValid
                        ? entity.getLocation().add(0, entity.getHeight() - 0.2, 0)
                        : playerEye;
                return new ConversationManager.Focus.Observation(
                        player.isOnline(),
                        npcValid,
                        playerLocation.getWorld() == null ? "" : playerLocation.getWorld().getName(),
                        playerLocation.getX(),
                        playerLocation.getZ(),
                        playerLocation.getYaw(),
                        playerLocation.getPitch(),
                        playerEye.getX(),
                        playerEye.getY(),
                        playerEye.getZ(),
                        npcEye.getX(),
                        npcEye.getY(),
                        npcEye.getZ());
            }

            @Override
            public void applySlowness() {
                player.addPotionEffect(FOCUS_SLOWNESS);
            }

            @Override
            public void rotate(final float yaw, final float pitch) {
                final Location target = player.getLocation();
                target.setYaw(yaw);
                target.setPitch(pitch);
                player.teleport(target);
            }

            @Override
            public void cleanup() {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        };
    }

    private String npcName(final NPC npc) {
        return main.getMiniMessage().serialize(
                LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(npc.getName().replace("§", "&")));
    }

    @EventHandler
    private void onCitizensEnable(final CitizensEnableEvent event) {
        main.getCorePlugin().nativeNpcIntegrationReloaded(
                main.integrations().citizens()::registerQuestGiverTrait);
    }

    @EventHandler
    private void onCitizensReload(final CitizensReloadEvent event) {
        main.getCorePlugin().nativeNpcIntegrationReloaded(
                main.integrations().citizens()::registerQuestGiverTrait);
    }
}
