package com.notquests.core.conversation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.config.CategoryFiles;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class ConversationManager {
    private final Map<String, StoredConversation> conversations = new LinkedHashMap<>();
    private final Map<String, ActiveConversation> activeConversationByPlayer = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<Component>> chatReplayHistory = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<Component>> conversationReplayHistory = new ConcurrentHashMap<>();
    private final Map<Integer, Set<UUID>> activePlayersByNpc = new ConcurrentHashMap<>();
    private final Map<Integer, Runnable> npcIdleEffects = new ConcurrentHashMap<>();
    private final Map<String, FocusSession> focusByPlayer = new ConcurrentHashMap<>();
    private ConversationRuntime runtime = ConversationRuntime.noop();
    private Display display = Display.plain();

    public static Path create(
            final Path dataFolder,
            final String conversationName,
            final boolean demo,
            final String categoryName) throws IOException {
        CategoryFiles.copyMissingCategory(dataFolder, categoryName);
        return createInFolder(
                conversationsFolder(dataFolder, categoryName),
                conversationName,
                demo);
    }

    public static Path createInFolder(
            final Path conversationsFolder,
            final String conversationName,
            final boolean demo) throws IOException {
        final String safeName = sanitizeConversationName(conversationName);
        Files.createDirectories(conversationsFolder);
        final Path target = conversationsFolder.resolve(safeName + ".yml");
        if (Files.exists(target)) {
            throw new IOException("StoredConversation file already exists: " + target);
        }
        Files.writeString(
                target,
                ConfigurationManager.bundledResourceText(demo ? "conversations/demo.yml" : "conversations/empty.yml"),
                StandardCharsets.UTF_8);
        return target;
    }

    public static Path conversationsFolder(final Path dataFolder, final String categoryName) {
        return CategoryFiles.categoryFolder(dataFolder, categoryName).resolve(CategoryFiles.CONVERSATIONS_FOLDER);
    }

    public void runtime(final ConversationRuntime runtime) {
        this.runtime = runtime == null ? ConversationRuntime.noop() : runtime;
    }

    public void display(final Display display) {
        this.display = display == null ? Display.plain() : display;
    }

    public void save(final String name, final List<String> lines) {
        save(name, lines, "");
    }

    public void save(final String name, final List<String> lines, final String category) {
        final String id = normalizedName(name);
        if (id.isBlank()) {
            throw new IllegalArgumentException("StoredConversation name cannot be blank.");
        }
        final List<String> cleanLines = lines == null ? List.of() : lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .toList();
        if (cleanLines.isEmpty()) {
            throw new IllegalArgumentException("StoredConversation must contain at least one line.");
        }
        final StoredConversation existing = conversations.get(id);
        final StoredConversation conversation = StoredConversation.simple(name, cleanLines, selectedCategory(category, existing), 0);
        if (existing != null) {
            conversation.copyEditableMetadataFrom(existing);
        }
        conversations.put(id, conversation);
    }

    public void save(final String name, final Map<String, Object> yaml) {
        save(name, yaml, stringValue(yaml == null ? null : value(yaml, "category")));
    }

    public void save(final String name, final Map<String, Object> yaml, final String category) {
        final String id = normalizedName(name);
        if (id.isBlank()) {
            throw new IllegalArgumentException("StoredConversation name cannot be blank.");
        }
        final StoredConversation existing = conversations.get(id);
        final StoredConversation conversation = parse(name, yaml, selectedCategory(category, existing));
        conversations.put(id, conversation);
    }

    public Conversation conversation(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? null : conversation.snapshot();
    }

    public List<String> names() {
        return conversations.values().stream()
                .map(StoredConversation::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<Conversation> conversations() {
        return conversations.values().stream()
                .map(StoredConversation::snapshot)
                .sorted(Comparator.comparing(Conversation::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void clear() {
        conversations.clear();
        activeConversationByPlayer.clear();
        chatReplayHistory.clear();
        conversationReplayHistory.clear();
        clearFocusSessions();
        clearNpcSessions();
    }

    public void startNpcSession(final int npcId, final UUID playerId) {
        startNpcSession(npcId, playerId, null);
    }

    public synchronized void startNpcSession(
            final int npcId,
            final UUID playerId,
            final Runnable whenNpcBecomesIdle) {
        if (playerId != null) {
            activePlayersByNpc.computeIfAbsent(npcId, id -> ConcurrentHashMap.newKeySet()).add(playerId);
            if (whenNpcBecomesIdle != null) {
                npcIdleEffects.put(npcId, whenNpcBecomesIdle);
            }
        }
    }

    public void stopNpcSession(final int npcId, final UUID playerId) {
        Runnable idleEffect = null;
        synchronized (this) {
            if (playerId != null) {
                final Set<UUID> players = activePlayersByNpc.get(npcId);
                if (players != null) {
                    players.remove(playerId);
                    if (players.isEmpty()) {
                        activePlayersByNpc.remove(npcId);
                        idleEffect = npcIdleEffects.remove(npcId);
                    }
                }
            }
        }
        if (idleEffect != null) {
            idleEffect.run();
        }
    }

    public void clearNpcSessions() {
        final List<Runnable> idleEffects;
        synchronized (this) {
            activePlayersByNpc.clear();
            idleEffects = List.copyOf(npcIdleEffects.values());
            npcIdleEffects.clear();
        }
        idleEffects.forEach(Runnable::run);
    }

    public Map<Integer, List<UUID>> npcSessionSnapshot() {
        final Map<Integer, List<UUID>> copy = new ConcurrentHashMap<>();
        activePlayersByNpc.forEach((npcId, players) -> copy.put(npcId, new ArrayList<>(players)));
        return copy;
    }

    public boolean hasNpcSession(final int npcId) {
        final Set<UUID> players = activePlayersByNpc.get(npcId);
        return players != null && !players.isEmpty();
    }

    public void rememberConversationMessage(final String playerId, final Component component) {
        if (component == null || playerId == null || playerId.isBlank()) {
            return;
        }
        conversationReplayHistory.compute(playerId, (id, history) -> {
            final ArrayList<Component> list = history == null ? new ArrayList<>() : history;
            synchronized (list) {
                list.add(component);
            }
            return list;
        });
    }

    public void rememberNonConversationMessage(final String playerId, final Component component, final int maxHistory) {
        if (component == null || playerId == null || playerId.isBlank()) {
            return;
        }
        final ArrayList<Component> activeConvHistory = conversationReplayHistory.get(playerId);
        if (activeConvHistory != null) {
            synchronized (activeConvHistory) {
                if (activeConvHistory.remove(component)) {
                    return;
                }
            }
        }
        chatReplayHistory.compute(playerId, (id, history) -> {
            final ArrayList<Component> list = history == null ? new ArrayList<>() : history;
            synchronized (list) {
                list.add(component);
                final int toRemove = list.size() - Math.max(0, maxHistory);
                if (toRemove > 0) {
                    list.subList(0, toRemove).clear();
                }
            }
            return list;
        });
    }

    public Component removeConversationMessages(final String playerId) {
        final ArrayList<Component> allConv = conversationReplayHistory.get(playerId);
        if (allConv == null) {
            return null;
        }
        final ArrayList<Component> allChat = chatReplayHistory.computeIfAbsent(
                playerId,
                ignored -> new ArrayList<>());
        Component collective = Component.text("");
        synchronized (allChat) {
            for (final Component c : allChat) {
                if (c != null) {
                    collective = collective.append(c).append(Component.newline());
                }
            }
        }
        final Component replay = Component.text("\n".repeat(100)).append(collective);
        synchronized (allConv) {
            allConv.add(replay);
        }
        return replay;
    }

    public int lineCount(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? 0 : conversation.lines().size();
    }

    public int startingLineCount(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? 0 : conversation.getStartingLines().size();
    }

    public List<String> lines(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? List.of() : conversation.lines();
    }

    public List<String> analyze(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        if (conversation == null) {
            return List.of();
        }
        return conversation.analyze();
    }

    public boolean start(final PlatformPlayer questPlayer, final String name, final boolean endPrevious) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return false;
        }
        final String playerId = questPlayer.playerIdentifier();
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        if (endPrevious) {
            removeConversation(playerId);
        } else if (activeConversationByPlayer.containsKey(playerId)) {
            return false;
        }
        final StoredConversation conversation = conversations.get(normalizedName(name));
        if (conversation == null) {
            return false;
        }
        final ActiveConversation activeConversation = new ActiveConversation(conversation);
        activeConversationByPlayer.put(playerId, activeConversation);
        if (conversation.simpleLines()) {
            for (final String line : conversation.lines()) {
                showMessage(questPlayer, line, runtime.component(line), true);
            }
            return true;
        }
        final boolean started = playStartingLine(questPlayer, activeConversation);
        if (!started) {
            if (activeConversationByPlayer.remove(playerId, activeConversation)) {
                conversationEnded(playerId);
            }
        }
        return started;
    }

    public String activeConversation(final String playerId) {
        final ActiveConversation activeConversation = activeConversationByPlayer.get(playerId);
        return activeConversation == null ? null : activeConversation.conversation().name();
    }

    public boolean stop(final PlatformPlayer questPlayer) {
        return questPlayer != null && stop(questPlayer.playerIdentifier());
    }

    public boolean stop(final String playerId) {
        return removeConversation(playerId);
    }

    public List<String> optionIds(final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of();
        }
        final ActiveConversation activeConversation = activeConversationByPlayer.get(questPlayer.playerIdentifier());
        if (activeConversation == null) {
            return List.of();
        }
        final int optionCount = activeConversation.conversation().simpleLines()
                ? activeConversation.conversation().lines().size()
                : activeConversation.currentPlayerLines().size();
        final List<String> options = new ArrayList<>();
        for (int option = 1; option <= optionCount; option++) {
            options.add(String.valueOf(option));
        }
        return List.copyOf(options);
    }

    public boolean chooseOption(final PlatformPlayer questPlayer, final int optionId) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return false;
        }
        final String playerId = questPlayer.playerIdentifier();
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        final ActiveConversation activeConversation = activeConversationByPlayer.get(playerId);
        if (activeConversation == null) {
            return false;
        }
        if (activeConversation.conversation().simpleLines()) {
            removeConversation(playerId);
            return optionId > 0;
        }
        if (optionId < 1 || optionId > activeConversation.currentPlayerLines().size()) {
            return false;
        }
        final StoredConversationLine option = activeConversation.currentPlayerLines().get(optionId - 1);
        executeActions(option, questPlayer);
        if (!isCurrentConversation(playerId, activeConversation)) {
            return true;
        }
        activeConversation.currentPlayerLines().clear();
        final ArrayList<StoredConversationLine> next = playable(option.getNext(), questPlayer);
        if (next == null || next.isEmpty()) {
            finishWhenScheduledWorkCompletes(playerId, activeConversation);
            return true;
        }
        if (next.size() == 1 && !next.getFirst().getSpeaker().isPlayer()) {
            continueLine(questPlayer, activeConversation, next.getFirst(), true, new HashSet<>());
        } else {
            sendPlayerOptions(questPlayer, activeConversation, next);
        }
        return true;
    }

    public boolean exists(final String name) {
        return conversations.containsKey(normalizedName(name));
    }

    public String category(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? "" : conversation.category();
    }

    public boolean category(final String name, final String category) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        if (conversation == null) {
            return false;
        }
        conversation.category(category);
        return true;
    }

    public boolean addSpeaker(final String name, final String speakerName, final String color) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation != null && conversation.addSpeaker(speakerName, color);
    }

    public List<Speaker> speakers(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? List.of() : conversation.speakers();
    }

    public boolean removeSpeaker(final String name, final String speakerName) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation != null && conversation.removeSpeaker(speakerName);
    }

    public boolean addNpcAttachment(
            final String name,
            final String npcType,
            final NQNPCID npcId,
            final String npcName) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        if (conversation == null) {
            return false;
        }
        conversation.addNpcAttachment(npcType, npcId, npcName);
        return true;
    }

    public boolean removeNpcAttachment(
            final String name,
            final String npcType,
            final NQNPCID npcId) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation != null && conversation.removeNpcAttachment(npcType, npcId);
    }

    public boolean clearNpcAttachments(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        if (conversation == null) {
            return false;
        }
        conversation.clearNpcAttachments();
        return true;
    }

    public List<NpcAttachment> npcAttachments(final String name) {
        final StoredConversation conversation = conversations.get(normalizedName(name));
        return conversation == null ? List.of() : conversation.npcAttachments();
    }

    public String conversationAttachedToNpc(final String npcType, final NQNPCID npcId) {
        if (npcId == null) {
            return null;
        }
        final String checkedType = npcType == null ? "" : npcType;
        for (final StoredConversation conversation : conversations.values()) {
            for (final NpcAttachment attachment : conversation.npcAttachments()) {
                if (Objects.equals(attachment.npcId(), npcId)
                        && (attachment.npcType().isBlank()
                                || attachment.npcType().equalsIgnoreCase(checkedType))) {
                    return conversation.name();
                }
            }
        }
        return null;
    }

    private static StoredConversation parse(
            final String name,
            final Map<String, Object> yaml,
            final String category) {
        final Map<String, Object> root = yaml == null ? Map.of() : yaml;
        final List<String> simpleLines = strings(value(root, "lines"));
        final Map<String, Object> speakers = mapValue(value(root, "Lines"));
        if (speakers.isEmpty()) {
            if (simpleLines.isEmpty()) {
                throw new IllegalArgumentException("StoredConversation must contain at least one line.");
            }
            final StoredConversation conversation = StoredConversation.simple(
                    name,
                    simpleLines,
                    category,
                    intValue(value(root, "delay"), 0));
            for (final NpcAttachment attachment : npcAttachments(root)) {
                conversation.addNpcAttachment(attachment.npcType(), attachment.npcId(), attachment.npcName());
            }
            return conversation;
        }

        final int delayMillis = intValue(value(root, "delay"), 0);
        final StoredConversation conversation = new StoredConversation(name, category, delayMillis, false);
        final List<String> startIds = lineIds(stringValue(value(root, "start")));
        final Set<String> normalizedStartIds = new HashSet<>();
        for (final String startId : startIds) {
            normalizedStartIds.add(normalizedName(startId));
        }

        for (final Map.Entry<String, Object> speakerEntry : speakers.entrySet()) {
            final String speakerName = speakerEntry.getKey();
            if (speakerName == null || speakerName.isBlank()) {
                continue;
            }
            final Map<String, Object> speakerMap = mapValue(speakerEntry.getValue());
            final Speaker speaker = new Speaker(speakerName, delayMillis);
            final String color = stringValue(value(speakerMap, "color"));
            if (!color.isBlank()) {
                speaker.setColor(color);
            }
            if (containsKey(speakerMap, "delay")) {
                speaker.setDelayInMS(intValue(value(speakerMap, "delay"), speaker.getDelayInMS()));
            }
            if (containsKey(speakerMap, "talkSpeed")) {
                speaker.setTalkSpeed(intValue(value(speakerMap, "talkSpeed"), speaker.getTalkSpeed()));
            }
            if (speakerName.equalsIgnoreCase("player") || boolValue(value(speakerMap, "player"), false)) {
                speaker.setPlayer(true);
            }
            conversation.addSpeaker(speaker);
        }

        for (final Map.Entry<String, Object> speakerEntry : speakers.entrySet()) {
            final Speaker speaker = conversation.speaker(speakerEntry.getKey());
            if (speaker == null) {
                continue;
            }
            final Map<String, Object> speakerMap = mapValue(speakerEntry.getValue());
            for (final Map.Entry<String, Object> lineEntry : speakerMap.entrySet()) {
                if (isSpeakerMetadata(lineEntry.getKey())) {
                    continue;
                }
                final Map<String, Object> lineMap = mapValue(lineEntry.getValue());
                if (lineMap.isEmpty()) {
                    continue;
                }
                final String fullIdentifier = speaker.getSpeakerName() + "." + lineEntry.getKey();
                final boolean startingLine = normalizedStartIds.contains(normalizedName(fullIdentifier));
                final MessageValues messageValues = messages(lineMap, startingLine);
                final StoredConversationLine line = new StoredConversationLine(
                        speaker,
                        lineEntry.getKey(),
                        messageValues.messages(),
                        messageValues.textsList());
                for (final String nextId : lineIds(stringValue(value(lineMap, "next")))) {
                    line.addNextId(nextId);
                }
                for (final String action : strings(value(lineMap, "actions"))) {
                    line.addAction(action);
                }
                for (final String condition : strings(value(lineMap, "conditions"))) {
                    line.addCondition(condition);
                }
                line.setShouting(boolValue(value(lineMap, "shout"), false));
                if (containsKey(lineMap, "delay")) {
                    final int lineDelay = intValue(value(lineMap, "delay"), 0);
                    if (lineDelay > 0) {
                        line.setDelayInMS(lineDelay);
                    }
                }
                if (!messageValues.messages().isEmpty()
                        && isSkipMarker(messageValues.messages().getFirst())) {
                    line.setSkipMessage(true);
                }
                conversation.addLine(line);
            }
        }

        for (final StoredConversationLine line : conversation.allLines()) {
            for (final String nextId : line.nextIds()) {
                final StoredConversationLine nextLine = conversation.line(nextId);
                if (nextLine != null) {
                    line.addNext(nextLine);
                }
            }
        }
        for (final String startId : startIds) {
            final StoredConversationLine line = conversation.line(startId);
            if (line != null) {
                conversation.addStarterConversationLine(line);
            }
        }
        if (conversation.getStartingLines().isEmpty() && !conversation.allLines().isEmpty()) {
            conversation.addStarterConversationLine(conversation.allLines().getFirst());
        }
        if (conversation.allLines().isEmpty()) {
            throw new IllegalArgumentException("StoredConversation must contain at least one line.");
        }
        for (final NpcAttachment attachment : npcAttachments(root)) {
            conversation.addNpcAttachment(attachment.npcType(), attachment.npcId(), attachment.npcName());
        }
        return conversation;
    }

    private boolean playStartingLine(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation) {
        final ArrayList<StoredConversationLine> startingLines =
                playable(activeConversation.conversation().getStartingLines(), questPlayer);
        if (startingLines == null || startingLines.isEmpty()) {
            return false;
        }
        return continueLine(questPlayer, activeConversation, startingLines.getFirst(), true, new HashSet<>());
    }

    private boolean continueLine(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation,
            final StoredConversationLine currentLine,
            final boolean deletePrevious,
            final Set<String> visited) {
        final String playerId = questPlayer.playerIdentifier();
        if (!isCurrentConversation(playerId, activeConversation)) {
            return false;
        }
        if (!visited.add(normalizedName(currentLine.getFullIdentifier()))) {
            finishWhenScheduledWorkCompletes(playerId, activeConversation);
            return true;
        }
        scheduleSessionWork(
                questPlayer,
                activeConversation,
                currentLine.getDelayInMS(),
                () -> sendLine(questPlayer, activeConversation, currentLine, deletePrevious));
        if (!isCurrentConversation(playerId, activeConversation)) {
            return true;
        }
        final ArrayList<StoredConversationLine> next = playable(currentLine.getNext(), questPlayer);
        if (next == null || next.isEmpty()) {
            finishWhenScheduledWorkCompletes(playerId, activeConversation);
            return true;
        }
        if (next.size() == 1 && !next.getFirst().getSpeaker().isPlayer()) {
            return continueLine(questPlayer, activeConversation, next.getFirst(), currentLine.getSpeaker().isPlayer(), visited);
        }
        sendPlayerOptions(questPlayer, activeConversation, next);
        return true;
    }

    private void sendPlayerOptions(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation,
            final List<StoredConversationLine> playerLines) {
        if (playerLines == null || playerLines.isEmpty()) {
            activeConversation.currentPlayerLines().clear();
            finishWhenScheduledWorkCompletes(questPlayer.playerIdentifier(), activeConversation);
            return;
        }
        scheduleSessionWork(
                questPlayer,
                activeConversation,
                playerLines.getFirst().getDelayInMS(),
                () -> sendPlayerOptionsNow(questPlayer, activeConversation, playerLines));
    }

    private void sendLine(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation,
            final StoredConversationLine line,
            final boolean deletePrevious) {
        if (!line.isSkipMessage()) {
            final Speaker speaker = line.getSpeaker();
            final String message = speaker.getSpeakerName().isBlank()
                    ? line.getOneMessage()
                    : runtime.speakerLine(questPlayer, speaker, line.getOneMessage());
            showMessage(questPlayer, message, runtime.component(message), deletePrevious);
        }
        executeActions(line, questPlayer);
    }

    private void sendPlayerOptionsNow(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation,
            final List<StoredConversationLine> playerLines) {
        activeConversation.currentPlayerLines().clear();
        activeConversation.currentPlayerLines().addAll(playerLines);
        final String prefix = runtime.chooseAnswerPrefix(questPlayer);
        if (!prefix.isBlank()) {
            showMessage(questPlayer, prefix, runtime.component(prefix), false);
        }
        int optionNumber = 1;
        for (final StoredConversationLine playerLine : playerLines) {
            if (!playerLine.isSkipMessage()) {
                final String option = runtime.answerOptionLine(
                        questPlayer,
                        playerLine.getSpeaker(),
                        playerLine.getOneMessage(),
                        optionNumber);
                final Component component = runtime.component(option)
                        .clickEvent(ClickEvent.runCommand("/notquests continueConversation " + optionNumber))
                        .hoverEvent(HoverEvent.showText(runtime.component(runtime.chooseAnswerHover(questPlayer))));
                showMessage(questPlayer, option, component, false);
            }
            optionNumber++;
        }
    }

    private void showMessage(
            final PlatformPlayer questPlayer,
            final String miniMessage,
            final Component component,
            final boolean deletePrevious) {
        Component replay = null;
        if (runtime.deletePreviousMessages()) {
            if (deletePrevious) {
                replay = removeConversationMessages(questPlayer.playerIdentifier());
            }
            rememberConversationMessage(questPlayer.playerIdentifier(), component);
        }
        display.message(
                questPlayer,
                new DisplayMessage(miniMessage == null ? "" : miniMessage, component, replay));
    }

    private void scheduleSessionWork(
            final PlatformPlayer questPlayer,
            final ActiveConversation activeConversation,
            final int delayMillis,
            final Runnable work) {
        final String playerId = questPlayer.playerIdentifier();
        if (delayMillis <= 0) {
            if (isCurrentConversation(playerId, activeConversation)) {
                work.run();
                removeFinishedConversation(playerId, activeConversation);
            }
            return;
        }
        activeConversation.scheduledWorkStarted();
        try {
            runtime.schedule(Duration.ofMillis(delayMillis), () -> {
                if (!isCurrentConversation(playerId, activeConversation)) {
                    return;
                }
                try {
                    work.run();
                } finally {
                    activeConversation.scheduledWorkFinished();
                    removeFinishedConversation(playerId, activeConversation);
                }
            });
        } catch (final RuntimeException exception) {
            activeConversation.scheduledWorkFinished();
            removeFinishedConversation(playerId, activeConversation);
            throw exception;
        }
    }

    private void finishWhenScheduledWorkCompletes(
            final String playerId,
            final ActiveConversation activeConversation) {
        activeConversation.finishWhenScheduledWorkCompletes();
        removeFinishedConversation(playerId, activeConversation);
    }

    private void removeFinishedConversation(
            final String playerId,
            final ActiveConversation activeConversation) {
        if (activeConversation.readyToFinish()) {
            if (activeConversationByPlayer.remove(playerId, activeConversation)) {
                conversationEnded(playerId);
            }
        }
    }

    private boolean removeConversation(final String playerId) {
        if (playerId == null || activeConversationByPlayer.remove(playerId) == null) {
            return false;
        }
        conversationEnded(playerId);
        return true;
    }

    private void conversationEnded(final String playerId) {
        stopFocus(playerId);
        final UUID playerIdAsUuid;
        try {
            playerIdAsUuid = UUID.fromString(playerId);
        } catch (final IllegalArgumentException exception) {
            return;
        }
        for (final int npcId : List.copyOf(activePlayersByNpc.keySet())) {
            stopNpcSession(npcId, playerIdAsUuid);
        }
    }

    private boolean isCurrentConversation(
            final String playerId,
            final ActiveConversation activeConversation) {
        return activeConversationByPlayer.get(playerId) == activeConversation;
    }

    private ArrayList<StoredConversationLine> playable(
            final List<StoredConversationLine> lines,
            final PlatformPlayer questPlayer) {
        return findPlayableLines(
                lines,
                condition -> runtime.checkCondition(condition, questPlayer),
                ignored -> {});
    }

    private void executeActions(
            final StoredConversationLine line,
            final PlatformPlayer questPlayer) {
        for (final String action : line.getActions()) {
            runtime.executeAction(action, questPlayer);
        }
    }

    private static String selectedCategory(final String category, final StoredConversation existing) {
        if (category != null && !category.isBlank()) {
            return category;
        }
        return existing == null ? "" : existing.category();
    }

    private static String normalizedName(final String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeConversationName(final String conversationName) {
        final String safeName = conversationName == null ? "" : conversationName.replaceAll("[^0-9a-zA-Z-._]", "_");
        if (safeName.isBlank()) {
            throw new IllegalArgumentException("StoredConversation name cannot be blank.");
        }
        return safeName;
    }

    private static boolean isSpeakerMetadata(final String key) {
        return switch (normalizedName(key)) {
            case "color", "delay", "talkspeed", "player", "displayname" -> true;
            default -> false;
        };
    }

    private static boolean isSkipMarker(final String message) {
        return "/skip/".equals(message) || "/next/".equals(message);
    }

    private static MessageValues messages(
            final Map<String, Object> lineMap,
            final boolean startingLine) {
        final String defaultMarker = startingLine ? "/skip/" : "/next/";
        final Object rawText = value(lineMap, "text");
        final String message = rawText == null ? defaultMarker : stringValue(rawText);
        if ((message.isBlank() || defaultMarker.equals(message)) && containsKey(lineMap, "texts")) {
            final List<String> texts = strings(value(lineMap, "texts"));
            if (!texts.isEmpty()) {
                return new MessageValues(texts, true);
            }
        }
        if (message.isBlank()) {
            return new MessageValues(List.of(defaultMarker), false);
        }
        return new MessageValues(List.of(message), false);
    }

    private static List<String> lineIds(final String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        final ArrayList<String> ids = new ArrayList<>();
        for (final String part : raw.replace(" ", "").split(",")) {
            if (!part.isBlank()) {
                ids.add(part);
            }
        }
        return List.copyOf(ids);
    }

    private static List<NpcAttachment> npcAttachments(final Map<String, Object> root) {
        final ArrayList<NpcAttachment> attachments = new ArrayList<>();
        final Map<String, Object> npcs = mapValue(value(root, "npcs"));
        for (final Map.Entry<String, Object> entry : npcs.entrySet()) {
            final Map<String, Object> npc = mapValue(entry.getValue());
            if (npc.isEmpty()) {
                continue;
            }
            final String type = stringValue(value(npc, "type"));
            final String name = stringValue(value(npc, "name"));
            final NQNPCID id = npcId(npc);
            if (id != null) {
                attachments.add(new NpcAttachment(type, id, name, false));
            }
        }
        return List.copyOf(attachments);
    }

    private static NQNPCID npcId(final Map<String, Object> npc) {
        final String stringId = stringValue(value(npc, "stringID"));
        if (!stringId.isBlank()) {
            return NQNPCID.fromString(stringId);
        }
        final String uuid = stringValue(value(npc, "uuidID"));
        if (!uuid.isBlank()) {
            try {
                return NQNPCID.fromUUID(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        final Object integer = value(npc, "integerID");
        if (integer instanceof Number number) {
            return NQNPCID.fromInteger(number.intValue());
        }
        if (integer instanceof String string && !string.isBlank()) {
            try {
                return NQNPCID.fromInteger(Integer.parseInt(string));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object value(final Map<String, Object> map, final String key) {
        if (map == null || key == null) {
            return null;
        }
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public interface ConversationRuntime {
        ConditionCheck.Result checkCondition(String rawCondition, PlatformPlayer questPlayer);

        void executeAction(String rawAction, PlatformPlayer questPlayer);

        void schedule(Duration delay, Runnable action);

        String speakerLine(PlatformPlayer questPlayer, Speaker speaker, String message);

        String answerOptionLine(
                PlatformPlayer questPlayer,
                Speaker speaker,
                String message,
                int optionNumber);

        String chooseAnswerPrefix(PlatformPlayer questPlayer);

        String chooseAnswerHover(PlatformPlayer questPlayer);

        Component component(String miniMessage);

        boolean deletePreviousMessages();

        static ConversationRuntime noop() {
            return new ConversationRuntime() {
                @Override
                public ConditionCheck.Result checkCondition(
                        final String rawCondition,
                        final PlatformPlayer questPlayer) {
                    return new ConditionCheck.Result(true, "");
                }

                @Override
                public void executeAction(final String rawAction, final PlatformPlayer questPlayer) {}

                @Override
                public void schedule(final Duration delay, final Runnable action) {
                    action.run();
                }

                @Override
                public String speakerLine(
                        final PlatformPlayer questPlayer,
                        final Speaker speaker,
                        final String message) {
                    return message == null ? "" : message;
                }

                @Override
                public String answerOptionLine(
                        final PlatformPlayer questPlayer,
                        final Speaker speaker,
                        final String message,
                        final int optionNumber) {
                    return message == null ? "" : message;
                }

                @Override
                public String chooseAnswerPrefix(final PlatformPlayer questPlayer) {
                    return "";
                }

                @Override
                public String chooseAnswerHover(final PlatformPlayer questPlayer) {
                    return "";
                }

                @Override
                public Component component(final String miniMessage) {
                    return Component.text(miniMessage == null ? "" : miniMessage);
                }

                @Override
                public boolean deletePreviousMessages() {
                    return false;
                }
            };
        }
    }

    @FunctionalInterface
    private interface ConditionEvaluator {
        ConditionCheck.Result check(String condition);
    }

    private record RejectedLine(StoredConversationLine line, String condition, ConditionCheck.Result result) {}

    private static ArrayList<StoredConversationLine> findPlayableLines(
            final List<StoredConversationLine> conversationLines,
            final ConditionEvaluator conditionEvaluator,
            final Consumer<RejectedLine> rejectionConsumer) {
        if (conversationLines == null || conversationLines.isEmpty()) {
            return null;
        }

        final ArrayList<StoredConversationLine> nextLines = new ArrayList<>();
        conversationLineLoop:
        for (final StoredConversationLine conversationLineToCheck : conversationLines) {
            if (conversationLineToCheck.getSpeaker().isPlayer()) {
                for (final String condition : conversationLineToCheck.getConditions()) {
                    final ConditionCheck.Result result = conditionEvaluator.check(condition);
                    if (!result.fulfilled()) {
                        rejectionConsumer.accept(new RejectedLine(conversationLineToCheck, condition, result));
                        continue conversationLineLoop;
                    }
                }
                nextLines.add(conversationLineToCheck);
            } else if (nextLines.isEmpty()) {
                for (final String condition : conversationLineToCheck.getConditions()) {
                    final ConditionCheck.Result result = conditionEvaluator.check(condition);
                    if (!result.fulfilled()) {
                        rejectionConsumer.accept(new RejectedLine(conversationLineToCheck, condition, result));
                        continue conversationLineLoop;
                    }
                }
                nextLines.add(conversationLineToCheck);
                return nextLines;
            }
        }
        return nextLines;
    }

    public interface Display {
        void message(PlatformPlayer questPlayer, DisplayMessage message);

        static Display plain() {
            return (questPlayer, message) -> questPlayer.sendMessage(message.miniMessage());
        }
    }

    public record DisplayMessage(String miniMessage, Component component, Component replay) {}

    private static boolean containsKey(final Map<String, Object> map, final String key) {
        if (map == null || key == null) {
            return false;
        }
        if (map.containsKey(key)) {
            return true;
        }
        return map.keySet().stream().anyMatch(existing -> existing != null && existing.equalsIgnoreCase(key));
    }

    private static Map<String, Object> mapValue(final Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static List<String> strings(final Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        if (raw instanceof String string && !string.isBlank()) {
            return List.of(string);
        }
        return List.of();
    }

    private static String stringValue(final Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    private static int intValue(final Object raw, final int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean boolValue(final Object raw, final boolean fallback) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value && !value.isBlank()) {
            return Boolean.parseBoolean(value);
        }
        return fallback;
    }

    private record MessageValues(List<String> messages, boolean textsList) {}

    private static final class ActiveConversation {
        private final StoredConversation conversation;
        private final ArrayList<StoredConversationLine> currentPlayerLines = new ArrayList<>();
        private int scheduledWork;
        private boolean finishWhenScheduledWorkCompletes;

        private ActiveConversation(final StoredConversation conversation) {
            this.conversation = conversation;
        }

        private StoredConversation conversation() {
            return conversation;
        }

        private ArrayList<StoredConversationLine> currentPlayerLines() {
            return currentPlayerLines;
        }

        private synchronized void scheduledWorkStarted() {
            scheduledWork++;
        }

        private synchronized void scheduledWorkFinished() {
            if (scheduledWork > 0) {
                scheduledWork--;
            }
        }

        private synchronized void finishWhenScheduledWorkCompletes() {
            finishWhenScheduledWorkCompletes = true;
        }

        private synchronized boolean readyToFinish() {
            return finishWhenScheduledWorkCompletes && scheduledWork == 0 && currentPlayerLines.isEmpty();
        }
    }

    private static final class StoredConversation {
        private final ArrayList<Speaker> speakers = new ArrayList<>();
        private final ArrayList<StoredConversationLine> startingLines = new ArrayList<>();
        private final CopyOnWriteArrayList<NpcAttachment> npcs = new CopyOnWriteArrayList<>();
        private final Map<String, StoredConversationLine> linesById = new LinkedHashMap<>();
        private final ArrayList<String> simpleLines;
        private final boolean simpleLinesFormat;
        private final String identifier;
        private int delayInMS;
        private String category;

        private StoredConversation(
                final String name,
                final String category,
                final int delayMillis,
                final boolean simpleLinesFormat) {
            this.identifier = name;
            this.delayInMS = delayMillis;
            this.category = category == null ? "" : category;
            this.simpleLinesFormat = simpleLinesFormat;
            this.simpleLines = new ArrayList<>();
        }

        private static StoredConversation simple(
                final String name,
                final List<String> lines,
                final String category,
                final int delayMillis) {
            final StoredConversation conversation = new StoredConversation(name, category, delayMillis, true);
            conversation.simpleLines.addAll(lines);
            return conversation;
        }

        private String name() {
            return identifier;
        }

        private ArrayList<Speaker> getSpeakers() {
            return speakers;
        }

        private boolean hasSpeaker(final Speaker speaker) {
            if (speakers.contains(speaker)) {
                return true;
            }
            for (final Speaker speakerToCheck : speakers) {
                if (speakerToCheck.getSpeakerName().equalsIgnoreCase(speaker.getSpeakerName())) {
                    return true;
                }
            }
            return false;
        }

        private boolean addSpeaker(final Speaker speaker) {
            if (speaker == null || hasSpeaker(speaker)) {
                return false;
            }
            speakers.add(speaker);
            return true;
        }

        private CopyOnWriteArrayList<NpcAttachment> getNPCs() {
            return npcs;
        }

        private void addNpc(final NpcAttachment npc) {
            npcs.add(npc);
        }

        private void addStarterConversationLine(final StoredConversationLine conversationLine) {
            startingLines.add(conversationLine);
        }

        private ArrayList<StoredConversationLine> getStartingLines() {
            return startingLines;
        }

        private void setDelayInMS(final int delayInMS) {
            this.delayInMS = delayInMS;
        }

        private int getDelayInMS() {
            return delayInMS;
        }

        private String category() {
            return category;
        }

        private void category(final String category) {
            this.category = category == null ? "" : category;
        }

        private boolean simpleLines() {
            return simpleLinesFormat;
        }

        private List<String> lines() {
            if (simpleLinesFormat) {
                return List.copyOf(simpleLines);
            }
            final ArrayList<String> flattened = new ArrayList<>();
            final HashSet<String> visited = new HashSet<>();
            for (final StoredConversationLine start : getStartingLines()) {
                flatten(start, flattened, visited);
            }
            if (flattened.isEmpty()) {
                for (final StoredConversationLine line : allLines()) {
                    flattened.addAll(line.rawMessages());
                }
            }
            return List.copyOf(flattened);
        }

        private void flatten(
                final StoredConversationLine line,
                final List<String> flattened,
                final Set<String> visited) {
            if (line == null || !visited.add(normalizedName(line.getFullIdentifier()))) {
                return;
            }
            if (!line.isSkipMessage()) {
                flattened.addAll(line.rawMessages());
            }
            for (final StoredConversationLine next : line.getNext()) {
                flatten(next, flattened, visited);
            }
        }

        private boolean addSpeaker(final String speakerName, final String color) {
            if (speakerName == null || speakerName.isBlank()) {
                return false;
            }
            final Speaker speaker = new Speaker(speakerName, getDelayInMS());
            if (color != null && !color.isBlank()) {
                speaker.setColor(color);
            }
            if (speakerName.equalsIgnoreCase("player")) {
                speaker.setPlayer(true);
            }
            return addSpeaker(speaker);
        }

        private Speaker speaker(final String speakerName) {
            for (final Speaker speaker : getSpeakers()) {
                if (speaker.getSpeakerName().equalsIgnoreCase(speakerName)) {
                    return speaker;
                }
            }
            return null;
        }

        private List<Speaker> speakers() {
            return getSpeakers().stream()
                    .map(StoredConversation::copySpeaker)
                    .toList();
        }

        private static Speaker copySpeaker(final Speaker original) {
            final Speaker copy = new Speaker(original.getSpeakerName(), original.getDelayInMS());
            copy.setColor(original.getColor());
            copy.setTalkSpeed(original.getTalkSpeed());
            copy.setPlayer(original.isPlayer());
            return copy;
        }

        private boolean removeSpeaker(final String speakerName) {
            return getSpeakers().removeIf(speaker -> speaker.getSpeakerName().equalsIgnoreCase(speakerName));
        }

        private void addLine(final StoredConversationLine line) {
            linesById.put(normalizedName(line.getFullIdentifier()), line);
        }

        private StoredConversationLine line(final String fullIdentifier) {
            return linesById.get(normalizedName(fullIdentifier));
        }

        private List<StoredConversationLine> allLines() {
            return List.copyOf(linesById.values());
        }

        private void addNpcAttachment(final String npcType, final NQNPCID npcId, final String npcName) {
            removeNpcAttachment(npcType, npcId);
            addNpc(new NpcAttachment(npcType, npcId, npcName, false));
        }

        private boolean removeNpcAttachment(final String npcType, final NQNPCID npcId) {
            return getNPCs().removeIf(attachment ->
                    Objects.equals(attachment.npcType(), npcType == null ? "" : npcType)
                            && Objects.equals(attachment.npcId(), npcId));
        }

        private void clearNpcAttachments() {
            getNPCs().clear();
        }

        private List<NpcAttachment> npcAttachments() {
            return List.copyOf(getNPCs());
        }

        private void copyEditableMetadataFrom(final StoredConversation existing) {
            for (final Speaker speaker : existing.speakers()) {
                addSpeaker(speaker.getSpeakerName(), speaker.getColor());
            }
            for (final NpcAttachment attachment : existing.npcAttachments()) {
                addNpcAttachment(attachment.npcType(), attachment.npcId(), attachment.npcName());
            }
        }

        private Conversation snapshot() {
            return new Conversation(
                    name(),
                    lines(),
                    category(),
                    speakers(),
                    npcAttachments(),
                    getDelayInMS(),
                    getStartingLines().stream().map(StoredConversationLine::snapshot).toList(),
                    allLines().stream().map(StoredConversationLine::snapshot).toList(),
                    simpleLinesFormat);
        }

        private List<String> analyze() {
            if (simpleLinesFormat) {
                final ArrayList<String> analyzed = new ArrayList<>();
                for (int index = 0; index < simpleLines.size(); index++) {
                    analyzed.add("  - " + (index + 1) + ":\n"
                            + "    Speaker: default\n"
                            + "    Message: [" + simpleLines.get(index) + "]\n");
                }
                return List.copyOf(analyzed);
            }
            final ArrayList<String> analyzed = new ArrayList<>();
            for (final StoredConversationLine start : getStartingLines()) {
                analyzed.add(analyze(start, "  ", new HashSet<>()));
            }
            return List.copyOf(analyzed);
        }

        private String analyze(
                final StoredConversationLine line,
                final String beginningSpaces,
                final Set<String> visited) {
            final StringBuilder result = new StringBuilder();
            result.append(beginningSpaces)
                    .append("- ")
                    .append(line.getIdentifier())
                    .append(":\n")
                    .append(beginningSpaces)
                    .append("  Speaker: ")
                    .append(line.getSpeaker().getSpeakerName())
                    .append("\n");
            for (int index = 0; index < line.getConditions().size(); index++) {
                result.append(beginningSpaces)
                        .append("  ")
                        .append(index + 1)
                        .append(". Condition: ")
                        .append(line.getConditions().get(index))
                        .append("\n");
            }
            for (int index = 0; index < line.getActions().size(); index++) {
                result.append(beginningSpaces)
                        .append("  ")
                        .append(index + 1)
                        .append(". Action: ")
                        .append(line.getActions().get(index))
                        .append("\n");
            }
            result.append(beginningSpaces)
                    .append("  Message: ")
                    .append(line.rawMessages())
                    .append("\n");
            if (line.isShouting()) {
                result.append(beginningSpaces).append("  Shout: true\n");
            }
            if (line.isSkipMessage()) {
                result.append(beginningSpaces).append("  Skip: true\n");
            }
            if (line.getDelayInMS() > 0) {
                result.append(beginningSpaces)
                        .append("  Delay: ")
                        .append(line.getDelayInMS())
                        .append("\n");
            }
            if (!line.nextIds().isEmpty()) {
                result.append(beginningSpaces)
                        .append("  Next: ")
                        .append(line.nextIds())
                        .append("\n");
            }
            if (!visited.add(normalizedName(line.getFullIdentifier()))) {
                result.append(beginningSpaces).append("  Already analyzed above.\n");
                return result.toString();
            }
            for (final StoredConversationLine next : line.getNext()) {
                result.append(analyze(next, beginningSpaces + "  ", visited));
            }
            return result.toString();
        }
    }

    private static final class StoredConversationLine {
        private final Speaker speaker;
        private final List<String> messages;
        private final ArrayList<StoredConversationLine> next = new ArrayList<>();
        private final ArrayList<String> actions = new ArrayList<>();
        private final ArrayList<String> conditions = new ArrayList<>();
        private final String identifier;
        private final String fullIdentifier;
        private final List<String> rawMessages;
        private final ArrayList<String> nextIds = new ArrayList<>();
        private final boolean textsList;
        private String color = "<GRAY>";
        private boolean shout = false;
        private boolean skipMessage = false;
        private int delayInMS;

        private StoredConversationLine(
                final Speaker speaker,
                final String identifier,
                final List<String> messages,
                final boolean textsList) {
            this.speaker = speaker;
            this.identifier = identifier;
            this.messages = List.copyOf(messages);
            this.fullIdentifier = speaker.getSpeakerName() + "." + identifier;
            this.delayInMS = speaker.getDelayInMS();
            this.rawMessages = List.copyOf(messages);
            this.textsList = textsList;
        }

        private ArrayList<String> getConditions() {
            return conditions;
        }

        private void addCondition(final String condition) {
            conditions.add(condition);
        }

        private String getFullIdentifier() {
            return fullIdentifier;
        }

        private String getIdentifier() {
            return identifier;
        }

        private List<String> getMessages() {
            if (isShouting()) {
                return messages.stream().map(s -> "<bold>" + s + "</bold>").toList();
            }
            return messages;
        }

        private String getOneMessage() {
            final int random = ThreadLocalRandom.current().nextInt(messages.size());
            if (isShouting()) {
                return "<bold>" + messages.get(random) + "</bold>";
            }
            return messages.get(random);
        }

        private Speaker getSpeaker() {
            return speaker;
        }

        private ArrayList<StoredConversationLine> getNext() {
            return next;
        }

        private void addNext(final StoredConversationLine nextLine) {
            next.add(nextLine);
        }

        private String getColor() {
            return color;
        }

        private void setColor(final String color) {
            this.color = color;
        }

        private boolean isShouting() {
            return shout;
        }

        private void setShouting(final boolean shouting) {
            this.shout = shouting;
        }

        private ArrayList<String> getActions() {
            return actions;
        }

        private void addAction(final String newAction) {
            actions.add(newAction);
        }

        private boolean isSkipMessage() {
            return skipMessage;
        }

        private void setSkipMessage(final boolean skipMessage) {
            this.skipMessage = skipMessage;
        }

        private void setDelayInMS(final int delayInMS) {
            this.delayInMS = delayInMS;
        }

        private int getDelayInMS() {
            return delayInMS;
        }

        private int getDelayInTicks() {
            return delayInMS / 50;
        }

        private void addNextId(final String nextId) {
            if (nextId != null && !nextId.isBlank()) {
                nextIds.add(nextId);
            }
        }

        private List<String> nextIds() {
            return List.copyOf(nextIds);
        }

        private List<String> rawMessages() {
            return rawMessages;
        }

        private ConversationLine snapshot() {
            return new ConversationLine(
                    getFullIdentifier(),
                    getSpeaker().getSpeakerName(),
                    getIdentifier(),
                    rawMessages(),
                    textsList,
                    nextIds(),
                    List.copyOf(getActions()),
                    List.copyOf(getConditions()),
                    isShouting(),
                    isSkipMessage(),
                    getDelayInMS(),
                    getSpeaker().getColor(),
                    getSpeaker().isPlayer());
        }
    }

    public boolean startFocus(
            final PlatformPlayer questPlayer,
            final String conversationName,
            final float ticksToRotate,
            final boolean stopConversationWhenLeaving,
            final Focus.Native nativeFocus,
            final Runnable stopConversation) {
        if (questPlayer == null
                || conversationName == null
                || conversationName.isBlank()
                || nativeFocus == null) {
            return false;
        }
        final Focus.Observation initial = nativeFocus.observe();
        if (initial == null || !initial.online() || !initial.npcValid()) {
            nativeFocus.cleanup();
            return false;
        }
        final String playerId = questPlayer.playerIdentifier();
        final FocusSession session = new FocusSession(
                playerId,
                conversationName,
                new Focus(
                        initial.worldName(),
                        initial.playerX(),
                        initial.playerZ(),
                        initial.yaw(),
                        initial.pitch(),
                        ticksToRotate,
                        stopConversationWhenLeaving),
                nativeFocus,
                stopConversation);
        final FocusSession previous = focusByPlayer.put(playerId, session);
        if (previous != null) {
            previous.cleanup();
        }
        advanceFocus(session, initial);
        return focusByPlayer.get(playerId) == session;
    }

    public void stopFocus(final String playerId) {
        if (playerId == null) {
            return;
        }
        final FocusSession session = focusByPlayer.remove(playerId);
        if (session != null) {
            session.cleanup();
        }
    }

    private void clearFocusSessions() {
        final List<FocusSession> sessions = List.copyOf(focusByPlayer.values());
        focusByPlayer.clear();
        sessions.forEach(FocusSession::cleanup);
    }

    private void advanceFocus(
            final FocusSession session,
            final Focus.Observation currentObservation) {
        if (focusByPlayer.get(session.playerId()) != session) {
            return;
        }
        try {
            final Focus.Observation observation = currentObservation == null
                    ? session.nativeFocus().observe()
                    : currentObservation;
            final boolean conversationActive = session.conversationName().equalsIgnoreCase(
                    Objects.toString(activeConversation(session.playerId()), ""));
            final Focus.Step step = session.focus().next(observation, conversationActive);
            if (step.cancel()) {
                finishFocus(session);
                if (step.stopConversation() && session.stopConversation() != null) {
                    session.stopConversation().run();
                }
                return;
            }
            session.nativeFocus().applySlowness();
            if (step.rotate()) {
                session.nativeFocus().rotate(step.yaw(), step.pitch());
            }
            runtime.schedule(Duration.ofMillis(100), () -> advanceFocus(session, null));
        } catch (final RuntimeException exception) {
            finishFocus(session);
            throw exception;
        }
    }

    private void finishFocus(final FocusSession session) {
        if (focusByPlayer.remove(session.playerId(), session)) {
            session.cleanup();
        }
    }

    private static final class FocusSession {
        private final String playerId;
        private final String conversationName;
        private final Focus focus;
        private final Focus.Native nativeFocus;
        private final Runnable stopConversation;
        private boolean cleaned;

        private FocusSession(
                final String playerId,
                final String conversationName,
                final Focus focus,
                final Focus.Native nativeFocus,
                final Runnable stopConversation) {
            this.playerId = playerId;
            this.conversationName = conversationName;
            this.focus = focus;
            this.nativeFocus = nativeFocus;
            this.stopConversation = stopConversation;
        }

        private String playerId() {
            return playerId;
        }

        private String conversationName() {
            return conversationName;
        }

        private Focus focus() {
            return focus;
        }

        private Focus.Native nativeFocus() {
            return nativeFocus;
        }

        private Runnable stopConversation() {
            return stopConversation;
        }

        private synchronized void cleanup() {
            if (!cleaned) {
                cleaned = true;
                nativeFocus.cleanup();
            }
        }
    }

    /** Owns the state and geometry for smoothly turning a player toward a conversation NPC. */
    public static final class Focus {
        private final String baseWorld;
        private final double baseX;
        private final double baseZ;
        private final float ticksToRotate;
        private final boolean stopConversationWhenLeaving;
        private FocusPhase phase = FocusPhase.FOCUSING;
        private float previousYaw;
        private float previousPitch;
        private float startYaw;
        private float startPitch;
        private float targetYaw;
        private float targetPitch;
        private boolean rotationInitialized;
        private int tick;

        public Focus(
                final String baseWorld,
                final double baseX,
                final double baseZ,
                final float initialYaw,
                final float initialPitch,
                final float ticksToRotate,
                final boolean stopConversationWhenLeaving) {
            this.baseWorld = baseWorld == null ? "" : baseWorld;
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.previousYaw = initialYaw;
            this.previousPitch = initialPitch;
            this.startYaw = initialYaw;
            this.startPitch = initialPitch;
            this.ticksToRotate = Math.max(1f, ticksToRotate);
            this.stopConversationWhenLeaving = stopConversationWhenLeaving;
        }

        public Step next(final Observation observation, final boolean conversationActive) {
            if (observation == null || !observation.online() || !observation.npcValid() || !conversationActive) {
                return Step.cancel(false);
            }
            if (!rotationInitialized) {
                calculateRotation(observation);
                rotationInitialized = true;
            }
            final double deltaX = observation.playerX() - baseX;
            final double deltaZ = observation.playerZ() - baseZ;
            if (!baseWorld.equals(observation.worldName()) || deltaX * deltaX + deltaZ * deltaZ > 0.04) {
                return Step.cancel(stopConversationWhenLeaving);
            }
            if (observation.yaw() != previousYaw || observation.pitch() != previousPitch) {
                tick = 0;
                phase = FocusPhase.WAITING;
                previousYaw = observation.yaw();
                previousPitch = observation.pitch();
                return Step.waiting();
            }
            if (phase == FocusPhase.WAITING && tick == 5) {
                phase = FocusPhase.FOCUSING;
                tick = -1;
                calculateRotation(observation);
            }
            Step step = Step.waiting();
            if (phase == FocusPhase.FOCUSING && tick <= ticksToRotate) {
                final float progress = tick / ticksToRotate;
                step = Step.rotate(
                        (1 - progress) * startYaw + progress * targetYaw,
                        (1 - progress) * startPitch + progress * targetPitch);
            } else if (phase == FocusPhase.FOCUSING) {
                phase = FocusPhase.DONE;
            }
            previousYaw = observation.yaw();
            previousPitch = observation.pitch();
            tick++;
            return step;
        }

        private void calculateRotation(final Observation observation) {
            startYaw = observation.yaw();
            startPitch = observation.pitch();
            final double x = observation.playerEyeX() - observation.npcEyeX();
            final double y = observation.playerEyeY() - observation.npcEyeY();
            final double z = observation.playerEyeZ() - observation.npcEyeZ();
            targetYaw = normalizeYaw((float) (180f - Math.toDegrees(Math.atan2(x, z))));
            targetPitch = normalizePitch((float) Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z))));
        }

        private static float normalizeYaw(final float yaw) {
            float normalized = yaw % 360f;
            if (normalized >= 180f) {
                normalized -= 360f;
            }
            if (normalized < -180f) {
                normalized += 360f;
            }
            return normalized;
        }

        private static float normalizePitch(final float pitch) {
            return Math.max(-90f, Math.min(90f, pitch));
        }

        private enum FocusPhase {
            WAITING,
            FOCUSING,
            DONE
        }

        public interface Native {
            Observation observe();

            void applySlowness();

            void rotate(float yaw, float pitch);

            void cleanup();
        }

        public record Observation(
                boolean online,
                boolean npcValid,
                String worldName,
                double playerX,
                double playerZ,
                float yaw,
                float pitch,
                double playerEyeX,
                double playerEyeY,
                double playerEyeZ,
                double npcEyeX,
                double npcEyeY,
                double npcEyeZ) {
            public Observation {
                worldName = worldName == null ? "" : worldName;
            }
        }

        public record Step(boolean cancel, boolean stopConversation, boolean rotate, float yaw, float pitch) {
            private static Step cancel(final boolean stopConversation) {
                return new Step(true, stopConversation, false, 0, 0);
            }

            private static Step waiting() {
                return new Step(false, false, false, 0, 0);
            }

            private static Step rotate(final float yaw, final float pitch) {
                return new Step(false, false, true, yaw, pitch);
            }
        }
    }

    public record Conversation(
            String name,
            List<String> lines,
            String category,
            List<Speaker> speakers,
            List<NpcAttachment> npcAttachments,
            int delayMillis,
            List<ConversationLine> startingLines,
            List<ConversationLine> graphLines,
            boolean simpleLines) {
        public int startingLineCount() {
            return startingLines.size();
        }
    }

    public record ConversationLine(
            String fullIdentifier,
            String speaker,
            String identifier,
            List<String> messages,
            boolean textsList,
            List<String> next,
            List<String> actions,
            List<String> conditions,
            boolean shout,
            boolean skipMessage,
            int delayMillis,
            String color,
            boolean playerLine) {}

}
