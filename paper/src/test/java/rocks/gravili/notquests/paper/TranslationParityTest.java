/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards NotQuests' shipped translations: every locale file must contain all of the leaf keys in
 * the source-of-truth {@code en-US.yml}. This is the real regression that was fixed when the locale
 * files had drifted out of sync with the English keys (missing GUI keys, stale renamed keys); this
 * test fails if any locale ever falls behind {@code en-US} again.
 */
class TranslationParityTest {

  private static Path translationsDir() {
    // Gradle runs tests with the module dir (paper/) as the working dir; fall back to repo root.
    return Stream.of(
            Path.of("../plugin/src/main/resources/translations"),
            Path.of("plugin/src/main/resources/translations"))
        .filter(Files::isDirectory)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Could not locate the translations resource directory"));
  }

  @SuppressWarnings("unchecked")
  private static Set<String> leafKeys(final Path yamlFile) throws IOException {
    try (InputStream in = Files.newInputStream(yamlFile)) {
      final Object root = new Yaml().load(in);
      final Set<String> keys = new LinkedHashSet<>();
      if (root instanceof Map) {
        collect((Map<String, Object>) root, "", keys);
      }
      return keys;
    }
  }

  @SuppressWarnings("unchecked")
  private static void collect(
      final Map<String, Object> node, final String prefix, final Set<String> out) {
    for (final Map.Entry<String, Object> entry : node.entrySet()) {
      final String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      if (entry.getValue() instanceof Map) {
        collect((Map<String, Object>) entry.getValue(), key, out);
      } else {
        out.add(key);
      }
    }
  }

  static List<Path> localeFiles() throws IOException {
    try (Stream<Path> files = Files.list(translationsDir())) {
      return files
          .filter(p -> p.getFileName().toString().endsWith(".yml"))
          .filter(p -> !p.getFileName().toString().equals("en-US.yml"))
          .sorted()
          .toList();
    }
  }

  @ParameterizedTest(name = "{0} has all en-US keys")
  @MethodSource("localeFiles")
  @DisplayName("every locale file is in sync with en-US.yml")
  void localeHasAllEnglishKeys(final Path localeFile) throws IOException {
    final Set<String> english = leafKeys(translationsDir().resolve("en-US.yml"));
    final Set<String> locale = leafKeys(localeFile);

    final Set<String> missing = new TreeSet<>(english);
    missing.removeAll(locale);

    assertTrue(
        missing.isEmpty(),
        () ->
            localeFile.getFileName()
                + " is missing "
                + missing.size()
                + " key(s) present in en-US.yml: "
                + new ArrayList<>(missing));
  }
}
