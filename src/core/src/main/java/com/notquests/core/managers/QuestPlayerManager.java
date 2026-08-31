package com.notquests.core.managers;

import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer;
import com.notquests.core.triggers.ActiveTrigger;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Owns every loaded player's profile, quest progress, and connected platform handle. */
public final class QuestPlayerManager {
    private final Map<String, QuestPlayer> players = new ConcurrentHashMap<>();
    private final Map<String, String> activeProfiles = new ConcurrentHashMap<>();
    private final Map<String, PlatformPlayer> connectedPlayers = new ConcurrentHashMap<>();
    private final Set<String> loadingPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean loadingAllPlayers = new AtomicBoolean();
    private final ActiveObjectives activeObjectives = new ActiveObjectives();
    private final Map<String, CopyOnWriteArrayList<ActiveTrigger>> activeTriggers =
            new ConcurrentHashMap<>();
    private final ThreadLocal<PlayerProfile> runtimeProfile = new ThreadLocal<>();

    private Consumer<ActiveObjective> hideObjectiveMarker = ignored -> {};
    private Consumer<PlatformPlayer> hideProgress = ignored -> {};
    private Consumer<ActiveObjective> showObjectiveMarker = ignored -> {};

    public QuestPlayerManager() {
        activeObjectives.activeProfileLookup(this::getActiveProfile);
        activeObjectives.players(this::getQuestPlayer, this::getAllQuestPlayers);
    }

    public void configureObjectives(
            final ActiveObjective.ConditionGate conditionGate,
            final ActiveObjective.ProgressChangeHandler progressChangeHandler,
            final Consumer<ActiveObjective> completionHandler,
            final Predicate<ActiveObjective> unlockHandler) {
        activeObjectives.conditionGate(conditionGate);
        activeObjectives.progressChangeHandler(progressChangeHandler);
        activeObjectives.completionHandler(completionHandler);
        activeObjectives.unlockHandler(unlockHandler);
    }

    public void configureProfileDisplay(
            final Consumer<ActiveObjective> hideObjectiveMarker,
            final Consumer<PlatformPlayer> hideProgress,
            final Consumer<ActiveObjective> showObjectiveMarker) {
        this.hideObjectiveMarker = hideObjectiveMarker == null ? ignored -> {} : hideObjectiveMarker;
        this.hideProgress = hideProgress == null ? ignored -> {} : hideProgress;
        this.showObjectiveMarker = showObjectiveMarker == null ? ignored -> {} : showObjectiveMarker;
    }

    public ActiveObjectives getActiveObjectives() {
        return activeObjectives;
    }

    public List<QuestPlayer> getAllQuestPlayers() {
        return players.values().stream()
                .sorted(Comparator
                        .comparing(QuestPlayer::getPlayerIdentifier, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(QuestPlayer::getProfile, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> getPlayerIdentifiers() {
        return players.values().stream()
                .map(QuestPlayer::getPlayerIdentifier)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Map<String, String> getActiveProfiles() {
        return Map.copyOf(activeProfiles);
    }

    public QuestPlayer getQuestPlayer(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            throw new IllegalArgumentException("Player identifier cannot be blank.");
        }
        final String cleanProfile = cleanProfile(profile);
        activeProfiles.putIfAbsent(playerIdentifier, cleanProfile);
        return players.computeIfAbsent(
                playerProfileKey(playerIdentifier, cleanProfile),
                ignored -> new QuestPlayer(playerIdentifier, cleanProfile));
    }

    public QuestPlayer getQuestPlayer(final PlatformPlayer platformPlayer) {
        if (platformPlayer == null || platformPlayer.playerIdentifier().isBlank()) {
            return null;
        }
        return getQuestPlayer(platformPlayer.playerIdentifier(), getActiveProfile(platformPlayer.playerIdentifier()));
    }

    public QuestPlayer getQuestPlayerIfLoaded(final String playerIdentifier) {
        return getQuestPlayerIfLoaded(playerIdentifier, getActiveProfile(playerIdentifier));
    }

    public QuestPlayer getQuestPlayerIfLoaded(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return null;
        }
        return players.get(playerProfileKey(playerIdentifier, cleanProfile(profile)));
    }

    public QuestPlayer getActiveQuestPlayer(final String playerIdentifier) {
        return getQuestPlayer(playerIdentifier, getActiveProfile(playerIdentifier));
    }

    public PlatformPlayer getPlatformPlayer(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return null;
        }
        return connectedPlayers.get(playerIdentifier);
    }

    public PlatformPlayer register(
            final PlatformPlayer platformPlayer,
            final String profile,
            final boolean active) {
        if (platformPlayer == null
                || platformPlayer.playerIdentifier() == null
                || platformPlayer.playerIdentifier().isBlank()) {
            return null;
        }
        final String cleanProfile = cleanProfile(profile);
        getQuestPlayer(platformPlayer.playerIdentifier(), cleanProfile);
        connectedPlayers.put(platformPlayer.playerIdentifier(), platformPlayer);
        if (active) {
            activateProfile(platformPlayer.playerIdentifier(), cleanProfile);
        }
        return platformPlayer;
    }

    public List<PlatformPlayer> getConnectedPlayers() {
        return List.copyOf(connectedPlayers.values());
    }

    public List<PlatformPlayer> getActivePlatformPlayers() {
        return activeProfiles.keySet().stream()
                .map(connectedPlayers::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public void disconnect(final String playerIdentifier) {
        if (playerIdentifier != null && !playerIdentifier.isBlank()) {
            connectedPlayers.remove(playerIdentifier);
        }
    }

    public String getActiveProfile(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "default";
        }
        final PlayerProfile restoring = runtimeProfile.get();
        if (restoring != null && restoring.playerIdentifier().equals(playerIdentifier)) {
            return restoring.profile();
        }
        return activeProfiles.getOrDefault(playerIdentifier, "default");
    }

    public void setActiveProfile(final String playerIdentifier, final String profile) {
        if (playerIdentifier != null && !playerIdentifier.isBlank()) {
            activeProfiles.put(playerIdentifier, cleanProfile(profile));
        }
    }

    public List<String> getProfileNames(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return List.of();
        }
        return players.values().stream()
                .filter(player -> player.getPlayerIdentifier().equalsIgnoreCase(playerIdentifier))
                .map(QuestPlayer::getProfile)
                .distinct()
                .sorted()
                .toList();
    }

    public boolean createProfile(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank() || !isValidProfileName(profile)) {
            return false;
        }
        final String cleanProfile = cleanProfile(profile);
        final QuestPlayer created = new QuestPlayer(playerIdentifier, cleanProfile);
        final QuestPlayer existing = players.putIfAbsent(
                playerProfileKey(playerIdentifier, cleanProfile), created);
        if (existing == null) {
            markLoaded(created);
        }
        activeProfiles.putIfAbsent(playerIdentifier, "default");
        return existing == null;
    }

    public boolean changeProfile(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank() || profile == null || profile.isBlank()) {
            return false;
        }
        final String cleanProfile = cleanProfile(profile);
        if (!players.containsKey(playerProfileKey(playerIdentifier, cleanProfile))) {
            return false;
        }
        switchProfile(playerIdentifier, cleanProfile);
        return true;
    }

    public QuestPlayer activateProfile(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank() || !isValidProfileName(profile)) {
            return null;
        }
        final String cleanProfile = cleanProfile(profile);
        final QuestPlayer player = getQuestPlayer(playerIdentifier, cleanProfile);
        switchProfile(playerIdentifier, cleanProfile);
        return player;
    }

    private void switchProfile(final String playerIdentifier, final String profile) {
        final String previousProfile = getActiveProfile(playerIdentifier);
        final PlatformPlayer platformPlayer = connectedPlayers.get(playerIdentifier);
        if (platformPlayer != null && !previousProfile.equalsIgnoreCase(profile)) {
            activeObjectives.activeObjectives(playerIdentifier, previousProfile)
                    .forEach(hideObjectiveMarker);
            hideProgress.accept(platformPlayer);
        }
        activeProfiles.put(playerIdentifier, profile);
        if (platformPlayer == null || previousProfile.equalsIgnoreCase(profile)) {
            return;
        }
        activeObjectives.refreshObjectiveUnlocks(platformPlayer, profile);
        activeObjectives.activeObjectives(playerIdentifier, profile).stream()
                .filter(objective -> objective.isUnlocked() && !objective.hasBeenCompleted())
                .forEach(showObjectiveMarker);
    }

    public void withProfile(
            final String playerIdentifier,
            final String profile,
            final Runnable action) {
        final PlayerProfile previous = runtimeProfile.get();
        runtimeProfile.set(new PlayerProfile(playerIdentifier, cleanProfile(profile)));
        try {
            action.run();
        } finally {
            if (previous == null) {
                runtimeProfile.remove();
            } else {
                runtimeProfile.set(previous);
            }
        }
    }

    public void beginLoading(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            loadingAllPlayers.set(true);
        } else {
            loadingPlayers.add(playerIdentifier);
        }
    }

    public void finishLoading(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            loadingAllPlayers.set(false);
        } else {
            loadingPlayers.remove(playerIdentifier);
        }
    }

    public boolean isLoading(final String playerIdentifier) {
        return loadingAllPlayers.get()
                || (playerIdentifier != null && loadingPlayers.contains(playerIdentifier));
    }

    public boolean isLoadingAllPlayers() {
        return loadingAllPlayers.get();
    }

    public Set<String> getPlayersReadyToSave() {
        return getAllQuestPlayers().stream()
                .filter(player -> player.isFinishedLoadingGeneralData()
                        && !loadingPlayers.contains(player.getPlayerIdentifier()))
                .map(QuestPlayer::getPlayerIdentifier)
                .filter(identifier -> identifier != null && !identifier.isBlank())
                .collect(Collectors.toSet());
    }

    public void markLoaded(final PlatformPlayer platformPlayer) {
        if (platformPlayer != null) {
            markLoaded(getQuestPlayer(platformPlayer));
        }
    }

    public void markLoaded(final QuestPlayer player) {
        if (player == null) {
            return;
        }
        player.setCurrentlyLoading(false);
        player.setFinishedLoadingGeneralData(true);
        player.setFinishedLoadingTags(true);
    }

    public void clear() {
        players.clear();
        activeProfiles.clear();
        connectedPlayers.clear();
        loadingPlayers.clear();
        loadingAllPlayers.set(false);
        clearProgress();
        runtimeProfile.remove();
    }

    public void clearProgress() {
        activeObjectives.clear();
        activeTriggers.clear();
    }

    public void clear(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return;
        }
        players.entrySet().removeIf(entry -> entry.getValue() != null
                && playerIdentifier.equals(entry.getValue().getPlayerIdentifier()));
        activeProfiles.remove(playerIdentifier);
        connectedPlayers.remove(playerIdentifier);
        activeObjectives.removePlayer(playerIdentifier);
        activeTriggers.keySet().removeIf(key -> playerProfileKeyBelongsTo(key, playerIdentifier));
    }

    public List<ActiveTrigger> getActiveTriggers(final String playerIdentifier) {
        return getActiveTriggers(playerIdentifier, getActiveProfile(playerIdentifier));
    }

    public List<ActiveTrigger> getActiveTriggers(final String playerIdentifier, final String profile) {
        final List<ActiveTrigger> triggers = activeTriggers.get(playerProfileKey(playerIdentifier, profile));
        return triggers == null ? List.of() : List.copyOf(triggers);
    }

    public void activateTriggers(
            final PlatformPlayer platformPlayer,
            final String profile,
            final Quest quest) {
        if (platformPlayer == null || quest == null) {
            return;
        }
        final CopyOnWriteArrayList<ActiveTrigger> triggers = activeTriggers.computeIfAbsent(
                playerProfileKey(platformPlayer.playerIdentifier(), profile),
                ignored -> new CopyOnWriteArrayList<>());
        triggers.removeIf(trigger -> trigger.questName().equalsIgnoreCase(quest.getIdentifier()));
        quest.getTriggers().stream()
                .map(trigger -> new ActiveTrigger(quest.getIdentifier(), trigger))
                .forEach(triggers::add);
    }

    public void removeActiveTriggers(final String playerIdentifier, final String questName) {
        removeActiveTriggers(playerIdentifier, getActiveProfile(playerIdentifier), questName);
    }

    public void removeActiveTriggers(
            final String playerIdentifier,
            final String profile,
            final String questName) {
        final List<ActiveTrigger> triggers = activeTriggers.get(playerProfileKey(playerIdentifier, profile));
        if (triggers != null && questName != null && !questName.isBlank()) {
            triggers.removeIf(trigger -> trigger.questName().equalsIgnoreCase(questName));
        }
    }

    public boolean setActiveTriggerProgress(
            final String playerIdentifier,
            final String profile,
            final String questName,
            final int triggerId,
            final long progress) {
        if (questName == null || questName.isBlank()) {
            return false;
        }
        for (final ActiveTrigger trigger : getActiveTriggers(playerIdentifier, profile)) {
            if (trigger.questName().equalsIgnoreCase(questName) && trigger.triggerId() == triggerId) {
                trigger.setCurrentProgress(progress);
                return true;
            }
        }
        return false;
    }

    public static String cleanProfile(final String profile) {
        return profile == null || profile.isBlank() ? "default" : profile;
    }

    public static boolean isValidProfileName(final String profile) {
        return profile != null && !profile.isBlank() && profile.matches("[0-9A-Za-z._-]+");
    }

    private static String playerProfileKey(final String playerIdentifier, final String profile) {
        return playerIdentifier + "\u0000" + cleanProfile(profile).toLowerCase(Locale.ROOT);
    }

    private static boolean playerProfileKeyBelongsTo(
            final String playerProfileKey,
            final String playerIdentifier) {
        return playerProfileKey != null
                && playerIdentifier != null
                && playerProfileKey.startsWith(playerIdentifier + "\u0000");
    }

    private record PlayerProfile(String playerIdentifier, String profile) {}
}
