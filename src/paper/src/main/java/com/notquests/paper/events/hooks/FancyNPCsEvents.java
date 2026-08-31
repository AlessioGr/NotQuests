package com.notquests.paper.events.hooks;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.notquests.core.npc.NQNPCID;
import com.notquests.paper.NotQuests;

/**
 * Handles clicks on FancyNPCs NPCs (via the API's {@link NpcInteractEvent}). FancyNPCs has no
 * Citizens-style trait/navigator system, so this mirrors the generic Citizens click behaviour:
 * deliver-items / talk-to-npc / completion-npc objectives, quest previews and conversations.
 * (Citizens-only features such as escort and movement pausing are not applicable to packet NPCs.)
 */
public class FancyNPCsEvents implements Listener {
  private final NotQuests main;

  public FancyNPCsEvents(final NotQuests main) {
    this.main = main;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onNpcInteract(final NpcInteractEvent event) {
    final String npcId = event.getNpc().getData().getId();
    final Player player = event.getPlayer();
    main.getCorePlugin().nativeNpcClicked(
        player.getUniqueId().toString(),
        "fancynpcs",
        NQNPCID.fromString(npcId),
        event.getNpc().getData().getName(),
        selectionActionId(player),
        null,
        null,
        null);
  }

  private int selectionActionId(final Player player) {
    final ItemStack heldItem = player.getInventory().getItemInMainHand();
    if (heldItem.getType() == Material.AIR || heldItem.getItemMeta() == null) {
      return -1;
    }
    final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();
    final Integer actionId = container.get(
        new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action"),
        PersistentDataType.INTEGER);
    return actionId == null ? -1 : actionId;
  }
}
