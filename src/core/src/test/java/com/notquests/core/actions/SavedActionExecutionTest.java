package com.notquests.core.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.SavedActions.ActionCondition;
import com.notquests.core.managers.LanguageManager;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest.ConditionSettings;
import com.notquests.core.TestData;
import com.notquests.core.test.TestPlatformPlayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SavedActionExecutionTest {
    @Test
    void coreActionExecutionKeepsDelayWhenConditionsPass() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<ScheduledAction> scheduledActions = new ArrayList<>();
        final List<String> executed = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        plugin.actionScheduler((delay, action) -> scheduledActions.add(new ScheduledAction(delay, action)));

        final NotQuestsAdapter adapter = plugin.registry().createAdapter(new NotQuestsRegistry.PlatformHooks(
                warnings::add,
                ignored -> {},
                ignored -> {}));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always passes.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.actions()
                .action("Recording")
                .displayName("Recording")
                .description("Records execution.")
                .execute((action, questPlayer, objects) -> executed.add("ran"))
                .register();

        assertTrue(plugin.executeConfiguredAction(
                "Recording",
                new TestData(Map.of()),
                Duration.ofMillis(1_000),
                List.of(new ActionCondition(
                        1,
                        "Static",
                        new TestData(Map.of()),
                        new ConditionSettings(1, false, "", "", false))),
                new TestPlayer(),
                warnings::add,
                "Action failed"));

        assertEquals(List.of(), executed, "delayed action should not run immediately");
        assertEquals(1, scheduledActions.size());
        assertEquals(Duration.ofMillis(1_000), scheduledActions.getFirst().delay());

        scheduledActions.getFirst().action().run();

        assertEquals(List.of("ran"), executed, "delayed action should run when the scheduler fires");
        assertEquals(List.of(), warnings);
    }

    private record ScheduledAction(Duration delay, Runnable action) {}

    private static final class TestPlayer implements TestPlatformPlayer {
        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "test-player";
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final Duration fadeIn,
                final Duration stay,
                final Duration fadeOut) {}
        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }
}
