package com.notquests.builtin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class BuiltinArchitectureTest {
    private static final List<String> FORBIDDEN_PLATFORM_IMPORTS =
            List.of(
                    "org.bukkit.",
                    "io.papermc.",
                    "net.minecraft.",
                    "net.neoforged.",
                    "net.citizensnpcs.",
                    "de.oliver.",
                    "net.milkbowl.",
                    "me.clip.",
                    "com.sk89q.",
                    "com.gamingmesh.",
                    "io.lumine.",
                    "com.magmaguy.",
                    "com.notquests.paper.",
                    "com.notquests.neoforge.");

    @Test
    void builtinDoesNotDependOnPaperOrBukkit() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(BuiltinArchitectureTest::containsForbiddenPlatformImport)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "builtin must stay platform-neutral, but these files import platform code: " + offenders);
    }

    @Test
    void builtinDoesNotRegisterVaultEconomyFeatures() throws IOException {
        final Path moneyVariable = Path.of("src/main/java/com/notquests/builtin/variables/MoneyVariable.java");
        final String builtInPack = Files.readString(Path.of("src/main/java/com/notquests/builtin/BuiltInPack.java"));

        assertTrue(
                Files.notExists(moneyVariable) && !builtInPack.contains("MoneyVariable"),
                "Vault/economy-backed Money must stay in the Paper module, not platform-neutral builtin.");
    }

    private static boolean containsForbiddenPlatformImport(final Path path) {
        try {
            final String source = Files.readString(path);
            return FORBIDDEN_PLATFORM_IMPORTS.stream().anyMatch(source::contains);
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
