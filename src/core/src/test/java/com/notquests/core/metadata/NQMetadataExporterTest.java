package com.notquests.core.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry;

class NQMetadataExporterTest {
    @Test
    void convertsRegistryActionsAndVariablesToMetadata() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        adapter.actions()
                .action("SendMessage")
                .displayName("Send Message")
                .description("Sends a message to the target player.")
                .field("message", adapter.fields().greedyText(), "Message text to send.")
                .flag("silent", adapter.fields().presenceFlag(), "Run silently.")
                .execute((action, questPlayer, objects) -> {})
                .actionDescription((action, questPlayer, objects) -> action.text("message"))
                .register();

        adapter.variables()
                .numberVariable("NearbyEntityCount")
                .displayName("Nearby Entity Count")
                .description("Counts nearby entities.")
                .singular("Nearby Entity")
                .plural("Nearby Entities")
                .field("radius", adapter.fields().doubleNumber(0), "Radius in blocks.")
                .get(context -> 0)
                .register();

        final var action = NQMetadataExporter.actions(registry).getFirst();
        assertEquals("SendMessage", action.id());
        assertEquals("Message text to send.", action.fields().getFirst().description());
        assertEquals("silent", action.flags().getFirst().name());

        final var variable = NQMetadataExporter.variables(registry).getFirst();
        assertEquals("NearbyEntityCount", variable.id());
        assertEquals("number", variable.valueType());
        assertTrue(variable.numberArguments().contains("radius"));
    }
}
