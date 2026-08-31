package com.notquests.core.migrations.v6_3_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.migrations.ConfigurationMigrations.Context;
import com.notquests.core.migrations.ConfigurationMigrations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class Version630MigrationTest {
    @TempDir
    Path dataFolder;

    @Test
    void migrationVersionStartsAtTheReleasedConfigVersion() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "config-version-do-not-edit", "6.3.0"));

        assertTrue(ConfigurationMigrations.prepareDataVersion(configuration, "6.3.0"));
        assertEquals("6.3.0", ConfigurationMigrations.dataVersion(configuration));

        ConfigurationMigrations.saveDataVersion(configuration, "7.0.0-beta.1");
        assertEquals("7.0.0-beta.1", ConfigurationMigrations.dataVersion(configuration));
    }

    @Test
    void runnerExecutesTheSixThreeUpgradeOnlyOnce() {
        final ConfigurationMigrations migrations = new ConfigurationMigrations();
        final Context context = new Context(dataFolder, YamlConfig.empty(), (message, exception) -> {});
        final AtomicInteger backups = new AtomicInteger();
        final AtomicInteger saves = new AtomicInteger();
        final AtomicReference<String> savedVersion = new AtomicReference<>();

        migrations.run(new ConfigurationMigrations.MigrationRun<>(
                "6.3.0",
                "7.0.0-beta.1",
                context,
                ignored -> {},
                ignored -> backups.incrementAndGet(),
                version -> {
                    savedVersion.set(version);
                    saves.incrementAndGet();
                }));

        assertEquals(1, backups.get());
        assertEquals(1, saves.get());
        assertEquals("7.0.0-beta.1", savedVersion.get());

        migrations.run(new ConfigurationMigrations.MigrationRun<>(
                savedVersion.get(),
                "7.0.0-beta.1",
                context,
                ignored -> {},
                ignored -> backups.incrementAndGet(),
                ignored -> saves.incrementAndGet()));

        assertEquals(1, backups.get());
        assertEquals(1, saves.get());
    }

    @Test
    void failedUpgradeDoesNotAdvanceTheMigrationVersion() throws Exception {
        final Path category = dataFolder.resolve("default");
        Files.createDirectories(category);
        Files.writeString(category.resolve("category.yml"), "displayName: Default\n");
        Files.writeString(category.resolve("quests.yml"), "quests: [\n");
        Files.writeString(category.resolve("actions.yml"), "actions: {}\n");
        final AtomicInteger saves = new AtomicInteger();

        assertThrows(
                IllegalStateException.class,
                () -> new ConfigurationMigrations().run(new ConfigurationMigrations.MigrationRun<>(
                        "6.3.0",
                        "7.0.0-beta.1",
                        new Context(dataFolder, YamlConfig.empty(), (message, exception) -> {}),
                        ignored -> {},
                        ignored -> {},
                        ignored -> saves.incrementAndGet())));

        assertEquals(0, saves.get());
    }

    @Test
    void convertsSixThreeLocationsAndExactItemsOnce() throws Exception {
        final Path quests = dataFolder.resolve("default/quests.yml");
        Files.createDirectories(quests.getParent());
        Files.writeString(quests.getParent().resolve("category.yml"), "displayName: Default\n");
        Files.writeString(dataFolder.resolve("general.yml"), """
                storage:
                  database:
                    host: 127.0.0.1
                    port: 3306
                    database: notquests
                    username: nq
                    password: secret
                general:
                  journal-item:
                    item:
                      ==: org.bukkit.inventory.ItemStack
                      v: 2865
                      type: ENCHANTED_BOOK
                      amount: 1
                      meta:
                        ==: ItemMeta
                        display-name: '{\"text\":\"My journal\"}'
                        custom-model-data: 73
                        enchants:
                          DAMAGE_ALL: 5
                        stored-enchants:
                          minecraft:mending: 1
                        ItemFlags:
                          - HIDE_ENCHANTS
                          - HIDE_UNBREAKABLE
                        Unbreakable: true
                        potion-type: strong_healing
                visual:
                  colors:
                    console:
                      info:
                        default: '<old-main>'
                        data: '<old-data>'
                        language: '<old-language>'
                      warn:
                        default: '<old-warn>'
                      severe:
                        default: '<old-error>'
                      debug:
                        default: '<old-debug>'
                """);
        final Path conversation = dataFolder.resolve("default/conversations/Guide.yml");
        Files.createDirectories(conversation.getParent());
        Files.writeString(conversation, """
                start: Guide.hello
                npcID: 7
                npcs:
                  citizens-8:
                    npcType: citizens
                    npcName: Existing guide
                    integerId: 8
                  fancynpcs-merchant:
                    PluginNPCType: fancynpcs
                    displayName: Merchant
                    PluginNPCStringID: merchant
                Lines:
                  Guide:
                    hello:
                      text: Hello
                """);
        final Path unrelatedGui = dataFolder.resolve("guis/custom.yml");
        Files.createDirectories(unrelatedGui.getParent());
        final String unrelatedGuiContents = """
                # This is GUI data, not an item-selection value.
                button:
                  materials:
                    primary: STONE
                """;
        Files.writeString(unrelatedGui, unrelatedGuiContents);
        Files.writeString(quests, """
                # Keep this administrator note.
                quests:
                  Example:
                    maxAccepts: 3
                    acceptCooldown: 1250
                    takeItem:
                      ==: org.bukkit.inventory.ItemStack
                      v: 2865
                      type: DIAMOND_SWORD
                      amount: 2
                      meta:
                        ==: ItemMeta
                        custom-model-data: 42
                    objectives:
                      '1':
                        objectiveType: ReachLocation
                        completionNPC:
                          type: citizens
                          name: Guide
                          integerID: 44
                        specifics:
                          location:
                            ==: Location
                            world: world_nether
                            x: 12.5
                            y: 64.0
                            z: -3.25
                            yaw: 90.0
                            pitch: 15.0
                      '2':
                        objectiveType: BreakBlocks
                        progressNeeded: 64
                        specifics:
                          blockToBreak:
                            material: STONE
                      '3':
                        objectiveType: DeliverItems
                        specifics:
                          nqitem: QuestKey
                          recipientNPCID: 12
                      '4':
                        objectiveType: TalkToNPC
                        specifics:
                          ArmorStandToTalkUUID: 123e4567-e89b-12d3-a456-426614174000
                      '5':
                        objectiveType: Objective
                        objectives:
                          '1':
                            objectiveType: BreakBlocks
                            completionNPC:
                              type: armorstand
                              uuidID: 123e4567-e89b-12d3-a456-426614174000
                            progressNeeded: 3
                            specifics:
                              blockToBreak:
                                material: DEEPSLATE
                            rewards:
                              '1':
                                actionType: GiveItem
                                specifics:
                                  rewardItem:
                                    ==: org.bukkit.inventory.ItemStack
                                    type: EMERALD
                                    meta:
                                      ==: ItemMeta
                                      custom-model-data: 11
                    rewards:
                      '1':
                        actionType: GiveItem
                        specifics:
                          rewardItem:
                            ==: org.bukkit.inventory.ItemStack
                            type: GOLDEN_APPLE
                            meta:
                              ==: ItemMeta
                              custom-model-data: 9
                      '2':
                        actionType: Action
                        specifics:
                          action: FollowUp
                """);

        final Version630Migration migration = new Version630Migration();
        final YamlConfig generalConfig = YamlConfig.load(dataFolder.resolve("general.yml"));
        final List<String> warnings = new ArrayList<>();
        final Context context = new Context(
                dataFolder,
                generalConfig,
                (message, exception) -> warnings.add(message));

        assertTrue(migration.migrate(context));
        assertFalse(migration.migrate(context), "the one-off conversion must be idempotent");
        YamlConfig.save(generalConfig, dataFolder.resolve("general.yml"));

        final YamlConfig migrated = YamlConfig.load(quests);
        assertEquals(
                "location",
                migrated.getString("quests.Example.objectives.1.specifics.location.$type"));
        assertEquals("citizens:44", migrated.getString("quests.Example.objectives.1.completionNPC"));
        assertEquals(90.0, migrated.getDouble("quests.Example.objectives.1.specifics.location.yaw"));
        assertEquals(15.0, migrated.getDouble("quests.Example.objectives.1.specifics.location.pitch"));

        assertExactItem(
                migrated.get("quests.Example.takeItem.exactItems"),
                "diamond_sword",
                42);
        assertEquals(
                List.of("STONE"),
                migrated.get("quests.Example.objectives.2.specifics.itemStackSelection.materials"));
        assertEquals(
                "64",
                migrated.getString("quests.Example.objectives.2.progressNeededExpression"));
        assertEquals(
                List.of("QuestKey"),
                migrated.get("quests.Example.objectives.3.specifics.itemStackSelection.nqItems"));
        assertEquals(
                "citizens",
                migrated.getString("quests.Example.objectives.3.specifics.recipientNPC.type"));
        assertEquals(
                12,
                migrated.getInt("quests.Example.objectives.3.specifics.recipientNPC.integerID"));
        assertEquals(
                "armorstand",
                migrated.getString("quests.Example.objectives.4.specifics.npcToTalkTo.type"));
        assertExactItem(
                migrated.get("quests.Example.rewards.1.specifics.itemStackSelection.exactItems"),
                "golden_apple",
                9);
        assertEquals(
                "3",
                migrated.getString(
                        "quests.Example.objectives.5.objectives.1.progressNeededExpression"));
        assertEquals(
                "armorstand:123e4567-e89b-12d3-a456-426614174000",
                migrated.getString("quests.Example.objectives.5.objectives.1.completionNPC"));
        assertEquals(
                List.of("DEEPSLATE"),
                migrated.get(
                        "quests.Example.objectives.5.objectives.1.specifics.itemStackSelection.materials"));
        assertExactItem(
                migrated.get(
                        "quests.Example.objectives.5.objectives.1.rewards.1.specifics.itemStackSelection.exactItems"),
                "emerald",
                11);
        assertEquals("FollowUp", migrated.getString("quests.Example.rewards.2.specifics.actions"));
        assertEquals(3, migrated.getInt("quests.Example.limits.completions"));
        assertEquals(1250, migrated.getInt("quests.Example.acceptCooldown.complete"));
        assertFalse(migrated.contains("quests.Example.maxAccepts"));
        assertTrue(Files.readString(quests).contains("# Keep this administrator note."));

        final YamlConfig general = YamlConfig.load(dataFolder.resolve("general.yml"));
        assertTrue(general.getBoolean("storage.database.enabled"));
        final Map<String, Object> journal = map(general.get("general.journal-item.item"));
        assertEquals("ENCHANTED_BOOK", journal.get("material"));
        assertEquals("My journal", journal.get("display-name"));
        assertEquals(73, ((Number) journal.get("custom-model-data")).intValue());
        assertEquals(Map.of("minecraft:sharpness", 5), map(journal.get("enchantments")));
        assertEquals(Map.of("minecraft:mending", 1), map(journal.get("stored-enchantments")));
        assertEquals(List.of("enchantments", "unbreakable"), journal.get("hidden-components"));
        assertEquals(true, journal.get("unbreakable"));
        assertFalse(journal.containsKey("platform-item"));
        assertTrue(warnings.stream().anyMatch(message -> message.contains("meta.potion-type")));
        assertEquals(
                unrelatedGuiContents,
                Files.readString(unrelatedGui),
                "the migration must not rewrite unrelated GUI, language, or administrator YAML");

        assertEquals("<old-main>", general.getString("visual.colors.console.info.default.normal"));
        assertEquals("<old-data>", general.getString("visual.colors.console.info.data.normal"));
        assertEquals("<old-language>", general.getString("visual.colors.console.info.language.normal"));
        assertEquals("<old-warn>", general.getString("visual.colors.console.warn.default.normal"));
        assertEquals("<old-error>", general.getString("visual.colors.console.severe.default.normal"));
        assertEquals("<old-debug>", general.getString("visual.colors.console.debug.default.normal"));

        final YamlConfig migratedConversation = YamlConfig.load(conversation);
        assertFalse(migratedConversation.contains("npcID"));
        assertEquals("citizens", migratedConversation.getString("npcs.citizens-7.type"));
        assertEquals(7, migratedConversation.getInt("npcs.citizens-7.integerID"));
        assertEquals("citizens", migratedConversation.getString("npcs.citizens-8.type"));
        assertEquals("Existing guide", migratedConversation.getString("npcs.citizens-8.name"));
        assertEquals(8, migratedConversation.getInt("npcs.citizens-8.integerID"));
        assertFalse(migratedConversation.contains("npcs.citizens-8.npcType"));
        assertEquals("fancynpcs", migratedConversation.getString("npcs.fancynpcs-merchant.type"));
        assertEquals("Merchant", migratedConversation.getString("npcs.fancynpcs-merchant.name"));
        assertEquals("merchant", migratedConversation.getString("npcs.fancynpcs-merchant.stringID"));
    }

    @Test
    void leavesBackupsAndUnchangedYamlUntouched() throws Exception {
        final Path unchanged = dataFolder.resolve("general.yml");
        Files.writeString(unchanged, "# comment\nconfig-version: 7.0.0\n");
        final Path backup = dataFolder.resolve("backups/6.3.0/quests.yml");
        Files.createDirectories(backup.getParent());
        Files.writeString(backup, "item:\n  ==: org.bukkit.inventory.ItemStack\n  type: STONE\n");

        final Version630Migration migration = new Version630Migration();
        assertFalse(migration.migrate(new Context(
                dataFolder,
                YamlConfig.load(unchanged),
                (message, exception) -> {})));

        assertEquals("# comment\nconfig-version: 7.0.0\n", Files.readString(unchanged));
        assertTrue(Files.readString(backup).contains("==: org.bukkit.inventory.ItemStack"));
    }

    @SuppressWarnings("unchecked")
    private static void assertExactItem(
            final Object value,
            final String material,
            final int customModelData) {
        final List<Map<String, Object>> exactItems = (List<Map<String, Object>>) value;
        assertEquals(1, exactItems.size());
        final Map<String, Object> exactItem = exactItems.getFirst();
        assertEquals("paper", exactItem.get("platform"));
        assertEquals(material, exactItem.get("material"));
        final Map<String, Object> data = (Map<String, Object>) exactItem.get("data");
        assertNotNull(data);
        assertEquals("ItemStack", data.get("__notquestsType"));
        final Map<String, Object> serialized = (Map<String, Object>) data.get("serialized");
        final Map<String, Object> meta = (Map<String, Object>) serialized.get("meta");
        assertEquals(customModelData, ((Number) meta.get("custom-model-data")).intValue());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(final Object value) {
        return (Map<String, Object>) value;
    }
}
