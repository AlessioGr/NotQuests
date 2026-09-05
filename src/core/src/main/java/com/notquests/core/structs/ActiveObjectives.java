package com.notquests.core.structs;

import com.notquests.core.actions.SavedActions;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class ActiveObjectives {
    private static final int BEAM_MAX_DISTANCE = 88;
    private static final int END_GATEWAY_MIN_Y = 192;
    private static final double CLIENT_EXACT_MARKER_RADIUS = 96.0d;
    private static final double CLIENT_FAR_MARKER_RADIUS = 80.0d;

    private Function<String, String> activeProfileLookup = ignored -> "default";
    private BiFunction<String, String, QuestPlayer> playerLookup = (ignoredPlayer, ignoredProfile) -> null;
    private Supplier<List<QuestPlayer>> allPlayers = List::of;
    private ActiveObjective.ConditionGate conditionGate = ActiveObjective.ConditionGate.ALWAYS;
    private Consumer<ActiveObjective> completionHandler = ignored -> {};
    private Predicate<ActiveObjective> unlockHandler = ignored -> true;
    private ActiveObjective.ProgressChangeHandler progressChangeHandler = (ignored, oldValue, newValue) -> {};
    private final PlayerPlacedHarvestBlocks playerPlacedHarvestBlocks = new PlayerPlacedHarvestBlocks();
    private final Map<String, List<BrewedItem>> freshlyBrewedItems = new ConcurrentHashMap<>();
    private final Map<String, PlayerPosition> playerPositions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> playerSneaking = new ConcurrentHashMap<>();
    private final Map<String, Map<String, NQLocation>> objectiveMarkers = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> objectiveMarkerVersions = new ConcurrentHashMap<>();
    private long objectiveMarkerVersion;
    private int conditionRefreshSeconds;
    private int unlockRefreshSeconds;
    private int markerRefreshSeconds;

    public enum InventoryClick {
        LEFT,
        RIGHT,
        NUMBER_KEY,
        DROP,
        CONTROL_DROP,
        SWAP_OFFHAND,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        OTHER
    }

    public enum TakenItem {
        BREWED,
        SMELTED,
        TRADED
    }

    public record InventoryItem(int amount, int maxStackSize, boolean empty, boolean similarToTarget) {
        public InventoryItem {
            amount = Math.max(amount, 0);
            maxStackSize = Math.max(maxStackSize, 1);
        }

        public static InventoryItem empty(final int targetMaxStackSize) {
            return new InventoryItem(0, targetMaxStackSize, true, false);
        }

        public static InventoryItem target(final int amount, final int maxStackSize) {
            return new InventoryItem(amount, maxStackSize, false, true);
        }
    }

    public record BrewedItem(String itemKey, int amount) {
        public BrewedItem {
            itemKey = itemKey == null ? "" : itemKey;
            amount = Math.max(amount, 0);
        }
    }

    public record BeamPoint(double x, double y, double z) {}

    public record BeamBlock(int x, int y, int z) {}

    public record BlockBeam(BeamBlock block, boolean targetReached) {}

    public record ClientBeam(double x, double y, double z, double height, boolean proxy) {}

    public static BlockBeam blockBeam(
            final BeamPoint player,
            final BeamPoint target,
            final boolean sameWorld,
            final boolean beaconMode,
            final ToIntFunction<BeamBlock> highestBlockY,
            final Predicate<BeamBlock> blockIsAir,
            final int minWorldHeight) {
        if (!sameWorld || player == null || target == null) {
            return null;
        }
        final boolean targetReached = distanceSquared(player, target)
                <= BEAM_MAX_DISTANCE * BEAM_MAX_DISTANCE;
        final BeamBlock projectedColumn = beamBlock(projectBeamMarker(player, target, false, 0));
        final int projectedHeight = beaconMode && highestBlockY != null
                ? highestBlockY.applyAsInt(projectedColumn)
                : 0;
        final BeamBlock marker = beamBlock(projectBeamMarker(
                player,
                target,
                beaconMode,
                projectedHeight));
        return new BlockBeam(
                visibleBeamBlock(
                        marker,
                        blockIsAir != null && blockIsAir.test(marker),
                        minWorldHeight),
                targetReached);
    }

    private static BeamPoint projectBeamMarker(
            final BeamPoint player,
            final BeamPoint target,
            final boolean beaconMode,
            final int highestBlockYAtProjectedColumn) {
        if (distanceSquared(player, target) <= BEAM_MAX_DISTANCE * BEAM_MAX_DISTANCE) {
            return target;
        }
        final double dx = target.x() - player.x();
        final double dy = target.y() - player.y();
        final double dz = target.z() - player.z();
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            return player;
        }
        return new BeamPoint(
                player.x() + dx / length * BEAM_MAX_DISTANCE,
                beaconMode ? highestBlockYAtProjectedColumn : Math.max(player.y(), END_GATEWAY_MIN_Y),
                player.z() + dz / length * BEAM_MAX_DISTANCE);
    }

    private static BeamBlock beamBlock(final BeamPoint point) {
        return new BeamBlock(block(point.x()), block(point.y()), block(point.z()));
    }

    private static BeamBlock visibleBeamBlock(
            final BeamBlock markerBlock,
            final boolean markerBlockIsAir,
            final int minWorldHeight) {
        if (markerBlockIsAir) {
            return markerBlock;
        }
        final int belowY = markerBlock.y() - 1;
        return belowY >= minWorldHeight
                ? new BeamBlock(markerBlock.x(), belowY, markerBlock.z())
                : markerBlock;
    }

    public static ClientBeam clientBeam(
            final BeamPoint camera,
            final BeamPoint target) {
        final double targetX = target.x() + 0.5d;
        final double targetY = target.y() + 0.25d;
        final double targetZ = target.z() + 0.5d;
        final double dx = targetX - camera.x();
        final double dz = targetZ - camera.z();
        final double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= CLIENT_EXACT_MARKER_RADIUS || horizontalDistance < 0.0001d) {
            return new ClientBeam(targetX, targetY, targetZ, 192.0d, false);
        }
        final double visibleDistance = Math.min(
                CLIENT_FAR_MARKER_RADIUS,
                Math.max(32.0d, horizontalDistance - 8.0d));
        final double scale = visibleDistance / horizontalDistance;
        return new ClientBeam(
                camera.x() + dx * scale,
                Math.max(camera.y() - 48.0d, Math.min(camera.y() + 48.0d, targetY)),
                camera.z() + dz * scale,
                128.0d,
                true);
    }

    private static double distanceSquared(final BeamPoint first, final BeamPoint second) {
        final double dx = first.x() - second.x();
        final double dy = first.y() - second.y();
        final double dz = first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static int block(final double value) {
        return (int) Math.floor(value);
    }

    public static int takenResultAmount(
            final InventoryItem result,
            final InventoryItem cursor,
            final InventoryClick click,
            final boolean hotbarSlotOccupied,
            final boolean offhandOccupied,
            final int inventorySpaceLeftForResult) {
        if (result == null) {
            return 0;
        }
        final InventoryItem checkedCursor = cursor == null
                ? InventoryItem.empty(result.maxStackSize())
                : cursor;
        int amount = result.amount();
        switch (click == null ? InventoryClick.OTHER : click) {
            case LEFT -> {
                if (!checkedCursor.empty()
                        && (!checkedCursor.similarToTarget()
                                || checkedCursor.amount() + result.amount() > checkedCursor.maxStackSize())) {
                    amount = 0;
                }
            }
            case RIGHT -> {
                if (!checkedCursor.empty()
                        && (!checkedCursor.similarToTarget()
                                || checkedCursor.amount() + result.amount() > checkedCursor.maxStackSize())) {
                    amount = 0;
                }
                amount = (amount + 1) / 2;
            }
            case NUMBER_KEY -> {
                if (hotbarSlotOccupied) {
                    amount = 0;
                }
            }
            case DROP -> {
                if (!checkedCursor.empty()) {
                    amount = 0;
                }
                amount = 1;
            }
            case CONTROL_DROP -> {
                if (!checkedCursor.empty()) {
                    amount = 0;
                }
            }
            case SWAP_OFFHAND -> {
                if (offhandOccupied) {
                    amount = 0;
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> amount = Math.min(Math.max(inventorySpaceLeftForResult, 0), amount);
            default -> amount = 0;
        }
        return Math.max(amount, 0);
    }

    public static int craftAmount(
            final InventoryItem result,
            final InventoryItem cursor,
            final InventoryClick click,
            final boolean hotbarSlotOccupied,
            final boolean offhandOccupied,
            final int maxCraftable,
            final int resultInventoryCapacity) {
        if (result == null) {
            return 0;
        }
        final InventoryItem checkedCursor = cursor == null
                ? InventoryItem.empty(result.maxStackSize())
                : cursor;
        int amount = result.amount();
        switch (click == null ? InventoryClick.OTHER : click) {
            case LEFT, RIGHT -> {
                if (!checkedCursor.empty()
                        && (!checkedCursor.similarToTarget()
                                || checkedCursor.amount() + result.amount() > checkedCursor.maxStackSize())) {
                    amount = 0;
                }
            }
            case NUMBER_KEY -> {
                if (hotbarSlotOccupied) {
                    amount = 0;
                }
            }
            case DROP, CONTROL_DROP -> {
                if (!checkedCursor.empty()) {
                    amount = 0;
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                amount = Math.max(maxCraftable, 0);
                if (resultInventoryCapacity < amount) {
                    amount = ((Math.max(resultInventoryCapacity, 0) + result.amount() - 1)
                            / result.amount()) * result.amount();
                }
            }
            case SWAP_OFFHAND -> {
                if (offhandOccupied) {
                    amount = 0;
                }
            }
            default -> amount = 0;
        }
        return Math.max(amount, 0);
    }

    public static int inventorySpaceLeft(final List<InventoryItem> inventory, final InventoryItem target) {
        if (target == null) {
            return 0;
        }
        int remaining = 0;
        for (final InventoryItem item : inventory == null ? List.<InventoryItem>of() : inventory) {
            if (item == null || item.empty()) {
                remaining += target.maxStackSize();
            } else if (item.similarToTarget()) {
                remaining += Math.max(target.maxStackSize() - item.amount(), 0);
            }
        }
        return remaining;
    }

    public static int maxCraftAmount(final int resultAmount, final List<Integer> ingredientAmounts) {
        int materialCount = Integer.MAX_VALUE;
        for (final Integer amount : ingredientAmounts == null ? List.<Integer>of() : ingredientAmounts) {
            if (amount != null && amount < materialCount) {
                materialCount = amount;
            }
        }
        return materialCount == Integer.MAX_VALUE ? 0 : Math.max(resultAmount, 0) * Math.max(materialCount, 0);
    }

    public static String blockKey(
            final String worldIdentifier,
            final int x,
            final int y,
            final int z) {
        return (worldIdentifier == null ? "" : worldIdentifier) + ":" + x + ":" + y + ":" + z;
    }

    public static boolean isFullyGrownHarvestable(
            final String materialId,
            final boolean samePlantBelow,
            final boolean ageableAtMaxAge) {
        final String normalized = normalizeMaterial(materialId);
        if (isStackGrownPlant(normalized)) {
            return samePlantBelow;
        }
        if (isFruitBlock(normalized)) {
            return true;
        }
        return !normalized.endsWith("_stem") && ageableAtMaxAge;
    }

    public static boolean shouldTrackPlayerPlacedHarvestBlock(
            final String materialId,
            final boolean ageableAtMaxAge) {
        final String normalized = normalizeMaterial(materialId);
        return isStackGrownPlant(normalized)
                || isFruitBlock(normalized)
                || (!normalized.endsWith("_stem") && ageableAtMaxAge);
    }

    public static List<String> harvestedMaterialAliases(final String materialId) {
        return switch (normalizeMaterial(materialId)) {
            case "carrots" -> List.of("carrot");
            case "potatoes" -> List.of("potato");
            case "beetroots" -> List.of("beetroot");
            case "melon" -> List.of("melon_slice");
            case "sweet_berry_bush" -> List.of("sweet_berries");
            case "cocoa" -> List.of("cocoa_beans");
            case "torchflower_crop" -> List.of("torchflower", "torchflower_seeds");
            case "pitcher_crop" -> List.of("pitcher_plant", "pitcher_pod");
            default -> List.of();
        };
    }

    private static boolean isStackGrownPlant(final String materialId) {
        return materialId.equals("sugar_cane") || materialId.equals("cactus");
    }

    private static boolean isFruitBlock(final String materialId) {
        return materialId.equals("melon") || materialId.equals("pumpkin");
    }

    private static String normalizeMaterial(final String materialId) {
        return materialId == null ? "" : materialId.toLowerCase(Locale.ROOT);
    }

    public static final class PlayerPlacedHarvestBlocks {
        private final Set<String> blockKeys = new HashSet<>();

        public void onBlockPlaced(final String blockKey, final String materialId, final boolean fullyGrown) {
            if (shouldTrackPlayerPlacedHarvestBlock(materialId, fullyGrown)) {
                blockKeys.add(blockKey);
            }
        }

        public void onBlockBroken(final String blockKey) {
            blockKeys.remove(blockKey);
        }

        public boolean contains(final String blockKey) {
            return blockKeys.contains(blockKey);
        }

        public void clear() {
            blockKeys.clear();
        }
    }

    public void conditionGate(final ActiveObjective.ConditionGate conditionGate) {
        this.conditionGate = conditionGate == null ? ActiveObjective.ConditionGate.ALWAYS : conditionGate;
    }

    public void activeProfileLookup(final Function<String, String> activeProfileLookup) {
        this.activeProfileLookup = activeProfileLookup == null ? ignored -> "default" : activeProfileLookup;
    }

    public void players(
            final BiFunction<String, String, QuestPlayer> playerLookup,
            final Supplier<List<QuestPlayer>> allPlayers) {
        this.playerLookup = playerLookup == null ? (ignoredPlayer, ignoredProfile) -> null : playerLookup;
        this.allPlayers = allPlayers == null ? List::of : allPlayers;
    }

    public void completionHandler(final Consumer<ActiveObjective> completionHandler) {
        this.completionHandler = completionHandler == null ? ignored -> {} : completionHandler;
    }

    public void unlockHandler(final Predicate<ActiveObjective> unlockHandler) {
        this.unlockHandler = unlockHandler == null ? ignored -> true : unlockHandler;
    }

    public void progressChangeHandler(final ActiveObjective.ProgressChangeHandler progressChangeHandler) {
        this.progressChangeHandler = progressChangeHandler == null
                ? (ignored, oldValue, newValue) -> {}
                : progressChangeHandler;
    }

    public ActiveObjective activateObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final Objective entry,
            final String ownerProgressOrder,
            final Objectives.Type objectiveType) {
        return activateObjective(
                questPlayer,
                questName,
                entry,
                new int[] {entry.id()},
                ownerProgressOrder,
                objectiveType);
    }

    public ActiveObjective activateObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final Objective entry,
            final int[] objectivePath,
            final String ownerProgressOrder,
            final Objectives.Type objectiveType) {
        return activateObjective(
                questPlayer,
                questName,
                entry,
                objectivePath,
                ownerProgressOrder,
                objectiveType,
                true);
    }

    public ActiveObjective activateObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final Objective entry,
            final int[] objectivePath,
            final String ownerProgressOrder,
            final Objectives.Type objectiveType,
            final boolean updateAfterActivation) {
        final String playerId = playerId(questPlayer);
        return activateObjective(
                questPlayer,
                activeProfile(playerId),
                questName,
                entry,
                objectivePath,
                ownerProgressOrder,
                objectiveType,
                updateAfterActivation);
    }

    public ActiveObjective activateObjective(
            final PlatformPlayer questPlayer,
            final String profile,
            final String questName,
            final Objective entry,
            final int[] objectivePath,
            final String ownerProgressOrder,
            final Objectives.Type objectiveType,
            final boolean updateAfterActivation) {
        final String playerId = playerId(questPlayer);
        final PlayerProfile playerProfile = playerProfile(playerId, profile);
        final QuestPlayer player = player(playerProfile);
        if (player == null) {
            return null;
        }
        final ActiveObjective progress = new ActiveObjective(
                questName,
                entry.id(),
                objectivePath,
                ownerProgressOrder,
                objectiveType,
                entry,
                questPlayer,
                progressNeeded(objectiveType, entry),
                conditionGate,
                completionHandler,
                progressChangeHandler);
        final ActiveQuest activeQuest = player.ensureActiveQuest(questName);
        if (activeQuest == null || !activeQuest.addActiveObjective(progress)) {
            return null;
        }
        if (updateAfterActivation) {
            updateUnlocked(playerProfile, false);
        }
        return progress;
    }

    public void refreshObjectiveUnlocks(final PlatformPlayer questPlayer) {
        final String playerId = playerId(questPlayer);
        updateUnlocked(activePlayerProfile(playerId), false);
    }

    public void refreshObjectiveUnlocks(final PlatformPlayer questPlayer, final String profile) {
        refreshObjectiveUnlocks(questPlayer, profile, false);
    }

    public void refreshObjectiveUnlocks(
            final PlatformPlayer questPlayer,
            final String profile,
            final boolean loading) {
        updateUnlocked(playerProfile(playerId(questPlayer), profile), loading);
    }

    public List<ActiveObjective> activeObjectives(final String playerId) {
        return activeObjectives(playerId, activeProfile(playerId));
    }

    public List<ActiveObjective> activeObjectives(final String playerId, final String profile) {
        final QuestPlayer player = player(playerProfile(playerId, profile));
        return player == null ? List.of() : player.getActiveObjectives();
    }

    public List<ActiveObjective> dispatchableActiveObjectives(final String playerId) {
        final PlayerProfile playerProfile = activePlayerProfile(playerId);
        updateUnlocked(playerProfile, false);
        return activeObjectives(playerProfile).stream()
                .filter(objective -> objective.isUnlocked() && !objective.hasBeenCompleted())
                .toList();
    }

    public void clear() {
        for (final QuestPlayer player : players()) {
            for (final ActiveObjective objective : player.getActiveObjectives()) {
                runCompleteOrLockHandler(objective, false, false);
            }
            player.clearActiveObjectives();
        }
        playerPlacedHarvestBlocks.clear();
        freshlyBrewedItems.clear();
        playerPositions.clear();
        playerSneaking.clear();
        objectiveMarkers.clear();
        objectiveMarkerVersions.clear();
        conditionRefreshSeconds = 0;
        unlockRefreshSeconds = 0;
        markerRefreshSeconds = 0;
    }

    public void removePlayer(final String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            players().stream()
                    .filter(player -> player.getPlayerIdentifier().equals(playerId))
                    .forEach(player -> {
                        for (final ActiveObjective objective : player.getActiveObjectives()) {
                            runCompleteOrLockHandler(objective, false, false);
                        }
                        player.clearActiveObjectives();
                    });
            removePlayerObservations(playerId);
        }
    }

    public void removePlayerObservations(final String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        playerPositions.remove(playerId);
        playerSneaking.remove(playerId);
    }

    public boolean showObjectiveMarker(
            final PlatformPlayer player,
            final String name,
            final NQLocation location,
            final boolean useBeaconBlocks) {
        if (!validMarker(player, name, location)) {
            return false;
        }
        final String playerId = player.playerIdentifier();
        objectiveMarkers
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(name, location);
        objectiveMarkerVersions
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(name, nextObjectiveMarkerVersion());
        return renderObjectiveMarkers(player, useBeaconBlocks, true);
    }

    public boolean showTemporaryObjectiveMarker(
            final PlatformPlayer player,
            final String name,
            final NQLocation location,
            final Duration duration,
            final SavedActions.ActionScheduler scheduler,
            final boolean useBeaconBlocks) {
        if (!validMarker(player, name, location)) {
            return false;
        }
        final String playerId = player.playerIdentifier();
        final Map<String, NQLocation> markers = objectiveMarkers
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        final NQLocation previous = markers.put(name, location);
        final long version = nextObjectiveMarkerVersion();
        objectiveMarkerVersions
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(name, version);
        final boolean shown = renderObjectiveMarkers(player, useBeaconBlocks, true);
        final SavedActions.ActionScheduler checkedScheduler = scheduler == null
                ? SavedActions.ActionScheduler.immediate()
                : scheduler;
        checkedScheduler.schedule(duration == null ? Duration.ZERO : duration, () -> {
            final Map<String, Long> versions = objectiveMarkerVersions.get(playerId);
            if (versions == null || !Long.valueOf(version).equals(versions.get(name))) {
                return;
            }
            versions.remove(name);
            if (versions.isEmpty()) {
                objectiveMarkerVersions.remove(playerId);
            }
            final Map<String, NQLocation> currentMarkers = objectiveMarkers.get(playerId);
            if (currentMarkers == null) {
                return;
            }
            if (previous == null) {
                currentMarkers.remove(name);
            } else {
                currentMarkers.put(name, previous);
            }
            if (currentMarkers.isEmpty()) {
                objectiveMarkers.remove(playerId);
            }
            renderObjectiveMarkers(player, useBeaconBlocks, true);
        });
        return shown;
    }

    public boolean removeObjectiveMarker(
            final PlatformPlayer player,
            final String name,
            final boolean useBeaconBlocks) {
        if (player == null || name == null || name.isBlank()) {
            return false;
        }
        final String playerId = player.playerIdentifier();
        final Map<String, NQLocation> markers = objectiveMarkers.get(playerId);
        final boolean removed = markers != null && markers.remove(name) != null;
        if (markers != null && markers.isEmpty()) {
            objectiveMarkers.remove(playerId);
        }
        final Map<String, Long> versions = objectiveMarkerVersions.get(playerId);
        if (versions != null) {
            versions.remove(name);
            if (versions.isEmpty()) {
                objectiveMarkerVersions.remove(playerId);
            }
        }
        renderObjectiveMarkers(player, useBeaconBlocks, true);
        return removed;
    }

    public void clearObjectiveMarkers(
            final PlatformPlayer player,
            final boolean useBeaconBlocks) {
        if (player == null) {
            return;
        }
        final String playerId = player.playerIdentifier();
        objectiveMarkers.remove(playerId);
        objectiveMarkerVersions.remove(playerId);
        player.renderObjectiveMarkers(Map.of(), useBeaconBlocks, true);
    }

    public boolean refreshObjectiveMarkers(
            final PlatformPlayer player,
            final boolean useBeaconBlocks,
            final boolean force) {
        return renderObjectiveMarkers(player, useBeaconBlocks, force);
    }

    public Map<String, NQLocation> objectiveMarkers(final PlatformPlayer player) {
        if (player == null) {
            return Map.of();
        }
        final Map<String, NQLocation> markers = objectiveMarkers.get(player.playerIdentifier());
        return markers == null || markers.isEmpty() ? Map.of() : Map.copyOf(markers);
    }

    private boolean renderObjectiveMarkers(
            final PlatformPlayer player,
            final boolean useBeaconBlocks,
            final boolean force) {
        return player != null
                && player.hasPlayer()
                && player.renderObjectiveMarkers(objectiveMarkers(player), useBeaconBlocks, force);
    }

    private static boolean validMarker(
            final PlatformPlayer player,
            final String name,
            final NQLocation location) {
        return player != null
                && player.hasPlayer()
                && name != null
                && !name.isBlank()
                && location != null;
    }

    private synchronized long nextObjectiveMarkerVersion() {
        return ++objectiveMarkerVersion;
    }

    public void removeActiveObjective(final ActiveObjective progress) {
        removeActiveObjective(progress, false);
    }

    private void removeActiveObjective(final ActiveObjective progress, final boolean loading) {
        if (progress == null) {
            return;
        }
        for (final QuestPlayer player : players()) {
            final ActiveQuest activeQuest = player.getActiveQuest(progress.getQuestIdentifier());
            if (activeQuest == null || activeQuest.getActiveObjective(progress.getObjectivePath()) != progress) {
                continue;
            }
            final int[] path = progress.getObjectivePath();
            final ActiveObjective parent = path.length <= 1
                    ? null
                    : activeQuest.getActiveObjective(Arrays.copyOf(path, path.length - 1));
            runCompleteOrLockHandler(progress, loading, progress.hasBeenCompleted());
            activeQuest.removeActiveObjective(path);
            checkParentAfterChildRemoved(parent, loading);
            return;
        }
    }

    public void removeActiveObjectives(final String playerId, final String questName) {
        removeActiveObjectives(playerId, activeProfile(playerId), questName);
    }

    public void removeActiveObjectives(
            final String playerId,
            final String profile,
            final String questName) {
        final QuestPlayer player = player(playerProfile(playerId, profile));
        final ActiveQuest activeQuest = player == null ? null : player.getActiveQuest(questName);
        if (activeQuest != null) {
            for (final ActiveObjective objective : activeQuest.getActiveObjectives()) {
                runCompleteOrLockHandler(objective, false, false);
            }
            activeQuest.clearActiveObjectives();
        }
    }

    public List<ActiveObjective> forceCompleteActiveObjectives(
            final String playerId,
            final String questName) {
        return forceCompleteActiveObjectives(playerId, activeProfile(playerId), questName);
    }

    public List<ActiveObjective> forceCompleteActiveObjectives(
            final String playerId,
            final String profile,
            final String questName) {
        final QuestPlayer player = player(playerProfile(playerId, profile));
        final ActiveQuest activeQuest = player == null ? null : player.getActiveQuest(questName);
        if (activeQuest == null || questName == null || questName.isBlank()) {
            return List.of();
        }
        final List<ActiveObjective> completed = activeQuest.getActiveObjectives();
        completed.forEach(objective -> {
            objective.forceCompleteWithoutEffects();
            runCompleteOrLockHandler(objective, false, true);
        });
        activeQuest.clearActiveObjectives();
        return completed;
    }

    private static void runCompleteOrLockHandler(
            final ActiveObjective objective,
            final boolean loading,
            final boolean completed) {
        if (objective != null && objective.getType().completeOrLockHandler() != null) {
            objective.getType().completeOrLockHandler().handle(
                    objective,
                    objective.getQuestPlayer(),
                    loading,
                    completed);
        }
    }

    public ActiveObjective activeObjective(
            final String playerId,
            final String questName,
            final int objectiveId) {
        return activeObjective(playerId, activeProfile(playerId), questName, objectiveId);
    }

    public ActiveObjective activeObjective(
            final String playerId,
            final String profile,
            final String questName,
            final int objectiveId) {
        return activeObjectives(playerProfile(playerId, profile)).stream()
                .filter(progress -> progress.getQuestIdentifier().equalsIgnoreCase(questName)
                        && progress.getObjectivePath().length == 1
                        && progress.getObjectiveID() == objectiveId)
                .findFirst()
                .orElse(null);
    }

    public ActiveObjective activeObjective(
            final String playerId,
            final String questName,
            final int[] objectivePath) {
        return activeObjective(playerId, activeProfile(playerId), questName, objectivePath);
    }

    public ActiveObjective activeObjective(
            final String playerId,
            final String profile,
            final String questName,
            final int[] objectivePath) {
        final QuestPlayer player = player(playerProfile(playerId, profile));
        final ActiveQuest activeQuest = player == null ? null : player.getActiveQuest(questName);
        return activeQuest == null ? null : activeQuest.getActiveObjective(objectivePath);
    }

    public boolean restoreObjectiveProgress(
            final String playerId,
            final String questName,
            final int objectiveId,
            final double progress,
            final boolean completed) {
        return restoreObjectiveProgress(
                playerId,
                activeProfile(playerId),
                questName,
                objectiveId,
                progress,
                completed);
    }

    public boolean restoreObjectiveProgress(
            final String playerId,
            final String profile,
            final String questName,
            final int objectiveId,
            final double progress,
            final boolean completed) {
        final ActiveObjective activeObjective = activeObjective(playerId, profile, questName, objectiveId);
        if (activeObjective == null) {
            return false;
        }
        activeObjective.restoreProgress(progress, completed);
        if (completed) {
            removeActiveObjective(activeObjective, false);
        }
        return true;
    }

    public boolean restoreObjectiveProgress(
            final String playerId,
            final String questName,
            final int[] objectivePath,
            final double progress,
            final boolean completed) {
        return restoreObjectiveProgress(
                playerId,
                activeProfile(playerId),
                questName,
                objectivePath,
                progress,
                completed);
    }

    public boolean restoreObjectiveProgress(
            final String playerId,
            final String profile,
            final String questName,
            final int[] objectivePath,
            final double progress,
            final boolean completed) {
        return restoreObjectiveProgress(
                playerId,
                profile,
                questName,
                objectivePath,
                progress,
                completed,
                false);
    }

    public boolean restoreObjectiveProgress(
            final String playerId,
            final String profile,
            final String questName,
            final int[] objectivePath,
            final double progress,
            final boolean completed,
            final boolean loading) {
        final ActiveObjective activeObjective = activeObjective(playerId, profile, questName, objectivePath);
        if (activeObjective == null) {
            return false;
        }
        activeObjective.restoreProgress(progress, completed);
        if (completed) {
            removeActiveObjective(activeObjective, loading);
        }
        return true;
    }

    public boolean hasActiveObjectives(final String playerId, final String questName) {
        final QuestPlayer player = player(activePlayerProfile(playerId));
        final ActiveQuest activeQuest = player == null ? null : player.getActiveQuest(questName);
        return activeQuest != null && !activeQuest.getActiveObjectives().isEmpty();
    }

    public List<String> activeTriggerCommandNames() {
        return players().stream()
                .flatMap(player -> player.getActiveObjectives().stream())
                .filter(objective -> objective.getType().id().equalsIgnoreCase("TriggerCommand"))
                .map(objective -> objective.text("triggerName"))
                .filter(triggerName -> !triggerName.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public void onPlayerHarvestBlock(
            final PlatformPlayer questPlayer,
            final Objectives.HarvestBlockEvent event) {
        dispatch(questPlayer, Objectives.Type::harvestBlockHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onBlockBroken(
            final PlatformPlayer questPlayer,
            final String blockKey,
            final String materialId,
            final boolean fullyGrownHarvestable,
            final boolean finalEvent) {
        final MaterialBlock event = new MaterialBlock(materialId);
        onPlayerBreakBlock(questPlayer, event);
        onPlayerHarvestBlock(
                questPlayer,
                new HarvestedBlock(event.materialId(), fullyGrownHarvestable, playerPlacedHarvestBlocks.contains(blockKey)));
        if (finalEvent) {
            blockBreakFinished(blockKey, false);
        }
    }

    public void blockBreakFinished(final String blockKey, final boolean brewingStand) {
        playerPlacedHarvestBlocks.onBlockBroken(blockKey);
        if (brewingStand && blockKey != null) {
            freshlyBrewedItems.remove(blockKey);
        }
    }

    public boolean isPlayerPlacedHarvestBlock(final String blockKey) {
        return playerPlacedHarvestBlocks.contains(blockKey);
    }

    public void onPlayerBreakBlock(
            final PlatformPlayer questPlayer,
            final Objectives.BlockEvent event) {
        dispatch(questPlayer, Objectives.Type::breakBlockHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerPlaceBlock(
            final PlatformPlayer questPlayer,
            final Objectives.BlockEvent event) {
        dispatch(questPlayer, Objectives.Type::placeBlockHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onBlockPlaced(
            final PlatformPlayer questPlayer,
            final String blockKey,
            final String materialId,
            final boolean ageableAtMaxAge) {
        playerPlacedHarvestBlocks.onBlockPlaced(blockKey, materialId, ageableAtMaxAge);
        if (questPlayer != null) {
            onPlayerPlaceBlock(questPlayer, new MaterialBlock(materialId));
        }
    }

    public void onPlayerPickupItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::pickupItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerDropItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::dropItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerTakeBrewedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::takeBrewedItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void brewingFinished(final String brewingStandKey, final List<BrewedItem> items) {
        if (brewingStandKey == null || brewingStandKey.isBlank()) {
            return;
        }
        final List<BrewedItem> brewed = items == null
                ? List.of()
                : items.stream().filter(item -> item != null && item.amount() > 0).toList();
        if (brewed.isEmpty()) {
            freshlyBrewedItems.remove(brewingStandKey);
        } else {
            freshlyBrewedItems.put(brewingStandKey, new ArrayList<>(brewed));
        }
    }

    public void onPlayerTakeInventoryItem(
            final PlatformPlayer questPlayer,
            final TakenItem takenItem,
            final String brewingStandKey,
            final String itemKey,
            final Objectives.ItemEvent event) {
        if (questPlayer == null || event == null || takenItem == null) {
            return;
        }
        switch (takenItem) {
            case BREWED -> {
                final int amount = consumeFreshlyBrewed(
                        brewingStandKey,
                        itemKey,
                        event.amount());
                if (amount > 0) {
                    onPlayerTakeBrewedItem(questPlayer, new ItemAmount(event, amount));
                }
            }
            case SMELTED -> onPlayerTakeSmeltedItem(questPlayer, event);
            case TRADED -> onPlayerTradeItem(questPlayer, event);
        }
    }

    private int consumeFreshlyBrewed(
            final String brewingStandKey,
            final String itemKey,
            final int requestedAmount) {
        final List<BrewedItem> brewedItems = freshlyBrewedItems.get(brewingStandKey);
        if (brewedItems == null || brewedItems.isEmpty() || itemKey == null || itemKey.isBlank()) {
            return 0;
        }
        int remaining = Math.max(requestedAmount, 0);
        int consumed = 0;
        final ArrayList<BrewedItem> remainingItems = new ArrayList<>();
        for (final BrewedItem brewedItem : brewedItems) {
            if (remaining <= 0 || !brewedItem.itemKey().equals(itemKey)) {
                remainingItems.add(brewedItem);
                continue;
            }
            final int amount = Math.min(remaining, brewedItem.amount());
            remaining -= amount;
            consumed += amount;
            if (brewedItem.amount() > amount) {
                remainingItems.add(new BrewedItem(brewedItem.itemKey(), brewedItem.amount() - amount));
            }
        }
        if (remainingItems.isEmpty()) {
            freshlyBrewedItems.remove(brewingStandKey);
        } else {
            freshlyBrewedItems.put(brewingStandKey, remainingItems);
        }
        return consumed;
    }

    public void onPlayerConsumeItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::consumeItemHandler, (handler, objective) ->
                handler.handle(new ItemAmount(event, 1), objective));
    }

    public void onPlayerFishItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::fishItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerCraftItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::craftItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerTakeSmeltedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::takeSmeltedItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerTakeSmithingResult(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::takeSmithingResultHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerTradeItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        dispatch(questPlayer, Objectives.Type::tradeItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerShearSheep(
            final PlatformPlayer questPlayer,
            final Objectives.ShearSheepEvent event) {
        dispatch(questPlayer, Objectives.Type::shearSheepHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerMilkCow(
            final PlatformPlayer questPlayer,
            final Objectives.MilkCowEvent event) {
        dispatch(questPlayer, Objectives.Type::milkCowHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerOpenBuriedTreasure(final PlatformPlayer questPlayer) {
        dispatch(questPlayer, Objectives.Type::openBuriedTreasureHandler, (handler, objective) ->
                handler.handle(objective));
    }

    public void onPlayerKillEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        dispatch(questPlayer, Objectives.Type::killEntityHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerBreedEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        dispatch(questPlayer, Objectives.Type::breedEntityHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerFeedEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        dispatch(questPlayer, Objectives.Type::feedEntityHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerTameEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        dispatch(questPlayer, Objectives.Type::tameEntityHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerInteractBlock(
            final PlatformPlayer questPlayer,
            final Objectives.InteractionEvent event) {
        dispatch(questPlayer, Objectives.Type::interactBlockHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerMove(
            final PlatformPlayer questPlayer,
            final Objectives.MoveEvent event) {
        dispatch(questPlayer, Objectives.Type::moveHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerShootProjectileHit(
            final PlatformPlayer questPlayer,
            final Objectives.ProjectileHitEvent event) {
        dispatch(questPlayer, Objectives.Type::projectileHitHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerJump(final PlatformPlayer questPlayer) {
        dispatch(questPlayer, Objectives.Type::jumpHandler, (handler, objective) ->
                handler.handle(objective));
    }

    public void onPlayerStartSneak(final PlatformPlayer questPlayer) {
        dispatch(questPlayer, Objectives.Type::startSneakHandler, (handler, objective) ->
                handler.handle(objective));
    }

    public void onPlayerDeath(
            final PlatformPlayer questPlayer,
            final Objectives.DeathEvent event) {
        dispatch(questPlayer, Objectives.Type::deathHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerEnchantItem(
            final PlatformPlayer questPlayer,
            final Objectives.EnchantEvent event) {
        dispatch(questPlayer, Objectives.Type::enchantItemHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public void onPlayerRunCommand(
            final PlatformPlayer questPlayer,
            final Objectives.CommandEvent event) {
        dispatch(questPlayer, Objectives.Type::runCommandHandler, (handler, objective) ->
                handler.handle(event, objective));
    }

    public boolean onPlayerInteractNpc(
            final PlatformPlayer questPlayer,
            final Objectives.NpcInteractionEvent event) {
        final String playerId = playerId(questPlayer);
        final PlayerProfile playerProfile = activePlayerProfile(playerId);
        final List<ActiveObjective> objectives = activeObjectives(playerProfile);
        if (objectives.isEmpty()) {
            return false;
        }
        updateUnlocked(playerProfile, false);
        boolean handled = false;
        for (final ActiveObjective objective : List.copyOf(objectives)) {
            if (!objective.isUnlocked() || objective.hasBeenCompleted()) {
                continue;
            }
            final double previousProgress = objective.currentProgress();
            final boolean wasComplete = objective.hasBeenCompleted();
            final Objectives.NpcInteractionObjectiveEventHandler handler =
                    objective.getType().npcInteractionHandler();
            if (handler != null) {
                handler.handle(event, objective);
            }
            final boolean completedByNpc = objective.completeWithNpc(event.npcSelector());
            handled = handled
                    || previousProgress != objective.currentProgress()
                    || (!wasComplete && (objective.hasBeenCompleted() || completedByNpc));
        }
        return handled;
    }

    public void refreshObjectives(
            final PlatformPlayer questPlayer,
            final Objectives.ObjectiveRefresh refresh) {
        dispatch(questPlayer, Objectives.Type::refreshHandler, (handler, objective) ->
                handler.handle(refresh == null ? Objectives.ObjectiveRefresh.periodic() : refresh, objective));
    }

    public void secondPassed(
            final List<PlatformPlayer> questPlayers,
            final int unlockConditionCheckIntervalSeconds) {
        conditionRefreshSeconds++;
        final boolean refreshConditions = conditionRefreshSeconds >= 2;
        if (refreshConditions) {
            conditionRefreshSeconds = 0;
        }

        final boolean refreshUnlocks;
        if (unlockConditionCheckIntervalSeconds > 0) {
            unlockRefreshSeconds++;
            refreshUnlocks = unlockRefreshSeconds >= unlockConditionCheckIntervalSeconds;
            if (refreshUnlocks) {
                unlockRefreshSeconds = 0;
            }
        } else {
            unlockRefreshSeconds = 0;
            refreshUnlocks = false;
        }

        for (final PlatformPlayer questPlayer : questPlayers == null
                ? List.<PlatformPlayer>of()
                : questPlayers) {
            if (refreshConditions) {
                refreshObjectives(questPlayer, Objectives.ObjectiveRefresh.periodic());
            }
            if (refreshUnlocks) {
                refreshObjectiveUnlocks(questPlayer);
            }
        }
    }

    public boolean objectiveMarkersDue() {
        markerRefreshSeconds++;
        if (markerRefreshSeconds < 4) {
            return false;
        }
        markerRefreshSeconds = 0;
        return true;
    }

    public void playerTick(
            final PlatformPlayer questPlayer,
            final NQLocation location,
            final boolean sneaking) {
        final String playerId = playerId(questPlayer);
        if (playerId.isBlank()) {
            return;
        }
        if (location != null) {
            final PlayerPosition current = PlayerPosition.at(location);
            final PlayerPosition previous = playerPositions.put(playerId, current);
            if (!current.equals(previous)) {
                onPlayerMove(questPlayer, () -> location);
            }
        }
        final boolean wasSneaking = playerSneaking.getOrDefault(playerId, false);
        playerSneaking.put(playerId, sneaking);
        if (sneaking && !wasSneaking) {
            onPlayerStartSneak(questPlayer);
        }
    }

    private <H> void dispatch(
            final PlatformPlayer questPlayer,
            final Function<Objectives.Type, H> handlerGetter,
            final BiConsumer<H, ActiveObjective> dispatcher) {
        final String playerId = playerId(questPlayer);
        final PlayerProfile playerProfile = activePlayerProfile(playerId);
        final List<ActiveObjective> objectives = activeObjectives(playerProfile);
        if (objectives.isEmpty()) {
            return;
        }
        updateUnlocked(playerProfile, false);
        for (final ActiveObjective objective : List.copyOf(objectives)) {
            if (!objective.isUnlocked() || objective.isComplete()) {
                continue;
            }
            final H handler = handlerGetter.apply(objective.getType());
            if (handler != null) {
                dispatcher.accept(handler, objective);
            }
        }
    }

    private List<ActiveObjective> activeObjectives(final PlayerProfile playerProfile) {
        final QuestPlayer player = player(playerProfile);
        return player == null ? List.of() : player.getActiveObjectives();
    }

    private static double progressNeeded(
            final Objectives.Type objectiveType,
            final Objectives.Data objective) {
        for (final RegistryField.Definition field : objectiveType.fields()) {
            if (!field.progressNeeded()) {
                continue;
            }
            final Object value = objective.value(field.name());
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.parseDouble(value.toString());
                } catch (final NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private void checkParentAfterChildRemoved(
            final ActiveObjective parent,
            final boolean loading) {
        if (parent == null) {
            return;
        }
        final boolean becameUnlocked = parent.updateUnlocked();
        if (becameUnlocked && !unlockHandler.test(parent)) {
            parent.lock();
            return;
        }
        if (parent.isUnlocked()) {
            runUnlockHandler(parent.getType(), parent, loading);
        }
        if (!loading) {
            parent.checkCompletion();
        }
    }

    private static void runUnlockHandler(
            final Objectives.Type objectiveType,
            final ActiveObjective progress,
            final boolean loading) {
        if (objectiveType.unlockHandler() != null && progress.isUnlocked()) {
            objectiveType.unlockHandler().handle(progress, progress.getQuestPlayer(), loading);
        }
    }

    private void updateUnlocked(final PlayerProfile playerProfile, final boolean loading) {
        final List<ActiveObjective> objectives = activeObjectives(playerProfile);
        if (objectives.isEmpty()) {
            return;
        }
        final List<ActiveObjective> snapshot = List.copyOf(objectives);
        for (final ActiveObjective progress : snapshot) {
            final boolean becameUnlocked = progress.updateUnlocked();
            if (progress.isUnlocked() && blockedByProgressOrder(progress, snapshot)) {
                progress.lock();
                continue;
            }
            if (becameUnlocked) {
                if (!unlockHandler.test(progress)) {
                    progress.lock();
                    continue;
                }
                runUnlockHandler(progress.getType(), progress, loading);
            }
            if (!loading) {
                progress.checkCompletion();
            }
        }
    }

    private static boolean blockedByProgressOrder(
            final ActiveObjective progress,
            final List<ActiveObjective> objectives) {
        final String order = progress.ownerProgressOrder();
        if (order == null || order.isBlank()) {
            return false;
        }
        if (order.equalsIgnoreCase("firstToLast")) {
            return objectives.stream()
                    .anyMatch(other -> sameObjectiveHolder(other, progress)
                            && other.getObjectiveID() < progress.getObjectiveID()
                            && !other.hasBeenCompleted());
        }
        if (order.equalsIgnoreCase("lastToFirst")) {
            return objectives.stream()
                    .anyMatch(other -> sameObjectiveHolder(other, progress)
                            && other.getObjectiveID() > progress.getObjectiveID()
                            && !other.hasBeenCompleted());
        }
        final List<Integer> customOrder = customOrder(order);
        if (customOrder.isEmpty()) {
            return false;
        }
        for (final int id : customOrder) {
            if (id == progress.getObjectiveID()) {
                return false;
            }
            final boolean earlierStillActive = objectives.stream()
                    .anyMatch(other -> sameObjectiveHolder(other, progress)
                            && other.getObjectiveID() == id
                            && !other.hasBeenCompleted());
            if (earlierStillActive) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameObjectiveHolder(
            final ActiveObjective first,
            final ActiveObjective second) {
        return first.getQuestIdentifier().equalsIgnoreCase(second.getQuestIdentifier())
                && first.getHolderPath().equalsIgnoreCase(second.getHolderPath());
    }

    private static List<Integer> customOrder(final String order) {
        final ArrayList<Integer> ids = new ArrayList<>();
        for (final String token : order.replace(",", " ").trim().split("\\s+")) {
            try {
                ids.add(Integer.parseInt(token));
            } catch (final NumberFormatException ignored) {
            }
        }
        return List.copyOf(ids);
    }

    private static String playerId(final PlatformPlayer questPlayer) {
        return questPlayer == null ? "" : questPlayer.playerIdentifier();
    }

    private String activeProfile(final String playerId) {
        final String profile = activeProfileLookup.apply(playerId == null ? "" : playerId);
        return profile == null || profile.isBlank() ? "default" : profile;
    }

    private PlayerProfile activePlayerProfile(final String playerId) {
        return playerProfile(playerId, activeProfile(playerId));
    }

    private QuestPlayer player(final PlayerProfile playerProfile) {
        return playerLookup.apply(playerProfile.playerId(), playerProfile.profile());
    }

    private List<QuestPlayer> players() {
        final List<QuestPlayer> players = allPlayers.get();
        return players == null ? List.of() : players;
    }

    private static PlayerProfile playerProfile(final String playerId, final String profile) {
        return new PlayerProfile(
                playerId == null ? "" : playerId,
                profile == null || profile.isBlank() ? "default" : profile.toLowerCase(Locale.ROOT));
    }

    private record PlayerProfile(String playerId, String profile) {
    }

    private record MaterialBlock(String materialId) implements Objectives.BlockEvent {
        private MaterialBlock {
            materialId = materialId == null ? "" : materialId.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record HarvestedBlock(String materialId, boolean fullyGrownHarvestable, boolean playerPlaced)
            implements Objectives.HarvestBlockEvent {
        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record ItemAmount(Objectives.ItemEvent item, int amount) implements Objectives.ItemEvent {
        @Override
        public String materialId() {
            return item.materialId();
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return item.matches(selection);
        }
    }

    private record PlayerPosition(String world, int x, int y, int z) {
        private static PlayerPosition at(final NQLocation location) {
            return new PlayerPosition(
                    location.worldName() == null ? "" : location.worldName(),
                    (int) Math.floor(location.x()),
                    (int) Math.floor(location.y()),
                    (int) Math.floor(location.z()));
        }
    }
}
