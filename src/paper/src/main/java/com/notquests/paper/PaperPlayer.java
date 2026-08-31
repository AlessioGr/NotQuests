package com.notquests.paper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.notquests.core.NotQuestsPlugin.ObjectiveCompass;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.core.structs.Quest;
import com.notquests.paper.PaperItems.Selection;
import com.notquests.paper.events.notquests.ObjectiveCompleteEvent;
import com.notquests.paper.events.notquests.QuestCompletedEvent;
import com.notquests.paper.events.notquests.QuestFailEvent;
import com.notquests.paper.events.notquests.QuestFinishAcceptEvent;
import com.notquests.paper.events.notquests.QuestPointsChangeEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Bukkit implementation of the core quest-player API. Persistent quest state is owned by core;
 * this class only exposes the live Bukkit player and renders platform effects.
 *
 * @author Alessio Gravili
 */
public class PaperPlayer implements PlatformPlayer {

    private final NotQuests main;

    private final UUID uuid;

    private final HashMap<String, Location> activeLocationAndBeams;
    private BossBar bossBar;
    private BossBar locationCompassBossBar;
    private Player player;

    public PaperPlayer(final NotQuests main, final UUID uuid) {
        this.main = main;
        this.uuid = uuid;

        activeLocationAndBeams = new HashMap<>();
    }

    private void clearActiveBeacons(final Player player, final boolean useBeaconBlocks) {
        if (player != null) {
            for(Location location : activeLocationAndBeams.values()){
                scheduleBeaconRemovalAt(location, player, useBeaconBlocks);
            }
        }

        activeLocationAndBeams.clear();
    }

    private void scheduleBeaconRemovalAt(
            final Location location,
            final Player player,
            final boolean useBeaconBlocks) {
        if (player == null || location == null || location.getWorld() == null) {
            return;
        }

        if (useBeaconBlocks) {
            sendRealBlock(player, location);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    sendRealBlock(player, location.clone().add(x, -1, z));
                }
            }
            return;
        }

        sendRealBlock(player, location);
    }

    @Override
    public void showLocationCompass(final ObjectiveCompass.Display display) {
        final Player player = getPlayer();
        if (player != null && display != null) {
            showLocationCompass(player, display);
        }
    }

    public void hideLocationCompass(final Player player) {
        if (locationCompassBossBar == null) {
            return;
        }
        if (player != null) {
            player.hideBossBar(locationCompassBossBar);
        }
        locationCompassBossBar = null;
    }

    @Override
    public boolean renderObjectiveMarkers(
            final Map<String, NQLocation> markers,
            final boolean useBeaconBlocks,
            final boolean force) {
        final Player player = getPlayer();
        if(player == null){
            activeLocationAndBeams.clear();
            return false;
        }
        if(markers == null || markers.isEmpty()){
            clearActiveBeacons(player, useBeaconBlocks);
            hideLocationCompass(player);
            return true;
        }
        final Location playerLocation = player.getLocation();
        final Set<String> renderedLocations = new HashSet<>();
        for(final Map.Entry<String, NQLocation> marker : markers.entrySet()){
            final String locationName = marker.getKey();
            final Location finalLocation = PaperNotQuestsAdapter.paperBukkitLocation(marker.getValue());

            if (finalLocation == null || finalLocation.getWorld() == null) {
                continue;
            }
            final boolean sameWorld = finalLocation.getWorld().getUID().equals(player.getWorld().getUID());
            final ActiveObjectives.BlockBeam beam = ActiveObjectives.blockBeam(
                    point(playerLocation),
                    point(finalLocation),
                    sameWorld,
                    useBeaconBlocks,
                    column -> player.getWorld().getHighestBlockYAt(column.x(), column.z()),
                    block -> player.getWorld().getBlockAt(block.x(), block.y(), block.z()).getType().isAir(),
                    player.getWorld().getMinHeight());
            if (beam == null) {
                continue;
            }
            final Location beamRenderLocation = new Location(
                    player.getWorld(), beam.block().x(), beam.block().y(), beam.block().z());
            final Location activeLocation = activeLocationAndBeams.get(locationName);
            if (!force && sameBlockLocation(activeLocation, beamRenderLocation)) {
                scheduleBeaconRemovalAt(beamRenderLocation, player, useBeaconBlocks);
                sendBeamMarker(player, beamRenderLocation, useBeaconBlocks);
                renderedLocations.add(locationName);
                continue;
            }
            if (activeLocation != null) {
                scheduleBeaconRemovalAt(activeLocation, player, useBeaconBlocks);
            }

            sendBeamMarker(player, beamRenderLocation, useBeaconBlocks);
            activeLocationAndBeams.put(locationName, beamRenderLocation.clone());

            renderedLocations.add(locationName);

        }

        final Iterator<Map.Entry<String, Location>> activeIterator = activeLocationAndBeams.entrySet().iterator();
        while (activeIterator.hasNext()) {
            final Map.Entry<String, Location> activeEntry = activeIterator.next();
            if (!renderedLocations.contains(activeEntry.getKey())) {
                scheduleBeaconRemovalAt(activeEntry.getValue(), player, useBeaconBlocks);
                activeIterator.remove();
            }
        }

        return true;
    }

    static boolean sameBlockLocation(final Location first, final Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().getUID().equals(second.getWorld().getUID())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private static ActiveObjectives.BeamPoint point(final Location location) {
        return new ActiveObjectives.BeamPoint(location.getX(), location.getY(), location.getZ());
    }

    private void sendBeamMarker(
            final Player player,
            final Location beamRenderLocation,
            final boolean useBeaconBlocks) {
        if (useBeaconBlocks) {
            BlockState beaconBlockState = beamRenderLocation.getBlock().getState();
            beaconBlockState.setType(Material.BEACON);

            BlockState ironBlockState = beamRenderLocation.getBlock().getState();
            ironBlockState.setType(Material.IRON_BLOCK);

            player.sendBlockChange(beamRenderLocation, beaconBlockState.getBlockData());
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    player.sendBlockChange(beamRenderLocation.clone().add(x, -1, z), ironBlockState.getBlockData());
                }
            }
            return;
        }

        BlockState beaconBlockState = beamRenderLocation.getBlock().getState();
        beaconBlockState.setType(Material.END_GATEWAY);
        player.sendBlockChange(beamRenderLocation, beaconBlockState.getBlockData());
    }

    static BossBar.Color compassColor(final ObjectiveCompass.Severity severity) {
        return switch (severity) {
            case GREEN -> BossBar.Color.GREEN;
            case YELLOW -> BossBar.Color.YELLOW;
            case RED -> BossBar.Color.RED;
        };
    }

    private void showLocationCompass(
            final Player player,
            final Component title,
            final float progress,
            final BossBar.Color color) {
        if (locationCompassBossBar == null) {
            locationCompassBossBar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
            player.showBossBar(locationCompassBossBar);
            return;
        }
        locationCompassBossBar.name(title);
        locationCompassBossBar.progress(progress);
        locationCompassBossBar.color(color);
        player.showBossBar(locationCompassBossBar);
    }

    private void showLocationCompass(
            final Player player,
            final ObjectiveCompass.Display display) {
        showLocationCompass(
                player,
                main.parse(display.title()),
                display.progress(),
                compassColor(display.severity()));
    }

    private static void sendRealBlock(final Player player, final Location location) {
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    public final UUID getUniqueId() {
        return uuid;
    }

    @Override
    public void sendMessage(final String message) {
        final Player player = getPlayer();
        if (player != null) {
            player.sendMessage(main.parse(message));
        }
    }

    public final Player getPlayer(){
        if(player != null){
            return player;
        }else{
            this.player = Bukkit.getPlayer(uuid);
            return player;
        }
    }

    public final BossBar getBossBar() {
        return bossBar;
    }

    public void showProgressBossBar(final Component title, final float progress) {
        final Player player = getPlayer();
        if (player == null) {
            return;
        }
        final float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        if (bossBar == null) {
            bossBar = BossBar.bossBar(title, clampedProgress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            player.showBossBar(bossBar);
        } else {
            bossBar.name(title);
            bossBar.progress(clampedProgress);
            player.showBossBar(bossBar);
        }
    }

    @Override
    public void hideProgressBossBar() {
        final Player player = getPlayer();
        if (player != null && bossBar != null) {
            player.hideBossBar(bossBar);
        }
        bossBar = null;
    }

    @Override
    public void hideLocationCompass() {
        hideLocationCompass(getPlayer());
    }

    public void detach(final Player player){
        bossBar = null;
        hideLocationCompass(player);
        this.player = null;
    }

    public void attach(final Player player){
        this.player = player;
    }

    // --- PaperPlayer implementation ---

    @Override
    public boolean hasPlayer() {
        return getPlayer() != null;
    }

    @Override
    public String playerName() {
        return hasPlayer() ? getPlayer().getName() : "";
    }

    @Override
    public String displayName() {
        if (!hasPlayer()) {
            return "";
        }
        final Component customName = getPlayer().customName();
        return customName == null
                ? getPlayer().getName()
                : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(customName);
    }

    @Override
    public String playerIdentifier() {
        return getUniqueId().toString();
    }

    @Override
    public boolean setDisplayName(final String displayName) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().customName(Component.text(displayName == null ? "" : displayName));
        getPlayer().setCustomNameVisible(displayName != null && !displayName.isBlank());
        return true;
    }

    @Override
    public boolean isFlying() {
        return hasPlayer() && getPlayer().isFlying();
    }

    @Override
    public boolean setFlying(final boolean flying) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setFlying(flying);
        return true;
    }

    @Override
    public boolean isSneaking() {
        return hasPlayer() && getPlayer().isSneaking();
    }

    @Override
    public boolean setSneaking(final boolean sneaking) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setSneaking(sneaking);
        return true;
    }

    @Override
    public boolean isSprinting() {
        return hasPlayer() && getPlayer().isSprinting();
    }

    @Override
    public boolean setSprinting(final boolean sprinting) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setSprinting(sprinting);
        return true;
    }

    @Override
    public boolean isSwimming() {
        return hasPlayer() && getPlayer().isSwimming();
    }

    @Override
    public boolean setSwimming(final boolean swimming) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setSwimming(swimming);
        return true;
    }

    @Override
    public double health() {
        return hasPlayer() ? getPlayer().getHealth() : 0;
    }

    @Override
    public double maxHealth() {
        if (!hasPlayer()) {
            return 20;
        }
        final var maxHealth = getPlayer().getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20 : maxHealth.getValue();
    }

    @Override
    public boolean setMaxHealth(final double maxHealth) {
        if (!hasPlayer() || !Double.isFinite(maxHealth) || maxHealth < 1.0d) {
            return false;
        }
        final var attribute = getPlayer().getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return false;
        }
        try {
            attribute.setBaseValue(maxHealth);
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
        if (getPlayer().getHealth() > attribute.getValue()) {
            getPlayer().setHealth(attribute.getValue());
        }
        return true;
    }

    @Override
    public boolean setHealth(final double health) {
        if (!hasPlayer() || !Double.isFinite(health) || health < 0.0d || health > maxHealth()) {
            return false;
        }
        getPlayer().setHealth(health);
        return true;
    }

    @Override
    public int foodLevel() {
        return hasPlayer() ? getPlayer().getFoodLevel() : 0;
    }

    @Override
    public boolean setFoodLevel(final int foodLevel) {
        if (!hasPlayer() || foodLevel < 0 || foodLevel > 20) {
            return false;
        }
        getPlayer().setFoodLevel(foodLevel);
        return true;
    }

    @Override
    public double saturation() {
        return hasPlayer() ? getPlayer().getSaturation() : 0;
    }

    @Override
    public boolean setSaturation(final double saturation) {
        if (!hasPlayer() || !Double.isFinite(saturation) || saturation < 0.0d || saturation > 20.0d) {
            return false;
        }
        getPlayer().setSaturation((float) saturation);
        return true;
    }

    @Override
    public int experienceLevel() {
        return hasPlayer() ? getPlayer().getLevel() : 0;
    }

    @Override
    public boolean setExperienceLevel(final int level) {
        if (!hasPlayer() || level < 0) {
            return false;
        }
        getPlayer().setLevel(level);
        return true;
    }

    @Override
    public int experiencePoints() {
        if (!hasPlayer()) {
            return 0;
        }
        final Player player = getPlayer();
        return experienceAtLevel(player.getLevel()) + Math.round(experienceToLevelUp(player.getLevel()) * player.getExp());
    }

    @Override
    public boolean setExperiencePoints(final int points) {
        if (!hasPlayer() || points < 0) {
            return false;
        }
        getPlayer().setExp(0);
        getPlayer().setLevel(0);
        getPlayer().giveExp(points);
        return true;
    }

    @Override
    public int pingMillis() {
        return hasPlayer() ? getPlayer().getPing() : 0;
    }

    @Override
    public double walkSpeed() {
        return hasPlayer() ? getPlayer().getWalkSpeed() : 0;
    }

    @Override
    public boolean setWalkSpeed(final double speed) {
        if (!hasPlayer() || !Double.isFinite(speed) || speed < -1.0d || speed > 1.0d) {
            return false;
        }
        getPlayer().setWalkSpeed((float) speed);
        return true;
    }

    @Override
    public double flySpeed() {
        return hasPlayer() ? getPlayer().getFlySpeed() : 0;
    }

    @Override
    public boolean setFlySpeed(final double speed) {
        if (!hasPlayer() || !Double.isFinite(speed) || speed < -1.0d || speed > 1.0d) {
            return false;
        }
        getPlayer().setFlySpeed((float) speed);
        return true;
    }

    @Override
    public boolean isGlowing() {
        return hasPlayer() && getPlayer().isGlowing();
    }

    @Override
    public boolean setGlowing(final boolean glowing) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setGlowing(glowing);
        return true;
    }

    @Override
    public boolean isOperator() {
        return hasPlayer() && getPlayer().isOp();
    }

    @Override
    public boolean setOperator(final boolean operator) {
        if (!hasPlayer()) {
            return false;
        }
        getPlayer().setOp(operator);
        return true;
    }

    @Override
    public boolean isSleeping() {
        return hasPlayer() && getPlayer().isSleeping();
    }

    @Override
    public boolean isClimbing() {
        return hasPlayer() && getPlayer().isClimbing();
    }

    @Override
    public boolean isInLava() {
        return hasPlayer() && getPlayer().isInLava();
    }

    @Override
    public boolean isInWater() {
        return hasPlayer() && getPlayer().isInWater();
    }

    @Override
    public String gameMode() {
        return hasPlayer() ? getPlayer().getGameMode().name().toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public boolean setGameMode(final String gameMode) {
        if (!hasPlayer() || gameMode == null || gameMode.isBlank()) {
            return false;
        }
        try {
            getPlayer().setGameMode(GameMode.valueOf(gameMode.toUpperCase(Locale.ROOT)));
            return true;
        } catch (final IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public List<String> availableGameModes() {
        return Arrays.stream(GameMode.values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public int playtimeTicks() {
        return hasPlayer() ? getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) : 0;
    }

    @Override
    public boolean setPlaytimeTicks(final int ticks) {
        if (!hasPlayer() || ticks < 0) {
            return false;
        }
        getPlayer().setStatistic(Statistic.PLAY_ONE_MINUTE, ticks);
        return true;
    }

    private static int experienceToLevelUp(final int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    private static int experienceAtLevel(final int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5d * level * level - 40.5d * level + 360.0d);
        }
        return (int) (4.5d * level * level - 162.5d * level + 2220.0d);
    }

    @Override
    public String worldName() {
        return hasPlayer() ? getPlayer().getWorld().getName() : "";
    }

    @Override
    public String worldIdentifier() {
        return hasPlayer() ? getPlayer().getWorld().getUID().toString() : "";
    }

    @Override
    public boolean teleportToWorldSpawn(final String worldName) {
        if (!hasPlayer()) {
            return false;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        return getPlayer().teleport(world.getSpawnLocation());
    }

    @Override
    public boolean teleport(final NQLocation location, final Double yaw, final Double pitch) {
        if (!hasPlayer()) {
            return false;
        }
        final Location paperLocation = PaperNotQuestsAdapter.paperBukkitLocation(location);
        if (paperLocation == null) {
            return false;
        }
        final Location target = paperLocation.clone();
        if (yaw != null) {
            target.setYaw(yaw.floatValue());
        }
        if (pitch != null) {
            target.setPitch(pitch.floatValue());
        }
        return getPlayer().teleport(target);
    }

    @Override
    public List<String> availableWorldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    @Override
    public double positionX() {
        return hasPlayer() ? getPlayer().getLocation().getX() : 0;
    }

    @Override
    public boolean setPositionX(final double x) {
        if (!hasPlayer()) {
            return false;
        }
        final Location location = getPlayer().getLocation();
        location.setX(x);
        return getPlayer().teleport(location);
    }

    @Override
    public double positionY() {
        return hasPlayer() ? getPlayer().getLocation().getY() : 0;
    }

    @Override
    public boolean setPositionY(final double y) {
        if (!hasPlayer()) {
            return false;
        }
        final Location location = getPlayer().getLocation();
        location.setY(y);
        return getPlayer().teleport(location);
    }

    @Override
    public double positionZ() {
        return hasPlayer() ? getPlayer().getLocation().getZ() : 0;
    }

    @Override
    public boolean setPositionZ(final double z) {
        if (!hasPlayer()) {
            return false;
        }
        final Location location = getPlayer().getLocation();
        location.setZ(z);
        return getPlayer().teleport(location);
    }

    @Override
    public double yawDegrees() {
        return hasPlayer() ? getPlayer().getLocation().getYaw() : 0;
    }

    @Override
    public double pitchDegrees() {
        return hasPlayer() ? getPlayer().getLocation().getPitch() : 0;
    }

    @Override
    public String biomeName() {
        if (!hasPlayer()) {
            return "";
        }
        final var biome = getPlayer().getLocation().getBlock().getBiome();
        return namespacedId(RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .getKeyOrThrow(biome));
    }

    @Override
    public List<String> availableBiomeNames() {
        final List<String> names = new ArrayList<>();
        final var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        for (final var biome : biomeRegistry) {
            names.add(namespacedId(biomeRegistry.getKeyOrThrow(biome)));
        }
        return names;
    }

    @Override
    public String weather() {
        if (!hasPlayer()) {
            return "";
        }
        final World world = getPlayer().getWorld();
        if (world.isThundering()) {
            return "thunder";
        }
        if (world.hasStorm()) {
            return "rain";
        }
        return "clear";
    }

    @Override
    public boolean setWeather(final String weather) {
        if (!hasPlayer()) {
            return false;
        }
        final World world = getPlayer().getWorld();
        if ("clear".equalsIgnoreCase(weather)) {
            world.setStorm(false);
            world.setThundering(false);
            return true;
        }
        if ("rain".equalsIgnoreCase(weather)) {
            world.setStorm(true);
            world.setThundering(false);
            return true;
        }
        if ("thunder".equalsIgnoreCase(weather)) {
            world.setStorm(true);
            world.setThundering(true);
            return true;
        }
        return false;
    }

    @Override
    public double distanceTo(final NQLocation location) {
        if (!hasPlayer() || location == null) {
            return Double.MAX_VALUE;
        }
        final Location target;
        if (location instanceof final PaperNotQuestsAdapter.PaperNQLocation paperLocation) {
            target = paperLocation.location();
        } else {
            final World world = Bukkit.getWorld(location.worldName());
            if (world == null) {
                return Double.MAX_VALUE;
            }
            target = new Location(world, location.x(), location.y(), location.z());
        }
        final Location current = getPlayer().getLocation();
        if (current.getWorld() == null || target.getWorld() == null || !current.getWorld().equals(target.getWorld())) {
            return Double.MAX_VALUE;
        }
        return current.distance(target);
    }

    @Override
    public List<String> nearbyEntityTypeIds(final double radius) {
        if (!hasPlayer() || !Double.isFinite(radius) || radius < 0) {
            return List.of();
        }
        final List<String> entityTypes = new ArrayList<>();
        for (final var entity : getPlayer().getWorld().getNearbyEntities(
                getPlayer().getLocation(), radius, radius, radius)) {
            if (entity.equals(getPlayer())) {
                continue;
            }
            entityTypes.add(entity.getType().getKey().asString());
        }
        return List.copyOf(entityTypes);
    }

    @Override
    public long currentWorldTimeTicks() {
        if (getPlayer() == null || getPlayer().getWorld() == null) {
            return 0;
        }
        return getPlayer().getWorld().getTime();
    }

    @Override
    public void sendCommandChoice(
            final String prefixMiniMessage,
            final String choiceMiniMessage,
            final String command,
            final String hoverMiniMessage) {
        if (!hasPlayer()) {
            return;
        }
        final Component choice = main.parse(choiceMiniMessage)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.callback(audience -> {
                    if (audience instanceof Player clicker) {
                        clicker.performCommand(command == null || command.isBlank()
                                ? ""
                                : command.startsWith("/") ? command.substring(1) : command);
                    }
                }))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(main.parse(hoverMiniMessage)));
        getPlayer().sendMessage(main.parse(prefixMiniMessage).append(choice));
    }

    @Override
    public void sendActionBar(final String miniMessage) {
        if (hasPlayer()) {
            getPlayer().sendActionBar(main.parse(miniMessage));
        }
    }

    @Override
    public String applyExternalPlaceholders(final String text) {
        final Player player = getPlayer();
        if (player == null) {
            return text == null ? "" : text;
        }
        return PlaceholderAPI.setPlaceholders(player, text == null ? "" : text);
    }

    @Override
    public boolean supportsExternalPlaceholders() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void showProgressBossBar(final String miniMessage, final double progress) {
        if (hasPlayer()) {
            showProgressBossBar(main.parse(miniMessage), (float) progress);
        }
    }

    @Override
    public void showTitle(
            final String title,
            final String subtitle,
            final Duration fadeIn,
            final Duration stay,
            final Duration fadeOut) {
        if (!hasPlayer()) {
            return;
        }
        final Runnable showTitle = () -> getPlayer().showTitle(Title.title(
                main.parse(title),
                subtitle == null || subtitle.isBlank()
                        ? Component.empty()
                        : main.parse(subtitle),
                Title.Times.times(fadeIn, stay, fadeOut)));
        if (Bukkit.isPrimaryThread()) {
            showTitle.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), showTitle);
        }
    }

    @Override
    public void chat(final String message) {
        if (!hasPlayer()) {
            return;
        }
        final Runnable chat = () -> getPlayer().chat(message);
        if (Bukkit.isPrimaryThread()) {
            chat.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), chat);
        }
    }

    @Override
    public void performCommand(final String command) {
        if (!hasPlayer()) {
            return;
        }
        final String trimmed = command.startsWith("/") ? command.substring(1) : command;
        final Runnable perform = () -> getPlayer().performCommand(trimmed);
        if (Bukkit.isPrimaryThread()) {
            perform.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), perform);
        }
    }

    @Override
    public void closeInventory() {
        if (hasPlayer()) {
            getPlayer().closeInventory();
        }
    }

    @Override
    public boolean giveItems(final List<SavedItems.ItemChoice> items) {
        if (!hasPlayer()) {
            return false;
        }
        final Selection paperSelection = main.getRegistryAdapter().materializeItems(items);
        if (paperSelection == null) {
            return false;
        }
        final List<ItemStack> itemStacks = paperSelection.toItemStackList();
        if (itemStacks.isEmpty()) {
            return false;
        }
        final Runnable giveItems = () -> {
            for (final ItemStack itemStack : itemStacks) {
                final ItemStack copy = itemStack.clone();
                final HashMap<Integer, ItemStack> leftovers = getPlayer().getInventory().addItem(copy);
                for (final ItemStack leftover : leftovers.values()) {
                    getPlayer().getWorld().dropItem(getPlayer().getLocation(), leftover);
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            giveItems.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), giveItems);
        }
        return true;
    }

    @Override
    public int removeItems(
            final List<SavedItems.ItemChoice> items,
            final int maxAmount) {
        if (!hasPlayer() || maxAmount <= 0) {
            return 0;
        }
        final Selection paperSelection = main.getRegistryAdapter().materializeItems(items);
        if (paperSelection == null) {
            return 0;
        }
        int remaining = maxAmount;
        int removed = 0;
        final ItemStack[] contents = getPlayer().getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            final ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            if (!main.getRegistryAdapter().itemSelectionIncludesItemStack(paperSelection, itemStack)) {
                continue;
            }
            final int toRemove = Math.min(remaining, itemStack.getAmount());
            itemStack.setAmount(itemStack.getAmount() - toRemove);
            if (itemStack.getAmount() <= 0) {
                getPlayer().getInventory().setItem(slot, null);
            }
            remaining -= toRemove;
            removed += toRemove;
        }
        if (removed > 0) {
            getPlayer().updateInventory();
        }
        return removed;
    }

    @Override
    public boolean addInventoryItems(
            final List<SavedItems.ItemChoice> items,
            final boolean dropOverflow) {
        if (!hasPlayer()) {
            return false;
        }
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = main.getRegistryAdapter().materializeItems(List.of(item));
            if (selection == null) {
                continue;
            }
            for (final ItemStack itemStack : selection.toItemStackList()) {
                final ItemStack copy = itemStack.clone();
                final int requested = copy.getAmount();
                final HashMap<Integer, ItemStack> leftovers =
                        getPlayer().getInventory().addItem(copy);
                final int remaining = leftovers.values().stream()
                        .mapToInt(ItemStack::getAmount)
                        .sum();
                if (dropOverflow) {
                    for (final ItemStack leftover : leftovers.values()) {
                        getPlayer().getWorld().dropItem(getPlayer().getLocation(), leftover);
                    }
                }
                changed |= requested > remaining || dropOverflow && remaining > 0;
            }
        }
        if (changed) {
            getPlayer().updateInventory();
        }
        return changed;
    }

    @Override
    public boolean removeInventoryItems(final List<SavedItems.ItemChoice> items) {
        if (!hasPlayer()) {
            return false;
        }
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            changed = removeItems(
                    List.of(item),
                    item == null || item.selection() == null ? 0 : item.selection().amount()) > 0 || changed;
        }
        if (changed) {
            getPlayer().updateInventory();
        }
        return changed;
    }

    @Override
    public List<ItemSelection> inventoryItems() {
        if (!hasPlayer()) {
            return List.of();
        }
        final ArrayList<ItemSelection> items = new ArrayList<>();
        for (final ItemStack itemStack : getPlayer().getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0) {
                continue;
            }
            final Selection selection = new Selection(main);
            selection.addItemStack(itemStack.clone());
            items.add(selection);
        }
        return List.copyOf(items);
    }

    @Override
    public boolean setInventoryItems(final List<SavedItems.ItemChoice> items) {
        if (!hasPlayer()) {
            return false;
        }
        final List<ItemStack> stacks = materializeItems(items);
        if (stacks == null) {
            return false;
        }
        getPlayer().getInventory().clear();
        final boolean complete = getPlayer().getInventory()
                .addItem(stacks.toArray(ItemStack[]::new))
                .isEmpty();
        getPlayer().updateInventory();
        return complete;
    }

    @Override
    public List<ItemSelection> enderChestItems() {
        if (!hasPlayer()) {
            return List.of();
        }
        final ArrayList<ItemSelection> items = new ArrayList<>();
        for (final ItemStack itemStack : getPlayer().getEnderChest().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0) {
                continue;
            }
            final Selection selection = new Selection(main);
            selection.addItemStack(itemStack.clone());
            items.add(selection);
        }
        return List.copyOf(items);
    }

    @Override
    public boolean addEnderChestItems(
            final List<SavedItems.ItemChoice> items,
            final boolean addOverflowToInventory,
            final boolean dropOverflow) {
        if (!hasPlayer()) {
            return false;
        }
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = main.getRegistryAdapter().materializeItems(List.of(item));
            if (selection == null) {
                continue;
            }
            for (final ItemStack itemStack : selection.toItemStackList()) {
                final int requested = itemStack.getAmount();
                HashMap<Integer, ItemStack> leftovers =
                        getPlayer().getEnderChest().addItem(itemStack.clone());
                if (addOverflowToInventory && !leftovers.isEmpty()) {
                    leftovers = getPlayer().getInventory().addItem(leftovers.values().toArray(new ItemStack[0]));
                }
                final int remaining = leftovers.values().stream()
                        .mapToInt(ItemStack::getAmount)
                        .sum();
                if (dropOverflow) {
                    for (final ItemStack leftover : leftovers.values()) {
                        getPlayer().getWorld().dropItem(getPlayer().getLocation(), leftover);
                    }
                }
                changed |= requested > remaining || dropOverflow && remaining > 0;
            }
        }
        if (changed) {
            getPlayer().updateInventory();
        }
        return changed;
    }

    @Override
    public boolean removeEnderChestItems(final List<SavedItems.ItemChoice> items) {
        if (!hasPlayer()) {
            return false;
        }
        final boolean changed = removeFromInventory(getPlayer().getEnderChest(), items);
        if (changed) {
            getPlayer().updateInventory();
        }
        return changed;
    }

    private boolean removeFromInventory(
            final Inventory inventory,
            final List<SavedItems.ItemChoice> items) {
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = main.getRegistryAdapter().materializeItems(List.of(item));
            if (selection == null) {
                continue;
            }
            int remaining = Math.max(1, item.selection().amount());
            final ItemStack[] contents = inventory.getContents();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                final ItemStack stack = contents[slot];
                if (stack == null || stack.getType().isAir()
                        || !main.getRegistryAdapter().itemSelectionIncludesItemStack(selection, stack)) {
                    continue;
                }
                final int removed = Math.min(remaining, stack.getAmount());
                remaining -= removed;
                changed = true;
                if (removed == stack.getAmount()) {
                    inventory.setItem(slot, null);
                } else {
                    stack.setAmount(stack.getAmount() - removed);
                }
            }
        }
        return changed;
    }

    private List<ItemStack> materializeItems(final List<SavedItems.ItemChoice> items) {
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = main.getRegistryAdapter().materializeItems(List.of(item));
            if (selection == null) {
                return null;
            }
            final List<ItemStack> selectedStacks = selection.toItemStackList();
            if (selectedStacks.isEmpty()) {
                return null;
            }
            selectedStacks.forEach(stack -> stacks.add(stack.clone()));
        }
        return List.copyOf(stacks);
    }

    @Override
    public boolean setEnderChestItems(final List<SavedItems.ItemChoice> items) {
        if (!hasPlayer()) {
            return false;
        }
        final List<ItemStack> stacks = materializeItems(items);
        if (stacks == null) {
            return false;
        }
        getPlayer().getEnderChest().clear();
        final boolean complete = getPlayer().getEnderChest()
                .addItem(stacks.toArray(ItemStack[]::new))
                .isEmpty();
        getPlayer().updateInventory();
        return complete;
    }

    @Override
    public boolean beforeQuestAccepted(
            final Quest quest,
            final boolean triggerAcceptQuestTrigger) {
        if (quest == null) {
            return false;
        }
        return onServerThread(() -> {
            final QuestFinishAcceptEvent event = new QuestFinishAcceptEvent(
                    this,
                    quest.getIdentifier(),
                    quest,
                    triggerAcceptQuestTrigger);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
    }

    @Override
    public boolean beforeQuestPointsChanged(final long newQuestPoints) {
        return onServerThread(() -> {
            final QuestPointsChangeEvent event = new QuestPointsChangeEvent(this, newQuestPoints);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
    }

    @Override
    public boolean beforeQuestCompleted(
            final Quest quest,
            final boolean forced) {
        if (quest == null) {
            return false;
        }
        return onServerThread(() -> {
            final QuestCompletedEvent event = new QuestCompletedEvent(
                    this,
                    quest.getIdentifier(),
                    quest,
                    forced);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
    }

    @Override
    public boolean beforeQuestFailed(final Quest quest) {
        if (quest == null) {
            return false;
        }
        return onServerThread(() -> {
            final QuestFailEvent event = new QuestFailEvent(
                    this,
                    quest.getIdentifier(),
                    quest);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
    }

    @Override
    public boolean beforeObjectiveCompleted(
            final Quest quest,
            final ActiveObjective activeObjective) {
        if (quest == null || activeObjective == null) {
            return false;
        }
        return onServerThread(() -> {
            final ObjectiveCompleteEvent event = new ObjectiveCompleteEvent(
                    this,
                    quest.getIdentifier(),
                    activeObjective.getObjectivePath(),
                    activeObjective.getObjectiveID(),
                    activeObjective.getHolderPath(),
                    quest,
                    activeObjective);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
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
        if (!hasPlayer()) {
            return false;
        }
        final Particle particle = findParticle(particleId);
        if (particle == null || particle.getDataType() != Void.class) {
            return false;
        }
        final Location target = PaperNotQuestsAdapter.paperBukkitLocation(location);
        if (target == null || target.getWorld() == null) {
            return false;
        }
        try {
            if (showToEveryone) {
                target.getWorld().spawnParticle(
                        particle, target, count, offsetX, offsetY, offsetZ, speed);
            } else {
                getPlayer().spawnParticle(
                        particle, target, count, offsetX, offsetY, offsetZ, speed);
            }
            return true;
        } catch (final IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void stopSounds() {
        onServerThread(() -> {
            final Player targetPlayer = getPlayer();
            if (targetPlayer != null) {
                targetPlayer.stopAllSounds();
            }
            return true;
        });
    }

    @Override
    public boolean playSound(
            final String soundId,
            final SoundAudience audience,
            final NQLocation soundLocation,
            final double volume,
            final double pitch,
            final String soundCategory) {
        if (!hasPlayer() || soundId == null || soundId.isBlank() || soundLocation == null) {
            return false;
        }
        return onServerThread(() -> {
            final Player targetPlayer = getPlayer();
            if (targetPlayer == null) {
                return false;
            }
            final Location location = PaperNotQuestsAdapter.paperBukkitLocation(soundLocation);
            if (location == null || location.getWorld() == null) {
                return false;
            }
            final SoundCategory category = parseSoundCategory(soundCategory);
            if (category == null) {
                return false;
            }
            try {
                switch (audience) {
                    case EVERYONE_AT_OWN_LOCATION -> {
                        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.playSound(
                                    onlinePlayer.getLocation(), soundId, category, (float) volume, (float) pitch);
                        }
                    }
                    case WORLD -> location.getWorld().playSound(
                            location, soundId, category, (float) volume, (float) pitch);
                    case PLAYER -> targetPlayer.playSound(
                            location, soundId, category, (float) volume, (float) pitch);
                }
                return true;
            } catch (final IllegalArgumentException ignored) {
                return false;
            }
        });
    }

    @Override
    public boolean spawnVanillaMob(
            final String entityTypeName,
            final NQLocation location) {
        if (!hasPlayer() || entityTypeName == null || entityTypeName.isBlank()) {
            return false;
        }
        return onServerThread(() -> {
            final Location baseLocation = PaperNotQuestsAdapter.paperBukkitLocation(location);
            if (baseLocation == null || baseLocation.getWorld() == null) {
                return false;
            }
            final EntityType entityType = findEntityType(entityTypeName);
            if (entityType != null) {
                try {
                    baseLocation.getWorld().spawnEntity(baseLocation, entityType);
                    return true;
                } catch (final IllegalArgumentException ignored) {
                    return false;
                }
            }
            return false;
        });
    }

    @Override
    public NQLocation lookingAtBlock(final double maxDistance) {
        if (!hasPlayer()) {
            return null;
        }
        final int distance = (int) Math.max(1, Math.round(maxDistance));
        final Block block = getPlayer().getTargetBlockExact(distance);
        return block == null ? null : PaperNotQuestsAdapter.paperNQLocation(block.getLocation());
    }

    @Override
    public boolean showGui(final ResolvedGui gui) {
        return onServerThread(() -> main.getPaperGuiRenderer().open(getPlayer(), gui, this));
    }

    private EntityType findEntityType(final String entityTypeName) {
        final NamespacedKey key = NamespacedKey.fromString(entityTypeName.toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).get(key);
    }

    private boolean onServerThread(final Callable<Boolean> action) {
        try {
            return main.getRegistryAdapter().callOnServerThread(action);
        } catch (final Exception exception) {
            main.getCorePlugin().warn(
                    "Could not execute a Paper player operation on the server thread: {}",
                    exception.getMessage());
            return false;
        }
    }

    private static String namespacedId(final NamespacedKey key) {
        if (key == null) {
            return "";
        }
        return NamespacedKey.MINECRAFT.equals(key.getNamespace())
                ? key.getKey()
                : key.asString();
    }

    private static Particle findParticle(final String particleName) {
        if (particleName == null || particleName.isBlank()) {
            return null;
        }
        for (final Particle particle : Particle.values()) {
            if (particle.name().equalsIgnoreCase(particleName)
                    || particle.getKey().getKey().equalsIgnoreCase(particleName)
                    || particle.getKey().asString().equalsIgnoreCase(particleName)) {
                return particle;
            }
        }
        return null;
    }

    private static SoundCategory parseSoundCategory(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SoundCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

}
