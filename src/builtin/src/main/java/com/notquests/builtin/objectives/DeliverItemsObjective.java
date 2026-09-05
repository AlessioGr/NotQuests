package com.notquests.builtin.objectives;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.util.Map;

public final class DeliverItemsObjective {
    private DeliverItemsObjective() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.objectives().objective("DeliverItems")
                .displayName("Deliver Items")
                .description("Counts selected items delivered to a configured NPC.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Material, custom item, hand item, any item, or comma-separated item list the player must deliver.")
                .field(
                        "amount",
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Number of matching items the player must deliver.")
                .field(
                        "npc",
                        adapter.fields().npcSelector().config("specifics.recipientNPC"),
                        "NPC that receives the delivered items.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final String description = adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.deliverItems.base",
                            questPlayer,
                            activeObjective,
                            Map.of(
                                    "%ITEMTODELIVERTYPE%",
                                    listedMaterials(objective.itemSelection("materials")),
                                    "%ITEMTODELIVERNAME%",
                                    "",
                                    "%(%",
                                    "",
                                    "%)%",
                                    ""));
                    final String npc = objective.text("npc");
                    if (npc == null || npc.isBlank()) {
                        return description + "\n" + adapter.objectiveTaskText(
                                "chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available",
                                questPlayer,
                                activeObjective,
                                Map.of());
                    }
                    return description + "\n" + adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.deliverItems.deliver-to-npc",
                            questPlayer,
                            activeObjective,
                            Map.of("%NPCNAME%", npcName(adapter, npc)));
                })
                .onPlayerInteractNpc((event, objective) -> {
                    if (!sameNpc(objective.text("npc"), event.npcSelector())) {
                        return;
                    }
                    final ItemSelection selection = objective.itemSelection("materials");
                    if (selection == null) {
                        return;
                    }
                    final PlatformPlayer questPlayer = objective.questPlayer();
                    if (questPlayer == null) {
                        return;
                    }
                    final int amountLeft = (int) Math.ceil(objective.progressNeeded() - objective.currentProgress());
                    if (amountLeft <= 0) {
                        return;
                    }
                    final int delivered = questPlayer.removeItems(
                            plugin.resolveItems(selection), amountLeft);
                    if (delivered <= 0) {
                        return;
                    }
                    objective.addProgress(delivered);
                    questPlayer.sendMessage("<GREEN>You have delivered <highlight>"
                            + delivered
                            + "</highlight> items to <highlight>"
                            + event.npcName());
                })
                .register();
    }

    private static boolean sameNpc(final String expected, final String actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(actual);
    }

    private static String listedMaterials(final ItemSelection selection) {
        return selection == null ? "???" : selection.listedMaterials("main");
    }

    private static String npcName(final NotQuestsAdapter adapter, final String selector) {
        final NotQuestsAdapter.NpcSelection selection = adapter.npcSelection(selector);
        if (selection == null) {
            return selector;
        }
        if (selection.npcName() != null && !selection.npcName().isBlank()) {
            return selection.npcName();
        }
        if (selection.npcId() != null) {
            return selection.npcId().getEitherAsString();
        }
        return selection.selector() == null || selection.selector().isBlank()
                ? selector
                : selection.selector();
    }
}
