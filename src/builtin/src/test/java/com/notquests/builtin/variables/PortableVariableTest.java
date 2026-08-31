package com.notquests.builtin.variables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestNotQuestsAdapter;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.npc.NpcAttachments.Detachments;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables.BooleanVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ItemStackListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.NumberVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Registry;
import com.notquests.core.registry.NotQuestsRegistry.Variables.StringVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PortableVariableTest {
    @Test
    void requestedPlayerVariablesArePortableAndUseThePlatformPlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(plugin, adapter);
        seedCondition(plugin, adapter, registry);
        final RecordingPlayer player = new RecordingPlayer();
        seedTags(plugin, player);

        final Variables.StringVariableHandler displayName = string(registry, "Name");
        assertEquals("Alex", displayName.getValue(player));
        assertTrue(displayName.setValue("Alicia", player));
        assertEquals("Alicia", displayName.getValue(player));

        final Variables.NumberVariableHandler health = number(registry, "Health");
        assertEquals(12.5d, health.getValue(player).doubleValue());
        assertTrue(health.canSet());
        assertTrue(health.setValue(7.5d, player));
        assertEquals(7.5d, player.health);

        final Variables.NumberVariableHandler food = number(registry, "FoodLevel");
        assertEquals(13, food.getValue(player).intValue());
        assertTrue(food.canSet());
        assertTrue(food.setValue(18, player));
        assertEquals(18, player.foodLevel);

        final Variables.NumberVariableHandler saturation = number(registry, "Saturation");
        assertTrue(saturation.setValue(12.5d, player));
        assertEquals(12.5d, player.saturation);

        final Variables.NumberVariableHandler maxHealth = number(registry, "MaxHealth");
        assertEquals(24d, maxHealth.getValue(player).doubleValue());
        assertTrue(maxHealth.canSet());
        assertTrue(maxHealth.setValue(30d, player));
        assertEquals(30d, player.maxHealth);

        final Variables.NumberVariableHandler experience = number(registry, "Experience");
        assertEquals(450, experience.getValue(player).intValue());
        assertTrue(experience.canSet());
        assertTrue(experience.setValue(900, player));
        assertEquals(900, player.experiencePoints);

        final Variables.NumberVariableHandler experienceLevel = number(registry, "ExperienceLevel");
        assertEquals(12, experienceLevel.getValue(player).intValue());
        assertTrue(experienceLevel.canSet());
        assertTrue(experienceLevel.setValue(15, player));
        assertEquals(15, player.experienceLevel);

        assertEquals(42, number(registry, "Ping").getValue(player).intValue());

        assertEquals(0.2d, number(registry, "WalkSpeed").getValue(player).doubleValue());
        assertTrue(number(registry, "WalkSpeed").setValue(0.4d, player));
        assertEquals(0.4d, player.walkSpeed);

        assertEquals(0.1d, number(registry, "FlySpeed").getValue(player).doubleValue());
        assertTrue(number(registry, "FlySpeed").setValue(0.3d, player));
        assertEquals(0.3d, player.flySpeed);

        assertTrue(bool(registry, "Glowing").getValue(player));
        assertTrue(bool(registry, "Glowing").setValue(false, player));
        assertFalse(player.glowing);

        assertFalse(bool(registry, "Op").getValue(player));
        assertTrue(bool(registry, "Op").setValue(true, player));
        assertTrue(player.operator);

        assertTrue(bool(registry, "Sleeping").getValue(player));
        assertTrue(bool(registry, "Climbing").getValue(player));
        assertTrue(bool(registry, "InLava").getValue(player));
        assertFalse(bool(registry, "InWater").getValue(player));

        final Variables.StringVariableHandler gameMode = string(registry, "GameMode");
        assertEquals("survival", gameMode.getValue(player));
        assertTrue(gameMode.canSet());
        assertTrue(gameMode.setValue("creative", player));
        assertEquals("creative", player.gameMode);

        assertEquals(72_000, number(registry, "PlaytimeTicks").getValue(player).intValue());
        assertTrue(number(registry, "PlaytimeTicks").setValue(1_200, player));
        assertEquals(1_200, player.playtimeTicks);
        assertEquals(1d, number(registry, "PlaytimeMinutes").getValue(player).doubleValue());
        assertEquals(1d / 60d, number(registry, "PlaytimeHours").getValue(player).doubleValue());

        final Variables.StringVariableHandler world = string(registry, "CurrentWorld");
        assertEquals("overworld", world.getValue(player));
        assertTrue(world.canSet());
        assertTrue(world.setValue("nether", player));
        assertEquals("nether", player.worldName);

        assertTrue(number(registry, "CurrentPositionX").setValue(10.5d, player));
        assertTrue(number(registry, "CurrentPositionY").setValue(70d, player));
        assertTrue(number(registry, "CurrentPositionZ").setValue(-4d, player));
        assertEquals(10.5d, player.x);
        assertEquals(70d, player.y);
        assertEquals(-4d, player.z);

        final Variables.StringVariableHandler biome = string(registry, "CurrentBiome");
        assertEquals("plains", biome.getValue(player));
        assertFalse(biome.canSet());

        final Variables.StringVariableHandler weather = string(registry, "Weather");
        assertEquals("clear", weather.getValue(player));
        assertTrue(weather.canSet());
        assertTrue(weather.setValue("rain", player));
        assertEquals("rain", player.weather);

        final Variables.NumberVariableHandler distance = number(registry, "DistanceToLocation");
        assertEquals(5d, distance.getValue(player, "overworld", 10.5d, 70d, 1d).doubleValue());

        final Variables.NumberVariableHandler nearbyEntities = number(registry, "NearbyEntityCount");
        assertEquals(3, nearbyEntities.getValue(player, "zombie", 10d).intValue());

        final Variables.ItemStackListVariableHandler inventory = itemList(registry, "Inventory");
        assertEquals("stone", inventory.getValue(player).get(0).listedMaterials(""));
        assertEquals(3, inventory.getValue(player).get(0).amount());
        assertTrue(inventory.setValue(List.of(ItemStackSelection.parse("diamond").withAmount(2)), player));
        assertEquals("diamond", player.inventoryItems.get(0).listedMaterials(""));
        assertEquals(2, player.inventoryItems.get(0).amount());

        final Variables.BooleanVariableHandler chance = bool(registry, "Chance");
        assertFalse(chance.getValue(player, 0d));
        assertTrue(chance.getValue(player, 100d));

        final Variables.NumberVariableHandler randomRange = number(registry, "RandomNumberBetweenRange");
        assertEquals(7, randomRange.getValue(player, 7d, 7d).intValue());
        final int reversedRangeValue = randomRange.getValue(player, 10d, 5d).intValue();
        assertTrue(reversedRangeValue >= 5 && reversedRangeValue <= 10);
    }

    @Test
    void numericPlayerVariablesCanonicalizeValuesBeforeCallingThePlatform() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        final RecordingPlayer player = new RecordingPlayer();

        assertTrue(number(registry, "MaxHealth").setValue(-5.0d, player));
        assertEquals(1.0d, player.maxHealth);
        assertTrue(number(registry, "MaxHealth").setValue(30.0d, player));

        assertTrue(number(registry, "Health").setValue(50.0d, player));
        assertEquals(30.0d, player.health);
        assertTrue(number(registry, "Health").setValue(-5.0d, player));
        assertEquals(0.0d, player.health);

        assertTrue(number(registry, "FoodLevel").setValue(50, player));
        assertEquals(20, player.foodLevel);
        assertTrue(number(registry, "Saturation").setValue(-5.0d, player));
        assertEquals(0.0d, player.saturation);
        assertTrue(number(registry, "Saturation").setValue(50.0d, player));
        assertEquals(20.0d, player.saturation);

        assertTrue(number(registry, "ExperienceLevel").setValue(-5, player));
        assertEquals(0, player.experienceLevel);
        assertTrue(number(registry, "Experience").setValue(-5, player));
        assertEquals(0, player.experiencePoints);

        assertTrue(number(registry, "WalkSpeed").setValue(5.0d, player));
        assertEquals(1.0d, player.walkSpeed);
        assertTrue(number(registry, "FlySpeed").setValue(-5.0d, player));
        assertEquals(-1.0d, player.flySpeed);

        assertTrue(number(registry, "PlaytimeTicks").setValue(-5, player));
        assertEquals(0, player.playtimeTicks);
        assertTrue(number(registry, "PlaytimeMinutes").setValue(2.9d, player));
        assertEquals(2_400, player.playtimeTicks);
        assertTrue(number(registry, "PlaytimeHours").setValue(Double.MAX_VALUE, player));
        assertEquals(Integer.MAX_VALUE, player.playtimeTicks);

        assertFalse(number(registry, "Health").setValue(Double.NaN, player));
        assertFalse(number(registry, "WalkSpeed").setValue(Double.POSITIVE_INFINITY, player));
    }

    @Test
    void questStatusVariablesUseCoreQuestState() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                new QuestStatusAdapter(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        BuiltInPack.register(plugin, adapter);
        com.notquests.builtin.objectives.JumpObjective.register(plugin.createRegistryAdapter(null));
        seedCondition(plugin, adapter, registry);
        final RecordingPlayer player = new RecordingPlayer();
        for (final String questName : List.of("daily", "active", "cooldown", "accepts", "completions", "fails")) {
            plugin.getOrCreateQuest(questName);
        }
        plugin.getOrCreateQuest("daily").addObjective(
                1,
                "Jump",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Jump once");
        plugin.getOrCreateQuest("daily").addObjective(
                2,
                "Jump",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Jump again");
        plugin.setActiveQuestNames(player, List.of("active"));
        plugin.recordCompletedQuest(player.playerIdentifier(), "default", "old", 1L);
        plugin.setQuestPoints(player, 25);
        plugin.setQuestAcceptCooldownComplete("cooldown", 60);
        plugin.recordCompletedQuest(player.playerIdentifier(), "default", "cooldown", System.currentTimeMillis());
        plugin.setQuestMaxAccepts("accepts", 1);
        plugin.recordCompletedQuest(player.playerIdentifier(), "default", "accepts", 1L);
        plugin.setQuestMaxCompletions("completions", 1);
        plugin.recordCompletedQuest(player.playerIdentifier(), "default", "completions", 1L);
        plugin.setQuestMaxFails("fails", 1);
        plugin.recordFailedQuest(player.playerIdentifier(), "default", "fails", 1L);
        seedTags(plugin, player);

        assertTrue(bool(registry, "QuestAbleToAccept").getValue(player, "daily"));
        assertTrue(bool(registry, "QuestOnCooldown").getValue(player, "cooldown"));
        assertTrue(bool(registry, "QuestReachedMaxAccepts").getValue(player, "accepts"));
        assertTrue(bool(registry, "QuestReachedMaxCompletions").getValue(player, "completions"));
        assertTrue(bool(registry, "QuestReachedMaxFails").getValue(player, "fails"));

        assertEquals(List.of("active"), list(registry, "ActiveQuests").getValue(player));
        assertTrue(list(registry, "ActiveQuests").setValue(List.of("daily"), player));
        assertEquals(List.of("daily"), plugin.activeQuestNames(player));

        assertEquals(List.of(), list(registry, "CompletedObjectiveIDsOfQuest").getValue(player, "daily"));
        assertTrue(list(registry, "CompletedObjectiveIDsOfQuest").setValue(List.of("1"), player, "daily"));
        assertEquals(List.of("1"), plugin.completedObjectiveIds(player, "daily"));

        assertEquals(
                List.of("old", "cooldown", "accepts", "completions"),
                list(registry, "CompletedQuests").getValue(player));
        assertTrue(list(registry, "CompletedQuests").setValue(List.of("daily", "fails"), player));
        assertEquals(List.of("daily", "fails"), plugin.completedQuestNames(player));

        assertEquals(
                List.of("minecraft:sharpness", "minecraft:unbreaking"),
                list(registry, "ItemInInventoryEnchantments").getValue(player, "HAND"));

        assertEquals(25, number(registry, "QuestPoints").getValue(player).intValue());
        assertTrue(number(registry, "QuestPoints").setValue(40, player));
        assertEquals(40, plugin.questPoints(player));

        assertTrue(bool(registry, "Permission").getValue(player, "notquests.test"));
        assertTrue(bool(registry, "Permission").setValue(false, player, "notquests.test"));
        assertFalse(adapter.hasPermission(player, "notquests.test"));

        assertEquals(6, number(registry, "Statistic").getValue(player, "JUMP").intValue());
        assertTrue(number(registry, "Statistic").setValue(9, player, "JUMP"));
        assertEquals(9, adapter.playerStatistic(player, "JUMP"));
        assertTrue(number(registry, "Statistic").setValue(-9, player, "JUMP"));
        assertEquals(0, adapter.playerStatistic(player, "JUMP"));

        assertTrue(bool(registry, "Advancement").getValue(player, "minecraft:story/mine_stone"));
        assertTrue(bool(registry, "Advancement").setValue(false, player, "minecraft:story/mine_stone"));
        assertFalse(adapter.hasAdvancement(player, "minecraft:story/mine_stone"));

        assertEquals("stone", string(registry, "Block").getValue(player, "overworld", 1, 64, 2));
        assertTrue(string(registry, "Block").setValue("dirt", player, "overworld", 1, 64, 2));
        assertEquals("dirt", adapter.blockMaterial(adapter.location("overworld", 1, 64, 2)));

        assertTrue(bool(registry, "TagBoolean").getValue(player, "flag"));
        assertTrue(bool(registry, "TagBoolean").setValue(false, player, "flag"));
        assertEquals(Boolean.FALSE, plugin.playerTagValue(player, "flag", TagType.BOOLEAN));

        assertEquals(3, number(registry, "TagInteger").getValue(player, "count").intValue());
        assertTrue(number(registry, "TagInteger").setValue(7, player, "count"));
        assertEquals(7, plugin.playerTagValue(player, "count", TagType.INTEGER));

        assertEquals(1.5f, number(registry, "TagFloat").getValue(player, "ratio").floatValue());
        assertTrue(number(registry, "TagFloat").setValue(2.5f, player, "ratio"));
        assertEquals(2.5f, plugin.playerTagValue(player, "ratio", TagType.FLOAT));

        assertEquals(4.25d, number(registry, "TagDouble").getValue(player, "score").doubleValue());
        assertTrue(number(registry, "TagDouble").setValue(6.5d, player, "score"));
        assertEquals(6.5d, plugin.playerTagValue(player, "score", TagType.DOUBLE));

        assertEquals("hello", string(registry, "TagString").getValue(player, "label"));
        assertTrue(string(registry, "TagString").setValue("updated", player, "label"));
        assertEquals("updated", plugin.playerTagValue(player, "label", TagType.STRING));

        final String reflectionTarget = ReflectionTarget.class.getName();
        ReflectionTarget.booleanValue = true;
        ReflectionTarget.integerValue = 3;
        ReflectionTarget.floatValue = 1.25f;
        ReflectionTarget.doubleValue = 4.5d;
        ReflectionTarget.stringValue = "before";
        assertTrue(bool(registry, "ReflectionStaticBoolean").getValue(player, reflectionTarget, "booleanValue"));
        assertTrue(bool(registry, "ReflectionStaticBoolean").setValue(false, player, reflectionTarget, "booleanValue"));
        assertFalse(ReflectionTarget.booleanValue);

        assertEquals(3, number(registry, "ReflectionStaticInteger").getValue(player, reflectionTarget, "integerValue").intValue());
        assertTrue(number(registry, "ReflectionStaticInteger").setValue(9, player, reflectionTarget, "integerValue"));
        assertEquals(9, ReflectionTarget.integerValue);

        assertEquals(1.25f, number(registry, "ReflectionStaticFloat").getValue(player, reflectionTarget, "floatValue").floatValue());
        assertTrue(number(registry, "ReflectionStaticFloat").setValue(2.5f, player, reflectionTarget, "floatValue"));
        assertEquals(2.5f, ReflectionTarget.floatValue);

        assertEquals(4.5d, number(registry, "ReflectionStaticDouble").getValue(player, reflectionTarget, "doubleValue").doubleValue());
        assertTrue(number(registry, "ReflectionStaticDouble").setValue(8.75d, player, reflectionTarget, "doubleValue"));
        assertEquals(8.75d, ReflectionTarget.doubleValue);

        assertEquals("before", string(registry, "ReflectionStaticString").getValue(player, reflectionTarget, "stringValue"));
        assertTrue(string(registry, "ReflectionStaticString").setValue("after", player, reflectionTarget, "stringValue"));
        assertEquals("after", ReflectionTarget.stringValue);

        assertTrue(bool(registry, "Condition").getValue(player, "can_enter"));
    }

    private static Variables.BooleanVariableHandler bool(final NotQuestsRegistry registry, final String id) {
        return (Variables.BooleanVariableHandler) variable(registry, id).handler();
    }

    private static Conditions.Type conditionType(final NotQuestsRegistry registry, final String id) {
        return registry.conditions().stream()
                .filter(condition -> condition.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void seedTags(final NotQuestsPlugin plugin, final PlatformPlayer player) {
        plugin.createTag(TagType.BOOLEAN, "flag");
        plugin.createTag(TagType.INTEGER, "count");
        plugin.createTag(TagType.FLOAT, "ratio");
        plugin.createTag(TagType.DOUBLE, "score");
        plugin.createTag(TagType.STRING, "label");
        plugin.setPlayerTagValue(player, "flag", TagType.BOOLEAN, Boolean.TRUE);
        plugin.setPlayerTagValue(player, "count", TagType.INTEGER, 3);
        plugin.setPlayerTagValue(player, "ratio", TagType.FLOAT, 1.5f);
        plugin.setPlayerTagValue(player, "score", TagType.DOUBLE, 4.25d);
        plugin.setPlayerTagValue(player, "label", TagType.STRING, "hello");
    }

    private static void seedCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final NotQuestsRegistry registry) {
        adapter.conditions()
                .condition("AlwaysTrue")
                .displayName("Always True")
                .description("Always succeeds.")
                .check((condition, questPlayer) -> "")
                .register();
        plugin.saveCondition("can_enter", conditionType(registry, "AlwaysTrue"), new com.notquests.builtin.TestData(Map.of()));
    }

    private static Variables.NumberVariableHandler number(final NotQuestsRegistry registry, final String id) {
        return (Variables.NumberVariableHandler) variable(registry, id).handler();
    }

    private static Variables.StringVariableHandler string(final NotQuestsRegistry registry, final String id) {
        return (Variables.StringVariableHandler) variable(registry, id).handler();
    }

    private static com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler list(
            final NotQuestsRegistry registry, final String id) {
        return (com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler) variable(registry, id).handler();
    }

    private static Variables.ItemStackListVariableHandler itemList(final NotQuestsRegistry registry, final String id) {
        return (Variables.ItemStackListVariableHandler) variable(registry, id).handler();
    }

    private static Variables.Type variable(final NotQuestsRegistry registry, final String id) {
        return registry.variables().stream()
                .filter(variable -> variable.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static final class RecordingPlayer implements TestPlatformPlayer {
        private String displayName = "Alex";
        private double health = 12.5d;
        private double maxHealth = 24d;
        private int foodLevel = 13;
        private double saturation = 5.0d;
        private int experiencePoints = 450;
        private int experienceLevel = 12;
        private int ping = 42;
        private double walkSpeed = 0.2d;
        private double flySpeed = 0.1d;
        private boolean glowing = true;
        private boolean operator;
        private boolean sleeping = true;
        private boolean climbing = true;
        private boolean inLava = true;
        private boolean inWater;
        private String gameMode = "survival";
        private int playtimeTicks = 72_000;
        private String worldName = "overworld";
        private double x = 1;
        private double y = 64;
        private double z = 2;
        private String weather = "clear";
        private List<ItemSelection> inventoryItems =
                new ArrayList<>(List.of(ItemStackSelection.parse("stone").withAmount(3)));

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "player";
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public boolean setDisplayName(final String displayName) {
            this.displayName = displayName;
            return true;
        }

        @Override
        public double health() {
            return health;
        }

        @Override
        public boolean setHealth(final double health) {
            this.health = health;
            return true;
        }

        @Override
        public double maxHealth() {
            return maxHealth;
        }

        @Override
        public boolean setMaxHealth(final double maxHealth) {
            this.maxHealth = maxHealth;
            return true;
        }

        @Override
        public int foodLevel() {
            return foodLevel;
        }

        @Override
        public boolean setFoodLevel(final int foodLevel) {
            this.foodLevel = foodLevel;
            return true;
        }

        @Override
        public double saturation() {
            return saturation;
        }

        @Override
        public boolean setSaturation(final double saturation) {
            this.saturation = saturation;
            return true;
        }

        @Override
        public int experienceLevel() {
            return experienceLevel;
        }

        @Override
        public boolean setExperienceLevel(final int level) {
            this.experienceLevel = level;
            return true;
        }

        @Override
        public int experiencePoints() {
            return experiencePoints;
        }

        @Override
        public boolean setExperiencePoints(final int points) {
            this.experiencePoints = points;
            return true;
        }

        @Override
        public int pingMillis() {
            return ping;
        }

        @Override
        public double walkSpeed() {
            return walkSpeed;
        }

        @Override
        public boolean setWalkSpeed(final double speed) {
            this.walkSpeed = speed;
            return true;
        }

        @Override
        public double flySpeed() {
            return flySpeed;
        }

        @Override
        public boolean setFlySpeed(final double speed) {
            this.flySpeed = speed;
            return true;
        }

        @Override
        public boolean isGlowing() {
            return glowing;
        }

        @Override
        public boolean setGlowing(final boolean glowing) {
            this.glowing = glowing;
            return true;
        }

        @Override
        public boolean isOperator() {
            return operator;
        }

        @Override
        public boolean setOperator(final boolean operator) {
            this.operator = operator;
            return true;
        }

        @Override
        public boolean isSleeping() {
            return sleeping;
        }

        @Override
        public boolean isClimbing() {
            return climbing;
        }

        @Override
        public boolean isInLava() {
            return inLava;
        }

        @Override
        public boolean isInWater() {
            return inWater;
        }

        @Override
        public String gameMode() {
            return gameMode;
        }

        @Override
        public boolean setGameMode(final String gameMode) {
            this.gameMode = gameMode;
            return true;
        }

        @Override
        public List<String> availableGameModes() {
            return List.of("survival", "creative");
        }

        @Override
        public int playtimeTicks() {
            return playtimeTicks;
        }

        @Override
        public boolean setPlaytimeTicks(final int ticks) {
            this.playtimeTicks = ticks;
            return true;
        }

        @Override
        public String worldName() {
            return worldName;
        }

        @Override
        public boolean teleportToWorldSpawn(final String worldName) {
            this.worldName = worldName;
            return true;
        }

        @Override
        public List<String> availableWorldNames() {
            return List.of("overworld", "nether");
        }

        @Override
        public double positionX() {
            return x;
        }

        @Override
        public boolean setPositionX(final double x) {
            this.x = x;
            return true;
        }

        @Override
        public double positionY() {
            return y;
        }

        @Override
        public boolean setPositionY(final double y) {
            this.y = y;
            return true;
        }

        @Override
        public double positionZ() {
            return z;
        }

        @Override
        public boolean setPositionZ(final double z) {
            this.z = z;
            return true;
        }

        @Override
        public String biomeName() {
            return "plains";
        }

        @Override
        public List<String> availableBiomeNames() {
            return List.of("plains", "forest");
        }

        @Override
        public String weather() {
            return weather;
        }

        @Override
        public boolean setWeather(final String weather) {
            this.weather = weather;
            return true;
        }

        @Override
        public double distanceTo(final NQLocation location) {
            final double dx = x - location.x();
            final double dy = y - location.y();
            final double dz = z - location.z();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override
        public List<String> nearbyEntityTypeIds(final double radius) {
            return radius == 10d
                    ? List.of("minecraft:zombie", "minecraft:zombie", "minecraft:zombie")
                    : List.of();
        }

        @Override
        public List<ItemSelection> inventoryItems() {
            return List.copyOf(inventoryItems);
        }

        @Override
        public boolean setInventoryItems(final List<SavedItems.ItemChoice> items) {
            inventoryItems = items.stream().map(SavedItems.ItemChoice::selection)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            return true;
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
        public NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private static final class QuestStatusAdapter implements TestNotQuestsAdapter {
        private final NotQuestsAdapter delegate;
        private boolean permission = true;
        private int statisticValue = 6;
        private boolean advancement = true;
        private String blockMaterial = "stone";

        private QuestStatusAdapter(final NotQuestsAdapter delegate) {
            this.delegate = delegate;
        }

        @Override
        public FieldFactories fields() {
            return delegate.fields();
        }

        @Override
        public Actions.Registry actions() {
            return delegate.actions();
        }

        @Override
        public Conditions.Registry conditions() {
            return delegate.conditions();
        }

        @Override
        public Objectives.Registry objectives() {
            return delegate.objectives();
        }

        @Override
        public Triggers.Registry triggers() {
            return delegate.triggers();
        }

        @Override
        public Variables.Registry variables() {
            return delegate.variables();
        }

        @Override
        public List<String> variableNames(final com.notquests.core.variables.VariableDataType type) {
            return delegate.variableNames(type);
        }

        @Override
        public com.notquests.core.variables.VariableDataType variableType(final String variableName) {
            return delegate.variableType(variableName);
        }

        @Override
        public String variableSingular(final String variableName) {
            return delegate.variableSingular(variableName);
        }

        @Override
        public String variablePlural(final String variableName) {
            return delegate.variablePlural(variableName);
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            return delegate.variableFields(variableName);
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            return delegate.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public String serverBrand() {
            return delegate.serverBrand();
        }

        @Override
        public List<String> damageTypeIds() {
            return delegate.damageTypeIds();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return delegate.onlinePlayerNames();
        }

        @Override
        public PlatformPlayer onlineQuestPlayer(final String playerName) {
            return delegate.onlineQuestPlayer(playerName);
        }

        @Override
        public List<String> itemSelectionOptions() {
            return delegate.itemSelectionOptions();
        }

        @Override
        public List<String> entityTypeIds() {
            return delegate.entityTypeIds();
        }

        @Override
        public List<String> particleTypeIds() {
            return delegate.particleTypeIds();
        }

        @Override
        public List<String> soundTypeIds() {
            return delegate.soundTypeIds();
        }

        @Override
        public List<String> soundCategoryIds() {
            return delegate.soundCategoryIds();
        }

        @Override
        public boolean supportsNpcAttachments() {
            return delegate.supportsNpcAttachments();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return delegate.supportsArmorStandAttachmentTools();
        }

        @Override
        public List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
            return delegate.npcSelectorOptions(allowNone, allowRightClickSelect);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return delegate.npcSelection(npcSelector);
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return delegate.setNpcQuestGiver(selection, enabled);
        }

        @Override
        public boolean giveArmorStandTool(
                final PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return delegate.giveArmorStandTool(actor, tool);
        }

        @Override
        public boolean giveNpcSelectionTool(
                final PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            return delegate.giveNpcSelectionTool(actor, selectionId, displayName, lore);
        }

        @Override
        public void schedule(final java.time.Duration delay, final Runnable action) {
            delegate.schedule(delay, action);
        }

        @Override
        public boolean isServerThread() {
            return delegate.isServerThread();
        }

        @Override
        public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
            return delegate.callOnServerThread(action);
        }

        @Override
        public List<String> inventorySlotIds() {
            return List.of("HAND", "0");
        }

        @Override
        public List<String> enchantmentIds() {
            return List.of("minecraft:sharpness", "minecraft:unbreaking");
        }

        @Override
        public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
            return slotId.equalsIgnoreCase("HAND")
                    ? List.of("minecraft:sharpness", "minecraft:unbreaking")
                    : List.of();
        }

        @Override
        public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
            return "notquests.test".equals(permission) && this.permission;
        }

        @Override
        public boolean supportsPermissionMutation() {
            return true;
        }

        @Override
        public boolean setPermission(final PlatformPlayer questPlayer, final String permission, final boolean value) {
            if (!"notquests.test".equals(permission)) {
                return false;
            }
            this.permission = value;
            return true;
        }

        @Override
        public List<String> statisticIds() {
            return List.of("JUMP");
        }

        @Override
        public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
            return "JUMP".equals(statisticId) ? statisticValue : 0;
        }

        @Override
        public boolean setPlayerStatistic(
                final PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            if (!"JUMP".equals(statisticId)) {
                return false;
            }
            statisticValue = value;
            return true;
        }

        @Override
        public List<String> advancementIds() {
            return List.of("minecraft:story/mine_stone");
        }

        @Override
        public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
            return "minecraft:story/mine_stone".equals(advancementId) && advancement;
        }

        @Override
        public boolean setAdvancement(
                final PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            if (!"minecraft:story/mine_stone".equals(advancementId)) {
                return false;
            }
            advancement = completed;
            return true;
        }

        @Override
        public List<String> worldNames() {
            return List.of("overworld");
        }

        @Override
        public List<String> blockMaterialOptions() {
            return List.of("stone", "dirt");
        }

        @Override
        public String blockMaterial(final NQLocation location) {
            return location != null && "overworld".equals(location.worldName()) ? blockMaterial : "";
        }

        @Override
        public boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final NQLocation location,
                final String materialOrKeyword) {
            if (location == null || !"overworld".equals(location.worldName())) {
                return false;
            }
            blockMaterial = materialOrKeyword;
            return true;
        }

        @Override
        public List<ItemSelection> containerInventoryItems(final NQLocation location) {
            return delegate.containerInventoryItems(location);
        }

        @Override
        public boolean addContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            return delegate.addContainerInventoryItems(location, items, dropOverflow);
        }

        @Override
        public boolean removeContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return delegate.removeContainerInventoryItems(location, items);
        }

        @Override
        public boolean setContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return delegate.setContainerInventoryItems(location, items);
        }

        @Override
        public ItemSelection parseItemSelection(final String input) {
            return delegate.parseItemSelection(input);
        }

        @Override
        public NQLocation location(final String worldName, final double x, final double y, final double z) {
            return delegate.location(worldName, x, y, z);
        }

        @Override
        public void warn(final String message) {
            delegate.warn(message);
        }

        @Override
        public void logInfo(final String message) {
            delegate.logInfo(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            delegate.broadcast(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            delegate.dispatchConsoleCommand(command);
        }

        @Override
        public String resolveActionText(
                final com.notquests.core.registry.NotQuestsRegistry.Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return delegate.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final com.notquests.core.structs.ActiveObjective activeObjective,
                final java.util.Map<String, String> replacements) {
            return delegate.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }

    private static final class ReflectionTarget {
        private static boolean booleanValue = true;
        private static int integerValue = 3;
        private static float floatValue = 1.25f;
        private static double doubleValue = 4.5d;
        private static String stringValue = "before";
    }
}
