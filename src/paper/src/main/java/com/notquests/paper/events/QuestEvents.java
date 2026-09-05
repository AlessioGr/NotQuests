package com.notquests.paper.events;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.loot.LootTables;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperItems;
import com.notquests.paper.PaperPlayer;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class QuestEvents implements Listener {
    private final NotQuests main;

    public QuestEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    private void onChunkLoad(PlayerChunkLoadEvent e){
        final Player player = e.getPlayer();
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        main.getCorePlugin().playerChunkLoaded(questPlayer);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent e) {
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            main.getCorePlugin().playerConsumedItem(
                    questPlayer,
                    new PaperItemEvent(e.getItem()),
                    () -> e.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        final boolean unopenedBuriedTreasure = e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null
                && e.getClickedBlock().getState() instanceof final Chest chest
                && chest.getLootTable() != null
                && chest.getLootTable().getKey().equals(LootTables.BURIED_TREASURE.getKey())
                && !chest.hasPlayerLooted(e.getPlayer().getUniqueId());
        main.getCorePlugin().playerInteractedWithBlock(
                questPlayer,
                e.getClickedBlock() == null ? null : new PaperInteractionEvent(e),
                unopenedBuriedTreasure);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        if (questPlayer == null) {
            return;
        }
        main.getCorePlugin().playerChangedWorld(
                questPlayer,
                e.getFrom().getName(),
                player.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        final Block block = e.getBlock();
        final String blockKey = blockKey(block);
        final Material material = block.getType();
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerBrokeBlock(
                    questPlayer,
                    blockKey,
                    material.name(),
                    block.getRelative(BlockFace.DOWN).getType() == material,
                    block.getBlockData() instanceof final Ageable ageable
                            && ageable.getAge() >= ageable.getMaximumAge(),
                    false);
        }
        main.getCorePlugin().blockBreakFinished(blockKey, material == Material.BREWING_STAND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockPlace(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
            if (questPlayer != null) {
                main.getCorePlugin().playerPlacedBlock(
                        questPlayer,
                        blockKey(e.getBlock()),
                        e.getBlock().getType().name(),
                        e.getBlock().getBlockData() instanceof final Ageable ageable
                                && ageable.getAge() >= ageable.getMaximumAge());
            }
        }

    }

    private static String blockKey(final Block block) {
        return ActiveObjectives.blockKey(
                block.getWorld().getUID().toString(),
                block.getX(),
                block.getY(),
                block.getZ());
    }

    private PaperPlayer activePaperPlayer(final Player player) {
        if (player == null) {
            return null;
        }
        return main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
    }

    private void runAsync(final Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), action);
        } else {
            action.run();
        }
    }

    private void runOnPlatformThread(final Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), action);
        }
    }

    private static int takenResultAmount(
            final Player player,
            final ItemStack result,
            final InventoryClickEvent event) {
        return ActiveObjectives.takenResultAmount(
                inventoryItem(result, result),
                inventoryItem(event.getCursor(), result),
                inventoryClick(event.getClick()),
                event.getHotbarButton() >= 0
                        && player.getInventory().getItem(event.getHotbarButton()) != null,
                !PaperItems.isEmpty(player.getInventory().getItemInOffHand()),
                inventorySpace(player.getInventory().getStorageContents(), result));
    }

    private static int craftAmount(final ItemStack result, final CraftItemEvent event) {
        final int maxCraftable = event.getInventory().getResult() == null
                ? 0
                : ActiveObjectives.maxCraftAmount(
                        event.getInventory().getResult().getAmount(),
                        Arrays.stream(event.getInventory().getMatrix())
                                .filter(Objects::nonNull)
                                .map(ItemStack::getAmount)
                                .toList());
        return ActiveObjectives.craftAmount(
                inventoryItem(result, result),
                inventoryItem(event.getCursor(), result),
                inventoryClick(event.getClick()),
                event.getHotbarButton() >= 0
                        && event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null,
                !PaperItems.isEmpty(event.getWhoClicked().getInventory().getItemInOffHand()),
                maxCraftable,
                inventorySpace(event.getView().getBottomInventory().getContents(), result));
    }

    private static int inventorySpace(final ItemStack[] contents, final ItemStack target) {
        return ActiveObjectives.inventorySpaceLeft(
                Arrays.stream(contents == null ? new ItemStack[0] : contents)
                        .map(item -> inventoryItem(item, target))
                        .toList(),
                ActiveObjectives.InventoryItem.target(target.getAmount(), target.getMaxStackSize()));
    }

    private static ActiveObjectives.InventoryItem inventoryItem(
            final ItemStack item,
            final ItemStack target) {
        if (PaperItems.isEmpty(item)) {
            return ActiveObjectives.InventoryItem.empty(target.getMaxStackSize());
        }
        return new ActiveObjectives.InventoryItem(
                item.getAmount(),
                item.getMaxStackSize(),
                false,
                item.isSimilar(target));
    }

    private static ActiveObjectives.InventoryClick inventoryClick(final ClickType click) {
        if (click == null) {
            return ActiveObjectives.InventoryClick.OTHER;
        }
        return switch (click) {
            case LEFT -> ActiveObjectives.InventoryClick.LEFT;
            case RIGHT -> ActiveObjectives.InventoryClick.RIGHT;
            case NUMBER_KEY -> ActiveObjectives.InventoryClick.NUMBER_KEY;
            case DROP -> ActiveObjectives.InventoryClick.DROP;
            case CONTROL_DROP -> ActiveObjectives.InventoryClick.CONTROL_DROP;
            case SWAP_OFFHAND -> ActiveObjectives.InventoryClick.SWAP_OFFHAND;
            case SHIFT_LEFT -> ActiveObjectives.InventoryClick.SHIFT_LEFT;
            case SHIFT_RIGHT -> ActiveObjectives.InventoryClick.SHIFT_RIGHT;
            default -> ActiveObjectives.InventoryClick.OTHER;
        };
    }

    private static String itemKey(final ItemStack item) {
        if (PaperItems.isEmpty(item)) {
            return "";
        }
        final ItemStack oneItem = item.clone();
        oneItem.setAmount(1);
        return Base64.getEncoder().encodeToString(oneItem.serializeAsBytes());
    }

    private static String deathDamageType(final Player player) {
        final EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        if (lastDamageCause == null) {
            return "unknown";
        }
        final net.kyori.adventure.key.Key key = lastDamageCause.getDamageSource().getDamageType().key();
        return "minecraft".equals(key.namespace()) ? key.value() : key.asString();
    }

    private static NQLocation paperLocation(final Location location) {
        return location == null || location.getWorld() == null ? null : new PaperNQLocation(location);
    }

    private record PaperNQLocation(org.bukkit.Location location) implements NQLocation {
        @Override
        public String worldName() {
            return location.getWorld() == null ? "" : location.getWorld().getName();
        }

        @Override
        public double x() {
            return location.getX();
        }

        @Override
        public double y() {
            return location.getY();
        }

        @Override
        public double z() {
            return location.getZ();
        }

        @Override
        public float yaw() {
            return location.getYaw();
        }

        @Override
        public float pitch() {
            return location.getPitch();
        }
    }

    private record PaperItemEvent(ItemStack item, int amount) implements Objectives.ItemEvent {
        private PaperItemEvent(final ItemStack item) {
            this(item, item == null ? 0 : item.getAmount());
        }

        @Override
        public String materialId() {
            return item == null ? "" : item.getType().name().toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return NotQuests.getInstance().getRegistryAdapter().itemSelectionIncludesItemStack(selection, item);
        }
    }

    private record PaperEntityEvent(
            String entityTypeName,
            String plainCustomName,
            boolean selfAttributedDeath)
            implements Objectives.EntityEvent {
        @Override
        public String entityTypeId() {
            return entityTypeName.toLowerCase(Locale.ROOT);
        }
    }

    static boolean isSelfAttributedDeath(final Player player, final LivingEntity entity) {
        return player != null
                && entity != null
                && player.getUniqueId().equals(entity.getUniqueId());
    }

    private record PaperInteractionEvent(PlayerInteractEvent event)
            implements Objectives.InteractionEvent {
        @Override
        public NQLocation location() {
            return paperLocation(event.getClickedBlock() == null ? null : event.getClickedBlock().getLocation());
        }

        @Override
        public boolean leftClick() {
            return event.getAction() == Action.LEFT_CLICK_BLOCK;
        }

        @Override
        public boolean rightClick() {
            return event.getAction() == Action.RIGHT_CLICK_BLOCK;
        }

        @Override
        public void cancel() {
            event.setCancelled(true);
        }
    }

    private record PaperShearSheepEvent(PlayerShearEntityEvent event)
            implements Objectives.ShearSheepEvent {
        @Override
        public void cancel() {
            event.setCancelled(true);
        }
    }

    private record PaperMilkCowEvent(PlayerInteractEntityEvent event)
            implements Objectives.MilkCowEvent {
        @Override
        public void cancel() {
            event.setCancelled(true);
        }
    }

    private record PaperMoveEvent(Location paperTo) implements Objectives.MoveEvent {
        @Override
        public NQLocation to() {
            return paperLocation(paperTo);
        }
    }

    private record PaperProjectileHitEvent(Location hitLocation) implements Objectives.ProjectileHitEvent {
        @Override
        public NQLocation location() {
            return paperLocation(hitLocation);
        }
    }

    private record PaperEnchantEvent(EnchantItemEvent event) implements Objectives.EnchantEvent {
        @Override
        public String materialId() {
            return event.getItem().getType().name().toLowerCase(Locale.ROOT);
        }

        @Override
        public int amount() {
            return 1;
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return NotQuests.getInstance().getRegistryAdapter().itemSelectionIncludesItemStack(selection, event.getItem());
        }

        @Override
        public Map<String, Integer> enchantments() {
            final Map<String, Integer> enchantments = new LinkedHashMap<>();
            for (final Map.Entry<Enchantment, Integer> enchantment : event.getEnchantsToAdd().entrySet()) {
                enchantments.put(enchantment.getKey().getKey().asString(), enchantment.getValue());
            }
            return Map.copyOf(enchantments);
        }
    }

    private record PaperDeathEvent(String damageTypeId) implements Objectives.DeathEvent {}

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEntityDeath(EntityDeathEvent e) { //KillMobs objectives & Death triggers
        final Player killer = e.getEntity().getKiller();
        final PaperPlayer deadPlayer = e.getEntity() instanceof final Player player
                ? main.getRegistryAdapter().activePaperPlayer(player.getUniqueId())
                : null;
        final PaperPlayer killerPlayer = activePaperPlayer(killer);
        main.getCorePlugin().entityDied(
                deadPlayer,
                deadPlayer == null ? null : new PaperDeathEvent(deathDamageType((Player) e.getEntity())),
                killerPlayer,
                killerPlayer == null
                        ? null
                        : new PaperEntityEvent(
                                e.getEntity().getType().name(),
                                e.getEntity().customName() == null
                                        ? ""
                                        : PlainTextComponentSerializer.plainText().serialize(e.getEntity().customName()),
                                isSelfAttributedDeath(killer, e.getEntity())));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerJump(PlayerJumpEvent e) {
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerJumped(questPlayer);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerStartedSneaking(questPlayer);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer != null) {
            main.getCorePlugin().playerPickedUpItem(
                    questPlayer,
                    new PaperItemEvent(e.getItem().getItemStack()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onDropItem(PlayerDropItemEvent e) {
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerDroppedItem(
                    questPlayer,
                    new PaperItemEvent(e.getItemDrop().getItemStack()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onBrew(BrewEvent e) {
        main.getCorePlugin().brewingFinished(
                blockKey(e.getBlock()),
                e.getResults().stream()
                        .filter(result -> result != null && !result.getType().isAir())
                        .map(result -> new ActiveObjectives.BrewedItem(itemKey(result), result.getAmount()))
                        .toList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer == null) {
            return;
        }
        final ItemStack currentItem = e.getCurrentItem();
        if (currentItem == null || currentItem.getType().isAir()) {
            return;
        }
        final ActiveObjectives.TakenItem takenItem;
        final String brewingStandKey;
        if (e.getClickedInventory() instanceof BrewerInventory brewerInventory
                && e.getSlot() >= 0
                && e.getSlot() <= 2
                && brewerInventory.getHolder() instanceof BrewingStand brewingStand) {
            takenItem = ActiveObjectives.TakenItem.BREWED;
            brewingStandKey = blockKey(brewingStand.getBlock());
        } else if ((e.getInventory().getType() == InventoryType.FURNACE
                        || e.getInventory().getType() == InventoryType.BLAST_FURNACE
                        || e.getInventory().getType() == InventoryType.SMOKER)
                && e.getRawSlot() == 2) {
            takenItem = ActiveObjectives.TakenItem.SMELTED;
            brewingStandKey = "";
        } else if (e.getInventory() instanceof MerchantInventory && e.getRawSlot() == 2) {
            takenItem = ActiveObjectives.TakenItem.TRADED;
            brewingStandKey = "";
        } else {
            return;
        }
        final int amount = takenItem == ActiveObjectives.TakenItem.BREWED
                ? currentItem.getAmount()
                : takenResultAmount(player, currentItem, e);
        if (amount > 0) {
            main.getCorePlugin().playerTookInventoryItem(
                    questPlayer,
                    takenItem,
                    brewingStandKey,
                    itemKey(currentItem),
                    new PaperItemEvent(currentItem, amount));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onSmithItem(SmithItemEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        final ItemStack currentItem = e.getCurrentItem();
        if (questPlayer != null && currentItem != null && !currentItem.getType().isAir()) {
            main.getCorePlugin().playerTookSmithingResult(
                    questPlayer,
                    new PaperItemEvent(currentItem));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo() == null
                || (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ())) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerMoved(
                    questPlayer,
                    new PaperMoveEvent(e.getTo()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof final Arrow arrow) || !(arrow.getShooter() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer != null) {
            main.getCorePlugin().playerProjectileHit(
                    questPlayer,
                    new PaperProjectileHitEvent(e.getEntity().getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onBreedEntity(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer != null) {
            main.getCorePlugin().playerBredEntity(
                    questPlayer,
                    new PaperEntityEvent(e.getEntityType().name(), "", false));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFeedEntity(EntityEnterLoveModeEvent e) {
        if (!(e.getHumanEntity() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer != null) {
            main.getCorePlugin().playerInteractedWithEntity(
                    questPlayer,
                    false,
                    new PaperEntityEvent(e.getEntityType().name(), "", false),
                    null,
                    null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onTameEntity(EntityTameEvent e) {
        if (!(e.getOwner() instanceof final Player player)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer != null) {
            main.getCorePlugin().playerTamedEntity(
                    questPlayer,
                    new PaperEntityEvent(e.getEntityType().name(), "", false));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFishItem(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof final Item item)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerFishedItem(
                    questPlayer,
                    new PaperItemEvent(item.getItemStack()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player) || e.getInventory().getResult() == null) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(player);
        if (questPlayer == null) {
            return;
        }
        final ItemStack result = e.getRecipe().getResult();
        final int amount = craftAmount(result, e);
        if (amount != 0) {
            main.getCorePlugin().playerCraftedItem(
                    questPlayer,
                    new PaperItemEvent(result, amount),
                    PaperItems.isEmpty(e.getCursor()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onShearSheep(PlayerShearEntityEvent e) {
        if (!isShearingSheep(e)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerInteractedWithEntity(
                    questPlayer,
                    false,
                    null,
                    null,
                    new PaperShearSheepEvent(e));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!isMilkingCow(e)) {
            return;
        }
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer == null) {
            return;
        }
        main.getCorePlugin().playerInteractedWithEntity(
                questPlayer,
                false,
                null,
                new PaperMilkCowEvent(e),
                null);
    }

    static boolean isShearingSheep(final PlayerShearEntityEvent event) {
        return event != null
                && !event.isCancelled()
                && event.getEntity() instanceof Sheep;
    }

    static boolean isMilkingCow(final PlayerInteractEntityEvent event) {
        if (event == null
                || event.isCancelled()
                || !(event.getRightClicked() instanceof Cow)
                || event.getHand() == null) {
            return false;
        }
        final ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
        return handItem != null && handItem.getType() == Material.BUCKET;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        final PaperPlayer questPlayer = activePaperPlayer(e.getPlayer());
        if (questPlayer != null) {
            main.getCorePlugin().playerRanCommand(
                    questPlayer,
                    e.getMessage(),
                    () -> e.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEnchantItem(EnchantItemEvent e) {
        final PaperPlayer questPlayer = activePaperPlayer(e.getEnchanter());
        if (questPlayer != null) {
            main.getCorePlugin().playerEnchantedItem(
                    questPlayer,
                    new PaperEnchantEvent(e));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPluginEnable(final PluginEnableEvent event) {
        main.getCorePlugin().integrationPluginEnabled(event.getPlugin().getName());
    }

    @EventHandler
    private void onDisconnectEvent(PlayerQuitEvent e) { //Disconnect objectives
        final Player player = e.getPlayer();
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        main.getCorePlugin().playerLeft(
                player.getUniqueId().toString(),
                questPlayer,
                player.getWorld().getName(),
                questPlayer == null ? null : () -> questPlayer.detach(player),
                this::runAsync);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        main.getCorePlugin().playerJoined(
                player.getUniqueId().toString(),
                main.getRegistryAdapter()::createPaperPlayer,
                player::isOnline,
                questPlayer -> ((PaperPlayer) questPlayer).attach(player),
                player::isOp,
                this::runAsync,
                this::runOnPlatformThread);
    }

    @EventHandler(ignoreCancelled = true)
    public void asyncChatEvent(AsyncChatEvent e) {
        final Player playerWhoChatted = e.getPlayer();
        final PaperPlayer questPlayer = main.getRegistryAdapter().activePaperPlayer(playerWhoChatted.getUniqueId());
        if (main.getCorePlugin().playerChatted(
                questPlayer,
                PlainTextComponentSerializer.plainText().serialize(e.message()))) {
            e.setCancelled(true);
        }
    }

}
