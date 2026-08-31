package com.notquests.paper;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.notquests.core.NotQuestsPlatform;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.managers.LogManager.ConsoleLine;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Pack;
import com.notquests.core.text.NotQuestsColors.Messages;
import com.notquests.core.text.NotQuestsMiniMessage;
import com.notquests.paper.commands.brigadier.PaperCoreCommandCompiler;
import com.notquests.paper.conversation.PaperConversationDisplay;
import com.notquests.paper.events.ArmorStandEvents;
import com.notquests.paper.events.InventoryEvents;
import com.notquests.paper.events.QuestEvents;
import com.notquests.paper.events.notquests.NotQuestsFullyLoadedEvent;
import com.notquests.paper.gui.PaperGuiRenderer;
import com.notquests.paper.integrations.PaperIntegrations;
import com.notquests.paper.metrics.Metrics;
import com.notquests.paper.npc.PaperArmorStands;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

public class NotQuests implements NotQuestsPlatform {
    private static NotQuests instance;
    private final JavaPlugin main;
    private final NotQuestsPlugin corePlugin = NotQuestsPlugin.create();
    private final PaperNotQuestsAdapter registryAdapter = new PaperNotQuestsAdapter(this);
    // Paper runtime leaves
    private PaperArmorStands armorStands;
    private PaperPackets packets;
    private Messages messages;
    private ItemStack journalItem;

    // Renderers and platform services
    private PaperGuiRenderer guiRenderer;
    private PaperIntegrations integrations;

    //Metrics
    private Metrics metrics;

    public final JavaPlugin getMain(){
        return main;
    }

    public NotQuestsPlugin getCorePlugin() {
        return corePlugin;
    }

    public PaperNotQuestsAdapter getRegistryAdapter() {
        return registryAdapter;
    }

    public NotQuests(JavaPlugin main){
        this.main = main;
        messages = new Messages(corePlugin.configuration()::colors);
        integrations = new PaperIntegrations(this);
        guiRenderer = new PaperGuiRenderer(this);
        armorStands = new PaperArmorStands(this);
    }

    public void addRegistryPack(final Pack<NotQuests> registryPack) {
        corePlugin.addRegistryPack(registryPack);
    }

    public void onLoad() {
        corePlugin.load(this);
    }

    @Override
    public Optional<PacketBridge> packetBridge(final boolean enabled, final boolean usePacketEvents) {
        packets = new PaperPackets(this, enabled, usePacketEvents);
        packets.load();
        return Optional.of(packets);
    }

    @Override
    public String platformName() {
        return "Paper";
    }

    @Override
    public Path dataFolder() {
        return main.getDataFolder().toPath();
    }

    @Override
    public String pluginVersion() {
        return main.getDescription().getVersion();
    }

    @Override
    public String serverVersion() {
        return Bukkit.getBukkitVersion();
    }

    @Override
    public NotQuestsAdapter adapter() {
        return registryAdapter;
    }

    @Override
    public void writeConsole(final ConsoleLine line) {
        sendConsoleLine(line);
    }

    @Override
    public void scheduleAction(final Duration delay, final Runnable action) {
        registryAdapter.schedule(delay, action);
    }

    @Override
    public void scheduleAsync(final Duration delay, final Runnable action) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                main,
                action,
                Math.max(1L, delay.toMillis() / 50L));
    }

    @Override
    public PlatformPlayer createPlayer(final String playerIdentifier) {
        return registryAdapter.createPaperPlayer(playerIdentifier);
    }

    @Override
    public List<String> onlinePlayerIdentifiers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(player -> player.getUniqueId().toString())
                .toList();
    }

    @Override
    public ConversationManager.Display conversationDisplay() {
        return new PaperConversationDisplay();
    }

    @Override
    public boolean materializeJournalItem(final ConfigurationManager.JournalItem journal) {
        final ItemStack materialized = materializeJournal(journal);
        if (materialized == null) {
            return false;
        }
        journalItem = materialized;
        return true;
    }

    public static NotQuests getInstance() {
        return instance;
    }

    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    public void onEnable() {
        instance = this;
        corePlugin.enable(this);
    }

    @Override
    public Optional<NativeIntegrations> nativeIntegrations() {
        return Optional.of(integrations);
    }

    @Override
    public void registerPlatformEvents() {
        main.getServer().getPluginManager().registerEvents(new QuestEvents(this), main);
        main.getServer().getPluginManager().registerEvents(new InventoryEvents(this), main);
        main.getServer().getPluginManager().registerEvents(new ArmorStandEvents(this), main);
    }

    @Override
    public double currentTps() {
        return Bukkit.getTPS()[0];
    }

    @Override
    public Optional<NpcIndicatorRenderer> npcIndicatorRenderer(final String npcType) {
        if ("armorstand".equalsIgnoreCase(npcType)) {
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
                    armorStands.renderIndicator(npcId, indicator);
                }
            });
        }
        if ("fancynpcs".equalsIgnoreCase(npcType) && integrations.fancyNpcs() != null) {
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
                    integrations.fancyNpcs().renderIndicator(npcId, indicator);
                }
            });
        }
        if ("citizens".equalsIgnoreCase(npcType) && integrations.citizens() != null) {
            return Optional.of(new NpcIndicatorRenderer() {
                @Override
                public Optional<NpcTextVisibility> textVisibility() {
                    return Optional.of(integrations.citizens()::nearbyPlayerIdentifiers);
                }

                @Override
                public void render(
                        final NQNPCID npcId,
                        final Indicator indicator,
                        final Set<String> visiblePlayerIdentifiers) {
                    integrations.citizens().renderIndicator(npcId, indicator, visiblePlayerIdentifiers);
                }
            });
        }
        return Optional.empty();
    }

    @Override
    public void registerCommands(final NotQuestsCommands commands) {
        main.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    final PaperCoreCommandCompiler compiler = new PaperCoreCommandCompiler(this, commands);
                    compiler.register(event.registrar(), commands.userCommandsWithAliases());
                    compiler.register(event.registrar(), commands.adminCommandsWithAliases());
                });
    }

    @Override
    public CompletionStage<Boolean> loadData(final BooleanSupplier dataLoad) {
        final CompletableFuture<Boolean> loaded = new CompletableFuture<>();
        try {
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                try {
                    loaded.complete(dataLoad.getAsBoolean());
                } catch (final Throwable exception) {
                    loaded.completeExceptionally(exception);
                }
            });
        } catch (final RuntimeException exception) {
            loaded.completeExceptionally(exception);
        }
        return loaded;
    }

    @Override
    public void runOnServerThread(final Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(main, action);
        }
    }

    @Override
    public Optional<MetricsBridge> metricsBridge() {
        return Optional.of(values -> {
            metrics = new Metrics(main, values.pluginId());
            values.singleLineCharts().forEach((name, value) ->
                    metrics.addCustomChart(new Metrics.SingleLineChart(name, value::get)));
            values.advancedPieCharts().forEach((name, value) ->
                    metrics.addCustomChart(new Metrics.AdvancedPie(name, value::get)));
        });
    }

    @Override
    public void publishLoadedEvent() {
        Bukkit.getPluginManager().callEvent(new NotQuestsFullyLoadedEvent(this));
    }

    /**
     * Called when the plugin is disabled or reloaded via ServerUtils / PlugMan
     */
    public void onDisable() {
        corePlugin.stop(this);
    }

    @Override
    public void closePlatform() {
        metrics = null;
    }

    public ItemStack journalItem() {
        return journalItem == null ? null : journalItem.clone();
    }

    private ItemStack materializeJournal(final ConfigurationManager.JournalItem journal) {
        final Material material = Material.matchMaterial(journal.material());
        if (material == null) {
            return null;
        }
        final ItemStack item = new ItemStack(material, journal.amount());
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            if (journal.damage() > 0
                    && itemMeta instanceof final Damageable damageable) {
                damageable.setDamage(Math.min(material.getMaxDurability(), journal.damage()));
            }
            if (!journal.displayName().isBlank()) {
                itemMeta.displayName(parse(journal.displayName()));
            }
            if (!journal.lore().isEmpty()) {
                itemMeta.lore(journal.lore().stream()
                        .map(line -> parse(line).decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            if (journal.customModelData() != null) {
                itemMeta.setCustomModelData(journal.customModelData());
            }
            applyEnchantments(itemMeta, journal.enchantments());
            if (itemMeta instanceof EnchantmentStorageMeta stored) {
                for (final var entry : journal.storedEnchantments().entrySet()) {
                    final Enchantment enchantment = journalEnchantment(entry.getKey());
                    if (enchantment != null) {
                        stored.addStoredEnchant(enchantment, entry.getValue(), true);
                    }
                }
            }
            itemMeta.setUnbreakable(journal.unbreakable());
            for (final String hiddenComponent : journal.hiddenComponents()) {
                final ItemFlag flag = journalItemFlag(hiddenComponent);
                if (flag != null) {
                    itemMeta.addItemFlags(flag);
                }
            }
            if (journal.glint()) {
                itemMeta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    private static void applyEnchantments(
            final ItemMeta itemMeta,
            final Map<String, Integer> enchantments) {
        for (final var entry : enchantments.entrySet()) {
            final Enchantment enchantment = journalEnchantment(entry.getKey());
            if (enchantment != null) {
                itemMeta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
    }

    private static Enchantment journalEnchantment(final String configuredId) {
        if (configuredId == null || configuredId.isBlank()) {
            return null;
        }
        final String value = configuredId.contains(":")
                ? configuredId
                : "minecraft:" + configuredId;
        final NamespacedKey key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    private static ItemFlag journalItemFlag(final String hiddenComponent) {
        return switch (hiddenComponent == null ? "" : hiddenComponent) {
            case "enchantments" -> ItemFlag.HIDE_ENCHANTS;
            case "attribute-modifiers" -> ItemFlag.HIDE_ATTRIBUTES;
            case "unbreakable" -> ItemFlag.HIDE_UNBREAKABLE;
            case "can-break" -> ItemFlag.HIDE_DESTROYS;
            case "can-place-on" -> ItemFlag.HIDE_PLACED_ON;
            case "additional-tooltip" -> ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
            case "dyed-color" -> ItemFlag.HIDE_DYE;
            case "trim" -> ItemFlag.HIDE_ARMOR_TRIM;
            case "stored-enchantments" -> ItemFlag.HIDE_STORED_ENCHANTS;
            default -> null;
        };
    }

    public Particle particle(final String particleType) {
        return Particle.valueOf(particleType.toUpperCase(Locale.ROOT));
    }

    public PaperArmorStands armorStands() {
        return armorStands;
    }

    public PaperIntegrations integrations() {
        return integrations;
    }

    public final Messages messages(){
        return messages;
    }

    public final MiniMessage getMiniMessage(){
        return messages.miniMessage();
    }

    private void sendConsoleLine(final ConsoleLine line) {
        if (!line.miniMessage()) {
            Bukkit.getConsoleSender().sendMessage(Component.text(line.prefix() + line.message()));
            return;
        }

        final Component prefix = parse(line.prefix());
        final Component message = parse(line.color() + line.message());
        if (!line.downsampleColors()) {
            Bukkit.getConsoleSender().sendMessage(prefix.append(message));
            return;
        }

        final Component downsampled = GsonComponentSerializer.gson().deserializeFromTree(
                GsonComponentSerializer.builder()
                        .editOptions(options -> options.value(JSONOptions.EMIT_RGB, false))
                        .build()
                        .serializeToTree(message));
        Bukkit.getConsoleSender().sendMessage(prefix.append(downsampled));
    }

    public final Component parse(final String miniMessage){
        return NotQuestsMiniMessage.deserialize(getMiniMessage(), miniMessage);
    }

    public void sendMessage(final CommandSender sender, final String message){
        if(sender != null){
            sender.sendMessage(parse(message == null ? "" : message));
        }
    }

    public void sendMessage(final CommandSender sender, final Component component){
        if(sender != null && component != null){
            sender.sendMessage(component);
        }
    }

    public PaperGuiRenderer getPaperGuiRenderer() {
        return guiRenderer;
    }

}
