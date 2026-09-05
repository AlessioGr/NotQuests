package com.notquests.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjectives;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NeoForgeObjectiveEvents {
    private static volatile NeoForgeObjectiveEvents activeEvents;

    private final NotQuestsPlugin plugin;
    private final NeoForgeText text;
    private final NeoForgeNpcAttachments npcAttachments;
    private final Supplier<MinecraftServer> server;
    private final NeoForgeBeamTracker beamTracker;
    private final NeoForgeGuiRenderer guiRenderer;
    private final Supplier<ItemStack> journalItem;
    private final Consumer<Runnable> runAsync;
    private final Map<UUID, String> journalDeathWorlds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> journalBlockUseTicks = new ConcurrentHashMap<>();

    NeoForgeObjectiveEvents(
            final NotQuestsPlugin plugin,
            final NeoForgeText text,
            final NeoForgeNpcAttachments npcAttachments,
            final Supplier<MinecraftServer> server,
            final NeoForgeBeamTracker beamTracker,
            final NeoForgeGuiRenderer guiRenderer,
            final Supplier<ItemStack> journalItem,
            final Consumer<Runnable> runAsync) {
        this.plugin = plugin;
        this.text = text;
        this.npcAttachments = npcAttachments;
        this.server = server;
        this.beamTracker = beamTracker;
        this.guiRenderer = guiRenderer;
        this.journalItem = journalItem;
        this.runAsync = runAsync;
    }

    void register() {
        activeEvents = this;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(this::onPickupItemPre);
        NeoForge.EVENT_BUS.addListener(this::onPickupItemPost);
        NeoForge.EVENT_BUS.addListener(this::onTossItem);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onRightClickEntity);
        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(this::onBrewedPotion);
        NeoForge.EVENT_BUS.addListener(this::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(this::onItemSmelted);
        NeoForge.EVENT_BUS.addListener(this::onItemUseStarted);
        NeoForge.EVENT_BUS.addListener(this::onItemConsumed);
        NeoForge.EVENT_BUS.addListener(this::onItemFished);
        NeoForge.EVENT_BUS.addListener(this::onTradeWithVillager);
        NeoForge.EVENT_BUS.addListener(this::onBreedEntity);
        NeoForge.EVENT_BUS.addListener(this::onTameEntity);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJump);
        NeoForge.EVENT_BUS.addListener(this::onEnchantItem);
        NeoForge.EVENT_BUS.addListener(this::onCommand);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerChat);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawned);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
    }

    void activate() {
        activeEvents = this;
    }

    public static void onSmithingResultTaken(final Player player, final ItemStack result) {
        final NeoForgeObjectiveEvents events = activeEvents;
        if (events == null || !(player instanceof ServerPlayer serverPlayer) || result == null || result.isEmpty()) {
            return;
        }
        events.plugin.playerTookSmithingResult(
                events.platformPlayer(serverPlayer), events.itemEvent(result));
    }

    public static boolean onContainerClicked(
            final AbstractContainerMenu menu,
            final int slotIndex,
            final int buttonNumber,
            final ContainerInput input,
            final Player clickedBy) {
        final NeoForgeObjectiveEvents events = activeEvents;
        if (events == null || menu == null || !(clickedBy instanceof ServerPlayer player)) {
            return false;
        }
        final ItemStack clicked = menu.isValidSlotIndex(slotIndex)
                ? menu.getSlot(slotIndex).getItem()
                : ItemStack.EMPTY;
        final ItemStack swapped = input == ContainerInput.SWAP
                        && buttonNumber >= 0
                        && buttonNumber < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(buttonNumber)
                : ItemStack.EMPTY;
        final boolean touchedJournal = events.isJournal(clicked)
                || events.isJournal(menu.getCarried())
                || events.isJournal(swapped);
        final boolean[] canceled = {false};
        events.plugin.journalInventoryClicked(
                worldName(player),
                touchedJournal,
                slot -> slot >= 0
                        && slot < player.getInventory().getContainerSize()
                        && events.isJournal(player.getInventory().getItem(slot)),
                () -> canceled[0] = true,
                menu::broadcastFullState,
                slot -> events.replaceJournal(player, slot));
        return canceled[0];
    }

    public static boolean onCreativeInventorySlot(
            final ServerPlayer player,
            final int menuSlot,
            final ItemStack replacement) {
        final NeoForgeObjectiveEvents events = activeEvents;
        if (events == null || player == null) {
            return false;
        }
        final ItemStack current = player.inventoryMenu.isValidSlotIndex(menuSlot)
                ? player.inventoryMenu.getSlot(menuSlot).getItem()
                : ItemStack.EMPTY;
        final boolean touchedJournal = events.isJournal(current) || events.isJournal(replacement);
        final boolean[] canceled = {false};
        events.plugin.journalInventoryClicked(
                worldName(player),
                touchedJournal,
                slot -> slot >= 0
                        && slot < player.getInventory().getContainerSize()
                        && events.isJournal(player.getInventory().getItem(slot)),
                () -> canceled[0] = true,
                player.inventoryMenu::broadcastFullState,
                slot -> events.replaceJournal(player, slot));
        return canceled[0];
    }

    private void onBreakBlock(final BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || event.getLevel().isClientSide()
                || event.isCanceled()
                || !(event.getLevel() instanceof Level level)) {
            return;
        }
        plugin.playerBrokeBlock(
                platformPlayer(player),
                blockKey(level, event.getPos()),
                materialId(event.getState()),
                materialId(level.getBlockState(event.getPos().below()))
                        .equals(materialId(event.getState())),
                ageableAtMaxAge(event.getState()),
                true);
    }

    private void onPlaceBlock(final BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || event.isCanceled()) {
            return;
        }
        final BlockState placedBlock = event.getPlacedBlock();
        plugin.playerPlacedBlock(
                platformPlayer(player),
                blockKey(player.level(), event.getPos()),
                materialId(placedBlock),
                ageableAtMaxAge(placedBlock));
    }

    private void onPickupItemPre(final ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        plugin.journalItemPickedUpOrDropped(
                worldName(player),
                isJournal(event.getItemEntity().getItem()),
                () -> event.setCanPickup(TriState.FALSE));
    }

    private void onPickupItemPost(final ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        final ItemStack pickedUp = event.getOriginalStack().copy();
        pickedUp.shrink(event.getCurrentStack().getCount());
        if (!pickedUp.isEmpty()) {
            plugin.playerPickedUpItem(platformPlayer(player), itemEvent(pickedUp));
        }
    }

    private void onTossItem(final ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        plugin.journalItemPickedUpOrDropped(
                worldName(player),
                isJournal(event.getEntity().getItem()),
                () -> {
                    final ItemStack restored = event.getEntity().getItem().copy();
                    event.setCanceled(true);
                    player.getInventory().add(restored);
                    if (!restored.isEmpty()) {
                        final MinecraftServer currentServer = server.get();
                        if (currentServer != null) {
                            currentServer.execute(() -> {
                                if (player.containerMenu.getCarried().isEmpty()) {
                                    player.containerMenu.setCarried(restored);
                                } else {
                                    player.getInventory().placeItemBackInInventory(restored);
                                }
                                player.containerMenu.broadcastChanges();
                            });
                        }
                    }
                    player.inventoryMenu.broadcastChanges();
                });
        if (event.isCanceled()) {
            return;
        }
        plugin.playerDroppedItem(platformPlayer(player), itemEvent(event.getEntity().getItem()));
    }

    private void onLivingDeath(final LivingDeathEvent event) {
        final ServerPlayer dead = event.getEntity() instanceof ServerPlayer player ? player : null;
        final ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
        final ServerPlayer dispatchPlayer = killer == null ? dead : killer;
        if (dispatchPlayer != null) {
            plugin.entityDied(
                    dead == null ? null : platformPlayer(dead),
                    dead == null ? null : () -> damageTypeId(event.getSource()),
                    killer == null ? null : platformPlayer(killer),
                    killer == null ? null : new NeoForgeEntityEvent(event.getEntity(), event.getEntity() == killer));
        }
    }

    private void onLivingDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        journalDeathWorlds.put(player.getUUID(), worldName(player));
        final boolean journalInDrops = event.getDrops().stream()
                .anyMatch(drop -> isJournal(drop.getItem()));
        plugin.journalPlayerDied(
                worldName(player),
                journalInDrops,
                () -> event.getDrops().removeIf(drop -> isJournal(drop.getItem())));
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            return;
        }
        plugin.playerJoined(
                player.getUUID().toString(),
                ignored -> new NeoForgePlayer(
                        plugin, player, currentServer, text, beamTracker, guiRenderer),
                () -> currentServer.getPlayerList().getPlayer(player.getUUID()) == player,
                ignored -> {},
                () -> currentServer.getPlayerList().isOp(player.nameAndId()),
                runAsync,
                currentServer::execute);
        plugin.journalPlayerJoined(
                worldName(player),
                slot -> replaceJournal(player, slot));
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            journalDeathWorlds.remove(player.getUUID());
            journalBlockUseTicks.remove(player.getUUID());
            plugin.playerLeft(
                    player.getUUID().toString(),
                    platformPlayer(player),
                    NeoForgeWorldNames.displayName(player.level().dimension().identifier()),
                    () -> {
                        beamTracker.removePlayer(player);
                        NeoForgePlayer.removePlayer(player);
                    },
                    runAsync);
        }
    }

    private void onPlayerRespawned(final PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        registerPlayerHandle(player);
        final String deathWorld = journalDeathWorlds.remove(player.getUUID());
        if (deathWorld == null) {
            return;
        }
        plugin.journalPlayerRespawned(
                deathWorld,
                worldName(player),
                slot -> slot >= 0
                        && slot < player.getInventory().getContainerSize()
                        && isJournal(player.getInventory().getItem(slot)),
                slot -> replaceJournal(player, slot));
    }

    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            plugin.playerChangedWorld(
                    platformPlayer(player),
                    NeoForgeWorldNames.displayName(event.getFrom().identifier()),
                    NeoForgeWorldNames.displayName(event.getTo().identifier()));
            plugin.playerChunkLoaded(platformPlayer(player));
            plugin.journalPlayerJoined(
                    NeoForgeWorldNames.displayName(event.getTo().identifier()),
                    slot -> replaceJournal(player, slot));
        }
    }

    private void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && isJournal(event.getItemStack())) {
            journalBlockUseTicks.put(player.getUUID(), player.level().getGameTime());
            useJournal(player, event.getItemStack());
        }
        final boolean buriedTreasure = event.getLevel().getBlockEntity(event.getPos())
                        instanceof RandomizableContainerBlockEntity container
                && BuiltInLootTables.BURIED_TREASURE.equals(container.getLootTable());
        final NeoForgeInteractionEvent interaction = new NeoForgeInteractionEvent(
                location(event.getLevel(), event.getPos()),
                false,
                true,
                () -> {
                    event.setCancellationResult(InteractionResult.CONSUME);
                    event.setCanceled(true);
                });
        plugin.playerInteractedWithBlock(platformPlayer(player), interaction, buriedTreasure);
    }

    private void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !isJournal(event.getItemStack())) {
            return;
        }
        final Long blockUseTick = journalBlockUseTicks.remove(player.getUUID());
        if (blockUseTick != null && blockUseTick == player.level().getGameTime()) {
            return;
        }
        useJournal(player, event.getItemStack());
    }

    private void onRightClickEntity(final PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final boolean npcHandled = event.getTarget() instanceof ArmorStand
                && npcAttachments != null
                && npcAttachments.handleEntityInteraction(
                        platformPlayer(player), event.getTarget(), event.getItemStack());
        if (npcHandled) {
            event.setCanceled(true);
        }
        if (!npcHandled && event.getTarget() instanceof Animal animal) {
            final boolean wasInLove = animal.isInLove();
            final MinecraftServer currentServer = server.get();
            if (currentServer != null) {
                currentServer.execute(() -> {
                    if (!wasInLove && animal.isAlive() && animal.isInLove()) {
                        plugin.playerInteractedWithEntity(
                                platformPlayer(player),
                                false,
                                new NeoForgeEntityEvent(animal, false),
                                null,
                                null);
                    }
                });
            }
        }
        final ItemStack itemStack = event.getItemStack();
        final NeoForgeMilkCowEvent milkCow = event.getTarget() instanceof Cow && itemStack.is(Items.BUCKET)
                ? new NeoForgeMilkCowEvent(
                    () -> {
                        event.setCancellationResult(InteractionResult.CONSUME);
                        event.setCanceled(true);
                    })
                : null;
        final NeoForgeShearSheepEvent shearSheep = event.getTarget() instanceof Sheep sheep
                && itemStack.is(Items.SHEARS)
                && !sheep.isSheared()
                ? new NeoForgeShearSheepEvent(
                    () -> {
                        event.setCancellationResult(InteractionResult.CONSUME);
                        event.setCanceled(true);
                    })
                : null;
        plugin.playerInteractedWithEntity(platformPlayer(player), npcHandled, null, milkCow, shearSheep);
    }

    private void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getLevel().isClientSide()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        final NeoForgeInteractionEvent interaction = new NeoForgeInteractionEvent(
                location(event.getLevel(), event.getPos()),
                true,
                false,
                () -> event.setCanceled(true));
        plugin.playerInteractedWithBlock(platformPlayer(player), interaction);
    }

    private void onPlayerTick(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        plugin.playerTick(
                platformPlayer(player),
                location(player.level(), player.blockPosition()),
                player.isShiftKeyDown());
    }

    private void onProjectileImpact(final ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow) || !(event.getProjectile().getOwner() instanceof ServerPlayer player)) {
            return;
        }
        final HitResult ray = event.getRayTraceResult();
        plugin.playerProjectileHit(
                platformPlayer(player), () -> location(event.getProjectile().level(), ray.getLocation()));
    }

    private void onBrewedPotion(final PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerTookBrewedItem(platformPlayer(player), itemEvent(event.getStack()));
    }

    private void onItemCrafted(final PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerCraftedItem(platformPlayer(player), itemEvent(event.getCrafting()));
    }

    private void onItemSmelted(final PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerTookSmeltedItem(platformPlayer(player), itemEvent(event.getSmelting()));
    }

    private void onItemConsumed(final LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerConsumedItem(platformPlayer(player), itemEvent(event.getItem()));
    }

    private void onItemUseStarted(final LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            plugin.playerStartedConsumingItem(platformPlayer(player), () -> event.setCanceled(true));
        }
    }

    private void onItemFished(final ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (final ItemStack drop : event.getDrops()) {
            plugin.playerFishedItem(platformPlayer(player), itemEvent(drop));
        }
    }

    private void onTradeWithVillager(final TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerTradedItem(platformPlayer(player), itemEvent(event.getMerchantOffer().getResult()));
    }

    private void onBreedEntity(final BabyEntitySpawnEvent event) {
        final Player source = event.getCausedByPlayer();
        final AgeableMob child = event.getChild();
        if (!(source instanceof ServerPlayer player) || child == null) {
            return;
        }
        plugin.playerBredEntity(platformPlayer(player), new NeoForgeEntityEvent(child, false));
    }

    private void onTameEntity(final AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerTamedEntity(platformPlayer(player), new NeoForgeEntityEvent(event.getAnimal(), false));
    }

    private void onPlayerJump(final LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        plugin.playerJumped(platformPlayer(player));
    }

    private void onEnchantItem(final PlayerEnchantItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            return;
        }
        plugin.playerEnchantedItem(
                platformPlayer(player),
                new NeoForgeEnchantEvent(
                        plugin,
                        currentServer.registryAccess(),
                        event.getEnchantedItem(),
                        event.getEnchantments()));
    }

    private void onCommand(final CommandEvent event) {
        final ServerPlayer player;
        try {
            player = event.getParseResults().getContext().getSource().getPlayerOrException();
        } catch (final Exception exception) {
            return;
        }
        plugin.playerRanCommand(
                platformPlayer(player),
                event.getParseResults().getReader().getString(),
                () -> event.setCanceled(true));
    }

    private void onServerChat(final ServerChatEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (plugin.playerChatted(platformPlayer(event.getPlayer()), event.getMessage().getString())) {
            event.setCanceled(true);
        }
    }

    private PlatformPlayer platformPlayer(final ServerPlayer player) {
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            throw new IllegalStateException("Cannot create a NotQuests player adapter before the server is available.");
        }
        final PlatformPlayer current = plugin.activePlatformPlayer(player.getUUID().toString());
        if (current instanceof NeoForgePlayer neoForgePlayer
                && neoForgePlayer.player() == player) {
            return current;
        }
        return registerPlayerHandle(player);
    }

    private PlatformPlayer registerPlayerHandle(final ServerPlayer player) {
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            throw new IllegalStateException("Cannot register a NotQuests player before the server is available.");
        }
        return plugin.registerQuestPlayer(
                new NeoForgePlayer(
                        plugin, player, currentServer, text, beamTracker, guiRenderer),
                plugin.activeProfile(player.getUUID().toString()),
                true);
    }

    private void useJournal(final ServerPlayer player, final ItemStack usedItem) {
        plugin.journalItemUsed(
                platformPlayer(player),
                true,
                isJournal(usedItem));
    }

    private boolean isJournal(final ItemStack item) {
        final ItemStack journal = journalItem.get();
        return item != null
                && journal != null
                && !item.isEmpty()
                && !journal.isEmpty()
                && ItemStack.isSameItemSameComponents(item, journal);
    }

    private void replaceJournal(final ServerPlayer player, final int slot) {
        final ItemStack journal = journalItem.get();
        if (journal == null || journal.isEmpty() || slot < 0
                || slot >= player.getInventory().getContainerSize()) {
            return;
        }
        for (int inventorySlot = 0;
                inventorySlot < player.getInventory().getContainerSize();
                inventorySlot++) {
            if (isJournal(player.getInventory().getItem(inventorySlot))) {
                player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
            }
        }
        player.getInventory().setItem(slot, journal.copy());
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static String worldName(final ServerPlayer player) {
        return NeoForgeWorldNames.displayName(player.level().dimension().identifier());
    }

    private static boolean ageableAtMaxAge(final BlockState state) {
        for (final Property<?> property : state.getProperties()) {
            if (property instanceof final IntegerProperty integerProperty
                    && integerProperty.getName().equals("age")) {
                final int age = state.getValue(integerProperty);
                final int maxAge = integerProperty.getPossibleValues().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(age);
                return age >= maxAge;
            }
        }
        return false;
    }

    private static String blockKey(final Level level, final BlockPos pos) {
        return ActiveObjectives.blockKey(
                level.dimension().identifier().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ());
    }

    private static NQLocation location(final Level level, final BlockPos pos) {
        return NQLocation.at(
                NeoForgeWorldNames.displayName(level.dimension().identifier()),
                pos.getX(),
                pos.getY(),
                pos.getZ());
    }

    private static NQLocation location(final Level level, final Vec3 pos) {
        return NQLocation.at(
                NeoForgeWorldNames.displayName(level.dimension().identifier()),
                pos.x(),
                pos.y(),
                pos.z());
    }

    private static String materialId(final BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String itemId(final ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private NeoForgeItemEvent itemEvent(final ItemStack stack) {
        final MinecraftServer currentServer = server.get();
        return new NeoForgeItemEvent(
                plugin,
                currentServer == null ? null : currentServer.registryAccess(),
                stack.copy());
    }

    private record NeoForgeItemEvent(
            NotQuestsPlugin plugin,
            RegistryAccess registryAccess,
            ItemStack stack) implements Objectives.ItemEvent {
        @Override
        public String materialId() {
            return itemId(stack);
        }

        @Override
        public int amount() {
            return stack.getCount();
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return NeoForgePlayer.matchesItem(plugin, registryAccess, selection, stack);
        }
    }

    private record NeoForgeEntityEvent(
            LivingEntity entity,
            boolean selfAttributedDeath) implements Objectives.EntityEvent {
        @Override
        public String entityTypeId() {
            return suggestionId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        }

        @Override
        public String plainCustomName() {
            return entity.getCustomName() == null ? "" : entity.getCustomName().getString();
        }
    }

    private record NeoForgeEnchantEvent(
            NotQuestsPlugin plugin,
            RegistryAccess registryAccess,
            ItemStack stack,
            List<EnchantmentInstance> nativeEnchantments)
            implements Objectives.EnchantEvent {
        @Override
        public String materialId() {
            return itemId(stack);
        }

        @Override
        public int amount() {
            return stack.getCount();
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return NeoForgePlayer.matchesItem(plugin, registryAccess, selection, stack);
        }

        @Override
        public Map<String, Integer> enchantments() {
            final Map<String, Integer> result = new LinkedHashMap<>();
            for (final EnchantmentInstance enchantment : nativeEnchantments) {
                final String key = enchantment.enchantment()
                        .unwrapKey()
                        .map(resourceKey -> resourceKey.identifier().toString())
                        .orElse("");
                if (!key.isBlank()) {
                    result.put(key, enchantment.level());
                }
            }
            return Map.copyOf(result);
        }
    }

    private record NeoForgeInteractionEvent(
            NQLocation location,
            boolean leftClick,
            boolean rightClick,
            Runnable cancelCallback)
            implements Objectives.InteractionEvent {
        @Override
        public void cancel() {
            cancelCallback.run();
        }
    }

    private record NeoForgeMilkCowEvent(Runnable cancelCallback)
            implements Objectives.MilkCowEvent {
        @Override
        public void cancel() {
            cancelCallback.run();
        }
    }

    private record NeoForgeShearSheepEvent(Runnable cancelCallback)
            implements Objectives.ShearSheepEvent {
        @Override
        public void cancel() {
            cancelCallback.run();
        }
    }

    void clear() {
        activeEvents = null;
        journalDeathWorlds.clear();
        journalBlockUseTicks.clear();
    }

    private static String damageTypeId(final DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(resourceKey -> suggestionId(resourceKey.identifier()))
                .orElseGet(source::getMsgId);
    }

    private static String suggestionId(final Identifier identifier) {
        return identifier == null
                ? ""
                : "minecraft".equals(identifier.getNamespace()) ? identifier.getPath() : identifier.toString();
    }
}
