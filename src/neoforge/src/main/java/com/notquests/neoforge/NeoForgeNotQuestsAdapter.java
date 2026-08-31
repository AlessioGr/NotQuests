package com.notquests.neoforge;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NQNPCID;
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
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;
import com.notquests.core.variables.VariableDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class NeoForgeNotQuestsAdapter extends NotQuestsAdapter.Platform {
    private final NotQuestsPlugin plugin;
    private final Supplier<MinecraftServer> server;
    private final NeoForgeText text;
    private final NeoForgeNpcAttachments npcAttachments;
    private final NeoForgeBeamTracker beamTracker;
    private final NeoForgeGuiRenderer guiRenderer;
    private final Logger logger;

    NeoForgeNotQuestsAdapter(
            final NotQuestsPlugin plugin,
            final Supplier<MinecraftServer> server,
            final NeoForgeText text,
            final NeoForgeNpcAttachments npcAttachments,
            final NeoForgeBeamTracker beamTracker,
            final NeoForgeGuiRenderer guiRenderer,
            final Logger logger) {
        super(plugin);
        this.server = server;
        this.text = text;
        this.npcAttachments = npcAttachments;
        this.beamTracker = beamTracker;
        this.guiRenderer = guiRenderer;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public List<String> damageTypeIds() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return List.of();
        }
        return minecraftServer.registryAccess()
                .lookup(Registries.DAMAGE_TYPE)
                .map(registry -> registry.keySet().stream()
                        .map(NeoForgeNotQuestsAdapter::suggestionId)
                        .sorted()
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public List<String> onlinePlayerNames() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return List.of();
        }
        return minecraftServer.getPlayerList().getPlayers().stream()
                .map(player -> player.getName().getString())
                .sorted()
                .toList();
    }

    @Override
    public PlatformPlayer onlineQuestPlayer(final String playerName) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null || playerName == null || playerName.isBlank()) {
            return null;
        }
        final ServerPlayer nativePlayer = minecraftServer.getPlayerList().getPlayers().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(playerName)
                        || player.getUUID().toString().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);
        return nativePlayer == null
                ? null
                : plugin.getOrCreatePlatformPlayer(nativePlayer.getUUID().toString());
    }

    public PlatformPlayer createQuestPlayer(final String playerIdentifier) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null || playerIdentifier == null || playerIdentifier.isBlank()) {
            return null;
        }
        final ServerPlayer player;
        try {
            player = minecraftServer.getPlayerList().getPlayer(
                    UUID.fromString(playerIdentifier));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
        return player == null
                ? null
                : new NeoForgePlayer(plugin, player, minecraftServer, text, beamTracker, guiRenderer);
    }

    @Override
    public List<String> worldNames() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return List.of();
        }
        final Set<String> names = new LinkedHashSet<>();
        for (final ServerLevel level : minecraftServer.getAllLevels()) {
            final Identifier identifier = level.dimension().identifier();
            names.add(NeoForgeWorldNames.displayName(identifier));
            names.add(identifier.toString());
        }
        return new ArrayList<>(names);
    }

    @Override
    public boolean supportsWorldEditSelection() {
        return false;
    }

    @Override
    public LocationRegion worldEditSelection(final PlatformPlayer questPlayer) {
        return null;
    }

    @Override
    public String serverBrand() {
        return "NeoForge";
    }

    @Override
    protected List<String> itemMaterialIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .filter(identifier -> BuiltInRegistries.ITEM.get(identifier)
                        .map(reference -> reference.value() != Items.AIR)
                        .orElse(false))
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    protected boolean nativeItemSelectionsAreSimilar(
            final ItemSelection required,
            final ItemSelection actual) {
        final MinecraftServer currentServer = server.get();
        return currentServer != null && NeoForgePlayer.itemSelectionsAreSimilar(
                text,
                currentServer.registryAccess(),
                required,
                actual);
    }

    @Override
    protected ItemSelection heldItemSelection(final PlatformPlayer questPlayer) {
        final ServerPlayer player = serverPlayer(questPlayer);
        final MinecraftServer currentServer = server.get();
        return player == null || currentServer == null || player.getMainHandItem().isEmpty()
                ? null
                : NeoForgePlayer.nativeItemSelection(
                        player.getMainHandItem(), currentServer.registryAccess());
    }

    @Override
    protected String nativeItemMaterialId(final String input) {
        final Identifier itemId = registryIdentifier(input);
        return itemId == null
                        || !BuiltInRegistries.ITEM.containsKey(itemId)
                        || BuiltInRegistries.ITEM.get(itemId)
                                .map(reference -> reference.value() == Items.AIR)
                                .orElse(true)
                ? null
                : suggestionId(itemId);
    }

    @Override
    protected List<String> nativeEntityTypeIds() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    public List<String> particleTypeIds() {
        return BuiltInRegistries.PARTICLE_TYPE.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof SimpleParticleType)
                .map(Map.Entry::getKey)
                .map(ResourceKey::identifier)
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    public List<String> soundTypeIds() {
        return BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    public List<String> soundCategoryIds() {
        return Arrays.stream(SoundSource.values())
                .map(SoundSource::getName)
                .sorted()
                .toList();
    }

    @Override
    public List<String> statisticIds() {
        return BuiltInRegistries.CUSTOM_STAT.keySet().stream()
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    public List<String> advancementIds() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return List.of();
        }
        return minecraftServer.getAdvancements().getAllAdvancements().stream()
                .map(advancement -> advancement.id().toString())
                .sorted()
                .toList();
    }

    @Override
    protected List<String> blockMaterialIds() {
        return BuiltInRegistries.BLOCK.keySet().stream()
                .filter(identifier -> BuiltInRegistries.BLOCK.get(identifier)
                        .map(reference -> reference.value() != Blocks.AIR)
                        .orElse(false))
                .map(NeoForgeNotQuestsAdapter::suggestionId)
                .sorted()
                .toList();
    }

    @Override
    public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
        final ServerPlayer player = serverPlayer(questPlayer);
        final Identifier statistic = nativeStatistic(statisticId);
        return player == null || statistic == null ? 0 : player.getStats().getValue(Stats.CUSTOM.get(statistic));
    }

    @Override
    public boolean setPlayerStatistic(
            final PlatformPlayer questPlayer,
            final String statisticId,
            final int value) {
        final ServerPlayer player = serverPlayer(questPlayer);
        final Identifier statistic = nativeStatistic(statisticId);
        if (player == null || statistic == null || value < 0) {
            return false;
        }
        player.getStats().setValue(player, Stats.CUSTOM.get(statistic), value);
        return true;
    }

    @Override
    public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
        final ServerPlayer player = serverPlayer(questPlayer);
        final AdvancementHolder advancement = nativeAdvancement(advancementId);
        return player != null
                && advancement != null
                && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @Override
    public boolean setAdvancement(
            final PlatformPlayer questPlayer,
            final String advancementId,
            final boolean completed) {
        final ServerPlayer player = serverPlayer(questPlayer);
        final AdvancementHolder advancement = nativeAdvancement(advancementId);
        if (player == null || advancement == null) {
            return false;
        }
        final AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        final ArrayList<String> criteria = new ArrayList<>();
        for (final String criterion : completed ? progress.getRemainingCriteria() : progress.getCompletedCriteria()) {
            criteria.add(criterion);
        }
        boolean changed = false;
        for (final String criterion : criteria) {
            changed |= completed
                    ? player.getAdvancements().award(advancement, criterion)
                    : player.getAdvancements().revoke(advancement, criterion);
        }
        return changed || progress.isDone() == completed;
    }

    @Override
    public String blockMaterial(final NQLocation location) {
        final ServerLevel level = level(location == null ? null : location.worldName());
        if (level == null || location == null) {
            return "";
        }
        final BlockPos pos = BlockPos.containing(location.x(), location.y(), location.z());
        return suggestionId(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
    }

    @Override
    protected boolean setNativeBlockMaterial(
            final NQLocation location,
            final String materialId) {
        final ServerLevel level = level(location == null ? null : location.worldName());
        final Identifier identifier = registryIdentifier(materialId);
        final Block block = identifier == null ? null : BuiltInRegistries.BLOCK.getValue(identifier);
        if (level == null || location == null || block == null || block == Blocks.AIR) {
            return false;
        }
        return level.setBlockAndUpdate(BlockPos.containing(location.x(), location.y(), location.z()), block.defaultBlockState());
    }

    @Override
    protected String heldBlockMaterial(final PlatformPlayer questPlayer) {
        final ServerPlayer player = serverPlayer(questPlayer);
        if (player == null || player.getMainHandItem().isEmpty()) {
            return null;
        }
        final Block block = Block.byItem(player.getMainHandItem().getItem());
        return block == Blocks.AIR ? null : suggestionId(BuiltInRegistries.BLOCK.getKey(block));
    }

    @Override
    protected String itemSelectionBlockMaterial(final List<SavedItems.ItemChoice> items) {
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            return null;
        }
        final List<ItemStack> stacks = NeoForgePlayer.itemStacks(
                text,
                currentServer.registryAccess(),
                items);
        if (stacks.isEmpty()) {
            return null;
        }
        final Block block = Block.byItem(stacks.getFirst().getItem());
        return block == Blocks.AIR ? null : suggestionId(BuiltInRegistries.BLOCK.getKey(block));
    }

    @Override
    public List<ItemSelection> containerInventoryItems(final NQLocation location) {
        final Container container = container(location);
        final MinecraftServer currentServer = server.get();
        return container == null || currentServer == null
                ? List.of()
                : NeoForgePlayer.containerItems(container, currentServer.registryAccess());
    }

    @Override
    public boolean addContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items,
            final boolean dropOverflow) {
        final Container container = container(location);
        if (container == null) {
            return false;
        }
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            return false;
        }
        final List<ItemStack> stacks = NeoForgePlayer.itemStacks(
                text, currentServer.registryAccess(), items);
        final int requested = stacks.stream().mapToInt(ItemStack::getCount).sum();
        final List<ItemStack> leftovers = NeoForgePlayer.addToContainer(container, stacks);
        final int remaining = leftovers.stream().mapToInt(ItemStack::getCount).sum();
        if (dropOverflow) {
            final ServerLevel level = level(location == null ? null : location.worldName());
            if (level != null && location != null) {
                for (final ItemStack leftover : leftovers) {
                    level.addFreshEntity(new ItemEntity(level, location.x(), location.y(), location.z(), leftover));
                }
            }
        }
        return requested > remaining || dropOverflow && remaining > 0;
    }

    @Override
    public boolean removeContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items) {
        final Container container = container(location);
        final MinecraftServer currentServer = server.get();
        return container != null && currentServer != null && NeoForgePlayer.removeFromContainer(
                plugin, currentServer.registryAccess(), container, items);
    }

    @Override
    public boolean setContainerInventoryItems(
            final NQLocation location,
            final List<SavedItems.ItemChoice> items) {
        final Container container = container(location);
        if (container == null) {
            return false;
        }
        final MinecraftServer currentServer = server.get();
        if (currentServer == null) {
            return false;
        }
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (final SavedItems.ItemChoice item
                : items == null ? List.<SavedItems.ItemChoice>of() : items) {
            if (item == null || item.selection() == null) {
                return false;
            }
            final List<ItemStack> selectedStacks = NeoForgePlayer.itemStacks(
                    text, currentServer.registryAccess(), List.of(item));
            if (selectedStacks.isEmpty()) {
                return false;
            }
            stacks.addAll(selectedStacks);
        }
        container.clearContent();
        return NeoForgePlayer.addToContainer(container, stacks).isEmpty();
    }

    @Override
    public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
        final ItemStack stack = inventoryItem(serverPlayer(questPlayer), slotId);
        if (stack == null || stack.isEmpty() || stack.getEnchantments().isEmpty()) {
            return List.of();
        }
        return stack.getEnchantments().keySet().stream()
                .map(NeoForgeNotQuestsAdapter::enchantmentId)
                .filter(id -> !id.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public List<String> enchantmentIds() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return List.of();
        }
        return minecraftServer.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .map(registry -> registry.keySet().stream()
                        .map(NeoForgeNotQuestsAdapter::suggestionId)
                        .sorted()
                .toList())
                .orElseGet(List::of);
    }

    @Override
    public List<String> inventorySlotIds() {
        final ArrayList<String> slots = new ArrayList<>();
        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            slots.add(switch (slot) {
                case MAINHAND -> "HAND";
                case OFFHAND -> "OFF_HAND";
                default -> slot.name();
            });
        }
        for (int slot = 0; slot <= 35; slot++) {
            slots.add(String.valueOf(slot));
        }
        return slots.stream().distinct().toList();
    }

    @Override
    public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
        final ServerPlayer player = serverPlayer(questPlayer);
        return NeoForgePermissions.hasPermission(player, permission);
    }

    @Override
    public boolean supportsArbitraryPermissionChecks() {
        return false;
    }

    @Override
    public boolean supportsPermissionMutation() {
        return false;
    }

    @Override
    public boolean setPermission(
            final PlatformPlayer questPlayer,
            final String permission,
            final boolean value) {
        return false;
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
        return List.of();
    }

    @Override
    protected NativeNpc nativeNpc(final String npcType, final NQNPCID npcId) {
        return null;
    }

    @Override
    public List<String> nativeNpcQuestGiverTypes() {
        return List.of();
    }

    @Override
    public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
        return false;
    }

    @Override
    public boolean giveArmorStandTool(
            final PlatformPlayer actor,
            final ArmorStandToolItem tool) {
        return npcAttachments.giveArmorStandTool(actor, tool);
    }

    @Override
    public boolean giveNpcSelectionTool(
            final PlatformPlayer actor,
            final int selectionId,
            final String displayName,
            final List<String> lore) {
        return npcAttachments.giveSelectionTool(actor, selectionId, displayName, lore);
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
        final ServerPlayer player = serverPlayer(questPlayer);
        if (player == null) {
            return false;
        }
        try {
            return callOnServerThread(() -> !NeoForge.EVENT_BUS.post(new NotQuestsEvents.ObjectiveUnlock(
                    player,
                    quest.getIdentifier(),
                    quest,
                    objective,
                    objective.getObjectivePath(),
                    objective.getObjectiveID(),
                    objective.getHolderPath(),
                    triggerAcceptQuestTrigger)).isCanceled());
        } catch (final Exception exception) {
            throw new IllegalStateException("Could not dispatch NeoForge ObjectiveUnlock event", exception);
        }
    }

    @Override
    public void broadcast(final String miniMessageText) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            throw new IllegalStateException("Cannot broadcast without a running Minecraft server.");
        }
        text.broadcast(minecraftServer, miniMessageText);
    }

    @Override
    public void dispatchConsoleCommand(final String command) {
        final MinecraftServer minecraftServer = server.get();
        final String commandWithoutSlash = command == null
                ? ""
                : command.startsWith("/") ? command.substring(1) : command;
        if (minecraftServer == null) {
            throw new IllegalStateException("Cannot dispatch a command without a running Minecraft server.");
        }
        minecraftServer.getCommands().performPrefixedCommand(
                minecraftServer.createCommandSourceStack(),
                "/" + commandWithoutSlash);
    }

    @Override
    public NQLocation location(final String worldName, final double x, final double y, final double z) {
        final ServerLevel level = level(worldName);
        return level == null ? null : NQLocation.at(NeoForgeWorldNames.displayName(level.dimension().identifier()), x, y, z);
    }

    private ServerPlayer serverPlayer(final PlatformPlayer questPlayer) {
        if (questPlayer instanceof final NeoForgePlayer neoForgeQuestPlayer) {
            return neoForgeQuestPlayer.player();
        }
        if (questPlayer == null || questPlayer.playerIdentifier() == null || questPlayer.playerIdentifier().isBlank()) {
            return null;
        }
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            return null;
        }
        return minecraftServer.getPlayerList().getPlayers().stream()
                .filter(player -> player.getUUID().toString().equalsIgnoreCase(questPlayer.playerIdentifier())
                        || player.getName().getString().equalsIgnoreCase(questPlayer.playerName()))
                .findFirst()
                .orElse(null);
    }

    private ServerLevel level(final String worldName) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        for (final ServerLevel level : minecraftServer.getAllLevels()) {
            final Identifier identifier = level.dimension().identifier();
            if (NeoForgeWorldNames.matches(identifier, worldName)) {
                return level;
            }
        }
        return null;
    }

    private Container container(final NQLocation location) {
        final ServerLevel level = level(location == null ? null : location.worldName());
        if (level == null || location == null) {
            return null;
        }
        final BlockPos pos = BlockPos.containing(location.x(), location.y(), location.z());
        return level.getBlockEntity(pos) instanceof final Container container ? container : null;
    }

    private ItemStack inventoryItem(final ServerPlayer player, final String slotId) {
        if (player == null || slotId == null || slotId.isBlank()) {
            return null;
        }
        final String normalized = slotId.trim()
                .replace("-", "_")
                .toUpperCase(Locale.ROOT);
        try {
            final EquipmentSlot slot = switch (normalized) {
                case "HAND" -> EquipmentSlot.MAINHAND;
                case "OFF_HAND" -> EquipmentSlot.OFFHAND;
                default -> EquipmentSlot.valueOf(normalized);
            };
            return player.getItemBySlot(slot);
        } catch (final IllegalArgumentException ignored) {
            // Try numeric inventory slot below.
        }
        try {
            return player.getInventory().getItem(Integer.parseInt(slotId));
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static String enchantmentId(final Holder<Enchantment> enchantment) {
        final Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
        return key.isPresent() ? suggestionId(key.get().identifier()) : enchantment.getRegisteredName();
    }

    private static Identifier nativeStatistic(final String statisticId) {
        final Identifier identifier = registryIdentifier(statisticId);
        if (identifier == null || !BuiltInRegistries.CUSTOM_STAT.containsKey(identifier)) {
            return null;
        }
        return identifier;
    }

    private AdvancementHolder nativeAdvancement(final String advancementId) {
        final MinecraftServer minecraftServer = server.get();
        final Identifier identifier = registryIdentifier(advancementId);
        if (minecraftServer == null || identifier == null) {
            return null;
        }
        return minecraftServer.getAdvancements().get(identifier);
    }

    @Override
    public void schedule(final Duration delay, final Runnable action) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            throw new IllegalStateException("Cannot schedule work without a running Minecraft server.");
        }
        if (delay == null || delay.isNegative() || delay.isZero()) {
            minecraftServer.execute(action);
            return;
        }
        CompletableFuture.delayedExecutor(
                        Math.max(0, delay.toMillis()),
                        TimeUnit.MILLISECONDS)
                .execute(() -> minecraftServer.execute(action));
    }

    @Override
    public boolean isServerThread() {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            throw new IllegalStateException("Cannot inspect the server thread before the server starts.");
        }
        return minecraftServer.isSameThread();
    }

    @Override
    public <T> T callOnServerThread(final Callable<T> action) throws Exception {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null) {
            throw new IllegalStateException("Cannot call the server thread before the server starts.");
        }
        if (minecraftServer.isSameThread()) {
            return action.call();
        }
        final CompletableFuture<T> future = new CompletableFuture<>();
        minecraftServer.execute(() -> {
            try {
                future.complete(action.call());
            } catch (final Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future.get();
    }

    private static String suggestionId(final Identifier identifier) {
        return "minecraft".equals(identifier.getNamespace()) ? identifier.getPath() : identifier.toString();
    }

    private static Identifier registryIdentifier(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return Identifier.tryParse(normalized.contains(":") ? normalized : "minecraft:" + normalized);
    }
}
