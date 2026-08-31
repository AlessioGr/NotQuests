package com.notquests.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

class NotQuestsRegistryMessagesTest {
    @Test
    void formatsRegistryCountsAndSortedTypeLists() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        adapter.actions()
                .action("SendMessage")
                .displayName("Send Message")
                .description("Sends a chat message.")
                .actionDescription((action, questPlayer, objects) -> "message")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("BroadcastMessage")
                .displayName("Broadcast Message")
                .description("Broadcasts a chat message.")
                .actionDescription((action, questPlayer, objects) -> "broadcast")
                .execute((action, questPlayer, objects) -> {})
                .register();

        assertEquals(
                List.of(
                        "NotQuests registry:",
                        "- Objectives: 0",
                        "- Actions: 2",
                        "- Conditions: 0",
                        "- Triggers: 0",
                        "- Variables: 0"),
                NotQuestsRegistry.summary(registry));
        assertEquals("<highlight>All reward types:\n<main>BroadcastMessage\n<main>SendMessage", NotQuestsRegistry.actionList(registry));
    }
}
