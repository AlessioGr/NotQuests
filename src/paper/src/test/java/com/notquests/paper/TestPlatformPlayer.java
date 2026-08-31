package com.notquests.paper;

import com.notquests.core.gui.GuiContext;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;

import java.time.Duration;
import java.util.List;

public interface TestPlatformPlayer extends PlatformPlayer {
    boolean hasPlayer();

    default String playerName() {
        return "";
    }

    default String displayName() {
        return playerName();
    }

    String playerIdentifier();

    default boolean setDisplayName(final String displayName) {
        return false;
    }

    default boolean isFlying() {
        return false;
    }

    default boolean setFlying(final boolean flying) {
        return false;
    }

    default boolean isSneaking() {
        return false;
    }

    default boolean setSneaking(final boolean sneaking) {
        return false;
    }

    default boolean isSprinting() {
        return false;
    }

    default boolean setSprinting(final boolean sprinting) {
        return false;
    }

    default boolean isSwimming() {
        return false;
    }

    default boolean setSwimming(final boolean swimming) {
        return false;
    }

    default double health() {
        return 0;
    }

    default double maxHealth() {
        return 20;
    }

    default boolean setMaxHealth(final double maxHealth) {
        return false;
    }

    default boolean setHealth(final double health) {
        return false;
    }

    default int foodLevel() {
        return 0;
    }

    default boolean setFoodLevel(final int foodLevel) {
        return false;
    }

    default double saturation() {
        return 0;
    }

    default boolean setSaturation(final double saturation) {
        return false;
    }

    default int experienceLevel() {
        return 0;
    }

    default boolean setExperienceLevel(final int level) {
        return false;
    }

    default int experiencePoints() {
        return 0;
    }

    default boolean setExperiencePoints(final int points) {
        return false;
    }

    default int pingMillis() {
        return 0;
    }

    default double walkSpeed() {
        return 0;
    }

    default boolean setWalkSpeed(final double speed) {
        return false;
    }

    default double flySpeed() {
        return 0;
    }

    default boolean setFlySpeed(final double speed) {
        return false;
    }

    default boolean isGlowing() {
        return false;
    }

    default boolean setGlowing(final boolean glowing) {
        return false;
    }

    default boolean isOperator() {
        return false;
    }

    default boolean setOperator(final boolean operator) {
        return false;
    }

    default boolean isSleeping() {
        return false;
    }

    default boolean isClimbing() {
        return false;
    }

    default boolean isInLava() {
        return false;
    }

    default boolean isInWater() {
        return false;
    }

    default String gameMode() {
        return "";
    }

    default boolean setGameMode(final String gameMode) {
        return false;
    }

    default List<String> availableGameModes() {
        return List.of();
    }

    default int playtimeTicks() {
        return 0;
    }

    default boolean setPlaytimeTicks(final int ticks) {
        return false;
    }

    default String worldName() {
        return "";
    }

    default String worldIdentifier() {
        return worldName();
    }

    default boolean teleportToWorldSpawn(final String worldName) {
        return false;
    }

    default boolean teleport(final NQLocation location, final Double yaw, final Double pitch) {
        return false;
    }

    default List<String> availableWorldNames() {
        return List.of();
    }

    default double positionX() {
        return 0;
    }

    default boolean setPositionX(final double x) {
        return false;
    }

    default double positionY() {
        return 0;
    }

    default boolean setPositionY(final double y) {
        return false;
    }

    default double positionZ() {
        return 0;
    }

    default boolean setPositionZ(final double z) {
        return false;
    }

    default double yawDegrees() {
        return 0;
    }

    default double pitchDegrees() {
        return 0;
    }

    default String biomeName() {
        return "";
    }

    default List<String> availableBiomeNames() {
        return List.of();
    }

    default String weather() {
        return "";
    }

    default boolean setWeather(final String weather) {
        return false;
    }

    default double distanceTo(final NQLocation location) {
        return Double.MAX_VALUE;
    }

    default List<String> nearbyEntityTypeIds(final double radius) {
        return List.of();
    }

    long currentWorldTimeTicks();

    void sendMessage(String miniMessage);

    default void sendCommandChoice(
            final String prefixMiniMessage,
            final String choiceMiniMessage,
            final String command,
            final String hoverMiniMessage) {
        sendMessage((prefixMiniMessage == null ? "" : prefixMiniMessage)
                + (choiceMiniMessage == null ? "" : choiceMiniMessage));
    }

    void sendActionBar(String miniMessage);

    default String applyExternalPlaceholders(final String text) {
        return text == null ? "" : text;
    }

    default boolean supportsExternalPlaceholders() {
        return false;
    }

    void showProgressBossBar(String miniMessage, double progress);

    void hideProgressBossBar();

    @Override
    default void hideLocationCompass() {}

    @Override
    default boolean renderObjectiveMarkers(
            final java.util.Map<String, NQLocation> markers,
            final boolean useBeaconBlocks,
            final boolean force) {
        return true;
    }

    @Override
    default void showLocationCompass(
            final com.notquests.core.NotQuestsPlugin.ObjectiveCompass.Display display) {}

    void showTitle(
            String title,
            String subtitle,
            Duration fadeIn,
            Duration stay,
            Duration fadeOut);

    void chat(String message);

    void performCommand(String command);

    void closeInventory();

    default boolean giveItems(final List<SavedItems.ItemChoice> items) {
        return false;
    }

    default int removeItems(final List<SavedItems.ItemChoice> items, final int maxAmount) {
        return 0;
    }

    default List<ItemSelection> inventoryItems() {
        return List.of();
    }

    default boolean addInventoryItems(
            final List<SavedItems.ItemChoice> items,
            final boolean dropOverflow) {
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            changed = giveItems(List.of(item)) || changed;
        }
        return changed;
    }

    default boolean removeInventoryItems(final List<SavedItems.ItemChoice> items) {
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            changed = removeItems(List.of(item), item.selection().amount()) > 0 || changed;
        }
        return changed;
    }

    default boolean setInventoryItems(final List<SavedItems.ItemChoice> items) {
        return false;
    }

    default List<ItemSelection> enderChestItems() {
        return List.of();
    }

    default boolean addEnderChestItems(
            final List<SavedItems.ItemChoice> items,
            final boolean addOverflowToInventory,
            final boolean dropOverflow) {
        return false;
    }

    default boolean removeEnderChestItems(final List<SavedItems.ItemChoice> items) {
        return false;
    }

    default boolean setEnderChestItems(final List<SavedItems.ItemChoice> items) {
        return false;
    }

    default boolean beforeQuestAccepted(
            final Quest quest,
            final boolean triggerAcceptQuestTrigger) {
        return true;
    }

    default boolean beforeQuestPointsChanged(final long newQuestPoints) {
        return true;
    }

    default boolean beforeQuestCompleted(
            final Quest quest,
            final boolean forced) {
        return true;
    }

    default boolean beforeQuestFailed(final Quest quest) {
        return true;
    }

    default boolean beforeObjectiveCompleted(
            final Quest quest,
            final ActiveObjective objective) {
        return true;
    }

    default boolean spawnParticle(
            final String particleId,
            final int count,
            final boolean showToEveryone,
            final NQLocation location,
            final double offsetX,
            final double offsetY,
            final double offsetZ,
            final double speed) {
        return false;
    }

    default void stopSounds() {}

    default boolean playSound(
            final String soundId,
            final SoundAudience audience,
            final NQLocation location,
            final double volume,
            final double pitch,
            final String soundCategory) {
        return false;
    }

    default boolean spawnVanillaMob(
            final String entityType,
            final NQLocation location) {
        return false;
    }

    NQLocation lookingAtBlock(double maxDistance);

    default boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
}
