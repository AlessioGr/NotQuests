package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.YamlConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class ConfigurationManagerFileTest {
    @TempDir
    Path temp;

    @Test
    void createsMissingFolderAndDefaultGeneralFileBeforeLoading() {
        final Path dataFolder = temp.resolve("NotQuests");
        final Hooks hooks = new Hooks();
        final ConfigurationManager configuration = new ConfigurationManager();
        final ConfigurationManager.Loaded loaded = configuration.open(
                dataFolder,
                new IO(),
                hooks);

        assertTrue(loaded.loaded());
        assertTrue(loaded.created());
        assertEquals(dataFolder.resolve("general.yml"), loaded.file());
        assertTrue(Files.exists(dataFolder));
        assertTrue(Files.exists(dataFolder.resolve("general.yml")));
        assertTrue(hooks.info.contains("Data Folder not found. Creating a new one..."));
        assertTrue(hooks.info.contains("General ConfigurationManager (general.yml) does not exist. Creating a new one..."));

        final ConfigurationManager.Loaded reopened = configuration.open(dataFolder, new IO(), hooks);
        assertFalse(reopened.created());
    }

    @Test
    void disablesSavingWhenGeneralFileCannotBeLoaded() {
        final Path dataFolder = temp.resolve("NotQuests");
        final Hooks hooks = new Hooks();
        final ConfigurationManager configuration = new ConfigurationManager();
        final ConfigurationManager.Loaded loaded = configuration.open(
                dataFolder,
                new IO() {
                    @Override
                    public YamlConfig load(final Path file) throws IOException {
                        throw new IOException("bad yaml");
                    }
                },
                hooks);

        assertFalse(loaded.loaded());
        assertEquals(List.of(
                "There was an error loading the general configuration file. It either doesn't exist or is invalid."),
                hooks.disableReasons);
    }

    @Test
    void reportsSaveFailureWithSharedMessage() {
        final Hooks hooks = new Hooks();
        final ConfigurationManager configuration = new ConfigurationManager();
        configuration.open(temp, new IO(), hooks);

        final boolean saved = configuration.save(
                new IO() {
                    @Override
                    public void save(final YamlConfig configuration, final Path file) throws IOException {
                        throw new IOException("read-only");
                    }
                },
                hooks);

        assertFalse(saved);
        assertEquals(List.of("General Config file could not be saved."), hooks.warn);
    }

    private static class IO implements ConfigurationManager.IO {
        @Override
        public YamlConfig load(final Path file) throws IOException {
            return YamlConfig.empty();
        }

        @Override
        public void save(final YamlConfig configuration, final Path file) throws IOException {
            Files.writeString(file, "saved");
        }
    }

    private static final class Hooks implements ConfigurationManager.Hooks {
        private final List<String> info = new ArrayList<>();
        private final List<String> warn = new ArrayList<>();
        private final List<String> disableReasons = new ArrayList<>();

        @Override
        public void info(final String message) {
            info.add(message);
        }

        @Override
        public void warn(final String message) {
            warn.add(message);
        }

        @Override
        public void disableSaving(final String reason, final Exception exception) {
            disableReasons.add(reason);
        }
    }
}
