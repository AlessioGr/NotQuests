/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.events.hooks;

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
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.DeliverItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.TalkToNPCObjective;

import java.util.concurrent.atomic.AtomicBoolean;

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
    main.getLogManager().info("Initialized FancyNPCsEvents");
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onNpcInteract(final NpcInteractEvent event) {
    final String npcId = event.getNpc().getData().getId();
    final NQNPC nqNPC = main.getNPCManager().getOrCreateNQNpc("fancynpcs", NQNPCID.fromString(npcId));
    final Player player = event.getPlayer();
    final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

    if (nqNPC == null) {
      questPlayer.sendDebugMessage("Error: NQNpc is null");
      return;
    }

    final String npcName = event.getNpc().getData().getName();

    // Special selector item (admin NQNPC selection action)
    final ItemStack heldItem = player.getInventory().getItemInMainHand();
    if (player.hasPermission("notquests.admin.armorstandeditingitems")
        && heldItem.getType() != Material.AIR
        && heldItem.getItemMeta() != null) {
      final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();
      final NamespacedKey specialActionItemKey =
          new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action");
      if (container.has(specialActionItemKey, PersistentDataType.INTEGER)) {
        final int id = container.get(specialActionItemKey, PersistentDataType.INTEGER);
        main.getNPCManager().executeNPCSelectionAction(nqNPC, id);
      }
    }

    final AtomicBoolean handledObjective = new AtomicBoolean(false);

    // Deliver items
    questPlayer.queueObjectiveCheck(activeObjective -> {
      if (activeObjective.getObjective() instanceof final DeliverItemsObjective deliverItemsObjective) {
        if (nqNPC.equals(deliverItemsObjective.getRecipientNPC())) {
          for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null) {
              if (!deliverItemsObjective.getItemStackSelection().checkIfIsIncluded(itemStack)) {
                continue;
              }
              final double progressLeft =
                  activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();
              if (progressLeft == 0) {
                continue;
              }
              handledObjective.set(true);
              if (progressLeft < itemStack.getAmount()) {
                itemStack.setAmount((itemStack.getAmount() - (int) progressLeft));
                activeObjective.addProgress(progressLeft, nqNPC);
                player.sendMessage(main.parse(
                    "<GREEN>You have delivered <highlight>" + progressLeft + "</highlight> items to <highlight>" + npcName));
                break;
              } else {
                player.getInventory().removeItemAnySlot(itemStack);
                activeObjective.addProgress(itemStack.getAmount(), nqNPC);
                player.sendMessage(main.parse(
                    "<GREEN>You have delivered <highlight>" + itemStack.getAmount() + "</highlight> items to <highlight>" + npcName));
              }
            }
          }
          player.updateInventory();
        }
      }
    });

    // Talk to NPC
    questPlayer.queueObjectiveCheck(activeObjective -> {
      if (activeObjective.getObjective() instanceof final TalkToNPCObjective talkToNPCObjective) {
        if (nqNPC.equals(talkToNPCObjective.getNPCtoTalkTo())) {
          activeObjective.addProgress(1, nqNPC);
          player.sendMessage(main.parse("<GREEN>You talked to <highlight>" + npcName));
          handledObjective.set(true);
        }
      }
    });

    // Completion NPC (objectives that complete when a specific NPC is clicked)
    questPlayer.queueObjectiveCheck(activeObjective -> {
      if (activeObjective.getObjective().getCompletionNPC() != null) {
        activeObjective.addProgress(0, nqNPC);
      }
    });

    questPlayer.checkQueuedObjectives();

    if (handledObjective.get()) {
      questPlayer.sendDebugMessage("Returning because of handled objective");
      return;
    }

    // Quest preview
    main.getQuestManager().sendQuestsPreviewOfQuestShownNPCs(nqNPC, questPlayer);

    // Conversations
    final ConversationManager manager = main.getConversationManager();
    if (manager != null) {
      final Conversation foundConversation = manager.getConversationForNPC(nqNPC);
      if (foundConversation != null) {
        manager.playConversation(questPlayer, foundConversation, nqNPC);
      }
    }
  }
}
