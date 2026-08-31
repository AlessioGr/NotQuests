package com.notquests.core.platform;

import com.notquests.core.NotQuestsPlugin.ObjectiveCompass;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.SavedItems.ItemChoice;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The complete set of live-player capabilities required by core.
 *
 * <p>Every platform adapter implements this contract explicitly. There are deliberately no fallback
 * implementations: adding a capability to core must produce a compile error in every adapter until
 * that platform defines its real behavior.
 */
public interface PlatformPlayer {
    boolean hasPlayer();

    String playerName();

    String displayName();

    String playerIdentifier();

    boolean setDisplayName(String displayName);

    boolean isFlying();

    boolean setFlying(boolean flying);

    boolean isSneaking();

    boolean setSneaking(boolean sneaking);

    boolean isSprinting();

    boolean setSprinting(boolean sprinting);

    boolean isSwimming();

    boolean setSwimming(boolean swimming);

    double health();

    double maxHealth();

    boolean setMaxHealth(double maxHealth);

    boolean setHealth(double health);

    int foodLevel();

    boolean setFoodLevel(int foodLevel);

    double saturation();

    boolean setSaturation(double saturation);

    int experienceLevel();

    boolean setExperienceLevel(int level);

    int experiencePoints();

    boolean setExperiencePoints(int points);

    int pingMillis();

    double walkSpeed();

    boolean setWalkSpeed(double speed);

    double flySpeed();

    boolean setFlySpeed(double speed);

    boolean isGlowing();

    boolean setGlowing(boolean glowing);

    boolean isOperator();

    boolean setOperator(boolean operator);

    boolean isSleeping();

    boolean isClimbing();

    boolean isInLava();

    boolean isInWater();

    String gameMode();

    boolean setGameMode(String gameMode);

    List<String> availableGameModes();

    int playtimeTicks();

    boolean setPlaytimeTicks(int ticks);

    String worldName();

    String worldIdentifier();

    boolean teleportToWorldSpawn(String worldName);

    boolean teleport(NQLocation location, Double yaw, Double pitch);

    List<String> availableWorldNames();

    double positionX();

    boolean setPositionX(double x);

    double positionY();

    boolean setPositionY(double y);

    double positionZ();

    boolean setPositionZ(double z);

    double yawDegrees();

    double pitchDegrees();

    String biomeName();

    List<String> availableBiomeNames();

    String weather();

    boolean setWeather(String weather);

    double distanceTo(NQLocation location);

    List<String> nearbyEntityTypeIds(double radius);

    long currentWorldTimeTicks();

    void sendMessage(String miniMessage);

    void sendCommandChoice(
            String prefixMiniMessage,
            String choiceMiniMessage,
            String command,
            String hoverMiniMessage);

    void sendActionBar(String miniMessage);

    boolean supportsExternalPlaceholders();

    String applyExternalPlaceholders(String text);

    void showProgressBossBar(String miniMessage, double progress);

    void hideProgressBossBar();

    void hideLocationCompass();

    /** Reconciles the native marker rendering with the complete core-owned marker set. */
    boolean renderObjectiveMarkers(
            Map<String, NQLocation> markers,
            boolean useBeaconBlocks,
            boolean force);

    /** Renders a compass display already resolved by core. */
    void showLocationCompass(ObjectiveCompass.Display display);

    void showTitle(
            String title,
            String subtitle,
            Duration fadeIn,
            Duration stay,
            Duration fadeOut);

    void chat(String message);

    void performCommand(String command);

    void closeInventory();

    boolean giveItems(List<ItemChoice> items);

    int removeItems(List<ItemChoice> items, int maxAmount);

    List<ItemSelection> inventoryItems();

    boolean addInventoryItems(
            List<ItemChoice> items,
            boolean dropOverflow);

    boolean removeInventoryItems(List<ItemChoice> items);

    boolean setInventoryItems(List<ItemChoice> items);

    List<ItemSelection> enderChestItems();

    boolean addEnderChestItems(
            List<ItemChoice> items,
            boolean addOverflowToInventory,
            boolean dropOverflow);

    boolean removeEnderChestItems(List<ItemChoice> items);

    boolean setEnderChestItems(List<ItemChoice> items);

    boolean beforeQuestAccepted(
            Quest quest,
            boolean triggerAcceptQuestTrigger);

    boolean beforeQuestPointsChanged(long newQuestPoints);

    boolean beforeQuestCompleted(Quest quest, boolean forced);

    boolean beforeQuestFailed(Quest quest);

    boolean beforeObjectiveCompleted(
            Quest quest,
            ActiveObjective objective);

    boolean spawnParticle(
            String particleId,
            int count,
            boolean showToEveryone,
            NQLocation location,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed);

    void stopSounds();

    boolean playSound(
            String soundId,
            SoundAudience audience,
            NQLocation location,
            double volume,
            double pitch,
            String soundCategory);

    boolean spawnVanillaMob(String entityType, NQLocation location);

    NQLocation lookingAtBlock(double maxDistance);

    boolean showGui(ResolvedGui gui);

    enum SoundAudience {
        PLAYER,
        WORLD,
        EVERYONE_AT_OWN_LOCATION
    }
}
