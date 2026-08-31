package com.notquests.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.notquests.builtin.BuiltInPack;
import com.notquests.core.NotQuestsPlatform;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.managers.LogManager.ConsoleLine;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.text.NotQuestsMiniMessage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

@Mod(NotQuestsNeoForge.MOD_ID)
public final class NotQuestsNeoForge implements NotQuestsPlatform {
    public static final String MOD_ID = "notquests";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotQuestsNeoForge.class);

    private volatile MinecraftServer server;
    private volatile ScheduledExecutorService backgroundExecutor;
    private volatile CompletableFuture<Boolean> dataLoadTask;
    private final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    private final NeoForgeBeamTracker beamTracker = new NeoForgeBeamTracker(() -> server);
    private final MiniMessage miniMessage = NotQuestsMiniMessage.create(null);
    private final NeoForgeText text = new NeoForgeText(miniMessage, plugin);
    private final NeoForgeNotQuestsAdapter adapter;
    private NotQuestsCommands commands;
    private final NeoForgeGuiRenderer guiRenderer;
    private final NeoForgeNpcAttachments npcAttachments;
    private NeoForgeCoreCommandCompiler commandCompiler;
    private final NeoForgeObjectiveEvents objectiveEvents;
    private volatile ItemStack journalItem = ItemStack.EMPTY;
    private boolean platformEventsRegistered;

    public NotQuestsNeoForge(final IEventBus modBus) {
        NeoForgeArgumentTypes.register(modBus);
        NeoForgePermissions.register(CommandManager.permissionDefaults());
        modBus.addListener(NeoForgeBeamPayload::register);
        registerClientHooks(modBus);
        npcAttachments = new NeoForgeNpcAttachments(plugin, text, () -> server);
        guiRenderer = new NeoForgeGuiRenderer(plugin, text, LOGGER);
        adapter = new NeoForgeNotQuestsAdapter(
                plugin,
                () -> server,
                text,
                npcAttachments,
                beamTracker,
                guiRenderer,
                LOGGER);
        plugin.addRegistryPack((NotQuestsPlatform platform) ->
                BuiltInPack.register(plugin, platform.adapter()));

        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        objectiveEvents = new NeoForgeObjectiveEvents(
                plugin,
                text,
                npcAttachments,
                () -> server,
                beamTracker,
                guiRenderer,
                this::journalItem,
                action -> backgroundExecutor().execute(action));
    }

    private void onServerAboutToStart(final ServerAboutToStartEvent event) {
        server = event.getServer();
        startBackgroundExecutor();
        plugin.load(this);
        text.usePalette(plugin.configuration());
        plugin.enable(this);
    }

    private void registerClientHooks(final IEventBus modBus) {
        if (!"CLIENT".equals(FMLEnvironment.getDist().name())) {
            return;
        }
        try {
            final Class<?> renderer = Class.forName("com.notquests.neoforge.NeoForgeClientBeamRenderer");
            renderer.getMethod("register", IEventBus.class).invoke(null, modBus);
        } catch (final ReflectiveOperationException exception) {
            LOGGER.error("Failed to register NotQuests client beam renderer.", exception);
        }
    }

    @Override
    public String platformName() {
        return "NeoForge";
    }

    @Override
    public Path dataFolder() {
        final MinecraftServer currentServer = server;
        return currentServer == null
                ? Path.of("notquests")
                : notQuestsFolder(currentServer.getWorldPath(LevelResource.ROOT));
    }

    static Path notQuestsFolder(final Path worldRoot) {
        return Objects.requireNonNull(worldRoot, "worldRoot").resolve("notquests");
    }

    @Override
    public String pluginVersion() {
        return ModList.get()
                .getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElseGet(() -> getClass().getPackage().getImplementationVersion());
    }

    @Override
    public String serverVersion() {
        return server == null ? "" : server.getServerVersion();
    }

    @Override
    public double currentTps() {
        final MinecraftServer currentServer = server;
        if (currentServer == null) {
            return 20.0d;
        }
        final long averageTickTimeNanos = currentServer.getAverageTickTimeNanos();
        return averageTickTimeNanos <= 0L
                ? 20.0d
                : Math.min(20.0d, 1_000_000_000.0d / averageTickTimeNanos);
    }

    @Override
    public NotQuestsAdapter adapter() {
        return adapter;
    }

    @Override
    public void writeConsole(final ConsoleLine line) {
        final String message = NotQuestsMiniMessage.stripMiniMessage(line.message());
        if (line.level() == Level.SEVERE) {
            LOGGER.error(message);
        } else if (line.level() == Level.WARNING) {
            LOGGER.warn(message);
        } else if (line.level() == Level.FINE) {
            LOGGER.debug(message);
        } else {
            LOGGER.info(message);
        }
    }

    @Override
    public void scheduleAction(final Duration delay, final Runnable action) {
        adapter.schedule(delay, action);
    }

    @Override
    public void scheduleAsync(final Duration delay, final Runnable action) {
        backgroundExecutor().schedule(
                action,
                Math.max(1L, delay.toMillis()),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public PlatformPlayer createPlayer(final String playerIdentifier) {
        return adapter.createQuestPlayer(playerIdentifier);
    }

    @Override
    public List<String> onlinePlayerIdentifiers() {
        final MinecraftServer currentServer = server;
        return currentServer == null
                ? List.of()
                : currentServer.getPlayerList().getPlayers().stream()
                        .map(player -> player.getUUID().toString())
                        .toList();
    }

    @Override
    public ConversationManager.Display conversationDisplay() {
        return new NeoForgeConversationDisplay(text, () -> server);
    }

    @Override
    public boolean materializeJournalItem(
            final ConfigurationManager.JournalItem journal) {
        final MinecraftServer currentServer = server;
        if (currentServer == null) {
            return false;
        }
        final ItemStack materialized = createJournalItem(journal, text, currentServer);
        if (materialized.isEmpty()) {
            return false;
        }
        journalItem = materialized;
        return true;
    }

    @Override
    public Optional<PacketBridge> packetBridge(final boolean enabled, final boolean usePacketEvents) {
        return enabled
                ? Optional.of(new NeoForgePackets(plugin, text))
                : Optional.empty();
    }

    @Override
    public Optional<NativeIntegrations> nativeIntegrations() {
        return Optional.empty();
    }

    @Override
    public void registerPlatformEvents() {
        if (!platformEventsRegistered) {
            objectiveEvents.register();
            platformEventsRegistered = true;
            return;
        }
        objectiveEvents.activate();
    }

    @Override
    public Optional<NpcIndicatorRenderer> npcIndicatorRenderer(final String npcType) {
        if (!"armorstand".equalsIgnoreCase(npcType)) {
            return Optional.empty();
        }
        return Optional.of(new NpcIndicatorRenderer() {
            @Override
            public Optional<NpcTextVisibility> textVisibility() {
                return Optional.empty();
            }

            @Override
            public void render(
                    final NQNPCID npcId,
                    final Indicator indicator,
                    final Set<String> visiblePlayerIdentifiers) {
                npcAttachments.renderArmorStandIndicator(npcId, indicator);
            }
        });
    }

    @Override
    public void registerCommands(final NotQuestsCommands commands) {
        this.commands = commands;
        commandCompiler = new NeoForgeCoreCommandCompiler(
                plugin,
                commands,
                text,
                this::pluginVersion);
        final MinecraftServer currentServer = server;
        if (currentServer != null) {
            registerCommands(currentServer.getCommands().getDispatcher());
        }
    }

    @Override
    public CompletionStage<Boolean> loadData(final BooleanSupplier dataLoad) {
        final CompletableFuture<Boolean> loaded = CompletableFuture.supplyAsync(
                dataLoad::getAsBoolean,
                backgroundExecutor());
        dataLoadTask = loaded;
        loaded.whenComplete((ignored, exception) -> {
            if (dataLoadTask == loaded) {
                dataLoadTask = null;
            }
        });
        return loaded;
    }

    @Override
    public void runOnServerThread(final Runnable action) {
        final MinecraftServer currentServer = server;
        if (currentServer == null) {
            throw new IllegalStateException("Cannot run work before the Minecraft server starts.");
        }
        currentServer.execute(action);
    }

    @Override
    public Optional<MetricsBridge> metricsBridge() {
        return Optional.empty();
    }

    @Override
    public void publishLoadedEvent() {
        NeoForge.EVENT_BUS.post(new NotQuestsEvents.Loaded());
    }

    private void onServerStopped(final ServerStoppedEvent event) {
        plugin.stop(this);
    }

    @Override
    public void closePlatform() {
        final CompletableFuture<Boolean> loading = dataLoadTask;
        dataLoadTask = null;
        if (loading != null) {
            loading.cancel(true);
        }
        final ScheduledExecutorService executor = backgroundExecutor;
        backgroundExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
        objectiveEvents.clear();
        beamTracker.clear();
        journalItem = ItemStack.EMPTY;
        server = null;
    }

    private void startBackgroundExecutor() {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, action -> {
            final Thread thread = new Thread(action, "NotQuests NeoForge background");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        backgroundExecutor = executor;
    }

    private ScheduledExecutorService backgroundExecutor() {
        final ScheduledExecutorService executor = backgroundExecutor;
        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException("NotQuests background executor is not running.");
        }
        return executor;
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        if (commands == null || commandCompiler == null) {
            return;
        }
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
        for (final String rootName : commands.userRootNames()) {
            dispatcher.register(commandCompiler.compile(commands.userCommands(rootName), rootName));
        }
        for (final String rootName : commands.adminRootNames()) {
            dispatcher.register(commandCompiler.compile(commands.adminCommands(rootName), rootName));
        }
    }

    ItemStack journalItem() {
        return journalItem.copy();
    }

    static ItemStack createJournalItem(
            final ConfigurationManager.JournalItem journal,
            final NeoForgeText text,
            final MinecraftServer server) {
        if (journal == null || text == null || server == null) {
            return ItemStack.EMPTY;
        }
        final Item material = journalMaterial(journal.material());
        if (material == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack item = new ItemStack(material, journal.amount());
        if (journal.damage() > 0 && item.isDamageableItem()) {
            item.set(DataComponents.DAMAGE, Math.min(item.getMaxDamage(), journal.damage()));
        }
        applyJournalText(item, journal.displayName(), journal.lore(), text);
        if (journal.customModelData() != null) {
            item.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of(journal.customModelData().floatValue()), List.of(), List.of(), List.of()));
        }
        applyEnchantments(item, journal.enchantments(), server, false);
        applyEnchantments(item, journal.storedEnchantments(), server, true);
        if (journal.unbreakable()) {
            item.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }
        applyHiddenComponents(item, journal.hiddenComponents());
        if (journal.glint()) {
            item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return item;
    }

    private static void applyEnchantments(
            final ItemStack item,
            final Map<String, Integer> enchantments,
            final MinecraftServer server,
            final boolean stored) {
        if (enchantments.isEmpty()) {
            return;
        }
        final var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        final ItemEnchantments.Mutable storedEnchantments = stored
                ? new ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                : null;
        for (final var entry : enchantments.entrySet()) {
            try {
                final String value = entry.getKey().contains(":")
                        ? entry.getKey()
                        : "minecraft:" + entry.getKey();
                registry.get(Identifier.parse(value.toLowerCase(Locale.ROOT))).ifPresent(enchantment -> {
                    if (storedEnchantments == null) {
                        item.enchant(enchantment, entry.getValue());
                    } else {
                        storedEnchantments.set(enchantment, entry.getValue());
                    }
                });
            } catch (final RuntimeException ignored) {
                // An enchantment removed by the current game version cannot be materialized.
            }
        }
        if (storedEnchantments != null && !storedEnchantments.keySet().isEmpty()) {
            item.set(DataComponents.STORED_ENCHANTMENTS, storedEnchantments.toImmutable());
        }
    }

    private static void applyHiddenComponents(
            final ItemStack item,
            final List<String> hiddenComponents) {
        final LinkedHashSet<DataComponentType<?>> hidden = new LinkedHashSet<>();
        for (final String component : hiddenComponents) {
            switch (component == null ? "" : component) {
                case "enchantments" -> hidden.add(DataComponents.ENCHANTMENTS);
                case "attribute-modifiers" -> hidden.add(DataComponents.ATTRIBUTE_MODIFIERS);
                case "unbreakable" -> hidden.add(DataComponents.UNBREAKABLE);
                case "can-break" -> hidden.add(DataComponents.CAN_BREAK);
                case "can-place-on" -> hidden.add(DataComponents.CAN_PLACE_ON);
                case "additional-tooltip" -> {
                    hidden.add(DataComponents.POTION_CONTENTS);
                    hidden.add(DataComponents.WRITTEN_BOOK_CONTENT);
                    hidden.add(DataComponents.FIREWORKS);
                    hidden.add(DataComponents.FIREWORK_EXPLOSION);
                    hidden.add(DataComponents.INSTRUMENT);
                    hidden.add(DataComponents.LODESTONE_TRACKER);
                    hidden.add(DataComponents.BANNER_PATTERNS);
                }
                case "dyed-color" -> hidden.add(DataComponents.DYED_COLOR);
                case "trim" -> hidden.add(DataComponents.TRIM);
                case "stored-enchantments" -> hidden.add(DataComponents.STORED_ENCHANTMENTS);
                default -> {
                }
            }
        }
        if (!hidden.isEmpty()) {
            item.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hidden));
        }
    }

    private static void applyJournalText(
            final ItemStack item,
            final String displayName,
            final List<String> configuredLore,
            final NeoForgeText text) {
        if (displayName != null && !displayName.isBlank()) {
            item.set(DataComponents.CUSTOM_NAME, text.component(displayName));
        }
        if (configuredLore == null || configuredLore.isEmpty()) {
            return;
        }
        final List<Component> lore = configuredLore.stream()
                .limit(ItemLore.MAX_LINES)
                .map(text::component)
                .map(component -> (Component) component.copy().withStyle(style -> style.withItalic(false)))
                .toList();
        item.set(DataComponents.LORE, new ItemLore(lore, lore));
    }

    private static Item journalMaterial(final String configuredMaterial) {
        if (configuredMaterial == null || configuredMaterial.isBlank()) {
            return null;
        }
        try {
            final String material = configuredMaterial.trim().toLowerCase(Locale.ROOT);
            final Identifier identifier = Identifier.parse(
                    material.contains(":") ? material : "minecraft:" + material);
            final Item item = BuiltInRegistries.ITEM.get(identifier)
                    .map(reference -> reference.value())
                    .orElse(null);
            return item == Items.AIR ? null : item;
        } catch (final RuntimeException exception) {
            return null;
        }
    }

}
