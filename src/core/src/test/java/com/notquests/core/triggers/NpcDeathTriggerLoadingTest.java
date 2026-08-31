package com.notquests.core.triggers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.CategoryFiles;
import com.notquests.core.managers.DataManager;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest;

import java.nio.file.Files;
import java.nio.file.Path;

class NpcDeathTriggerLoadingTest {
    @TempDir
    Path dataFolder;

    @Test
    void loadedNpcFieldFiltersOtherNpcDeaths() throws Exception {
        final Path categoryFolder = dataFolder.resolve(Category.DEFAULT_NAME);
        Files.createDirectories(categoryFolder);
        Files.writeString(categoryFolder.resolve(CategoryFiles.CATEGORY_FILE), "id: default\n");
        Files.writeString(categoryFolder.resolve(CategoryFiles.QUESTS_FILE), """
                quests:
                  NpcQuest:
                    triggers:
                      1:
                        triggerType: NPCDEATH
                        specifics:
                          npcToDie: 12
                        amountNeeded: 1
                """);

        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.triggers().trigger("NPCDEATH")
                .displayName("NPC Death")
                .description("Runs after the configured NPC dies.")
                .field(
                        "NPC",
                        adapter.fields().integer(-1).config("specifics.npcToDie"),
                        "NPC id.")
                .field(
                        "amount",
                        adapter.fields().integer(1).config("amountNeeded"),
                        "Required deaths.")
                .register();

        assertTrue(new DataManager(plugin, adapter, dataFolder).reload(DataManager.ReloadTarget.ALL));
        final Trigger loadedTrigger = plugin.quest("NpcQuest").getTriggerFromID(1);
        final ActiveTrigger activeTrigger = new ActiveTrigger("NpcQuest", loadedTrigger);

        assertFalse(activeTrigger.matches(
                ActiveTrigger.Event.player("NPCDEATH", "world").withAttribute("npc", 99),
                ignored -> false));
        assertTrue(activeTrigger.matches(
                ActiveTrigger.Event.player("NPCDEATH", "world").withAttribute("npc", 12),
                ignored -> false));
    }
}
