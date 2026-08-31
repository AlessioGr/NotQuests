package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.test.TestPlatformPlayer;
import com.notquests.core.text.NotQuestsMiniMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class UtilManagerTest {
    @Test
    @DisplayName("text replacer lazily substitutes only present placeholders")
    void replacesFromMap() {
        final Map<String, Supplier<String>> replacements = new LinkedHashMap<>();
        replacements.put("%PLAYER%", () -> "Steve");
        replacements.put("%COUNT%", () -> "3");

        assertEquals(
                "Steve has 3 active quests",
                UtilManager.replaceFromMap("%PLAYER% has %COUNT% active quests", replacements));
    }

    @Test
    @DisplayName("MiniMessage token list contains standard and NotQuests custom tags")
    void miniMessageTokensIncludeCustomTags() {
        final var tokens = NotQuestsMiniMessage.completionTokens();
        assertTrue(tokens.contains("red"), "expected standard <red> tag");
        assertTrue(tokens.contains("main"), "expected NotQuests <main> tag");
        assertTrue(tokens.contains("highlight"), "expected NotQuests <highlight> tag");
        assertTrue(tokens.contains("error"), "expected NotQuests <error> tag");
        assertTrue(tokens.contains("center"), "expected NotQuests <center> helper tag");
    }

    @Test
    @DisplayName("NotQuests custom MiniMessage tags accept uppercase spelling")
    void customMiniMessageTagsAcceptUppercaseSpelling() {
        final var miniMessage = NotQuestsMiniMessage.create(null);

        assertEquals("<error>Error</error>", NotQuestsMiniMessage.normalizeTags("<ERROR>Error</ERROR>"));
        assertEquals("<dark_aqua>Name</dark_aqua>", NotQuestsMiniMessage.normalizeTags("<DARK_AQUA>Name</DARK_AQUA>"));
        assertTrue(NotQuestsMiniMessage.normalizeTags("<CENTER><SUCCESS>Done").endsWith("<success>Done"));
        assertDoesNotThrow(() -> miniMessage.deserialize("<ERROR>Error</ERROR>"));
        assertDoesNotThrow(() -> miniMessage.deserialize("<CENTER><SUCCESS>Done"));
    }

    @Test
    @DisplayName("center and empty helpers are expanded before MiniMessage parsing")
    void expandsCenterAndEmptyHelpers() {
        final String normalized = NotQuestsMiniMessage.normalizeTags("<CENTER><MAIN>[Quest Accepted]\n<EMPTY>");

        assertTrue(normalized.startsWith(" "), "centered line should start with padding spaces");
        assertTrue(normalized.contains("<main>[Quest Accepted]"));
        assertTrue(normalized.endsWith("\n "));
    }

    @Test
    @DisplayName("shared text formatter centers and wraps command-facing text")
    void formatsCenteredAndWrappedText() {
        final String centered = UtilManager.centered("<main>Hello");

        assertTrue(centered.startsWith(" "), "centered text should include leading spaces");
        assertTrue(centered.endsWith("<main>Hello"));
        assertEquals("alpha beta\ngamma", UtilManager.wrap("alpha beta gamma", 10, true));
        assertEquals(List.of("alpha beta", "gamma"), UtilManager.wrapToList("alpha beta gamma", 10, true));
    }

    @Test
    @DisplayName("shared text placeholders replace player, quest, and numeric expressions")
    void appliesSharedTextPlaceholders() {
        final PlatformPlayer player = new TestPlatformPlayer() {
            @Override
            public boolean hasPlayer() {
                return true;
            }

            @Override
            public String playerName() {
                return "Steve";
            }

            @Override
            public String playerIdentifier() {
                return "player-uuid";
            }

            @Override
            public String worldName() {
                return "world";
            }

            @Override
            public double positionX() {
                return 12.5;
            }

            @Override
            public double positionY() {
                return 64;
            }

            @Override
            public double positionZ() {
                return -3.25;
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
        };

        assertEquals(
                "Quest demo for Steve player-uuid in world at 12.5,64.0,-3.25 = 5.0 / 6",
                UtilManager.applyNotQuestsPlaceholders(
                        "Quest {QUEST} for {PLAYER} {PLAYERUUID} in {WORLD} at {PLAYERX},{PLAYERY},{PLAYERZ} = {{2 + 3}} / {{~5.6}}",
                        "demo",
                        player,
                        null));
    }
}
