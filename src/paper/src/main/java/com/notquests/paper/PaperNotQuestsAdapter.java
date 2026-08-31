package com.notquests.paper;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;

import com.notquests.core.commands.framework.NQDescription;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Registry;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;
import com.notquests.core.variables.VariableDataType;
import com.notquests.paper.PaperItems.Selection;
import com.notquests.paper.adapter.config.BukkitConfigurationValueCodec;
import com.notquests.paper.events.notquests.ObjectiveUnlockEvent;
import com.notquests.paper.npc.ArmorstandNPC;
import com.notquests.paper.npc.CitizensNPC;
import com.notquests.paper.npc.FancyNPC;
import com.notquests.paper.npc.NQNPC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class PaperNotQuestsAdapter extends NotQuestsAdapter.Platform {
    private final NotQuests main;

    public PaperNotQuestsAdapter(final NotQuests main) {
        super(main.getCorePlugin());
        this.main = main;
    }

    public PaperPlayer activePaperPlayer(final UUID playerId) {
        return playerId == null ? null : asPaperPlayer(main.getCorePlugin().activePlatformPlayer(playerId.toString()));
    }

    public PaperPlayer createPaperPlayer(final String playerIdentifier) {
        if (playerIdentifier == null) {
            return null;
        }
        try {
            return new PaperPlayer(main, UUID.fromString(playerIdentifier));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public List<String> damageTypeIds() {
        final List<String> completions = new ArrayList<>();
        final var damageTypeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
        for (final DamageType type : damageTypeRegistry) {
            completions.add(suggestionId(damageTypeRegistry.getKeyOrThrow(type)));
        }
        return completions;
    }

    @Override
    public List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public PaperPlayer onlineQuestPlayer(final String playerName) {
        final Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return null;
        }
        return asPaperPlayer(main.getCorePlugin().getOrCreatePlatformPlayer(
                player.getUniqueId().toString()));
    }

    @Override
    public List<String> worldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    @Override
    public String serverBrand() {
        return Bukkit.getServer().getName();
    }

    @Override
    public boolean supportsWorldEditSelection() {
        return main.integrations() != null && main.integrations().worldEdit() != null;
    }

    @Override
    public LocationRegion worldEditSelection(final PlatformPlayer questPlayer) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        if (paperPlayer == null
                || paperPlayer.getPlayer() == null
                || main.integrations() == null
                || main.integrations().worldEdit() == null) {
            return null;
        }
        return main.integrations().worldEdit().getSelectionRegionOrNull(paperPlayer.getPlayer());
    }

    @Override
    protected List<String> itemMaterialIds() {
        final List<String> materials = new ArrayList<>();
        for (final Material material : Material.values()) {
            if (material.isItem() && !material.isAir() && !material.isLegacy()) {
                materials.add(material.name().toLowerCase(Locale.ROOT));
            }
        }
        return materials;
    }

    @Override
    protected boolean nativeItemSelectionsAreSimilar(
            final ItemSelection required,
            final ItemSelection actual) {
        final Selection actualItems = materializeItems(List.of(
                new SavedItems.ItemChoice(actual, "")));
        final Selection requiredItems = materializeItems(List.of(
                new SavedItems.ItemChoice(required, "")));
        if (requiredItems == null || actualItems == null) {
            return false;
        }
        for (final ItemStack requiredItem : requiredItems.toItemStackList()) {
            for (final ItemStack actualItem : actualItems.toItemStackList()) {
                if (requiredItem.isSimilar(actualItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected ItemSelection heldItemSelection(final PlatformPlayer questPlayer) {
        final Player player = bukkitPlayer(questPlayer);
        if (player == null || player.getInventory().getItemInMainHand().getType().isAir()) {
            return null;
        }
        final Selection heldItem = new Selection(1);
        heldItem.addItemStack(player.getInventory().getItemInMainHand());
        return heldItem;
    }

    @Override
    protected String nativeItemMaterialId(final String input) {
        final Material material = parseCommandMaterial(input);
        return material == null ? null : material.getKey().getKey();
    }

    @Override
    protected List<String> nativeEntityTypeIds() {
        final List<String> completions = new ArrayList<>();
        for (final EntityType entityType : EntityType.values()) {
            if (entityType != EntityType.UNKNOWN) {
                completions.add(suggestionId(entityType.getKey()));
            }
        }
        if (main.integrations().mythicMobs() != null) {
            completions.addAll(main.integrations().mythicMobs().getMobNames());
        }
        if (main.integrations().ecoMobs() != null) {
            completions.addAll(main.integrations().ecoMobs().getMobNames());
        }
        return completions;
    }

    @Override
    public List<String> particleTypeIds() {
        final List<String> particles = new ArrayList<>();
        for (final Particle particle : Particle.values()) {
            if (particle.getDataType() == Void.class) {
                particles.add(particle.getKey().getKey().toLowerCase(Locale.ROOT));
            }
        }
        return particles;
    }

    @Override
    public List<String> soundTypeIds() {
        final List<String> sounds = new ArrayList<>();
        final var soundRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);
        for (final Sound sound : soundRegistry) {
            sounds.add(suggestionId(soundRegistry.getKeyOrThrow(sound)));
        }
        return sounds;
    }

    @Override
    public List<String> soundCategoryIds() {
        final List<String> categories = new ArrayList<>();
        for (final SoundCategory category : SoundCategory.values()) {
            categories.add(category.name().toLowerCase(Locale.ROOT));
        }
        return categories;
    }

    @Override
    public List<String> statisticIds() {
        final List<String> statistics = new ArrayList<>();
        for (final Statistic statistic : Statistic.values()) {
            if (statistic.getType() == Statistic.Type.UNTYPED) {
                statistics.add(statistic.name().toLowerCase(Locale.ROOT));
            }
        }
        return statistics;
    }

    @Override
    public List<String> advancementIds() {
        final List<String> advancements = new ArrayList<>();
        final Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
        while (iterator.hasNext()) {
            final Advancement advancement = iterator.next();
            advancements.add(advancement.getKey().asString());
        }
        return advancements;
    }

    @Override
    protected List<String> blockMaterialIds() {
        final List<String> materials = new ArrayList<>();
        for (final Material material : Material.values()) {
            if (material.isBlock() && !material.isAir() && !material.isLegacy()) {
                materials.add(material.name().toLowerCase(Locale.ROOT));
            }
        }
        return materials;
    }

    @Override
    public List<String> inventorySlotIds() {
        final List<String> slots = new ArrayList<>();
        for (final EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            slots.add(equipmentSlot.name());
        }
        for (int slot = 0; slot <= 35; slot++) {
            slots.add(String.valueOf(slot));
        }
        return slots;
    }

    @Override
    public List<String> enchantmentIds() {
        return Arrays.stream(Enchantment.values())
                .map(enchantment -> suggestionId(enchantment.getKey()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        return paperPlayer != null
                && paperPlayer.getPlayer() != null
                && permission != null
                && paperPlayer.getPlayer().hasPermission(permission);
    }

    @Override
    public boolean supportsArbitraryPermissionChecks() {
        return true;
    }

    @Override
    public boolean supportsPermissionMutation() {
        return main.integrations().luckPerms() != null;
    }

    @Override
    public boolean setPermission(
            final PlatformPlayer questPlayer, final String permission, final boolean value) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        if (paperPlayer == null
                || permission == null
                || permission.isBlank()
                || main.integrations().luckPerms() == null) {
            return false;
        }
        if (value) {
            main.integrations().luckPerms().givePermission(paperPlayer.getUniqueId(), permission);
        } else {
            main.integrations().luckPerms().denyPermission(paperPlayer.getUniqueId(), permission);
        }
        return true;
    }

    @Override
    public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        final Statistic statistic = paperStatistic(statisticId);
        if (paperPlayer == null || paperPlayer.getPlayer() == null || statistic == null) {
            return 0;
        }
        return paperPlayer.getPlayer().getStatistic(statistic);
    }

    @Override
    public boolean setPlayerStatistic(
            final PlatformPlayer questPlayer,
            final String statisticId,
            final int value) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        final Statistic statistic = paperStatistic(statisticId);
        if (paperPlayer == null || paperPlayer.getPlayer() == null || statistic == null || value < 0) {
            return false;
        }
        paperPlayer.getPlayer().setStatistic(statistic, value);
        return true;
    }

    @Override
    public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        final Advancement advancement = paperAdvancement(advancementId);
        return paperPlayer != null
                && paperPlayer.getPlayer() != null
                && advancement != null
                && paperPlayer.getPlayer().getAdvancementProgress(advancement).isDone();
    }

    @Override
    public boolean setAdvancement(
            final PlatformPlayer questPlayer,
            final String advancementId,
            final boolean completed) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        final Advancement advancement = paperAdvancement(advancementId);
        if (paperPlayer == null || paperPlayer.getPlayer() == null || advancement == null) {
            return false;
        }
        final AdvancementProgress progress = paperPlayer.getPlayer().getAdvancementProgress(advancement);
        if (completed) {
            for (final String criterion : progress.getRemainingCriteria()) {
                progress.awardCriteria(criterion);
            }
        } else {
            for (final String criterion : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criterion);
            }
        }
        return true;
    }

    @Override
    public String blockMaterial(final NQLocation location) {
        final Location paperLocation = paperBukkitLocation(location);
        if (paperLocation == null || paperLocation.getWorld() == null) {
            return "";
        }
        return paperLocation.getBlock().getType().name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected boolean setNativeBlockMaterial(
            final NQLocation location,
            final String materialId) {
        final Location paperLocation = paperBukkitLocation(location);
        final Material material = Material.matchMaterial(materialId);
        if (paperLocation == null || paperLocation.getWorld() == null || material == null || !material.isBlock()) {
            return false;
        }
        paperLocation.getBlock().setType(material);
        return true;
    }

    @Override
    protected String heldBlockMaterial(final PlatformPlayer questPlayer) {
        final Player player = bukkitPlayer(questPlayer);
        return player == null || player.getInventory().getItemInMainHand().getType().isAir()
                ? null
                : player.getInventory().getItemInMainHand().getType().getKey().asString();
    }

    @Override
    protected String itemSelectionBlockMaterial(final List<SavedItems.ItemChoice> items) {
        final Selection selection = materializeItems(items);
        final ItemStack itemStack = selection == null ? null : selection.toFirstItemStack();
        return itemStack == null || !itemStack.getType().isBlock()
                ? null
                : itemStack.getType().getKey().asString();
    }

    @Override
    public List<ItemSelection> containerInventoryItems(final NQLocation location) {
        final Container container = container(location);
        if (container == null) {
            return List.of();
        }
        final ArrayList<ItemSelection> items = new ArrayList<>();
        for (final ItemStack itemStack : container.getInventory().getStorageContents()) {
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
    public boolean addContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items,
            final boolean dropOverflow) {
        final Container container = container(location);
        final Location paperLocation = paperBukkitLocation(location);
        if (container == null || paperLocation == null || paperLocation.getWorld() == null) {
            return false;
        }
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = materializeItems(List.of(item));
            if (selection == null) {
                continue;
            }
            final List<ItemStack> stacks = selection.toItemStackList();
            final int requested = stacks.stream().mapToInt(ItemStack::getAmount).sum();
            final HashMap<Integer, ItemStack> leftovers =
                    container.getInventory().addItem(stacks.toArray(new ItemStack[0]));
            final int remaining = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (dropOverflow) {
                for (final ItemStack leftover : leftovers.values()) {
                    paperLocation.getWorld().dropItem(paperLocation, leftover);
                }
            }
            changed |= requested > remaining || dropOverflow && remaining > 0;
        }
        return changed;
    }

    @Override
    public boolean removeContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items) {
        final Container container = container(location);
        if (container == null) {
            return false;
        }
        return removeItems(container.getInventory(), items);
    }

    @Override
    public boolean setContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items) {
        final Container container = container(location);
        if (container == null) {
            return false;
        }
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = materializeItems(List.of(item));
            if (selection == null) {
                return false;
            }
            final List<ItemStack> selectedStacks = selection.toItemStackList();
            if (selectedStacks.isEmpty()) {
                return false;
            }
            selectedStacks.forEach(stack -> stacks.add(stack.clone()));
        }
        container.getInventory().clear();
        return container.getInventory().addItem(stacks.toArray(ItemStack[]::new)).isEmpty();
    }

    private boolean removeItems(
            final Inventory inventory,
            final List<SavedItems.ItemChoice> items) {
        boolean changed = false;
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            final Selection selection = materializeItems(List.of(item));
            if (selection == null) {
                continue;
            }
            int remaining = Math.max(1, item.selection().amount());
            final ItemStack[] contents = inventory.getContents();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                final ItemStack stack = contents[slot];
                if (stack == null || stack.getType().isAir()
                        || !itemSelectionsAreSimilar(selection, nativeItemSelection(stack))) {
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

    @Override
    public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        final ItemStack itemStack = inventoryItem(paperPlayer, slotId);
        if (itemStack == null) {
            return List.of();
        }
        return itemStack.getEnchantments().keySet().stream()
                .map(enchantment -> suggestionId(enchantment.getKey()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public boolean supportsNpcAttachments() {
        return true;
    }

    @Override
    public boolean supportsArmorStandAttachmentTools() {
        return true;
    }

    @Override
    protected List<String> nativeNpcSelectorIds() {
        final ArrayList<String> selectors = new ArrayList<>();
        if (main.integrations().citizens() != null) {
            for (final int npcId : main.integrations().citizens().getAllNPCIDs()) {
                selectors.add("citizens:" + npcId);
            }
        }
        if (main.integrations().fancyNpcs() != null) {
            for (final String npcId : main.integrations().fancyNpcs().getAllNPCIds()) {
                selectors.add("fancynpcs:" + npcId);
            }
        }
        return List.copyOf(selectors);
    }

    @Override
    protected NativeNpc nativeNpc(final String npcType, final NQNPCID npcId) {
        final NQNPC npc = paperNpc(npcType, npcId);
        return npc == null ? null : new NativeNpc(npc.getNPCType(), npc.getID(), npc.getName());
    }

    @Override
    public List<String> nativeNpcQuestGiverTypes() {
        return List.of("citizens");
    }

    @Override
    public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
        final NQNPC npc = selection == null ? null : paperNpc(selection.npcType(), selection.npcId());
        return npc instanceof CitizensNPC citizens && citizens.setQuestGiverTrait(enabled);
    }

    @Override
    public boolean giveArmorStandTool(
            final PlatformPlayer actor,
            final ArmorStandToolItem tool) {
        final Player player = bukkitPlayer(actor);
        if (player == null) {
            return false;
        }
        final Material material = parseCommandMaterial(tool == null ? "" : tool.materialId());
        if (tool == null || tool.itemId() < 0 || material == null || !material.isItem() || material.isAir()) {
            return false;
        }
        return giveSpecialArmorStandTool(
                player,
                material,
                tool.itemId(),
                tool.questName(),
                tool.displayName(),
                tool.loreLines());
    }

    @Override
    public boolean giveNpcSelectionTool(
            final PlatformPlayer actor,
            final int selectionId,
            final String displayName,
            final List<String> lore) {
        final Player player = bukkitPlayer(actor);
        if (player == null) {
            return false;
        }
        final ItemStack itemStack = new ItemStack(Material.PAPER);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        if (displayName != null && !displayName.isBlank()) {
            itemMeta.displayName(main.parse(displayName));
        }
        itemMeta.lore((lore == null ? List.<String>of() : lore).stream()
                .map(main::parse)
                .toList());
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemMeta.getPersistentDataContainer().set(
                new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action"),
                PersistentDataType.INTEGER,
                selectionId);
        itemStack.setItemMeta(itemMeta);
        return giveItem(player, itemStack);
    }

    @Override
    public void broadcast(final String miniMessage) {
        Bukkit.broadcast(main.parse(miniMessage));
    }

    @Override
    public void dispatchConsoleCommand(final String command) {
        final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        final String nativeCommand = command.startsWith("/") ? command.substring(1) : command;
        final Runnable dispatch = () -> Bukkit.dispatchCommand(console, nativeCommand);
        if (Bukkit.isPrimaryThread()) {
            dispatch.run();
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), dispatch);
        }
    }

    public boolean itemSelectionIncludesItemStack(final ItemSelection itemSelection, final ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        return itemSelectionsAreSimilar(itemSelection, nativeItemSelection(itemStack));
    }

    @Override
    public NQLocation location(final String worldName, final double x, final double y, final double z) {
        final World world = Bukkit.getWorld(worldName);
        return world == null ? null : paperNQLocation(new Location(world, x, y, z));
    }

    @Override
    public void schedule(final Duration delay, final Runnable action) {
        if (delay == null || delay.isNegative() || delay.isZero()) {
            Bukkit.getScheduler().runTask(main.getMain(), action);
            return;
        }
        final long ticks = Math.max(1L, Math.round(delay.toMillis() / 50.0d));
        Bukkit.getScheduler().runTaskLater(main.getMain(), action, ticks);
    }

    @Override
    public boolean isServerThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public <T> T callOnServerThread(final Callable<T> action) throws Exception {
        if (Bukkit.isPrimaryThread()) {
            return action.call();
        }
        return Bukkit.getScheduler().callSyncMethod(main.getMain(), action).get();
    }

    @Override
    public boolean allowObjectiveUnlock(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final ActiveObjective objective,
            final boolean triggerAcceptQuestTrigger) {
        if (quest == null || objective == null) {
            return false;
        }
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        if (paperPlayer == null) {
            return false;
        }
        try {
            return callOnServerThread(() -> {
                final ObjectiveUnlockEvent event = new ObjectiveUnlockEvent(
                        paperPlayer,
                        quest.getIdentifier(),
                        objective.getObjectivePath(),
                        objective.getObjectiveID(),
                        objective.getHolderPath(),
                        quest,
                        objective,
                        triggerAcceptQuestTrigger);
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            });
        } catch (final Exception exception) {
            throw new IllegalStateException("Could not dispatch ObjectiveUnlockEvent", exception);
        }
    }

    private PaperPlayer resolvePaperPlayer(final PlatformPlayer questPlayer) {
        final PaperPlayer direct = asPaperPlayer(questPlayer);
        if (direct != null) {
            return direct;
        }
        if (questPlayer == null) {
            return null;
        }
        Player player = null;
        try {
            player = Bukkit.getPlayer(UUID.fromString(questPlayer.playerIdentifier()));
        } catch (final IllegalArgumentException ignored) {
            // Fall through to name lookup.
        }
        if (player == null && questPlayer.playerName() != null && !questPlayer.playerName().isBlank()) {
            player = Bukkit.getPlayerExact(questPlayer.playerName());
        }
        return player == null
                ? null
                : asPaperPlayer(main.getCorePlugin().getOrCreatePlatformPlayer(
                        player.getUniqueId().toString()));
    }

    public static PaperPlayer asPaperPlayer(final PlatformPlayer questPlayer) {
        return questPlayer instanceof PaperPlayer qp ? qp : null;
    }

    private Player bukkitPlayer(final PlatformPlayer questPlayer) {
        final PaperPlayer paperPlayer = resolvePaperPlayer(questPlayer);
        return paperPlayer == null ? null : paperPlayer.getPlayer();
    }

    private NQNPC paperNpc(final String npcType, final NQNPCID npcId) {
        if (npcType == null || npcId == null) {
            return null;
        }
        final NQNPC npc;
        if ("armorstand".equalsIgnoreCase(npcType)) {
            npc = new ArmorstandNPC(main, npcId);
        } else if ("citizens".equalsIgnoreCase(npcType)
                && main.integrations().citizens() != null) {
            npc = new CitizensNPC(main, npcId);
        } else if ("fancynpcs".equalsIgnoreCase(npcType)
                && main.integrations().fancyNpcs() != null) {
            npc = new FancyNPC(main, npcId);
        } else {
            return null;
        }
        return npc.getName() == null ? null : npc;
    }

    private boolean giveSpecialArmorStandTool(
            final Player player,
            final Material material,
            final int itemId,
            final String questName,
            final String displayName,
            final List<String> loreLines) {
        final ItemStack itemStack = new ItemStack(material, 1);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return giveItem(player, itemStack);
        }
        itemMeta.displayName(main.parse(displayName));
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        final ArrayList<Component> lore = new ArrayList<>();
        for (final String loreLine : loreLines) {
            lore.add(main.parse(loreLine));
        }
        itemMeta.lore(lore);
        final var container = itemMeta.getPersistentDataContainer();
        container.set(new NamespacedKey(main.getMain(), "notquests-item"), PersistentDataType.INTEGER, itemId);
        if (questName != null && !questName.isBlank()) {
            container.set(new NamespacedKey(main.getMain(), "notquests-questname"), PersistentDataType.STRING, questName);
        }
        itemStack.setItemMeta(itemMeta);
        return giveItem(player, itemStack);
    }

    private static boolean giveItem(final Player player, final ItemStack itemStack) {
        final Map<Integer, ItemStack> overflow = player.getInventory().addItem(itemStack);
        for (final ItemStack remaining : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
        return true;
    }

    private static ItemStack inventoryItem(final PaperPlayer questPlayer, final String slotId) {
        if (questPlayer == null || questPlayer.getPlayer() == null || slotId == null || slotId.isBlank()) {
            return null;
        }
        try {
            final String normalized = slotId.trim().replace('-', '_').toUpperCase(Locale.ROOT);
            final EquipmentSlot equipmentSlot = EquipmentSlot.valueOf(switch (normalized) {
                case "MAINHAND" -> "HAND";
                case "OFFHAND" -> "OFF_HAND";
                default -> normalized;
            });
            return questPlayer.getPlayer().getEquipment().getItem(equipmentSlot);
        } catch (final IllegalArgumentException ignored) {
            // Try numeric inventory slot below.
        }
        try {
            return questPlayer.getPlayer().getInventory().getItem(Integer.parseInt(slotId));
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static Statistic paperStatistic(final String statisticId) {
        if (statisticId == null || statisticId.isBlank()) {
            return null;
        }
        try {
            final Statistic statistic = Statistic.valueOf(statisticId.toUpperCase(Locale.ROOT));
            if (statistic.getType() == Statistic.Type.UNTYPED) {
                return statistic;
            }
        } catch (final IllegalArgumentException ignored) {
            // handled below
        }
        return null;
    }

    private static Advancement paperAdvancement(final String advancementId) {
        if (advancementId == null || advancementId.isBlank()) {
            return null;
        }
        final NamespacedKey key = NamespacedKey.fromString(advancementId);
        if (key == null) {
            return null;
        }
        return Bukkit.getAdvancement(key);
    }

    public static Location paperBukkitLocation(final NQLocation location) {
        if (location == null) {
            return null;
        }
        if (location instanceof final PaperNQLocation paperLocation) {
            return paperLocation.location();
        }
        final World world = Bukkit.getWorld(location.worldName());
        return world == null
                ? null
                : new Location(
                        world,
                        location.x(),
                        location.y(),
                        location.z(),
                        location.yaw(),
                        location.pitch());
    }

    private static Container container(final NQLocation location) {
        final Location paperLocation = paperBukkitLocation(location);
        if (paperLocation == null || paperLocation.getWorld() == null) {
            return null;
        }
        return paperLocation.getBlock().getState() instanceof final Container container
                ? container
                : null;
    }

    private static Material parseCommandMaterial(final String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return null;
        }
        return Material.matchMaterial(materialName.trim());
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

    public Selection materializeItems(final List<SavedItems.ItemChoice> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        final Selection adjusted = new Selection(1);
        for (final SavedItems.ItemChoice choice : choices) {
            if (choice == null || choice.selection() == null) {
                continue;
            }
            final ItemSelection item = choice.selection();
            if (item.any()) {
                adjusted.setAny(true);
                continue;
            }
            final HashSet<String> exactMaterials = new HashSet<>();
            for (final Map<String, Object> exactItem : item.exactItems()) {
                if (!(exactItem.get("platform") instanceof String platform)
                        || !"paper".equalsIgnoreCase(platform)) {
                    continue;
                }
                final Object exactMaterial = exactItem.get("material");
                if (exactMaterial != null) {
                    exactMaterials.add(exactMaterial.toString()
                            .replace("minecraft:", "")
                            .toLowerCase(Locale.ROOT));
                }
                final Object encoded = exactItem.containsKey("data") ? exactItem.get("data") : exactItem;
                final Object decoded = BukkitConfigurationValueCodec.fromYamlValue(encoded);
                if (decoded instanceof final ItemStack itemStack) {
                    final ItemStack nativeItem = itemStack.clone();
                    nativeItem.setAmount(item.amount());
                    applySavedItemDisplayName(nativeItem, choice.displayName());
                    adjusted.addItemStack(nativeItem);
                }
            }
            for (final String materialId : item.materialIds()) {
                final String trimmed = materialId.trim();
                if (trimmed.isBlank()
                        || exactMaterials.contains(trimmed.replace("minecraft:", "").toLowerCase(Locale.ROOT))) {
                    continue;
                }
                final Material material = parseCommandMaterial(trimmed);
                if (material != null) {
                    final ItemStack nativeItem = new ItemStack(material, item.amount());
                    applySavedItemDisplayName(nativeItem, choice.displayName());
                    adjusted.addItemStack(nativeItem);
                }
            }
        }
        return adjusted;
    }

    private void applySavedItemDisplayName(
            final ItemStack itemStack,
            final String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(main.parse("<!italic>" + displayName));
        itemStack.setItemMeta(itemMeta);
    }

    public static ItemSelection nativeItemSelection(final ItemStack itemStack) {
        final Selection selection = new Selection(1);
        selection.addItemStack(itemStack);
        return selection;
    }

    private static String suggestionId(final NamespacedKey key) {
        return key == null
                ? ""
                : NamespacedKey.MINECRAFT.equals(key.getNamespace()) ? key.getKey() : key.asString();
    }

    public static NQLocation paperNQLocation(final Location location) {
        return location == null || location.getWorld() == null ? null : new PaperNQLocation(location);
    }

    public record PaperNQLocation(org.bukkit.Location location) implements NQLocation {
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

    private static String paperText(final String textValue, final Object rawValue) {
        if (textValue != null && !textValue.isBlank()) {
            return textValue;
        }
        if (rawValue instanceof Enchantment enchantment) {
            return enchantment.getKey().getKey();
        }
        return rawValue instanceof String string ? string : "";
    }

}
