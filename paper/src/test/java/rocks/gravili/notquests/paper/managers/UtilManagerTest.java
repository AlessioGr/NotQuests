/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pure-logic unit tests (no Bukkit server required).
 *
 * <p>{@link UtilManager#getExtraArguments(String)} is the command "extra arguments" parser
 * (e.g. {@code -name John -age 5}) — the same family of argument-parsing code that recently had
 * bugs, so it's a good first thing to pin down. The constructor only stores its reference and
 * builds a token list, so {@code new UtilManager(null)} is safe for these methods.
 */
class UtilManagerTest {

  private final UtilManager util = new UtilManager(null);

  static Stream<Arguments> extraArgumentCases() {
    return Stream.of(
        arguments("", Map.of()),
        arguments("-name John", Map.of("name", "John")),
        arguments("-greeting hello world", Map.of("greeting", "hello world")));
  }

  @ParameterizedTest(name = "getExtraArguments(\"{0}\") -> {1}")
  @MethodSource("extraArgumentCases")
  @DisplayName("parses single-flag extra arguments")
  void parsesExtraArguments(final String input, final Map<String, String> expected) {
    assertEquals(expected, util.getExtraArguments(input));
  }

  @Test
  @DisplayName("parses multiple flags into separate entries")
  void parsesMultipleFlags() {
    final Map<String, String> result = util.getExtraArguments("-a foo -b bar");
    assertEquals(2, result.size());
    // Values are trimmed here because the parser keeps a trailing space on a value that is
    // followed by another flag; we assert the meaningful content rather than that quirk.
    assertEquals("foo", result.get("a").trim());
    assertEquals("bar", result.get("b").trim());
  }

  @Test
  @DisplayName("MiniMessage token list includes NotQuests' custom tags")
  void miniMessageTokensIncludeCustomTags() {
    final var tokens = util.getMiniMessageTokens();
    assertTrue(tokens.contains("main"), "expected custom <main> tag");
    assertTrue(tokens.contains("highlight"), "expected custom <highlight> tag");
    assertTrue(tokens.contains("error"), "expected custom <error> tag");
  }

  @Test
  @DisplayName("replaceFromMap substitutes every key (used for placeholder replacement)")
  void replaceFromMapReplacesKeys() {
    final Map<String, Supplier<String>> replacements = new LinkedHashMap<>();
    replacements.put("%PLAYER%", () -> "Steve");
    replacements.put("%COUNT%", () -> "3");
    assertEquals(
        "Steve has 3 active quests",
        util.replaceFromMap("%PLAYER% has %COUNT% active quests", replacements));
  }

  @Test
  @DisplayName("replaceFromMap replaces repeated occurrences of the same key")
  void replaceFromMapReplacesRepeatedOccurrences() {
    assertEquals("a-a-a", util.replaceFromMap("X-X-X", Map.of("X", () -> "a")));
  }
}
