package com.notquests.paper.events;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.notquests.paper.NotQuests;

/** Translates Bukkit armor-stand events into core NPC operations. */
public class ArmorStandEvents implements Listener {
    private final NotQuests main;

    public ArmorStandEvents(final NotQuests main) {
        this.main = main;
    }

    @EventHandler
    private void onArmorStandClick(final PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof final ArmorStand armorStand)) {
            return;
        }

        final Player player = event.getPlayer();
        final String selector = "armorstand:" + armorStand.getUniqueId();
        final String npcName = main.armorStands().getArmorStandName(armorStand);
        final ItemStack heldItem = player.getInventory().getItem(event.getHand());

        int selectionId = -1;
        int itemId = -1;
        int objectiveId = -1;
        String questName = "";
        String conversationName = "";
        if (heldItem.getType() != Material.AIR && heldItem.hasItemMeta()) {
            final PersistentDataContainer data = heldItem.getItemMeta().getPersistentDataContainer();
            final NamespacedKey selectionKey = new NamespacedKey(
                    main.getMain(), "notquests-nqnpc-selector-with-action");
            final Integer storedSelectionId = data.get(selectionKey, PersistentDataType.INTEGER);
            final NamespacedKey toolKey = new NamespacedKey(main.getMain(), "notquests-item");
            final Integer storedItemId = data.get(toolKey, PersistentDataType.INTEGER);
            final NamespacedKey questKey = new NamespacedKey(main.getMain(), "notquests-questname");
            final NamespacedKey objectiveKey = new NamespacedKey(main.getMain(), "notquests-objectiveid");
            final NamespacedKey conversationKey = new NamespacedKey(main.getMain(), "notquests-conversation");
            final Integer storedObjectiveId = data.get(objectiveKey, PersistentDataType.INTEGER);
            selectionId = storedSelectionId == null ? -1 : storedSelectionId;
            itemId = storedItemId == null ? -1 : storedItemId;
            objectiveId = storedObjectiveId == null ? -1 : storedObjectiveId;
            questName = data.getOrDefault(questKey, PersistentDataType.STRING, "");
            conversationName = data.getOrDefault(conversationKey, PersistentDataType.STRING, "");
        }

        final var questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        final var interaction = main.getCorePlugin().playerInteractedWithArmorStand(
                questPlayer,
                selectionId,
                itemId,
                questName,
                objectiveId,
                conversationName,
                selector,
                npcName);
        for (final String message : interaction.messages()) {
            player.sendMessage(main.parse(message));
        }
        if (interaction.cancelPlatformInteraction()) {
            event.setCancelled(true);
        }
    }
}
