package com.notquests.core.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.test.TestPlatformPlayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class ConversationManagerTest {
    @Test
    void startsSavedConversationAndSendsLinesToPlayer() {
        final ConversationManager conversations = new ConversationManager();
        final RecordingPlayer player = new RecordingPlayer();

        conversations.save("Greeting", List.of("Hello", "Welcome to NotQuests"));

        assertEquals(List.of("Greeting"), conversations.names());
        assertTrue(conversations.start(player, "greeting", false));
        assertEquals(List.of("Hello", "Welcome to NotQuests"), player.messages);
        assertFalse(conversations.start(player, "missing", true));
    }

    @Test
    void storesPaperStyleConversationGraphMetadata() {
        final ConversationManager conversations = new ConversationManager();

        conversations.save("Intro", graphConversationYaml(), "Story");

        final ConversationManager.Conversation conversation = conversations.conversation("intro");
        assertNotNull(conversation);
        assertFalse(conversation.simpleLines());
        assertEquals("Story", conversation.category());
        assertEquals(200, conversation.delayMillis());
        assertEquals(1, conversation.startingLineCount());
        assertEquals(5, conversation.graphLines().size());
        assertFalse(conversation.lines().contains("/next/"));

        final Speaker guide = conversation.speakers().stream()
                .filter(speaker -> speaker.getSpeakerName().equals("Guide"))
                .findFirst()
                .orElseThrow();
        assertEquals("<gold>", guide.getColor());
        assertEquals(150, guide.getDelayInMS());

        final Speaker player = conversation.speakers().stream()
                .filter(speaker -> speaker.getSpeakerName().equals("Player"))
                .findFirst()
                .orElseThrow();
        assertTrue(player.isPlayer());

        final ConversationManager.ConversationLine start = conversation.startingLines().getFirst();
        assertEquals("Guide.start", start.fullIdentifier());
        assertTrue(start.textsList());
        assertTrue(start.shout());
        assertEquals(125, start.delayMillis());
        assertEquals(List.of("Hello", "Hi"), start.messages());
        assertEquals(List.of("Player.yes", "Player.no"), start.next());
        assertEquals(List.of("action StartQuest", "QuestPoints add 5"), start.actions());
        assertEquals(List.of("condition HasMetGuide"), start.conditions());

        assertEquals(1, conversation.npcAttachments().size());
        assertEquals("citizens", conversation.npcAttachments().getFirst().npcType());
        assertEquals(42, conversation.npcAttachments().getFirst().npcId().getIntegerID());
        assertEquals("Guide NPC", conversation.npcAttachments().getFirst().npcName());

        final String analysis = String.join("\n", conversations.analyze("intro"));
        assertTrue(analysis.contains("Condition: condition HasMetGuide"));
        assertTrue(analysis.contains("Action: QuestPoints add 5"));
        assertTrue(analysis.contains("Next: [Player.yes, Player.no]"));
    }

    @Test
    void startsParsedConversationAndContinuesThroughSkippedLine() {
        final ConversationManager conversations = new ConversationManager();
        final RecordingPlayer player = new RecordingPlayer();
        conversations.save("Intro", playableConversationYaml(), "Story");

        assertTrue(conversations.start(player, "intro", false));
        assertEquals("Intro", conversations.activeConversation("player"));
        assertEquals(List.of("1", "2"), conversations.optionIds(player));
        assertEquals(List.of("Welcome", "Yes", "No"), player.messages);

        assertTrue(conversations.chooseOption(player, 1));
        assertEquals(List.of("Welcome", "Yes", "No", "Done"), player.messages);
        assertEquals(List.of(), conversations.optionIds(player));
        assertNull(conversations.activeConversation("player"));
    }

    @Test
    void conversationRuntimeChecksConditionsAndExecutesActions() {
        final ConversationManager conversations = new ConversationManager();
        final RecordingPlayer player = new RecordingPlayer();
        final List<String> executedActions = new ArrayList<>();
        conversations.runtime(new ConversationManager.ConversationRuntime() {
            @Override
            public ConditionCheck.Result checkCondition(
                    final String rawCondition,
                    final PlatformPlayer questPlayer) {
                return new ConditionCheck.Result(!"blocked".equals(rawCondition), rawCondition);
            }

            @Override
            public void executeAction(final String rawAction, final PlatformPlayer questPlayer) {
                executedActions.add(rawAction);
            }

            @Override
            public void schedule(final java.time.Duration delay, final Runnable action) {
                action.run();
            }

            @Override
            public String speakerLine(
                    final PlatformPlayer questPlayer,
                    final Speaker speaker,
                    final String message) {
                return message;
            }

            @Override
            public String answerOptionLine(
                    final PlatformPlayer questPlayer,
                    final Speaker speaker,
                    final String message,
                    final int optionNumber) {
                return message;
            }

            @Override
            public String chooseAnswerPrefix(final PlatformPlayer questPlayer) {
                return "";
            }

            @Override
            public String chooseAnswerHover(final PlatformPlayer questPlayer) {
                return "";
            }

            @Override
            public net.kyori.adventure.text.Component component(final String miniMessage) {
                return net.kyori.adventure.text.Component.text(miniMessage);
            }

            @Override
            public boolean deletePreviousMessages() {
                return false;
            }
        });
        conversations.save("Intro", runtimeConversationYaml(), "Story");

        assertTrue(conversations.start(player, "intro", false));
        assertEquals(List.of("Welcome", "Yes"), player.messages);
        assertEquals(List.of("action StartQuest"), executedActions);

        assertTrue(conversations.chooseOption(player, 1));
        assertEquals(List.of("Welcome", "Yes", "Done"), player.messages);
        assertEquals(List.of("action StartQuest", "GiveQuest ExampleQuest"), executedActions);
    }

    @Test
    void conversationFocusOwnsRotationAndLeavingPolicy() {
        final ConversationManager.Focus focus =
                new ConversationManager.Focus("world", 0, 0, 0, 0, 10, true);

        final ConversationManager.Focus.Step first = focus.next(focusObservation(0, 0), true);
        assertTrue(first.rotate());
        assertFalse(first.cancel());

        final ConversationManager.Focus.Step moved = focus.next(focusObservation(0.3, 0), true);
        assertTrue(moved.cancel());
        assertTrue(moved.stopConversation());
    }

    @Test
    void conversationFocusCancelsQuietlyWhenConversationEnds() {
        final ConversationManager.Focus focus =
                new ConversationManager.Focus("world", 0, 0, 0, 0, 10, true);

        final ConversationManager.Focus.Step ended = focus.next(focusObservation(0, 0), false);

        assertTrue(ended.cancel());
        assertFalse(ended.stopConversation());
        assertFalse(ended.rotate());
    }

    @Test
    void npcIdleEffectRunsOnlyAfterTheLastConversationEnds() {
        final ConversationManager conversations = new ConversationManager();
        final RecordingPlayer first = new RecordingPlayer(UUID.randomUUID().toString());
        final RecordingPlayer second = new RecordingPlayer(UUID.randomUUID().toString());
        final AtomicInteger becameIdle = new AtomicInteger();
        conversations.save("Intro", List.of("Hello"));

        assertTrue(conversations.start(first, "Intro", false));
        assertTrue(conversations.start(second, "Intro", false));
        conversations.startNpcSession(42, UUID.fromString(first.playerIdentifier()), becameIdle::incrementAndGet);
        conversations.startNpcSession(42, UUID.fromString(second.playerIdentifier()), becameIdle::incrementAndGet);

        assertTrue(conversations.stop(first));
        assertTrue(conversations.hasNpcSession(42));
        assertEquals(0, becameIdle.get());

        assertTrue(conversations.stop(second));
        assertFalse(conversations.hasNpcSession(42));
        assertEquals(1, becameIdle.get());
    }

    @Test
    void coreSchedulesFocusAndAppliesTheLeavingStopPolicy() {
        final ConversationManager conversations = new ConversationManager();
        final RecordingPlayer player = new RecordingPlayer(UUID.randomUUID().toString());
        final QueuedRuntime runtime = new QueuedRuntime();
        final RecordingFocus nativeFocus = new RecordingFocus();
        final AtomicInteger stopRequests = new AtomicInteger();
        conversations.runtime(runtime);
        conversations.save("Intro", List.of("Hello"));
        assertTrue(conversations.start(player, "Intro", false));

        assertTrue(conversations.startFocus(
                player,
                "Intro",
                10,
                true,
                nativeFocus,
                stopRequests::incrementAndGet));
        assertEquals(1, nativeFocus.observations);
        assertEquals(1, nativeFocus.slownessApplications);
        assertEquals(1, nativeFocus.rotations);
        assertEquals(1, runtime.scheduled.size());

        nativeFocus.playerX = 0.3;
        runtime.runNext();

        assertEquals(1, stopRequests.get());
        assertEquals(1, nativeFocus.cleanups);
        assertEquals(0, runtime.scheduled.size());
    }

    private static ConversationManager.Focus.Observation focusObservation(
            final double playerX,
            final double playerZ) {
        return new ConversationManager.Focus.Observation(
                true,
                true,
                "world",
                playerX,
                playerZ,
                0,
                0,
                playerX,
                1.6,
                playerZ,
                1,
                1.6,
                0);
    }

    private static Map<String, Object> graphConversationYaml() {
        return map(
                "delay", 200,
                "start", "Guide.start",
                "npcs", map(
                        "citizens-42", map(
                                "type", "citizens",
                                "integerID", 42,
                                "name", "Guide NPC")),
                "Lines", map(
                        "Guide", map(
                                "color", "<gold>",
                                "delay", 150,
                                "start", map(
                                        "texts", List.of("Hello", "Hi"),
                                        "shout", true,
                                        "delay", 125,
                                        "actions", List.of("action StartQuest", "QuestPoints add 5"),
                                        "conditions", List.of("condition HasMetGuide"),
                                        "next", "Player.yes, Player.no"),
                                "skip", map(
                                        "text", "/next/",
                                        "actions", List.of("action Silent"),
                                        "next", "Guide.end"),
                                "end", map("text", "Done")),
                        "Player", map(
                                "color", "<green>",
                                "yes", map(
                                        "text", "Yes",
                                        "next", "Guide.skip"),
                                "no", map("text", "No"))));
    }

    private static Map<String, Object> playableConversationYaml() {
        return map(
                "start", "Guide.start",
                "Lines", map(
                        "Guide", map(
                                "start", map(
                                        "text", "Welcome",
                                        "next", "Player.yes, Player.no"),
                                "skip", map(
                                        "text", "/next/",
                                        "next", "Guide.end"),
                                "end", map("text", "Done")),
                        "Player", map(
                                "yes", map(
                                        "text", "Yes",
                                        "next", "Guide.skip"),
                                "no", map("text", "No"))));
    }

    private static Map<String, Object> runtimeConversationYaml() {
        return map(
                "start", "Guide.start",
                "Lines", map(
                        "Guide", map(
                                "start", map(
                                        "text", "Welcome",
                                        "actions", List.of("action StartQuest"),
                                        "conditions", List.of("allowed"),
                                        "next", "Player.yes, Player.blocked"),
                                "end", map("text", "Done")),
                        "Player", map(
                                "yes", map(
                                        "text", "Yes",
                                        "actions", List.of("GiveQuest ExampleQuest"),
                                        "next", "Guide.end"),
                                "blocked", map(
                                        "text", "Blocked",
                                        "conditions", List.of("blocked")))));
    }

    private static Map<String, Object> map(final Object... entries) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            map.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return map;
    }

    private static final class RecordingPlayer implements TestPlatformPlayer {
        private final String id;
        private final List<String> messages = new ArrayList<>();

        private RecordingPlayer() {
            this("player");
        }

        private RecordingPlayer(final String id) {
            this.id = id;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return id;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {
            messages.add(miniMessage);
        }

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
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
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

    private static final class QueuedRuntime implements ConversationManager.ConversationRuntime {
        private final List<Runnable> scheduled = new ArrayList<>();

        private void runNext() {
            scheduled.removeFirst().run();
        }

        @Override
        public ConditionCheck.Result checkCondition(
                final String rawCondition,
                final PlatformPlayer questPlayer) {
            return new ConditionCheck.Result(true, "");
        }

        @Override
        public void executeAction(final String rawAction, final PlatformPlayer questPlayer) {}

        @Override
        public void schedule(final Duration delay, final Runnable action) {
            scheduled.add(action);
        }

        @Override
        public String speakerLine(
                final PlatformPlayer questPlayer,
                final Speaker speaker,
                final String message) {
            return message;
        }

        @Override
        public String answerOptionLine(
                final PlatformPlayer questPlayer,
                final Speaker speaker,
                final String message,
                final int optionNumber) {
            return message;
        }

        @Override
        public String chooseAnswerPrefix(final PlatformPlayer questPlayer) {
            return "";
        }

        @Override
        public String chooseAnswerHover(final PlatformPlayer questPlayer) {
            return "";
        }

        @Override
        public net.kyori.adventure.text.Component component(final String miniMessage) {
            return net.kyori.adventure.text.Component.text(miniMessage);
        }

        @Override
        public boolean deletePreviousMessages() {
            return false;
        }
    }

    private static final class RecordingFocus implements ConversationManager.Focus.Native {
        private double playerX;
        private int observations;
        private int slownessApplications;
        private int rotations;
        private int cleanups;

        @Override
        public ConversationManager.Focus.Observation observe() {
            observations++;
            return focusObservation(playerX, 0);
        }

        @Override
        public void applySlowness() {
            slownessApplications++;
        }

        @Override
        public void rotate(final float yaw, final float pitch) {
            rotations++;
        }

        @Override
        public void cleanup() {
            cleanups++;
        }
    }
}
