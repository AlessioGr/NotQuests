package com.notquests.core.managers;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.actions.SavedActions;
import com.notquests.core.conditions.Condition;
import com.notquests.core.config.CategoryFiles.CategoryFolder;
import com.notquests.core.config.CategoryFiles;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.conversation.Speaker;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItem;
import com.notquests.core.managers.tags.TagManager.Tag;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer.FailedQuest;
import com.notquests.core.structs.QuestPlayer;
import com.notquests.core.triggers.Trigger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Owns configured YAML data and the YAML-backed player runtime. */
public class DataManager {
    public enum ReloadTarget {
        ALL,
        CONFIG,
        LANGUAGES,
        CONVERSATIONS
    }

    private final NotQuestsPlugin plugin;
    private final QuestManager questManager;
    private final NotQuestsAdapter adapter;
    private final Path dataFolder;
    private final Path runtimeFile;
    private final boolean persistRuntimeData;
    private final boolean configured;

    /**
     * Creates an unconfigured manager. Its persistence operations are deliberate no-ops until core
     * installs a configured manager during startup.
     */
    public DataManager() {
        plugin = null;
        questManager = null;
        adapter = null;
        dataFolder = null;
        runtimeFile = null;
        persistRuntimeData = false;
        configured = false;
    }

    public DataManager(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final Path dataFolder) {
        this(plugin, adapter, dataFolder, true);
    }

    public DataManager(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final Path dataFolder,
            final boolean persistRuntimeData) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.questManager = plugin.questManager();
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.runtimeFile = this.dataFolder.resolve("data").resolve("runtime.yml");
        this.persistRuntimeData = persistRuntimeData;
        this.configured = true;
    }

    public boolean save() {
        if (!configured) {
            return true;
        }
        try {
            ensureDefaultCategory();
            saveCategoryScopedData();
            if (persistRuntimeData) {
                saveRuntimeData();
            }
            return true;
        } catch (final IOException exception) {
            adapter.warn("Could not save NotQuests core data: " + exception.getMessage());
            return false;
        }
    }

    public boolean saveConfiguredData() {
        if (!configured) {
            return true;
        }
        try {
            ensureDefaultCategory();
            saveCategoryScopedData();
            return true;
        } catch (final IOException exception) {
            adapter.warn("Could not save NotQuests configured data: " + exception.getMessage());
            return false;
        }
    }

    public boolean savePlayerRuntime() {
        if (!configured || !persistRuntimeData) {
            return true;
        }
        try {
            saveRuntimeData();
            return true;
        } catch (final IOException exception) {
            adapter.warn("Could not save NotQuests runtime data: " + exception.getMessage());
            return false;
        }
    }

    public boolean loadPlayerRuntime(final boolean firstLoad) {
        if (!configured || !persistRuntimeData) {
            return true;
        }
        try {
            loadRuntimeData();
            return true;
        } catch (final IOException exception) {
            adapter.warn("Could not load NotQuests runtime data: " + exception.getMessage());
            return false;
        }
    }

    public boolean reload(final ReloadTarget target) {
        if (!configured) {
            return true;
        }
        final ReloadTarget reloadTarget = target == null ? ReloadTarget.ALL : target;
        if (reloadTarget == ReloadTarget.CONFIG || reloadTarget == ReloadTarget.LANGUAGES) {
            return true;
        }
        try {
            if (reloadTarget == ReloadTarget.CONVERSATIONS) {
                plugin.conversationManager().clear();
                loadConversations();
                ensureDefaultCategory();
                return true;
            }
            plugin.clearStoredData(persistRuntimeData);
            loadCategoryScopedData();
            if (!plugin.finishConfiguredDataLoad()) {
                return false;
            }
            if (persistRuntimeData) {
                loadRuntimeData();
            }
            ensureDefaultCategory();
            return true;
        } catch (final RuntimeException | IOException exception) {
            adapter.warn("Could not load NotQuests core data: " + exception.getMessage());
            return false;
        }
    }

    public boolean saveCategory(final Category category) {
        if (!configured) {
            return true;
        }
        if (category == null) {
            return false;
        }
        try {
            CategoryFiles.copyMissingCategory(dataFolder, category.getIdentifier());
            final Path categoryFolder = CategoryFiles.categoryFolder(dataFolder, category.getIdentifier());
            saveYaml(categoryFolder.resolve("category.yml"), categoryToYaml(category));
            return true;
        } catch (final IOException exception) {
            adapter.warn("Could not save NotQuests category '" + category.getIdentifier() + "': " + exception.getMessage());
            return false;
        }
    }

    private void ensureDefaultCategory() {
        questManager.ensureDefaultCategory();
    }

    private void saveCategoryScopedData() throws IOException {
        for (final String categoryName : categoryNamesWithData()) {
            CategoryFiles.copyMissingCategory(dataFolder, categoryName);
            final Path categoryFolder = CategoryFiles.categoryFolder(dataFolder, categoryName);
            saveYaml(categoryFolder.resolve("category.yml"), categoryToYaml(questManager.getOrCreateCategory(categoryName)));
            saveYaml(categoryFolder.resolve("quests.yml"), root("quests", keyed(questManager.getAllQuests().stream()
                    .filter(quest -> sameCategory(quest.getCategory(), categoryName))
                    .toList(), Quest::getIdentifier, this::questToYaml)));
            saveYaml(categoryFolder.resolve("actions.yml"), root("actions", keyed(plugin.savedActions().actions().stream()
                    .filter(action -> sameCategory(action.getCategory(), categoryName))
                    .toList(), SavedActions.SavedAction::getName, this::savedActionToYaml)));
            saveYaml(categoryFolder.resolve("conditions.yml"), root("conditions", keyed(plugin.savedConditions().stream()
                    .filter(condition -> sameCategory(condition.getCategory(), categoryName))
                    .toList(), NotQuestsPlugin.StoredCondition::getName, this::savedConditionToYaml)));
            saveYaml(categoryFolder.resolve("tags.yml"), root("tags", keyed(plugin.tags().stream()
                    .filter(tag -> sameCategory(tag.category(), categoryName))
                    .toList(), Tag::tagName, this::tagToYaml)));
            saveYaml(categoryFolder.resolve("items.yml"), root("items", keyed(plugin.savedItems().stream()
                    .filter(item -> sameCategory(item.getCategory(), categoryName))
                    .toList(), SavedItem::getName, this::savedItemToYaml)));
            saveConversations(categoryFolder.resolve("conversations"), categoryName);
        }
    }

    private void loadCategoryScopedData() throws IOException {
        final List<CategoryFolder> categoryFolders = categoryFolders();
        if (categoryFolders.isEmpty()) {
            ensureDefaultCategory();
            return;
        }
        for (final CategoryFolder categoryFolder : categoryFolders) {
            loadCategory(categoryFolder.categoryName(), loadYaml(categoryFolder.path().resolve("category.yml")));
        }
        for (final CategoryFolder categoryFolder : categoryFolders) {
            final String categoryName = categoryFolder.categoryName();
            final Path path = categoryFolder.path();
            for (final Map<String, Object> map : namedMaps(loadYaml(path.resolve("tags.yml")).get("tags"), "name")) {
                final TagType type = enumValue(TagType.class, string(map, "tagType"), null);
                final String tagName = string(map, "name");
                if (type == null) {
                    adapter.warn("Skipping tag '" + tagName + "': unknown tag type "
                            + string(map, "tagType"));
                    continue;
                }
                if (!plugin.createTag(type, tagName, categoryName)) {
                    adapter.warn("Skipping tag '" + tagName + "': duplicate or invalid tag name.");
                }
            }
            for (final Map<String, Object> map : namedMaps(loadYaml(path.resolve("items.yml")).get("items"), "name")) {
                loadSavedItem(withCategory(map, categoryName));
            }
            for (final Map<String, Object> map : namedMaps(loadYaml(path.resolve("actions.yml")).get("actions"), "name")) {
                loadSavedAction(withCategory(map, categoryName));
            }
            for (final Map<String, Object> map : namedMaps(loadYaml(path.resolve("conditions.yml")).get("conditions"), "name")) {
                loadSavedCondition(withCategory(map, categoryName));
            }
            for (final Map<String, Object> map : namedMaps(loadYaml(path.resolve("quests.yml")).get("quests"), "id")) {
                loadQuest(withCategory(map, categoryName));
            }
            loadConversations(categoryName, path.resolve("conversations"));
        }
    }

    private void loadConversations() throws IOException {
        for (final CategoryFolder categoryFolder : categoryFolders()) {
            loadConversations(categoryFolder.categoryName(), categoryFolder.path().resolve("conversations"));
        }
    }

    private void loadCategory(final String categoryName, final Map<String, Object> map) {
        plugin.loadCategory(
                categoryName,
                string(map, "displayName"),
                progressOrderString(map.get("predefinedProgressOrder")),
                itemSelection(map, "guiItem"),
                bool(map, "guiItemGlow", false),
                integerAtPath(map, "conversations.delay", 0));
    }

    private void saveRuntimeData() throws IOException {
        if (plugin.questPlayers().isEmpty() && plugin.activeProfiles().isEmpty()) {
            Files.deleteIfExists(runtimeFile);
            return;
        }
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("players", plugin.questPlayers().stream().map(this::playerToYaml).toList());
        root.put("activeProfiles", new LinkedHashMap<>(plugin.activeProfiles()));
        saveYaml(runtimeFile, root);
    }

    private void loadRuntimeData() throws IOException {
        if (!Files.exists(runtimeFile)) {
            return;
        }
        final Map<String, Object> root = loadYaml(runtimeFile);
        for (final Map<String, Object> map : maps(root.get("players"))) {
            loadPlayer(map);
        }
        if (root.get("activeProfiles") instanceof final Map<?, ?> activeProfiles) {
            for (final Map.Entry<?, ?> entry : activeProfiles.entrySet()) {
                plugin.setActiveProfile(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    private void saveConversations(final Path conversationsFolder, final String categoryName) throws IOException {
        Files.createDirectories(conversationsFolder);
        for (final ConversationManager.Conversation conversation : plugin.conversationManager().conversations()) {
            if (sameCategory(conversation.category(), categoryName)) {
                final Path conversationFile = conversationsFolder.resolve(safeFileName(conversation.name()) + ".yml");
                final Path sourceFile = existingConversationFile(conversation, conversationFile);
                final Map<String, Object> yaml = sourceFile != null
                        ? patchConversationYaml(loadYaml(sourceFile), conversation)
                        : conversationToYaml(conversation);
                saveYaml(conversationFile, yaml);
                deleteConversationCopiesOutsideCategory(conversation, conversationFile);
            }
        }
    }

    private Path existingConversationFile(
            final ConversationManager.Conversation conversation,
            final Path preferredFile) throws IOException {
        if (Files.exists(preferredFile)) {
            return preferredFile;
        }
        final String fileName = safeFileName(conversation.name()) + ".yml";
        for (final CategoryFolder categoryFolder : categoryFolders()) {
            if (sameCategory(categoryFolder.categoryName(), conversation.category())) {
                continue;
            }
            final Path candidate = categoryFolder.path().resolve("conversations").resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void deleteConversationCopiesOutsideCategory(
            final ConversationManager.Conversation conversation,
            final Path keptFile) throws IOException {
        final String fileName = safeFileName(conversation.name()) + ".yml";
        final Path kept = keptFile.toAbsolutePath().normalize();
        for (final CategoryFolder categoryFolder : categoryFolders()) {
            if (sameCategory(categoryFolder.categoryName(), conversation.category())) {
                continue;
            }
            final Path candidate = categoryFolder.path().resolve("conversations").resolve(fileName);
            if (!candidate.toAbsolutePath().normalize().equals(kept)) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    private void loadConversations(final String categoryName, final Path conversationsFolder) throws IOException {
        if (!Files.isDirectory(conversationsFolder)) {
            return;
        }
        try (Stream<Path> paths = Files.list(conversationsFolder)) {
            for (final Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .toList()) {
                final Map<String, Object> conversation = withCategory(loadYaml(path), categoryName);
                conversation.put("name", path.getFileName().toString().replaceFirst("\\.yml$", ""));
                loadConversation(conversation);
            }
        }
    }

    private List<CategoryFolder> categoryFolders() throws IOException {
        return CategoryFiles.discover(dataFolder);
    }

    private List<String> categoryNamesWithData() {
        final TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        addCategoryAndParents(names, Category.DEFAULT_NAME);
        for (final Category category : questManager.getAllCategories()) {
            addCategoryAndParents(names, categoryName(category.getIdentifier()));
        }
        for (final Quest quest : questManager.getAllQuests()) {
            addCategoryAndParents(names, categoryName(quest.getCategory()));
        }
        for (final SavedActions.SavedAction action : plugin.savedActions().actions()) {
            addCategoryAndParents(names, categoryName(action.getCategory()));
        }
        for (final NotQuestsPlugin.StoredCondition condition : plugin.savedConditions()) {
            addCategoryAndParents(names, categoryName(condition.getCategory()));
        }
        for (final Tag tag : plugin.tags()) {
            addCategoryAndParents(names, categoryName(tag.category()));
        }
        for (final SavedItem item : plugin.savedItems()) {
            addCategoryAndParents(names, categoryName(item.getCategory()));
        }
        for (final ConversationManager.Conversation conversation : plugin.conversationManager().conversations()) {
            addCategoryAndParents(names, categoryName(conversation.category()));
        }
        return List.copyOf(names);
    }

    private static void addCategoryAndParents(final TreeSet<String> names, final String categoryName) {
        final String safeCategoryName = categoryName(categoryName);
        String current = "";
        for (final String part : safeCategoryName.split("\\.")) {
            if (part.isBlank()) {
                continue;
            }
            current = current.isBlank() ? part : current + "." + part;
            names.add(current);
        }
    }

    private static Map<String, Object> root(final String key, final Map<String, Object> entries) {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put(key, entries);
        return root;
    }

    private static <T> Map<String, Object> keyed(
            final List<T> entries,
            final Function<T, String> key,
            final Function<T, Map<String, Object>> value) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (final T entry : entries) {
            map.put(key.apply(entry), value.apply(entry));
        }
        return map;
    }

    private static void saveYaml(final Path path, final Map<String, Object> map) throws IOException {
        YamlConfig.save(YamlConfig.fromMap(map), path);
    }

    private static Map<String, Object> loadYaml(final Path path) throws IOException {
        return Files.exists(path) ? YamlConfig.load(path).asMap() : Map.of();
    }

    private static Map<String, Object> withCategory(final Map<String, Object> source, final String categoryName) {
        final Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put("category", categoryName(categoryName));
        return copy;
    }

    private static boolean sameCategory(final String left, final String right) {
        return categoryName(left).equalsIgnoreCase(categoryName(right));
    }

    private static String categoryName(final String categoryName) {
        return NotQuestsPlugin.canonicalCategoryName(categoryName);
    }

    private static String safeFileName(final String fileName) {
        final String safeName = fileName == null ? "" : fileName.replaceAll("[^0-9a-zA-Z-._]", "_");
        if (safeName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be blank.");
        }
        return safeName;
    }

    private Map<String, Object> categoryToYaml(final Category category) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("displayName", category.getDisplayName());
        map.put("conversations", Map.of("delay", category.getConversationDelayInMS()));
        putProgressOrder(map, "predefinedProgressOrder", category.getProgressOrder());
        final Object guiItem = itemSelectionToYaml(category.getGuiItemSelection());
        if (guiItem != null) {
            map.put("guiItem", guiItem);
        }
        map.put("guiItemGlow", category.isGuiItemGlow());
        return map;
    }

    private Map<String, Object> tagToYaml(final Tag tag) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("tagType", tag.tagType().name());
        return map;
    }

    private Map<String, Object> questToYaml(final Quest quest) {
        final Map<String, Object> map = new LinkedHashMap<>();
        putNonBlank(map, "displayName", quest.getDisplayName());
        putNonBlank(map, "description", quest.getDescription());
        final Object takeItem = itemSelectionToYaml(quest.getGuiItemSelection());
        if (takeItem != null) {
            map.put("takeItem", takeItem);
        }
        if (quest.isGuiItemGlow()) {
            map.put("guiItemGlow", true);
        }
        final Map<String, Object> limits = new LinkedHashMap<>();
        if (quest.getMaxCompletions() != -1) {
            limits.put("completions", quest.getMaxCompletions());
        }
        if (quest.getMaxAccepts() != -1) {
            limits.put("accepts", quest.getMaxAccepts());
        }
        if (quest.getMaxFails() != -1) {
            limits.put("fails", quest.getMaxFails());
        }
        if (!limits.isEmpty()) {
            map.put("limits", limits);
        }
        if (quest.getAcceptCooldownComplete() != -1) {
            map.put("acceptCooldown", Map.of("complete", quest.getAcceptCooldownComplete()));
        }
        if (!quest.isTakeEnabled()) {
            map.put("takeEnabled", false);
        }
        if (!quest.isAbortEnabled()) {
            map.put("abortEnabled", false);
        }
        putProgressOrder(map, "predefinedProgressOrder", quest.getObjectiveProgressOrder());
        putNonEmpty(map, "objectives", keyed(quest.getObjectives(), entry -> String.valueOf(entry.id()), entry -> entryToYaml(entry, "objectiveType")));
        putNonEmpty(map, "requirements", keyed(quest.getRequirements(), entry -> String.valueOf(entry.id()), entry -> entryToYaml(entry, "conditionType")));
        putNonEmpty(map, "rewards", keyed(quest.getRewards(), entry -> String.valueOf(entry.id()), entry -> entryToYaml(entry, "actionType")));
        putNonEmpty(map, "triggers", keyed(quest.getTriggers(), entry -> String.valueOf(entry.id()), entry -> entryToYaml(entry, "triggerType")));
        putNonEmpty(map, "npcs", keyed(quest.getNpcAttachments(), NpcAttachment::identifyingString, this::npcAttachmentToYaml));
        return map;
    }

    private void loadQuest(final Map<String, Object> map) {
        final Quest quest = questManager.getOrCreateQuest(string(map, "id"));
        quest.setDisplayName(string(map, "displayName"));
        quest.setDescription(string(map, "description"));
        quest.setCategory(string(map, "category"));
        questManager.getOrCreateCategory(quest.getCategory());
        quest.setGuiItem(itemSelection(map, "takeItem"));
        quest.setGuiItemGlow(bool(map, "guiItemGlow", false));
        quest.setMaxCompletions(integerAtPath(map, "limits.completions", -1));
        quest.setMaxAccepts(integerAtPath(map, "limits.accepts", -1));
        quest.setMaxFails(integerAtPath(map, "limits.fails", -1));
        quest.setAcceptCooldownComplete(longAtPath(map, "acceptCooldown.complete", -1));
        quest.setTakeEnabled(bool(map, "takeEnabled", true));
        quest.setAbortEnabled(bool(map, "abortEnabled", true));
        quest.setObjectiveProgressOrder(progressOrderString(map.get("predefinedProgressOrder")));
        for (final Map<String, Object> entry : namedMaps(map.get("objectives"), "id")) {
            loadEntry(quest.addObjective(integer(entry, "id", 1), string(entry, "objectiveType"), data(entry, "objectiveType"), string(entry, "taskDescription")), entry);
        }
        for (final Map<String, Object> entry : namedMaps(map.get("requirements"), "id")) {
            loadEntry(quest.addRequirement(integer(entry, "id", 1), string(entry, "conditionType"), data(entry, "conditionType")), entry);
        }
        for (final Map<String, Object> entry : namedMaps(map.get("rewards"), "id")) {
            final Action action = quest.addReward(
                    integer(entry, "id", 1), string(entry, "actionType"), data(entry, "actionType"));
            loadAction(action, entry);
        }
        for (final Map<String, Object> entry : namedMaps(map.get("triggers"), "id")) {
            quest.addTrigger(integer(entry, "id", 1), string(entry, "triggerType"), data(entry, "triggerType"));
        }
        for (final Map<String, Object> npc : namedMaps(map.get("npcs"), "id")) {
            loadNpcAttachment(quest, npc);
        }
    }

    private Map<String, Object> npcAttachmentToYaml(final NpcAttachment attachment) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("questShowing", attachment.questShowing());
        final Map<String, Object> npcData = new LinkedHashMap<>();
        npcData.put("type", attachment.npcType());
        putNonBlank(npcData, "name", attachment.npcName());
        if (attachment.npcId() != null) {
            if (attachment.npcId().getStringID() != null) {
                npcData.put("stringID", attachment.npcId().getStringID());
            } else if (attachment.npcId().getIntegerID() != -1) {
                npcData.put("integerID", attachment.npcId().getIntegerID());
            } else if (attachment.npcId().getUUIDID() != null) {
                npcData.put("uuidID", attachment.npcId().getUUIDID().toString());
            }
        }
        map.put("npcData", npcData);
        return map;
    }

    private void loadNpcAttachment(final Quest quest, final Map<String, Object> map) {
        final Map<String, Object> npcData = mapValue(map.get("npcData"));
        final String type = string(npcData, "type");
        final NQNPCID id = npcId(npcData);
        if (type.isBlank() || id == null) {
            return;
        }
        quest.addNpcAttachment(
                type,
                id,
                string(npcData, "name"),
                bool(map, "questShowing", true));
    }

    private NQNPCID npcId(final Map<String, Object> npcData) {
        final String stringId = string(npcData, "stringID");
        if (!stringId.isBlank()) {
            return NQNPCID.fromString(stringId);
        }
        final String uuidId = string(npcData, "uuidID");
        if (!uuidId.isBlank()) {
            return NQNPCID.fromUUID(UUID.fromString(uuidId));
        }
        if (npcData.containsKey("integerID")) {
            return NQNPCID.fromInteger(integer(npcData, "integerID", -1));
        }
        return null;
    }

    private Map<String, Object> entryToYaml(final Objective objective, final String typeKey) {
        final Map<String, Object> map = configuredDataToYaml(objective.typeId(), objective.values(), typeKey);
        putNonBlank(map, "displayName", objective.getDisplayName());
        putNonBlank(map, "description", objective.getDescription());
        putNonBlank(map, "taskDescription", objective.getTaskDescription());
        putNonBlank(map, "completionNPC", objective.getCompletionNPC());
        putProgressOrder(map, "predefinedProgressOrder", objective.getChildObjectiveProgressOrder());
        if (objective.getLocation() != null) {
            map.put("location", encodeValue(objective.getLocation()));
        }
        if (objective.isLocationEnabled()) {
            map.put("showLocation", true);
        }
        putNonEmpty(map, "objectives", keyed(objective.getObjectives(), child -> String.valueOf(child.id()), child -> entryToYaml(child, "objectiveType")));
        putNonEmpty(map, "conditions", keyed(objective.getConditions("unlock"), condition -> String.valueOf(condition.id()), condition -> entryToYaml(condition, "conditionType")));
        putNonEmpty(map, "conditionsProgress", keyed(objective.getConditions("progress"), condition -> String.valueOf(condition.id()), condition -> entryToYaml(condition, "conditionType")));
        putNonEmpty(map, "conditionsComplete", keyed(objective.getConditions("complete"), condition -> String.valueOf(condition.id()), condition -> entryToYaml(condition, "conditionType")));
        putNonEmpty(map, "rewards", keyed(objective.getRewards(), reward -> String.valueOf(reward.id()), reward -> entryToYaml(reward, "actionType")));
        return map;
    }

    private Map<String, Object> entryToYaml(final Condition condition, final String typeKey) {
        final Map<String, Object> map = configuredDataToYaml(condition.typeId(), condition.values(), typeKey);
        putNonBlank(map, "description", condition.getDescription());
        putNonBlank(map, "hiddenStatusExpression", condition.getHiddenExpression());
        map.put("progressNeeded", condition.getProgressNeeded());
        map.put("negated", condition.isNegated());
        if (condition.isAllowProgressDecreaseIfNotFulfilled()) {
            map.put("allowProgressDecreaseIfNotFulfilled", true);
        }
        return map;
    }

    private Map<String, Object> entryToYaml(final Action action, final String typeKey) {
        final Map<String, Object> map = configuredDataToYaml(action.typeId(), action.values(), typeKey);
        putNonBlank(map, "displayName", action.getDisplayName());
        putNonBlank(map, "description", action.getDescription());
        putNonEmpty(map, "conditions", keyed(
                action.getConditions(), condition -> String.valueOf(condition.id()),
                condition -> entryToYaml(condition, "conditionType")));
        return map;
    }

    private Map<String, Object> entryToYaml(final Trigger trigger, final String typeKey) {
        return configuredDataToYaml(trigger.typeId(), trigger.values(), typeKey);
    }

    private Map<String, Object> configuredDataToYaml(
            final String typeId,
            final Map<String, Object> data,
            final String typeKey) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put(typeKey, typeId);
        putRegistryData(map, data, registryFields(typeKey, typeId));
        return map;
    }

    private void loadEntry(final Objective objective, final Map<String, Object> map) {
        objective.setDisplayName(string(map, "displayName"));
        objective.setDescription(string(map, "description"));
        objective.setTaskDescription(string(map, "taskDescription"));
        objective.setCompletionNpc(string(map, "completionNPC"));
        objective.setChildObjectiveProgressOrder(progressOrderString(map.get("predefinedProgressOrder")));
        objective.setLocation(location(map.get("location")));
        objective.setLocationEnabled(bool(map, "showLocation", false));
        for (final Map<String, Object> child : namedMaps(map.get("objectives"), "id")) {
            loadEntry(objective.addChildObjective(integer(child, "id", 1), string(child, "objectiveType"), data(child, "objectiveType"), string(child, "taskDescription")), child);
        }
        loadConditions(objective, "unlock", map.get("conditions"));
        loadConditions(objective, "progress", map.get("conditionsProgress"));
        loadConditions(objective, "complete", map.get("conditionsComplete"));
        for (final Map<String, Object> reward : namedMaps(map.get("rewards"), "id")) {
            final Action action = objective.addReward(
                    integer(reward, "id", 1), string(reward, "actionType"), data(reward, "actionType"));
            loadAction(action, reward);
        }
    }

    private void loadEntry(final Condition condition, final Map<String, Object> map) {
        condition.setDescription(string(map, "description"));
        condition.setHiddenExpression(string(map, "hiddenStatusExpression"));
        condition.setProgressNeeded(longValue(map, "progressNeeded", 1));
        condition.setNegated(bool(map, "negated", false));
        condition.setAllowProgressDecreaseIfNotFulfilled(bool(map, "allowProgressDecreaseIfNotFulfilled", false));
    }

    private void loadAction(final Action action, final Map<String, Object> map) {
        action.setDisplayName(string(map, "displayName"));
        action.setDescription(string(map, "description"));
        for (final Map<String, Object> condition : namedMaps(map.get("conditions"), "id")) {
            final Condition configuredCondition = action.addCondition(
                    integer(condition, "id", 1),
                    string(condition, "conditionType"),
                    data(condition, "conditionType"));
            loadEntry(configuredCondition, condition);
        }
    }

    private void loadConditions(
            final Objective owner,
            final String group,
            final Object rawConditions) {
        for (final Map<String, Object> condition : namedMaps(rawConditions, "id")) {
            loadEntry(owner.addCondition(group, integer(condition, "id", 1), string(condition, "conditionType"), data(condition, "conditionType")), condition);
        }
    }

    private Map<String, Object> savedActionToYaml(final SavedActions.SavedAction action) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("actionType", action.getType().id());
        map.put("displayName", action.getName());
        if (action.getExecutionDelay() != null && !action.getExecutionDelay().isNegative()) {
            map.put("executionDelay", action.getExecutionDelay().toMillis());
        }
        putRegistryData(map, action.getData().values(), registryFields("actionType", action.getType().id()));
        map.put("conditions", keyed(action.getConditions(), condition -> String.valueOf(condition.getID()), this::savedActionConditionToYaml));
        return map;
    }

    private Map<String, Object> savedItemToYaml(final SavedItem item) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", ItemStackSelection.toYamlValue(item.getItemSelection()));
        putNonBlank(map, "displayName", item.getDisplayName());
        return map;
    }

    private void loadSavedItem(final Map<String, Object> map) {
        final String itemName = string(map, "name");
        try {
            final ItemSelection itemSelection = itemSelection(map.get("material"));
            if (!plugin.putSavedItem(
                    itemName,
                    itemSelection,
                    string(map, "category"),
                    string(map, "displayName"))) {
                adapter.warn("Skipping saved item '" + itemName + "': duplicate or invalid saved item.");
            }
        } catch (final RuntimeException exception) {
            adapter.warn("Skipping saved item '" + itemName + "': " + exception.getMessage());
        }
    }

    private void loadSavedAction(final Map<String, Object> map) {
        final Actions.Type type = actionType(string(map, "actionType"));
        if (type == null) {
            adapter.warn("Skipping saved action '" + string(map, "name") + "': unknown type " + string(map, "actionType"));
            return;
        }
        final long delayMillis = longValue(map, "executionDelay", -1);
        plugin.savedActions().save(
                string(map, "name"),
                type,
                data(map, "actionType", "name", "category", "displayName", "executionDelay", "conditions"),
                delayMillis < 0 ? null : Duration.ofMillis(delayMillis));
        final SavedActions.SavedAction action = plugin.savedActions().action(string(map, "name"));
        if (action == null) {
            return;
        }
        action.setCategory(string(map, "category"));
        for (final Map<String, Object> condition : namedMaps(map.get("conditions"), "id")) {
            final Conditions.Type conditionType = conditionType(string(condition, "conditionType"));
            if (conditionType == null) {
                continue;
            }
            final int id = action.addCondition(
                    integer(condition, "id", 1),
                    conditionType,
                    data(condition, "conditionType", "id", "description", "hiddenStatusExpression", "progressNeeded", "negated"));
            final SavedActions.SavedCondition savedCondition = action.getConditionFromID(id);
            if (savedCondition != null) {
                savedCondition.setDescription(string(condition, "description"));
                savedCondition.setHiddenExpression(string(condition, "hiddenStatusExpression"));
                savedCondition.setProgressNeeded(longValue(condition, "progressNeeded", 1));
                savedCondition.setNegated(bool(condition, "negated", false));
            }
        }
    }

    private Map<String, Object> savedActionConditionToYaml(final SavedActions.SavedCondition condition) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("conditionType", condition.getType().id());
        putRegistryData(map, condition.getData().values(), registryFields("conditionType", condition.getType().id()));
        map.put("progressNeeded", condition.getProgressNeeded());
        map.put("negated", condition.isNegated());
        putNonBlank(map, "description", condition.getDescription());
        putNonBlank(map, "hiddenStatusExpression", condition.getHiddenExpression());
        return map;
    }

    private Map<String, Object> savedConditionToYaml(final NotQuestsPlugin.StoredCondition condition) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("conditionType", condition.getType().id());
        putRegistryData(map, condition.getData().values(), registryFields("conditionType", condition.getType().id()));
        map.put("progressNeeded", condition.getProgressNeeded());
        map.put("negated", condition.isNegated());
        putNonBlank(map, "description", condition.getDescription());
        putNonBlank(map, "hiddenStatusExpression", condition.getHiddenExpression());
        return map;
    }

    private void loadSavedCondition(final Map<String, Object> map) {
        final Conditions.Type type = conditionType(string(map, "conditionType"));
        if (type == null) {
            adapter.warn("Skipping saved condition '" + string(map, "name") + "': unknown type " + string(map, "conditionType"));
            return;
        }
        final NotQuestsPlugin.StoredCondition condition = NotQuestsPlugin.StoredCondition.restore(
                string(map, "name"),
                type,
                data(map, "conditionType", "name", "category", "description", "hiddenStatusExpression", "progressNeeded", "negated"));
        condition.setCategory(string(map, "category"));
        condition.setDescription(string(map, "description"));
        condition.setHiddenExpression(string(map, "hiddenStatusExpression"));
        condition.setProgressNeeded(longValue(map, "progressNeeded", 1));
        condition.setNegated(bool(map, "negated", false));
        plugin.putSavedCondition(condition);
    }

    private Map<String, Object> playerToYaml(final QuestPlayer questPlayer) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("player", questPlayer.getPlayerIdentifier());
        map.put("profile", questPlayer.getProfile());
        map.put("questPoints", questPlayer.getQuestPoints());
        map.put("activeQuests", new ArrayList<>(questPlayer.getActiveQuestIdentifiers()));
        map.put("completedObjectives", questPlayer.getCompletedObjectiveIDsByQuest());
        map.put("tags", encodeMap(questPlayer.getTags()));
        map.put("completed", questPlayer.getCompletedQuests().stream().map(completed -> {
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("quest", completed.questIdentifier());
            entry.put("time", completed.timeCompleted());
            return entry;
        }).toList());
        map.put("failed", questPlayer.getFailedQuests().stream().map(failed -> {
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("quest", failed.questIdentifier());
            entry.put("time", failed.timeFailed());
            return entry;
        }).toList());
        return map;
    }

    private void loadPlayer(final Map<String, Object> map) {
        final QuestPlayer questPlayer = plugin.questPlayer(string(map, "player"), string(map, "profile"));
        questPlayer.setQuestPoints(longValue(map, "questPoints", 0));
        for (final String questName : strings(map.get("activeQuests"))) {
            final Quest quest = questManager.getQuest(questName);
            if (quest == null) {
                questPlayer.addActiveQuest(questName);
            } else {
                questPlayer.addActiveQuest(quest);
            }
        }
        for (final Map.Entry<String, Object> entry : mapValue(map.get("completedObjectives")).entrySet()) {
            questPlayer.setCompletedObjectiveIds(entry.getKey(), strings(entry.getValue()));
        }
        for (final Map.Entry<String, Object> tag : decodedData(map.get("tags")).entrySet()) {
            questPlayer.setTagValue(tag.getKey(), tag.getValue());
        }
        for (final Map<String, Object> completed : maps(map.get("completed"))) {
            questPlayer.addCompletedQuest(new QuestPlayer.CompletedQuest(
                    string(completed, "quest"),
                    questPlayer.getPlayerIdentifier(),
                    longValue(completed, "time", 0)));
        }
        for (final Map<String, Object> failed : maps(map.get("failed"))) {
            questPlayer.addFailedQuest(new QuestPlayer.FailedQuest(
                    string(failed, "quest"),
                    questPlayer.getPlayerIdentifier(),
                    longValue(failed, "time", 0)));
        }
    }

    private Map<String, Object> conversationToYaml(final ConversationManager.Conversation conversation) {
        final Map<String, Object> map = new LinkedHashMap<>();
        final String speakerName = conversation.speakers().isEmpty()
                ? "NotQuests"
                : conversation.speakers().getFirst().getSpeakerName();
        map.put("start", speakerName + ".line1");
        final Map<String, Object> speaker = new LinkedHashMap<>();
        final String color = conversation.speakers().isEmpty()
                ? "<gray>"
                : conversation.speakers().getFirst().getColor();
        if (color != null && !color.isBlank()) {
            speaker.put("color", color);
        }
        int lineNumber = 1;
        for (final String line : conversation.lines()) {
            final Map<String, Object> lineMap = new LinkedHashMap<>();
            lineMap.put("text", line);
            if (lineNumber < conversation.lines().size()) {
                lineMap.put("next", speakerName + ".line" + (lineNumber + 1));
            }
            speaker.put("line" + lineNumber, lineMap);
            lineNumber++;
        }
        map.put("Lines", Map.of(speakerName, speaker));
        putConversationNpcAttachments(map, conversation, false);
        return map;
    }

    private Map<String, Object> patchConversationYaml(
            final Map<String, Object> existing,
            final ConversationManager.Conversation conversation) {
        final Map<String, Object> map = new LinkedHashMap<>(existing == null ? Map.of() : existing);
        final Map<String, Object> lines = new LinkedHashMap<>(mapValue(map.get("Lines")));
        for (final Speaker speaker : conversation.speakers()) {
            if (speaker.getSpeakerName() == null || speaker.getSpeakerName().isBlank()) {
                continue;
            }
            final Map<String, Object> speakerMap =
                    new LinkedHashMap<>(mapValue(lines.get(speaker.getSpeakerName())));
            if (speaker.getColor() == null || speaker.getColor().isBlank()) {
                speakerMap.remove("color");
            } else {
                speakerMap.put("color", speaker.getColor());
            }
            lines.put(speaker.getSpeakerName(), speakerMap);
        }
        if (!lines.isEmpty()) {
            map.put("Lines", lines);
        }
        putConversationNpcAttachments(map, conversation, true);
        return map;
    }

    private void putConversationNpcAttachments(
            final Map<String, Object> map,
            final ConversationManager.Conversation conversation,
            final boolean preserveExistingWhenEmpty) {
        final Map<String, Object> npcs = keyed(
                conversation.npcAttachments(),
                NpcAttachment::identifyingString,
                this::conversationNpcToYaml);
        if (npcs.isEmpty()) {
            if (!preserveExistingWhenEmpty) {
                map.remove("npcs");
            }
        } else {
            map.put("npcs", npcs);
        }
    }

    private Map<String, Object> conversationNpcToYaml(final NpcAttachment attachment) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", attachment.npcType());
        putNonBlank(map, "name", attachment.npcName());
        if (attachment.npcId() != null) {
            if (attachment.npcId().getStringID() != null) {
                map.put("stringID", attachment.npcId().getStringID());
            } else if (attachment.npcId().getIntegerID() != -1) {
                map.put("integerID", attachment.npcId().getIntegerID());
            } else if (attachment.npcId().getUUIDID() != null) {
                map.put("uuidID", attachment.npcId().getUUIDID().toString());
            }
        }
        return map;
    }

    private void loadConversation(final Map<String, Object> map) {
        final String name = string(map, "name");
        if (name.isBlank()) {
            return;
        }
        try {
            plugin.conversationManager().save(name, map, string(map, "category"));
        } catch (final IllegalArgumentException exception) {
            adapter.warn("Could not load conversation <highlight>" + name + "</highlight>: " + exception.getMessage());
        }
    }

    private LoadedValues data(
            final Map<String, Object> entry,
            final String typeKey,
            final String... additionallyReservedKeys) {
        final LinkedHashSet<String> reserved = new LinkedHashSet<>(List.of(
                "id",
                "objectiveType",
                "conditionType",
                "actionType",
                "triggerType",
                "displayName",
                "description",
                "taskDescription",
                "progressNeeded",
                "negated",
                "allowProgressDecreaseIfNotFulfilled",
                "hiddenStatusExpression",
                "completionNPC",
                "predefinedProgressOrder",
                "location",
                "showLocation",
                "objectives",
                "conditions",
                "conditionsProgress",
                "conditionsComplete",
                "npcs",
                "rewards"));
        reserved.addAll(List.of(additionallyReservedKeys));

        final List<RegistryField.Definition> fields = registryFields(typeKey, string(entry, typeKey));
        for (final RegistryField.Definition field : fields) {
            if (field.configPath() != null && !field.configPath().isBlank()) {
                reserved.add(firstPathPart(field.configPath()));
                reserved.add(field.name());
            }
        }

        final Map<String, Object> values = dataValues(entry, reserved.toArray(String[]::new));
        for (final RegistryField.Definition field : fields) {
            final Object configured = configuredFieldValue(entry, field);
            if (configured == null) {
                continue;
            }
            values.put(
                    field.name(),
                    field.invertedBooleanConfig()
                            ? !booleanValue(configured, true)
                            : decodeRegistryValue(field, configured));
        }
        return new LoadedValues(values);
    }

    /** Private YAML decoding buffer; configured domain objects copy these values immediately. */
    private static final class LoadedValues implements
            Actions.Data,
            Conditions.Data,
            Objectives.Data,
            Triggers.Data {
        private final Map<String, Object> values;

        private LoadedValues(final Map<String, Object> values) {
            this.values = new LinkedHashMap<>(values == null ? Map.of() : values);
        }

        @Override
        public void copyTo(final Actions.Draft action) {
            values.forEach(action::setValue);
        }

        @Override
        public void copyTo(final Conditions.Draft condition) {
            values.forEach(condition::setValue);
        }

        @Override
        public void copyTo(final Objectives.Draft objective) {
            values.forEach(objective::setValue);
        }

        @Override
        public void copyTo(final Triggers.Draft trigger) {
            values.forEach(trigger::setValue);
        }

        @Override
        public Object value(final String name) {
            return values.get(name);
        }

        @Override
        public <V> V value(final String name, final Class<V> type) {
            final Object value = value(name);
            return type.isInstance(value) ? type.cast(value) : null;
        }

        @Override
        public String text(final String name) {
            final Object value = value(name);
            return value == null ? "" : String.valueOf(value);
        }

        @Override
        public int integer(final String name) {
            return integer(name, 0);
        }

        @Override
        public int integer(final String name, final int fallback) {
            final Object value = value(name);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        @Override
        public boolean flag(final String name) {
            return Boolean.TRUE.equals(value(name));
        }

        @Override
        public double number(final String name, final double fallback) {
            final Object value = value(name);
            return value instanceof Number number ? number.doubleValue() : fallback;
        }

        @Override
        public NQLocation location(final String name) {
            return value(name) instanceof NQLocation location ? location : null;
        }

        @Override
        public ItemSelection itemSelection(final String name) {
            return value(name) instanceof ItemSelection item ? item : null;
        }
    }

    private void putRegistryData(
            final Map<String, Object> target,
            final Map<String, Object> data,
            final List<RegistryField.Definition> fields) {
        final Map<String, RegistryField.Definition> fieldsByName = fields.stream()
                .collect(Collectors.toMap(
                        field -> field.name().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        for (final Map.Entry<String, Object> entry : data.entrySet()) {
            final RegistryField.Definition field =
                    fieldsByName.get(entry.getKey().toLowerCase(Locale.ROOT));
            if (field == null || field.configPath() == null || field.configPath().isBlank()) {
                target.put(entry.getKey(), encodeValue(entry.getValue()));
                continue;
            }
            final Object value = field.invertedBooleanConfig()
                    ? !booleanValue(entry.getValue(), false)
                    : entry.getValue();
            putPath(target, field.configPath(), encodeValue(value));
        }
    }

    private Object configuredFieldValue(
            final Map<String, Object> entry,
            final RegistryField.Definition field) {
        if (field.configPath() == null || field.configPath().isBlank()) {
            return entry.get(field.name());
        }
        return valueAt(entry, field.configPath());
    }

    private Object decodeRegistryValue(
            final RegistryField.Definition field,
            final Object configured) {
        return switch (field.valueType()) {
            case "itemSelection", "itemStack" -> itemSelection(configured);
            case "location" -> location(configured);
            case "duration" -> {
                final Object decoded = decodeValue(configured);
                if (decoded instanceof Duration duration) {
                    yield duration;
                }
                if (configured instanceof Number number) {
                    yield Duration.ofMillis(number.longValue());
                }
                try {
                    yield Duration.ofMillis(Long.parseLong(String.valueOf(configured)));
                } catch (final NumberFormatException ignored) {
                    yield null;
                }
            }
            case "integer" -> configured instanceof Number number
                    ? number.intValue()
                    : parseInteger(configured);
            case "number", "optionalNumber" -> configured instanceof Number number
                    ? number.doubleValue()
                    : parseDouble(configured);
            default -> decodeValue(configured);
        };
    }

    private static Object parseInteger(final Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (final NumberFormatException ignored) {
            return value;
        }
    }

    private static Object parseDouble(final Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (final NumberFormatException ignored) {
            return value;
        }
    }

    private List<RegistryField.Definition> registryFields(
            final String typeKey,
            final String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return List.of();
        }
        return switch (typeKey) {
            case "objectiveType" -> {
                final Objectives.Type type = objectiveType(typeId);
                yield type == null ? List.of() : joinedFields(type.fields(), type.flags());
            }
            case "conditionType" -> {
                final Conditions.Type type = conditionType(typeId);
                yield type == null ? List.of() : joinedFields(type.fields(), type.flags());
            }
            case "actionType" -> {
                final Actions.Type type = actionType(typeId);
                yield type == null ? List.of() : joinedFields(type.fields(), type.flags());
            }
            case "triggerType" -> {
                final Triggers.Type type = triggerType(typeId);
                yield type == null ? List.of() : type.fields();
            }
            default -> List.of();
        };
    }

    private static List<RegistryField.Definition> joinedFields(
            final List<RegistryField.Definition> fields,
            final List<RegistryField.Definition> flags) {
        final ArrayList<RegistryField.Definition> joined = new ArrayList<>();
        joined.addAll(fields == null ? List.of() : fields);
        joined.addAll(flags == null ? List.of() : flags);
        return List.copyOf(joined);
    }

    private static Object valueAt(final Map<String, Object> map, final String path) {
        Object current = map;
        for (final String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(part);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static void putPath(
            final Map<String, Object> map,
            final String path,
            final Object value) {
        final String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int index = 0; index < parts.length - 1; index++) {
            final Object child = current.get(parts[index]);
            if (child instanceof Map<?, ?> childMap) {
                current = (Map<String, Object>) childMap;
            } else {
                final Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[index], created);
                current = created;
            }
        }
        current.put(parts[parts.length - 1], value);
    }

    private static String firstPathPart(final String path) {
        final int separator = path.indexOf('.');
        return separator < 0 ? path : path.substring(0, separator);
    }

    private static boolean booleanValue(final Object value, final boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return fallback;
    }

    private Object itemSelectionToYaml(final ItemSelection itemSelection) {
        if (itemSelection == null) {
            return null;
        }
        final Object yamlValue = ItemStackSelection.toYamlValue(itemSelection);
        if (yamlValue instanceof final String string && string.isBlank()) {
            return null;
        }
        return yamlValue;
    }

    private ItemSelection itemSelection(final Map<String, Object> map, final String key) {
        return map.containsKey(key) ? itemSelection(map.get(key)) : null;
    }

    private ItemSelection itemSelection(final Object yamlValue) {
        return ItemStackSelection.fromYamlValue(yamlValue, itemName -> plugin.savedItem(itemName) != null);
    }

    private Map<String, Object> encodeMap(final Map<String, Object> source) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            map.put(entry.getKey(), encodeValue(entry.getValue()));
        }
        return map;
    }

    private Object encodeValue(final Object value) {
        if (value instanceof final NQLocation location) {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("$type", "location");
            map.put("world", location.worldName());
            map.put("x", location.x());
            map.put("y", location.y());
            map.put("z", location.z());
            map.put("yaw", location.yaw());
            map.put("pitch", location.pitch());
            return map;
        }
        if (value instanceof final ItemSelection itemSelection) {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("$type", "itemSelection");
            map.put("value", ItemStackSelection.toYamlValue(itemSelection));
            return map;
        }
        if (value instanceof final Duration duration) {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("$type", "duration");
            map.put("millis", duration.toMillis());
            return map;
        }
        if (value instanceof final Map<?, ?> map) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), encodeValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof final List<?> list) {
            return list.stream().map(this::encodeValue).toList();
        }
        return value;
    }

    private Map<String, Object> decodedData(final Object raw) {
        final Map<String, Object> result = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> map)) {
            return result;
        }
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), decodeValue(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> dataValues(final Map<String, Object> map, final String... reservedKeys) {
        final Set<String> reserved = new HashSet<>();
        for (final String reservedKey : reservedKeys) {
            reserved.add(reservedKey.toLowerCase(Locale.ROOT));
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (!reserved.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                result.put(entry.getKey(), decodeValue(entry.getValue()));
            }
        }
        return result;
    }

    private Object decodeValue(final Object value) {
        if (value instanceof final Map<?, ?> map && map.get("$type") != null) {
            final String type = String.valueOf(map.get("$type"));
            return switch (type) {
                case "location" -> storedLocation(map);
                case "itemSelection" -> itemSelection(map.get("value"));
                case "duration" -> Duration.ofMillis(longValue(map.get("millis"), -1));
                default -> value;
            };
        }
        if (value instanceof final Map<?, ?> map && looksLikeItemSelection(map)) {
            return itemSelection(map);
        }
        if (value instanceof final Map<?, ?> map) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), decodeValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof final List<?> list) {
            return list.stream().map(this::decodeValue).toList();
        }
        return value;
    }

    private static boolean looksLikeItemSelection(final Map<?, ?> map) {
        return map.containsKey("materials")
                || map.containsKey("exactItems")
                || map.containsKey("nqItems")
                || (map.containsKey("any") && map.keySet().stream()
                        .allMatch(key -> "any".equals(String.valueOf(key))
                                || "materials".equals(String.valueOf(key))
                                || "exactItems".equals(String.valueOf(key))
                                || "nqItems".equals(String.valueOf(key))
                                || "amount".equals(String.valueOf(key))));
    }

    private NQLocation location(final Object raw) {
        final Object decoded = decodeValue(raw);
        if (decoded instanceof NQLocation location) {
            return location;
        }
        return null;
    }

    private static NQLocation storedLocation(final Map<?, ?> map) {
        return NQLocation.at(
                String.valueOf(map.get("world") == null ? "world" : map.get("world")),
                number(map.get("x"), 0),
                number(map.get("y"), 0),
                number(map.get("z"), 0),
                (float) number(map.get("yaw"), 0),
                (float) number(map.get("pitch"), 0));
    }

    private Actions.Type actionType(final String typeId) {
        return plugin.registry().actions().stream()
                .filter(type -> type.id().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);
    }

    private Objectives.Type objectiveType(final String typeId) {
        return plugin.registry().objectives().stream()
                .filter(type -> type.id().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);
    }

    private Conditions.Type conditionType(final String typeId) {
        return plugin.registry().conditions().stream()
                .filter(type -> type.id().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);
    }

    private Triggers.Type triggerType(final String typeId) {
        return plugin.registry().triggers().stream()
                .filter(type -> type.id().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(final Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        final List<Map<String, Object>> maps = new ArrayList<>();
        for (final Object value : list) {
            if (value instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        return maps;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> namedMaps(final Object raw, final String nameKey) {
        if (raw instanceof List<?>) {
            return maps(raw);
        }
        if (!(raw instanceof Map<?, ?> source)) {
            return List.of();
        }
        final List<Map<String, Object>> maps = new ArrayList<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            final Map<String, Object> map = new LinkedHashMap<>();
            if (entry.getValue() instanceof Map<?, ?> value) {
                for (final Map.Entry<?, ?> valueEntry : value.entrySet()) {
                    map.put(String.valueOf(valueEntry.getKey()), valueEntry.getValue());
                }
            }
            map.putIfAbsent(nameKey, String.valueOf(entry.getKey()));
            maps.add(map);
        }
        return maps;
    }

    private static List<String> strings(final Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
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

    private static String string(final Map<String, Object> map, final String key) {
        final Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private static void putNonBlank(final Map<String, Object> map, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static void putNonEmpty(final Map<String, Object> map, final String key, final Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    private static void putProgressOrder(final Map<String, Object> map, final String key, final String progressOrder) {
        if (progressOrder == null || progressOrder.isBlank()) {
            return;
        }
        final String trimmed = progressOrder.trim();
        final Map<String, Object> value = new LinkedHashMap<>();
        if (trimmed.equalsIgnoreCase("firstToLast")) {
            value.put("firstToLast", true);
        } else if (trimmed.equalsIgnoreCase("lastToFirst")) {
            value.put("lastToFirst", true);
        } else if (trimmed.regionMatches(true, 0, "custom", 0, "custom".length())) {
            final String custom = trimmed.substring("custom".length()).trim();
            if (!custom.isBlank()) {
                value.put("custom", List.of(custom.split("[,\\s]+")));
            }
        }
        if (!value.isEmpty()) {
            map.put(key, value);
        }
    }

    private static String progressOrderString(final Object raw) {
        if (raw instanceof String string) {
            return string;
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return "";
        }
        if (Boolean.TRUE.equals(map.get("firstToLast"))) {
            return "firstToLast";
        }
        if (Boolean.TRUE.equals(map.get("lastToFirst"))) {
            return "lastToFirst";
        }
        final Object custom = map.get("custom");
        if (custom instanceof List<?> list && !list.isEmpty()) {
            return "custom " + String.join(" ", list.stream().map(String::valueOf).toList());
        }
        return "";
    }

    private static int integer(final Map<String, Object> map, final String key, final int fallback) {
        final Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static int integerAtPath(final Map<String, Object> map, final String path, final int fallback) {
        final Object value = valueAt(map, path);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static long longValue(final Map<String, Object> map, final String key, final long fallback) {
        return longValue(map.get(key), fallback);
    }

    private static long longAtPath(final Map<String, Object> map, final String path, final long fallback) {
        return longValue(valueAt(map, path), fallback);
    }

    private static long longValue(final Object value, final long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static double number(final Object value, final double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean bool(final Map<String, Object> map, final String key, final boolean fallback) {
        final Object value = map.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static <E extends Enum<E>> E enumValue(
            final Class<E> enumClass,
            final String value,
            final E fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return fallback;
        }
    }
}
