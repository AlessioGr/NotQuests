package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class TalkToNPCObjective {
    private TalkToNPCObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("TalkToNPC")
                .displayName("Talk to NPC")
                .description("Counts when the player right-clicks a configured NPC.")
                .field(
                        "npc",
                        adapter.fields().npcSelector().config("specifics.npcToTalkTo"),
                        "NPC the player must talk to.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final String npc = objective.text("npc");
                    if (npc == null || npc.isBlank()) {
                        return adapter.objectiveTaskText(
                                "chat.objectives.taskDescription.talkToNPC.npc-not-available",
                                questPlayer,
                                activeObjective,
                                Map.of());
                    }
                    return adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.talkToNPC.base",
                            questPlayer,
                            activeObjective,
                            Map.of("%NAME%", npcName(adapter, npc)));
                })
                .onPlayerInteractNpc((event, objective) -> {
                    if (!sameNpc(objective.text("npc"), event.npcSelector())) {
                        return;
                    }
                    objective.addProgress(1);
                    event.questPlayer().sendMessage("<GREEN>You talked to <highlight>" + event.npcName());
                })
                .register();
    }

    private static boolean sameNpc(final String expected, final String actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(actual);
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
