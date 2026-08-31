package com.notquests.core.managers.integrations;

import com.notquests.core.NotQuestsPlatform;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Owns optional-plugin discovery and lifecycle. Platforms only perform native setup and cleanup. */
public final class IntegrationsManager {
    private static final List<String> NAMES = List.of(
            "EcoMobs",
            "PlaceholderAPI",
            "Vault",
            "MythicMobs",
            "EliteMobs",
            "WorldEdit",
            "Citizens",
            "FancyNpcs",
            "Slimefun",
            "LuckPerms",
            "Towny",
            "Jobs",
            "Floodgate",
            "BetonQuest");

    private final NotQuestsPlugin plugin;
    private final Map<String, NotQuestsPlatform.IntegrationPlugin> enabled = new LinkedHashMap<>();
    private final Map<String, NotQuestsPlatform.NativeIntegration> available = new LinkedHashMap<>();
    private Optional<NotQuestsPlatform.NativeIntegrations> nativeIntegrations = Optional.empty();
    private boolean dataLoaded;

    public IntegrationsManager(final NotQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void enableConfigured(final NotQuestsPlatform platform, final ConfigurationManager configuration) {
        nativeIntegrations = platform == null ? Optional.empty() : platform.nativeIntegrations();
        available.clear();
        nativeIntegrations.ifPresent(support -> support.integrations().forEach(
                integration -> available.put(key(integration.name()), integration)));
        for (final String name : NAMES) {
            enable(name, configuration, false);
        }
    }

    public void registerEvents() {
        for (final NotQuestsPlatform.IntegrationPlugin integration : List.copyOf(enabled.values())) {
            available.get(key(integration.name())).registerEvents().ifPresent(registerEvents -> {
                registerEvents.run();
                plugin.integrationEventsRegistered(integration.name());
            });
        }
    }

    public void dataLoaded(final ConfigurationManager configuration) {
        dataLoaded = true;
        for (final String name : NAMES) {
            enable(name, configuration, false);
        }
        for (final NotQuestsPlatform.IntegrationPlugin integration : List.copyOf(enabled.values())) {
            available.get(key(integration.name())).dataLoaded().ifPresent(Runnable::run);
        }
    }

    public void pluginEnabled(final String name, final ConfigurationManager configuration) {
        if (name == null || name.isBlank() || isEnabled(name)) {
            return;
        }
        final String configuredName = configuredName(name);
        if (configuredName == null || !enable(configuredName, configuration, true)) {
            return;
        }
        available.get(key(configuredName)).registerEvents().ifPresent(registerEvents -> {
            registerEvents.run();
            plugin.integrationEventsRegistered(configuredName);
        });
        if (dataLoaded) {
            available.get(key(configuredName)).dataLoaded().ifPresent(Runnable::run);
        }
    }

    public void close() {
        for (final NotQuestsPlatform.IntegrationPlugin integration : List.copyOf(enabled.values())) {
            available.get(key(integration.name())).close().ifPresent(Runnable::run);
        }
        enabled.clear();
        available.clear();
        dataLoaded = false;
        nativeIntegrations = Optional.empty();
    }

    public boolean isEnabled(final String name) {
        return name != null && enabled.containsKey(key(name));
    }

    public List<NotQuestsAdapter.Integration> getEnabledVersions() {
        final ArrayList<NotQuestsAdapter.Integration> versions = new ArrayList<>();
        for (final NotQuestsPlatform.IntegrationPlugin integration : enabled.values()) {
            versions.add(new NotQuestsAdapter.Integration(integration.name(), integration.version()));
        }
        return List.copyOf(versions);
    }

    public boolean spawnMythicMob(
            final String entityType,
            final NQLocation location) {
        return isEnabled("MythicMobs")
                && nativeIntegrations.map(support -> support.spawnMythicMob(entityType, location)).orElse(false);
    }

    public boolean spawnEcoMob(
            final String entityType,
            final NQLocation location) {
        return isEnabled("EcoMobs")
                && nativeIntegrations.map(support -> support.spawnEcoMob(entityType, location)).orElse(false);
    }

    private boolean enable(
            final String name,
            final ConfigurationManager configuration,
            final boolean late) {
        if (nativeIntegrations.isEmpty() || isEnabled(name) || !configured(name, configuration)) {
            return false;
        }
        final NotQuestsPlatform.NativeIntegration integration = available.get(key(name));
        if (integration == null) {
            if (name.equals("Citizens") || name.equals("FancyNpcs")) {
                plugin.integrationUnavailable(name);
            }
            return false;
        }
        final NotQuestsPlatform.IntegrationPlugin nativePlugin = integration.plugin().get();
        if (nativePlugin == null || !nativePlugin.enabled()) {
            if (name.equals("Citizens") || name.equals("FancyNpcs")) {
                plugin.integrationUnavailable(name);
            }
            return false;
        }
        if (!integration.enable().map(callback -> callback.getAsBoolean()).orElse(true)) {
            plugin.integrationUnavailable(name);
            return false;
        }
        enabled.put(key(name), nativePlugin);
        plugin.integrationEnabled(name, nativePlugin.version(), late);
        return true;
    }

    private static boolean configured(final String name, final ConfigurationManager configuration) {
        return switch (name) {
            case "EcoMobs" -> configuration.integrationEcoMobsEnabled();
            case "PlaceholderAPI" -> configuration.integrationPlaceholderApiEnabled();
            case "Vault" -> configuration.integrationVaultEnabled();
            case "MythicMobs" -> configuration.integrationMythicMobsEnabled();
            case "EliteMobs" -> configuration.integrationEliteMobsEnabled();
            case "WorldEdit" -> configuration.integrationWorldEditEnabled();
            case "Citizens" -> configuration.integrationCitizensEnabled();
            case "FancyNpcs" -> true;
            case "Slimefun" -> configuration.integrationSlimefunEnabled();
            case "LuckPerms" -> configuration.integrationLuckPermsEnabled();
            case "Towny" -> configuration.integrationTownyEnabled();
            case "Jobs" -> configuration.integrationJobsRebornEnabled();
            case "Floodgate" -> configuration.integrationFloodgateEnabled();
            case "BetonQuest" -> configuration.integrationBetonQuestEnabled();
            default -> false;
        };
    }

    private static String configuredName(final String name) {
        for (final String configured : NAMES) {
            if (configured.equalsIgnoreCase(name)) {
                return configured;
            }
        }
        return null;
    }

    private static String key(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
