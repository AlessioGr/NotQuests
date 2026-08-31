package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class NPCDeathTrigger {
    public static final String NPC = "NPC";

    private NPCDeathTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("NPCDEATH")
                .displayName("NPC Death")
                .description("Runs the selected action after a Citizens NPC dies the configured number of times.")
                .field(
                        NPC,
                        platform.fields().integer(-1).config("specifics.npcToDie"),
                        "Citizens NPC id that must die before this trigger gains progress.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of NPC deaths required before this trigger runs.")
                .triggerDescription(trigger -> "NPC to die ID: <WHITE>" + trigger.integer(NPC, -1))
                .register();
    }
}
