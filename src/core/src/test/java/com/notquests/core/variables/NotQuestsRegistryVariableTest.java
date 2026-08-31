package com.notquests.core.variables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.List;

class NotQuestsRegistryVariableTest {
    @Test
    void parsesNamedVariableFieldsInDeclarationOrder() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.variables()
                .numberVariable("NearbyEntityCount")
                .displayName("Nearby Entity Count")
                .description("Counts nearby entities.")
                .singular("Nearby Entity Count")
                .plural("Nearby Entity Counts")
                .field("entityType", adapter.fields().entityType(), "Entity type to count.")
                .field("radius", adapter.fields().doubleNumber(0), "Radius in blocks.")
                .get(context -> context.number("radius", 0))
                .register();

        final Object[] values = Variables.parse(adapter, registry.variables().getFirst(), "zombie 8.5");

        assertArrayEquals(new Object[] {"zombie", 8.5d}, values);
    }

    @Test
    void acceptsEmptyInputForVariablesWithoutFields() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.variables()
                .numberVariable("Health")
                .displayName("Health")
                .description("Reads health.")
                .singular("Health")
                .plural("Health")
                .get(context -> 20)
                .register();

        assertEquals(0, Variables.parse(adapter, registry.variables().getFirst(), "").length);
    }

    @Test
    void listVariablesReceiveFieldValuesThroughContext() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.variables()
                .listVariable("CompletedObjectiveIDsOfQuest")
                .displayName("Completed Objective IDs Of Quest")
                .description("Reads completed objective ids.")
                .singular("Completed Objective ID")
                .plural("Completed Objective IDs")
                .field("QuestName", adapter.fields().text(() -> List.of("Daily")), "Quest name.")
                .get(context -> List.of(context.text("QuestName")))
                .register();

        final Object[] values = Variables.parse(adapter, registry.variables().getFirst(), "Daily");

        assertArrayEquals(new Object[] {"Daily"}, values);
        assertEquals(
                List.of("Daily"),
                ((com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler) registry.variables().getFirst().handler())
                        .getValue(null, values));
    }
}
