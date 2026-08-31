package com.notquests.paper.integrations;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.notquests.core.NotQuestsPlatform.IntegrationPlugin;
import com.notquests.core.NotQuestsPlatform.NativeIntegration;
import com.notquests.core.NotQuestsPlatform.NativeIntegrations;
import com.notquests.core.platform.NQLocation;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.events.hooks.BetonQuestEvents;
import com.notquests.paper.events.hooks.CitizensEvents;
import com.notquests.paper.events.hooks.EcoMobsEvents;
import com.notquests.paper.events.hooks.EliteMobsEvents;
import com.notquests.paper.events.hooks.FancyNPCsEvents;
import com.notquests.paper.events.hooks.JobsRebornEvents;
import com.notquests.paper.events.hooks.MythicMobsEvents;
import com.notquests.paper.events.hooks.SlimefunEvents;
import com.notquests.paper.events.hooks.TownyEvents;
import com.notquests.paper.integrations.betonquest.BetonQuestIntegration;
import com.notquests.paper.integrations.citizens.CitizensIntegration;
import com.notquests.paper.integrations.fancynpcs.FancyNPCsIntegration;
import com.notquests.paper.placeholders.QuestPlaceholders;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Native optional-plugin handles. Core owns discovery, ordering, retries, and lifecycle state. */
public final class PaperIntegrations implements NativeIntegrations {
    private static final Pattern BETONQUEST_VERSION =
            Pattern.compile("^[^0-9]*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");
    private final NotQuests main;
    private VaultIntegration vault;
    private MythicMobsIntegration mythicMobs;
    private CitizensIntegration citizens;
    private FancyNPCsIntegration fancyNpcs;
    private WorldEditIntegration worldEdit;
    private LuckPermsIntegration luckPerms;
    private EcoMobsIntegration ecoMobs;
    private FloodgateIntegration floodgate;
    private BetonQuestIntegration betonQuest;
    private final List<NativeIntegration> integrations;

    public PaperIntegrations(final NotQuests main) {
        this.main = main;
        integrations = List.of(
                integration("EcoMobs", Optional.of((BooleanSupplier) () -> {
                    ecoMobs = new EcoMobsIntegration();
                    return true;
                }), Optional.of(() -> register(new EcoMobsEvents(main))), Optional.empty(), Optional.empty()),
                integration("PlaceholderAPI", Optional.empty(), Optional.empty(),
                        Optional.of(() -> new QuestPlaceholders(main).register()), Optional.empty()),
                integration("Vault", Optional.of(this::enableVault), Optional.empty(), Optional.empty(), Optional.empty()),
                integration("MythicMobs", Optional.of((BooleanSupplier) () -> {
                    mythicMobs = new MythicMobsIntegration(main);
                    return true;
                }), Optional.of(() -> register(new MythicMobsEvents(main))), Optional.empty(), Optional.empty()),
                integration("EliteMobs", Optional.empty(),
                        Optional.of(() -> register(new EliteMobsEvents(main))), Optional.empty(), Optional.empty()),
                integration("WorldEdit", Optional.of((BooleanSupplier) () -> {
                    worldEdit = new WorldEditIntegration(main);
                    return true;
                }), Optional.empty(), Optional.empty(), Optional.empty()),
                integration("Citizens", Optional.of(this::enableCitizens),
                        Optional.of(() -> register(new CitizensEvents(main))), Optional.empty(), Optional.of(this::closeCitizens)),
                integration("FancyNpcs", Optional.of((BooleanSupplier) () -> {
                    fancyNpcs = new FancyNPCsIntegration(main);
                    return true;
                }), Optional.of(() -> register(new FancyNPCsEvents(main))), Optional.empty(), Optional.empty()),
                integration("Slimefun", Optional.empty(),
                        Optional.of(() -> register(new SlimefunEvents(main))), Optional.empty(), Optional.empty()),
                integration("LuckPerms", Optional.of((BooleanSupplier) () -> {
                    luckPerms = new LuckPermsIntegration(main);
                    return true;
                }), Optional.empty(), Optional.empty(), Optional.empty()),
                integration("Towny", Optional.empty(),
                        Optional.of(() -> register(new TownyEvents(main))), Optional.empty(), Optional.empty()),
                integration("Jobs", Optional.empty(), Optional.of(this::registerJobs), Optional.empty(), Optional.empty()),
                integration("Floodgate", Optional.of((BooleanSupplier) () -> {
                    floodgate = new FloodgateIntegration(main);
                    return true;
                }), Optional.empty(), Optional.empty(), Optional.empty()),
                integration("BetonQuest", Optional.of(this::enableBetonQuest),
                        Optional.of(() -> register(new BetonQuestEvents(main))), Optional.empty(), Optional.empty()));
    }

    @Override
    public List<NativeIntegration> integrations() {
        return integrations;
    }

    private IntegrationPlugin plugin(final String name) {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin == null
                ? new IntegrationPlugin(name, "", false)
                : new IntegrationPlugin(
                        plugin.getName(),
                        plugin.getDescription().getVersion(),
                        plugin.isEnabled());
    }

    private NativeIntegration integration(
            final String name,
            final Optional<BooleanSupplier> enable,
            final Optional<Runnable> registerEvents,
            final Optional<Runnable> dataLoaded,
            final Optional<Runnable> close) {
        return new NativeIntegration(
                name,
                () -> plugin(name),
                enable,
                registerEvents,
                dataLoaded,
                close);
    }

    private void register(final Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, main.getMain());
    }

    private boolean enableVault() {
        vault = new VaultIntegration(main);
        if (!vault.setupEconomy()) {
            vault = null;
            return false;
        }
        vault.setupPermissions();
        vault.setupChat();
        return true;
    }

    private boolean enableCitizens() {
        citizens = new CitizensIntegration(main);
        citizens.registerQuestGiverTrait();
        return true;
    }

    private void closeCitizens() {
        if (citizens != null) {
            citizens.onDisable();
        }
    }

    private void registerJobs() {
        register(new JobsRebornEvents(main));
        main.getCorePlugin().enableJobsLevelSync(JobsRebornEvents::currentLevel);
    }

    @Override
    public boolean spawnMythicMob(final String entityType, final NQLocation location) {
        final Location nativeLocation = PaperNotQuestsAdapter.paperBukkitLocation(location);
        return nativeLocation != null
                && nativeLocation.getWorld() != null
                && mythicMobs != null
                && mythicMobs.isMythicMob(entityType)
                && mythicMobs.spawnOneMob(entityType, nativeLocation);
    }

    @Override
    public boolean spawnEcoMob(final String entityType, final NQLocation location) {
        final Location nativeLocation = PaperNotQuestsAdapter.paperBukkitLocation(location);
        return nativeLocation != null
                && nativeLocation.getWorld() != null
                && ecoMobs != null
                && ecoMobs.isEcoMob(entityType)
                && ecoMobs.spawnOneMob(entityType, nativeLocation);
    }

    private boolean enableBetonQuest() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("BetonQuest");
        final String version = plugin == null ? "" : plugin.getDescription().getVersion();
        if (!supportsBetonQuestVersion(version)) {
            return false;
        }
        betonQuest = new BetonQuestIntegration(main);
        return betonQuest.enable();
    }

    static boolean supportsBetonQuestVersion(final String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        final Matcher matcher = BETONQUEST_VERSION.matcher(version.trim());
        if (!matcher.matches()) {
            return false;
        }
        return Integer.parseInt(matcher.group(1)) >= 3;
    }

    public MythicMobsIntegration mythicMobs() { return mythicMobs; }
    public EcoMobsIntegration ecoMobs() { return ecoMobs; }
    public WorldEditIntegration worldEdit() { return worldEdit; }
    public LuckPermsIntegration luckPerms() { return luckPerms; }
    public CitizensIntegration citizens() { return citizens; }
    public FancyNPCsIntegration fancyNpcs() { return fancyNpcs; }
    public FloodgateIntegration floodgate() { return floodgate; }
    public BetonQuestIntegration betonQuest() { return betonQuest; }
    public VaultIntegration vault() { return vault; }
}
