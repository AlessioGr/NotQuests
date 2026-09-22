package com.notquests.neoforge;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

final class NeoForgePlayer implements PlatformPlayer {
    private static final ConcurrentHashMap<UUID, ServerBossEvent> PROGRESS_BOSS_BARS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ServerBossEvent> COMPASS_BOSS_BARS = new ConcurrentHashMap<>();

    private final NotQuestsPlugin plugin;
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final NeoForgeText text;
    private final NeoForgeBeamTracker beamTracker;
    private final NeoForgeGuiRenderer guiRenderer;
    NeoForgePlayer(
            final NotQuestsPlugin plugin,
            final ServerPlayer player,
            final MinecraftServer server,
            final NeoForgeText text,
            final NeoForgeBeamTracker beamTracker,
            final NeoForgeGuiRenderer guiRenderer) {
        this.plugin = plugin;
        this.player = player;
        this.server = server;
        this.text = text;
        this.beamTracker = beamTracker;
        this.guiRenderer = guiRenderer;
    }

    ServerPlayer player() {
        return player;
    }

    static void removePlayer(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final ServerBossEvent bossBar = PROGRESS_BOSS_BARS.remove(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        final ServerBossEvent compass = COMPASS_BOSS_BARS.remove(player.getUUID());
        if (compass != null) {
            compass.removePlayer(player);
        }
    }

    @Override
    public boolean hasPlayer() {
        return true;
    }

    @Override
    public String playerName() {
        return player.getName().getString();
    }

    @Override
    public String displayName() {
        return player.getCustomName() == null
                ? player.getName().getString()
                : player.getCustomName().getString();
    }

    @Override
    public String playerIdentifier() {
        return player.getUUID().toString();
    }

    @Override
    public boolean setDisplayName(final String displayName) {
        player.setCustomName(Component.literal(displayName == null ? "" : displayName));
        player.setCustomNameVisible(displayName != null && !displayName.isBlank());
        return true;
    }

    @Override
    public boolean isFlying() {
        return player.getAbilities().flying;
    }

    @Override
    public boolean setFlying(final boolean flying) {
        player.getAbilities().flying = flying;
        player.onUpdateAbilities();
        return true;
    }

    @Override
    public boolean isSneaking() {
        return player.isShiftKeyDown();
    }

    @Override
    public boolean setSneaking(final boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        return true;
    }

    @Override
    public boolean isSprinting() {
        return player.isSprinting();
    }

    @Override
    public boolean setSprinting(final boolean sprinting) {
        player.setSprinting(sprinting);
        return true;
    }

    @Override
    public boolean isSwimming() {
        return player.isSwimming();
    }

    @Override
    public boolean setSwimming(final boolean swimming) {
        player.setSwimming(swimming);
        return true;
    }

    @Override
    public double health() {
        return player.getHealth();
    }

    @Override
    public double maxHealth() {
        return player.getMaxHealth();
    }

    @Override
    public boolean setMaxHealth(final double maxHealth) {
        if (!Double.isFinite(maxHealth) || maxHealth < 1.0d) {
            return false;
        }
        final var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return false;
        }
        try {
            attribute.setBaseValue(maxHealth);
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
        if (player.getHealth() > attribute.getValue()) {
            player.setHealth((float) attribute.getValue());
        }
        return true;
    }

    @Override
    public boolean setHealth(final double health) {
        if (!Double.isFinite(health) || health < 0.0d || health > player.getMaxHealth()) {
            return false;
        }
        player.setHealth((float) health);
        return true;
    }

    @Override
    public int foodLevel() {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    public boolean setFoodLevel(final int foodLevel) {
        if (foodLevel < 0 || foodLevel > 20) {
            return false;
        }
        player.getFoodData().setFoodLevel(foodLevel);
        return true;
    }

    @Override
    public double saturation() {
        return player.getFoodData().getSaturationLevel();
    }

    @Override
    public boolean setSaturation(final double saturation) {
        if (!Double.isFinite(saturation) || saturation < 0.0d || saturation > 20.0d) {
            return false;
        }
        player.getFoodData().setSaturation((float) saturation);
        return true;
    }

    @Override
    public int experienceLevel() {
        return player.experienceLevel;
    }

    @Override
    public boolean setExperienceLevel(final int level) {
        if (level < 0) {
            return false;
        }
        player.setExperienceLevels(level - player.experienceLevel);
        return true;
    }

    @Override
    public int experiencePoints() {
        return player.totalExperience;
    }

    @Override
    public boolean setExperiencePoints(final int points) {
        if (points < 0) {
            return false;
        }
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;
        player.giveExperiencePoints(points);
        return true;
    }

    @Override
    public int pingMillis() {
        return player.connection.latency();
    }

    @Override
    public double walkSpeed() {
        return player.getAbilities().getWalkingSpeed();
    }

    @Override
    public boolean setWalkSpeed(final double speed) {
        if (!Double.isFinite(speed) || speed < -1.0d || speed > 1.0d) {
            return false;
        }
        player.getAbilities().setWalkingSpeed((float) speed);
        player.onUpdateAbilities();
        return true;
    }

    @Override
    public double flySpeed() {
        return player.getAbilities().getFlyingSpeed();
    }

    @Override
    public boolean setFlySpeed(final double speed) {
        if (!Double.isFinite(speed) || speed < -1.0d || speed > 1.0d) {
            return false;
        }
        player.getAbilities().setFlyingSpeed((float) speed);
        player.onUpdateAbilities();
        return true;
    }

    @Override
    public boolean isGlowing() {
        return player.isCurrentlyGlowing();
    }

    @Override
    public boolean setGlowing(final boolean glowing) {
        player.setGlowingTag(glowing);
        return true;
    }

    @Override
    public boolean isOperator() {
        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }

    @Override
    public boolean setOperator(final boolean operator) {
        final NameAndId nameAndId = new NameAndId(player.getGameProfile());
        if (operator) {
            server.getPlayerList().op(nameAndId);
        } else {
            server.getPlayerList().deop(nameAndId);
        }
        return true;
    }

    @Override
    public boolean isSleeping() {
        return player.isSleeping();
    }

    @Override
    public boolean isClimbing() {
        return player.onClimbable();
    }

    @Override
    public boolean isInLava() {
        return player.isInLava();
    }

    @Override
    public boolean isInWater() {
        return player.isInWater();
    }

    @Override
    public String gameMode() {
        return player.gameMode.getGameModeForPlayer().getName();
    }

    @Override
    public boolean setGameMode(final String gameMode) {
        final GameType type = GameType.byName(gameMode, null);
        return type != null && player.setGameMode(type);
    }

    @Override
    public List<String> availableGameModes() {
        return Arrays.stream(GameType.values())
                .map(GameType::getName)
                .sorted()
                .toList();
    }

    @Override
    public int playtimeTicks() {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    @Override
    public boolean setPlaytimeTicks(final int ticks) {
        if (ticks < 0) {
            return false;
        }
        player.getStats().setValue(player, Stats.CUSTOM.get(Stats.PLAY_TIME), ticks);
        return true;
    }

    @Override
    public String worldName() {
        return NeoForgeWorldNames.displayName(player.level().dimension().identifier());
    }

    @Override
    public String worldIdentifier() {
        return player.level().dimension().identifier().toString();
    }

    @Override
    public boolean teleportToWorldSpawn(final String worldName) {
        final ServerLevel level = level(worldName);
        if (level == null) {
            return false;
        }
        final BlockPos spawn = level.getRespawnData().pos();
        player.teleportTo(
                level,
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5,
                Set.of(),
                level.getRespawnData().yaw(),
                level.getRespawnData().pitch(),
                true);
        return true;
    }

    @Override
    public boolean teleport(final NQLocation location, final Double yaw, final Double pitch) {
        if (location == null) {
            return false;
        }
        final ServerLevel level = level(location.worldName());
        if (level == null) {
            return false;
        }
        player.teleportTo(
                level,
                location.x(),
                location.y(),
                location.z(),
                Set.of(),
                yaw == null ? location.yaw() : yaw.floatValue(),
                pitch == null ? location.pitch() : pitch.floatValue(),
                true);
        return true;
    }

    @Override
    public List<String> availableWorldNames() {
        final List<String> names = new ArrayList<>();
        for (final ServerLevel level : server.getAllLevels()) {
            names.add(NeoForgeWorldNames.displayName(level.dimension().identifier()));
            names.add(level.dimension().identifier().toString());
        }
        return names;
    }

    @Override
    public double positionX() {
        return player.getX();
    }

    @Override
    public boolean setPositionX(final double x) {
        player.teleportTo(x, player.getY(), player.getZ());
        return true;
    }

    @Override
    public double positionY() {
        return player.getY();
    }

    @Override
    public boolean setPositionY(final double y) {
        player.teleportTo(player.getX(), y, player.getZ());
        return true;
    }

    @Override
    public double positionZ() {
        return player.getZ();
    }

    @Override
    public boolean setPositionZ(final double z) {
        player.teleportTo(player.getX(), player.getY(), z);
        return true;
    }

    @Override
    public double yawDegrees() {
        return player.getYRot();
    }

    @Override
    public double pitchDegrees() {
        return player.getXRot();
    }

    @Override
    public String biomeName() {
        return player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .map(key -> suggestionId(key.identifier()))
                .orElse("");
    }

    @Override
    public List<String> availableBiomeNames() {
        return server.registryAccess()
                .lookup(Registries.BIOME)
                .map(registry -> registry.keySet().stream()
                        .map(NeoForgePlayer::suggestionId)
                        .sorted()
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public String weather() {
        final ServerLevel level = player.level();
        if (level.isThundering()) {
            return "thunder";
        }
        return level.isRaining() ? "rain" : "clear";
    }

    @Override
    public boolean setWeather(final String weather) {
        final String normalized = weather == null ? "" : weather.toLowerCase(Locale.ROOT);
        final ServerLevel level = player.level();
        final WeatherData weatherData = level.getWeatherData();
        switch (normalized) {
            case "clear" -> {
                weatherData.setClearWeatherTime(12_000);
                weatherData.setRainTime(0);
                weatherData.setThunderTime(0);
                weatherData.setRaining(false);
                weatherData.setThundering(false);
            }
            case "rain" -> {
                weatherData.setClearWeatherTime(0);
                weatherData.setRainTime(12_000);
                weatherData.setThunderTime(0);
                weatherData.setRaining(true);
                weatherData.setThundering(false);
            }
            case "thunder" -> {
                weatherData.setClearWeatherTime(0);
                weatherData.setRainTime(12_000);
                weatherData.setThunderTime(12_000);
                weatherData.setRaining(true);
                weatherData.setThundering(true);
            }
            default -> {
                return false;
            }
        }
        weatherData.setDirty();
        return true;
    }

    @Override
    public double distanceTo(final NQLocation location) {
        if (location == null) {
            return Double.MAX_VALUE;
        }
        final ServerLevel level = level(location.worldName());
        if (level == null || !player.level().equals(level)) {
            return Double.MAX_VALUE;
        }
        final double dx = player.getX() - location.x();
        final double dy = player.getY() - location.y();
        final double dz = player.getZ() - location.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public List<String> nearbyEntityTypeIds(final double radius) {
        if (!Double.isFinite(radius) || radius < 0) {
            return List.of();
        }
        final AABB area = new AABB(
                player.getX() - radius,
                player.getY() - radius,
                player.getZ() - radius,
                player.getX() + radius,
                player.getY() + radius,
                player.getZ() + radius);
        final List<String> entityTypes = new ArrayList<>();
        for (final Entity entity : player.level().getEntities(player, area)) {
            final String fullId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            entityTypes.add(fullId);
        }
        return List.copyOf(entityTypes);
    }

    @Override
    public long currentWorldTimeTicks() {
        return player.level().getDefaultClockTime() % 24_000L;
    }

    @Override
    public void sendMessage(final String miniMessage) {
        text.sendMessage(player, miniMessage);
    }

    @Override
    public void sendCommandChoice(
            final String prefixMiniMessage,
            final String choiceMiniMessage,
            final String command,
            final String hoverMiniMessage) {
        final var choice = text.component(choiceMiniMessage)
                .copy()
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(
                                command == null || command.isBlank()
                                        ? ""
                                        : command.startsWith("/") ? command : "/" + command))
                        .withHoverEvent(new HoverEvent.ShowText(text.component(hoverMiniMessage))));
        player.sendSystemMessage(Component.empty()
                .append(text.component(prefixMiniMessage))
                .append(choice));
    }

    @Override
    public void sendActionBar(final String miniMessage) {
        text.sendActionBar(player, miniMessage);
    }

    @Override
    public String applyExternalPlaceholders(final String value) {
        // NeoForge has no PlaceholderAPI-style external placeholder provider. The explicit
        // capability result is therefore the original text, while NotQuests' own placeholders
        // continue to be resolved by core before this platform hook is called.
        return value == null ? "" : value;
    }

    @Override
    public boolean supportsExternalPlaceholders() {
        return false;
    }

    @Override
    public void showProgressBossBar(final String miniMessage, final double progress) {
        final ServerBossEvent bossBar = PROGRESS_BOSS_BARS.computeIfAbsent(
                player.getUUID(),
                ignored -> new ServerBossEvent(
                        UUID.randomUUID(),
                        text.component(miniMessage),
                        BossEvent.BossBarColor.BLUE,
                        BossEvent.BossBarOverlay.PROGRESS));
        bossBar.setName(text.component(miniMessage));
        bossBar.setProgress((float) Math.max(0.0d, Math.min(1.0d, progress)));
        bossBar.setVisible(true);
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void hideProgressBossBar() {
        final ServerBossEvent bossBar = PROGRESS_BOSS_BARS.remove(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    @Override
    public boolean renderObjectiveMarkers(
            final Map<String, NQLocation> markers,
            final boolean useBeaconBlocks,
            final boolean force) {
        return beamTracker != null && beamTracker.render(player, markers, useBeaconBlocks, force);
    }

    @Override
    public void showLocationCompass(final NotQuestsPlugin.ObjectiveCompass.Display display) {
        if (display == null) {
            return;
        }
        final ServerBossEvent compass = COMPASS_BOSS_BARS.computeIfAbsent(
                player.getUUID(),
                ignored -> new ServerBossEvent(
                        UUID.randomUUID(),
                        text.component(display.title()),
                        compassColor(display.severity()),
                        BossEvent.BossBarOverlay.PROGRESS));
        compass.setName(text.component(display.title()));
        compass.setProgress(display.progress());
        compass.setColor(compassColor(display.severity()));
        compass.setVisible(true);
        if (!compass.getPlayers().contains(player)) {
            compass.addPlayer(player);
        }
    }

    @Override
    public void hideLocationCompass() {
        final ServerBossEvent compass = COMPASS_BOSS_BARS.remove(player.getUUID());
        if (compass != null) {
            compass.removePlayer(player);
        }
    }

    private static BossEvent.BossBarColor compassColor(
            final NotQuestsPlugin.ObjectiveCompass.Severity severity) {
        return switch (severity) {
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            case RED -> BossEvent.BossBarColor.RED;
        };
    }

    @Override
    public void showTitle(
            final String title,
            final String subtitle,
            final Duration fadeIn,
            final Duration stay,
            final Duration fadeOut) {
        text.showTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void chat(final String message) {
        final Runnable chat = () -> {
            final String rawMessage = message == null ? "" : message;
            final Component decorated = CommonHooks.onServerChatSubmittedEvent(
                    player,
                    rawMessage,
                    Component.literal(rawMessage));
            if (decorated != null) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("chat.type.text", player.getDisplayName(), decorated),
                        false);
            }
        };
        if (server.isSameThread()) {
            chat.run();
        } else {
            server.execute(chat);
        }
    }

    @Override
    public void performCommand(final String command) {
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command.startsWith("/") ? command : "/" + command);
    }

    @Override
    public void closeInventory() {
        player.closeContainer();
    }

    @Override
    public boolean beforeQuestAccepted(
            final Quest quest,
            final boolean triggerAcceptQuestTrigger) {
        if (quest == null) {
            return false;
        }
        return allowLifecycleEvent("QuestAccept", () -> !NeoForge.EVENT_BUS.post(
                new NotQuestsEvents.QuestAccept(
                        player,
                        quest.getIdentifier(),
                        quest,
                        triggerAcceptQuestTrigger)).isCanceled());
    }

    @Override
    public boolean beforeQuestPointsChanged(final long newQuestPoints) {
        return allowLifecycleEvent("QuestPointsChange", () -> !NeoForge.EVENT_BUS.post(
                new NotQuestsEvents.QuestPointsChange(player, newQuestPoints)).isCanceled());
    }

    @Override
    public boolean beforeQuestCompleted(
            final Quest quest,
            final boolean forced) {
        if (quest == null) {
            return false;
        }
        return allowLifecycleEvent("QuestComplete", () -> !NeoForge.EVENT_BUS.post(
                new NotQuestsEvents.QuestComplete(
                        player,
                        quest.getIdentifier(),
                        quest,
                        forced)).isCanceled());
    }

    @Override
    public boolean beforeQuestFailed(final Quest quest) {
        if (quest == null) {
            return false;
        }
        return allowLifecycleEvent("QuestFail", () -> !NeoForge.EVENT_BUS.post(
                new NotQuestsEvents.QuestFail(
                        player,
                        quest.getIdentifier(),
                        quest)).isCanceled());
    }

    @Override
    public boolean beforeObjectiveCompleted(
            final Quest quest,
            final ActiveObjective objective) {
        if (quest == null || objective == null) {
            return false;
        }
        return allowLifecycleEvent("ObjectiveComplete", () -> !NeoForge.EVENT_BUS.post(
                new NotQuestsEvents.ObjectiveComplete(
                        player,
                        quest.getIdentifier(),
                        quest,
                        objective,
                        objective.getObjectivePath(),
                        objective.getObjectiveID(),
                        objective.getHolderPath())).isCanceled());
    }

    private boolean allowLifecycleEvent(
            final String eventName,
            final Callable<Boolean> dispatch) {
        try {
            if (server.isSameThread()) {
                return dispatch.call();
            }
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            server.execute(() -> {
                try {
                    result.complete(dispatch.call());
                } catch (final Exception exception) {
                    result.completeExceptionally(exception);
                }
            });
            return result.get();
        } catch (final Exception exception) {
            plugin.warn("Could not dispatch NeoForge " + eventName + " event: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean giveItems(final List<SavedItems.ItemChoice> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        boolean gaveAny = false;
        for (final ItemStack stack : itemStacks(text, server.registryAccess(), items)) {
            if (!player.addItem(stack)) {
                player.drop(stack, false, Prediction.SERVER_ONLY);
            }
            gaveAny = true;
        }
        return gaveAny;
    }

    @Override
    public int removeItems(
            final List<SavedItems.ItemChoice> items,
            final int maxAmount) {
        if (items == null || items.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        int remaining = maxAmount;
        int removed = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (remaining <= 0) {
                break;
            }
            final ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !matchesAny(plugin, server.registryAccess(), items, stack)) {
                continue;
            }
            final int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            remaining -= toRemove;
            removed += toRemove;
        }
        if (removed > 0) {
            player.inventoryMenu.broadcastChanges();
        }
        return removed;
    }

    @Override
    public boolean addInventoryItems(
            final List<SavedItems.ItemChoice> items,
            final boolean dropOverflow) {
        final List<ItemStack> stacks = itemStacks(text, server.registryAccess(), items);
        final int requested = stacks.stream().mapToInt(ItemStack::getCount).sum();
        final List<ItemStack> leftovers = addToPlayerInventory(stacks);
        final int remaining = leftovers.stream().mapToInt(ItemStack::getCount).sum();
        if (dropOverflow) {
            leftovers.forEach(stack -> player.drop(stack, false, Prediction.SERVER_ONLY));
        }
        final boolean changed = requested > remaining || dropOverflow && remaining > 0;
        if (changed) {
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    @Override
    public boolean removeInventoryItems(final List<SavedItems.ItemChoice> items) {
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            if (item == null || item.selection() == null) {
                continue;
            }
            changed = removeItems(List.of(item), item.selection().amount()) > 0 || changed;
        }
        return changed;
    }

    @Override
    public List<ItemSelection> inventoryItems() {
        final List<ItemSelection> items = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            items.add(new NativeItemSelection(stack, server.registryAccess()));
        }
        return List.copyOf(items);
    }

    @Override
    public boolean setInventoryItems(final List<SavedItems.ItemChoice> items) {
        final List<ItemStack> stacks = materializeItems(items);
        if (stacks == null) {
            return false;
        }
        player.getInventory().clearContent();
        final boolean complete = addToPlayerInventory(stacks).isEmpty();
        player.inventoryMenu.broadcastChanges();
        return complete;
    }

    @Override
    public List<ItemSelection> enderChestItems() {
        return containerItems(player.getEnderChestInventory(), server.registryAccess());
    }

    @Override
    public boolean addEnderChestItems(
            final List<SavedItems.ItemChoice> items,
            final boolean addOverflowToInventory,
            final boolean dropOverflow) {
        final List<ItemStack> stacks = itemStacks(text, server.registryAccess(), items);
        final int requested = stacks.stream().mapToInt(ItemStack::getCount).sum();
        List<ItemStack> leftovers = addToContainer(
                player.getEnderChestInventory(),
                stacks);
        if (addOverflowToInventory) {
            leftovers = addToPlayerInventory(leftovers);
        }
        final int remaining = leftovers.stream().mapToInt(ItemStack::getCount).sum();
        if (dropOverflow) {
            leftovers.forEach(stack -> player.drop(stack, false, Prediction.SERVER_ONLY));
        }
        player.inventoryMenu.broadcastChanges();
        return requested > remaining || dropOverflow && remaining > 0;
    }

    @Override
    public boolean removeEnderChestItems(final List<SavedItems.ItemChoice> items) {
        final boolean changed = removeFromContainer(
                plugin, server.registryAccess(), player.getEnderChestInventory(), items);
        if (changed) {
            player.getEnderChestInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    @Override
    public boolean setEnderChestItems(final List<SavedItems.ItemChoice> items) {
        final List<ItemStack> stacks = materializeItems(items);
        if (stacks == null) {
            return false;
        }
        player.getEnderChestInventory().clearContent();
        final boolean complete = addToContainer(player.getEnderChestInventory(), stacks).isEmpty();
        player.getEnderChestInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return complete;
    }

    @Override
    public boolean spawnParticle(
            final String particleId,
            final int count,
            final boolean showToEveryone,
            final NQLocation location,
            final double offsetX,
            final double offsetY,
            final double offsetZ,
            final double speed) {
        final ParticleType<?> particleType = particleType(particleId);
        if (!(particleType instanceof final SimpleParticleType simpleParticleType) || count < 0) {
            return false;
        }
        if (location == null) {
            return false;
        }
        final ServerLevel level = level(location.worldName());
        if (level == null) {
            return false;
        }
        final double x = location.x();
        final double y = location.y();
        final double z = location.z();
        if (showToEveryone) {
            return level.sendParticles(
                    simpleParticleType,
                    false,
                    true,
                    x,
                    y,
                    z,
                    count,
                    offsetX,
                    offsetY,
                    offsetZ,
                    speed) > 0;
        }
        return level.sendParticles(
                player,
                simpleParticleType,
                false,
                true,
                x,
                y,
                z,
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed);
    }

    @Override
    public void stopSounds() {
        player.connection.send(new ClientboundStopSoundPacket(null, null));
    }

    @Override
    public boolean playSound(
            final String soundId,
            final SoundAudience audience,
            final NQLocation location,
            final double volume,
            final double pitch,
            final String soundCategory) {
        final Holder<SoundEvent> sound = soundHolder(soundId);
        final SoundSource source = soundSource(soundCategory);
        if (sound == null || source == null || location == null) {
            return false;
        }
        final SoundEvent soundEvent = sound.value();
        final ServerLevel level = level(location.worldName());
        if (level == null) {
            return false;
        }
        if (audience == SoundAudience.EVERYONE_AT_OWN_LOCATION) {
            for (final ServerPlayer targetPlayer : server.getPlayerList().getPlayers()) {
                targetPlayer.connection.send(new ClientboundSoundPacket(
                        sound,
                        source,
                        targetPlayer.getX(),
                        targetPlayer.getY(),
                        targetPlayer.getZ(),
                        (float) volume,
                        (float) pitch,
                        ThreadLocalRandom.current().nextLong()));
            }
            return true;
        }
        if (audience == SoundAudience.WORLD) {
            level.playSound(
                    null,
                    location.x(),
                    location.y(),
                    location.z(),
                    soundEvent,
                    source,
                    (float) volume,
                    (float) pitch);
        } else {
            player.connection.send(new ClientboundSoundPacket(
                    sound,
                    source,
                    location.x(),
                    location.y(),
                    location.z(),
                    (float) volume,
                    (float) pitch,
                    ThreadLocalRandom.current().nextLong()));
        }
        return true;
    }

    @Override
    public boolean spawnVanillaMob(
            final String entityType,
            final NQLocation location) {
        if (location == null) {
            return false;
        }
        final ServerLevel level = level(location.worldName());
        if (level == null) {
            return false;
        }

        final EntityType<?> type = entityType(entityType);
        if (type == null) {
            return false;
        }
        final Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return false;
        }
        entity.setPos(location.x(), location.y(), location.z());
        entity.setYRot(location.yaw());
        entity.setXRot(location.pitch());
        if (entity instanceof final Mob mob) {
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(entity.blockPosition()),
                    EntitySpawnReason.COMMAND,
                    null);
        }
        return level.addFreshEntity(entity);
    }

    @Override
    public NQLocation lookingAtBlock(final double maxDistance) {
        final HitResult hit = player.pick(Math.max(1.0d, maxDistance), 0.0f, false);
        if (!(hit instanceof final BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        final BlockPos position = blockHit.getBlockPos();
        return NQLocation.at(
                worldName(),
                position.getX(),
                position.getY(),
                position.getZ());
    }

    @Override
    public boolean showGui(final ResolvedGui gui) {
        if (guiRenderer == null) {
            return false;
        }
        return guiRenderer.open(player, gui, this);
    }

    private ServerLevel level(final String worldName) {
        for (final ServerLevel level : server.getAllLevels()) {
            final var key = level.dimension();
            if (NeoForgeWorldNames.matches(key.identifier(), worldName)) {
                return level;
            }
        }
        return null;
    }

    private static Item item(final String material) {
        if (material == null || material.isBlank() || material.equalsIgnoreCase("any")) {
            return null;
        }
        final Identifier id = Identifier.tryParse(material.contains(":") ? material : "minecraft:" + material);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(id)
                .map(reference -> reference.value() == Items.AIR
                        ? null
                        : reference.value())
                .orElse(null);
    }

    static List<ItemSelection> containerItems(
            final Container container,
            final RegistryAccess registryAccess) {
        if (container == null) {
            return List.of();
        }
        final ArrayList<ItemSelection> items = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            final ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                items.add(new NativeItemSelection(stack, registryAccess));
            }
        }
        return List.copyOf(items);
    }

    static List<ItemStack> itemStacks(
            final NeoForgeText text,
            final RegistryAccess registryAccess,
            final List<SavedItems.ItemChoice> choices) {
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (final SavedItems.ItemChoice choice
                : choices == null ? List.<SavedItems.ItemChoice>of() : choices) {
            stacks.addAll(itemStacks(text, registryAccess, choice));
        }
        return List.copyOf(stacks);
    }

    private List<ItemStack> materializeItems(final List<SavedItems.ItemChoice> choices) {
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (final SavedItems.ItemChoice choice
                : choices == null ? List.<SavedItems.ItemChoice>of() : choices) {
            if (choice == null || choice.selection() == null) {
                return null;
            }
            final List<ItemStack> selected = itemStacks(text, server.registryAccess(), choice);
            if (selected.isEmpty()) {
                return null;
            }
            selected.forEach(stack -> stacks.add(stack.copy()));
        }
        return List.copyOf(stacks);
    }

    private List<ItemStack> addToPlayerInventory(final List<ItemStack> stacks) {
        final ArrayList<ItemStack> leftovers = new ArrayList<>();
        for (final ItemStack original : stacks == null ? List.<ItemStack>of() : stacks) {
            final ItemStack remaining = original.copy();
            player.addItem(remaining);
            if (!remaining.isEmpty()) {
                leftovers.add(remaining.copy());
            }
        }
        return List.copyOf(leftovers);
    }

    private static List<ItemStack> itemStacks(
            final NeoForgeText text,
            final RegistryAccess registryAccess,
            final SavedItems.ItemChoice choice) {
        final ItemSelection selection = choice == null ? null : choice.selection();
        if (selection instanceof final NativeItemSelection nativeItem) {
            final ItemStack stack = nativeItem.stack();
            stack.setCount(Math.max(1, selection.amount()));
            applyDisplayName(stack, choice.displayName(), text);
            return List.of(stack);
        }
        if (selection == null || selection.any() || !selection.savedItemNames().isEmpty()) {
            return List.of();
        }
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        final Set<String> exactMaterials = new HashSet<>();
        for (final Map<String, Object> exactItem : selection.exactItems()) {
            if (!"neoforge".equalsIgnoreCase(String.valueOf(exactItem.get("platform")))) {
                continue;
            }
            final ItemStack stack = decodeItemStack(exactItem.get("data"), registryAccess);
            if (stack.isEmpty()) {
                continue;
            }
            stack.setCount(Math.max(1, selection.amount()));
            applyDisplayName(stack, choice.displayName(), text);
            exactMaterials.add(itemId(stack));
            stacks.add(stack);
        }
        for (final String material : selection.materialIds()) {
            final Item item = item(material);
            if (item != null && !exactMaterials.contains(BuiltInRegistries.ITEM.getKey(item).toString())) {
                final ItemStack stack = new ItemStack(item, Math.max(1, selection.amount()));
                applyDisplayName(stack, choice.displayName(), text);
                stacks.add(stack);
            }
        }
        return List.copyOf(stacks);
    }

    private static void applyDisplayName(
            final ItemStack stack,
            final String displayName,
            final NeoForgeText text) {
        if (stack != null && text != null && displayName != null && !displayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, text.component(displayName));
        }
    }

    static List<ItemSelection> selections(
            final List<ItemStack> stacks,
            final RegistryAccess registryAccess) {
        final ArrayList<ItemSelection> selections = new ArrayList<>();
        for (final ItemStack stack : stacks == null ? List.<ItemStack>of() : stacks) {
            if (!stack.isEmpty()) {
                selections.add(new NativeItemSelection(stack, registryAccess));
            }
        }
        return List.copyOf(selections);
    }

    static boolean itemSelectionsAreSimilar(
            final NeoForgeText text,
            final RegistryAccess registryAccess,
            final ItemSelection required,
            final ItemSelection actual) {
        if (required == null || actual == null) {
            return false;
        }
        final List<ItemStack> actualItems = itemStacks(
                text, registryAccess, List.of(new SavedItems.ItemChoice(actual, "")));
        final List<ItemStack> requiredItems = itemStacks(
                text, registryAccess, List.of(new SavedItems.ItemChoice(required, "")));
        for (final ItemStack requiredItem : requiredItems) {
            for (final ItemStack actualItem : actualItems) {
                if (ItemStack.isSameItemSameComponents(requiredItem, actualItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<ItemStack> addToContainer(final Container container, final List<ItemStack> itemStacks) {
        if (container == null) {
            return itemStacks == null ? List.of() : List.copyOf(itemStacks);
        }
        final ArrayList<ItemStack> leftovers = new ArrayList<>();
        for (final ItemStack original : itemStacks == null ? List.<ItemStack>of() : itemStacks) {
            ItemStack remaining = original.copy();
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                final ItemStack existing = container.getItem(slot);
                if (existing.isEmpty()) {
                    final int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    final ItemStack inserted = remaining.copy();
                    inserted.setCount(moved);
                    container.setItem(slot, inserted);
                    remaining.shrink(moved);
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    final int moved = Math.min(
                            remaining.getCount(),
                            existing.getMaxStackSize() - existing.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                leftovers.add(remaining.copy());
            }
        }
        container.setChanged();
        return List.copyOf(leftovers);
    }

    static boolean removeFromContainer(
            final NotQuestsPlugin plugin,
            final RegistryAccess registryAccess,
            final Container container,
            final List<SavedItems.ItemChoice> items) {
        if (container == null || items == null || items.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (final SavedItems.ItemChoice item : items) {
            if (item == null || item.selection() == null) {
                continue;
            }
            final ItemSelection selection = item.selection();
            int remaining = Math.max(1, selection.amount());
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                final ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !plugin.itemsAreSimilar(
                        selection, new NativeItemSelection(stack, registryAccess))) {
                    continue;
                }
                final int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
                changed = true;
            }
        }
        if (changed) {
            container.setChanged();
        }
        return changed;
    }

    static boolean matchesItem(
            final NotQuestsPlugin plugin,
            final RegistryAccess registryAccess,
            final ItemSelection selection,
            final ItemStack stack) {
        return selection != null
                && stack != null
                && !stack.isEmpty()
                && plugin.itemsAreSimilar(
                        selection, new NativeItemSelection(stack, registryAccess));
    }

    private static boolean matchesAny(
            final NotQuestsPlugin plugin,
            final RegistryAccess registryAccess,
            final List<SavedItems.ItemChoice> items,
            final ItemStack stack) {
        for (final SavedItems.ItemChoice item : items) {
            if (item != null && plugin.itemsAreSimilar(
                    item.selection(), new NativeItemSelection(stack, registryAccess))) {
                return true;
            }
        }
        return false;
    }

    private static String itemId(final ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String suggestionId(final Identifier identifier) {
        return "minecraft".equals(identifier.getNamespace())
                ? identifier.getPath()
                : identifier.toString();
    }

    static ItemSelection nativeItemSelection(
            final ItemStack stack,
            final RegistryAccess registryAccess) {
        return stack == null || stack.isEmpty()
                ? null
                : new NativeItemSelection(stack, registryAccess);
    }

    private static ItemStack decodeItemStack(final Object encoded, final RegistryAccess registryAccess) {
        if (encoded == null || registryAccess == null) {
            return ItemStack.EMPTY;
        }
        try {
            return ItemStack.CODEC.parse(
                            registryAccess.createSerializationContext(JsonOps.INSTANCE),
                            JsonParser.parseString(String.valueOf(encoded)))
                    .result()
                    .map(ItemStack::copy)
                    .orElse(ItemStack.EMPTY);
        } catch (final RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static final class NativeItemSelection implements ItemSelection {
        private final ItemStack itemStack;
        private final RegistryAccess registryAccess;

        private NativeItemSelection(final ItemStack itemStack, final RegistryAccess registryAccess) {
            this.itemStack = itemStack.copy();
            this.registryAccess = registryAccess;
        }

        private ItemStack stack() {
            return itemStack.copy();
        }

        @Override
        public boolean includesMaterial(final String materialId) {
            if (materialId == null || materialId.isBlank()) {
                return false;
            }
            final String normalized = materialId.contains(":") ? materialId : "minecraft:" + materialId;
            return itemId(itemStack).equalsIgnoreCase(normalized);
        }

        @Override
        public String listedMaterials(final String miniMessageTag) {
            return itemId(itemStack);
        }

        @Override
        public boolean any() {
            return false;
        }

        @Override
        public List<String> materialIds() {
            return List.of(itemId(itemStack));
        }

        @Override
        public List<String> savedItemNames() {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> exactItems() {
            if (registryAccess == null) {
                return List.of();
            }
            return ItemStack.CODEC.encodeStart(
                            registryAccess.createSerializationContext(JsonOps.INSTANCE),
                            itemStack)
                    .result()
                    .map(encoded -> List.<Map<String, Object>>of(Map.of(
                            "platform", "neoforge",
                            "material", itemId(itemStack),
                            "data", encoded.toString())))
                    .orElse(List.of());
        }

        @Override
        public int amount() {
            return Math.max(1, itemStack.getCount());
        }

        @Override
        public ItemSelection withAmount(final int amount) {
            final ItemStack copy = itemStack.copy();
            copy.setCount(Math.max(1, amount));
            return new NativeItemSelection(copy, registryAccess);
        }
    }

    private static EntityType<?> entityType(final String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return null;
        }
        final Identifier id = Identifier.tryParse(
                entityType.contains(":") ? entityType : "minecraft:" + entityType);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
    }

    private static ParticleType<?> particleType(final String particleType) {
        if (particleType == null || particleType.isBlank()) {
            return null;
        }
        final Identifier id = Identifier.tryParse(
                particleType.contains(":") ? particleType : "minecraft:" + particleType);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.PARTICLE_TYPE.get(id).map(reference -> reference.value()).orElse(null);
    }

    private static Holder<SoundEvent> soundHolder(final String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return null;
        }
        final Identifier id = Identifier.tryParse(soundId.contains(":") ? soundId : "minecraft:" + soundId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.get(id).map(reference -> (Holder<SoundEvent>) reference).orElse(null);
    }

    private static SoundSource soundSource(final String soundCategory) {
        if (soundCategory == null || soundCategory.isBlank()) {
            return null;
        }
        for (final SoundSource source : SoundSource.values()) {
            if (source.name().equalsIgnoreCase(soundCategory) || source.getName().equalsIgnoreCase(soundCategory)) {
                return source;
            }
        }
        return null;
    }

}
