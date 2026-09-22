package com.notquests.core.migrations.v6_3_0;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import com.notquests.core.config.CategoryFiles;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.migrations.ConfigurationMigrations.Context;
import com.notquests.core.migrations.ConfigurationMigrations.Migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Converts the complete released 6.3 configuration shape to the canonical v7 shape once. */
public final class Version630Migration implements Migration<Context> {
  private static final String ITEM_SELECTION_PATH = "specifics.itemStackSelection";
  private static final String BUKKIT_TYPE = "==";
  private static final String JOURNAL_ITEM = "general.journal-item.item";
  private static final String PAPER_ITEM_TYPE = "ItemStack";
  private static final String PAPER_ITEM_MARKER = "__notquestsType";
  private static final String PAPER_ITEM_DATA = "serialized";
  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final GsonComponentSerializer JSON_COMPONENTS = GsonComponentSerializer.gson();
  private static final Map<String, String> RELEASED_ENCHANTMENT_NAMES = Map.ofEntries(
      Map.entry("protection_environmental", "protection"),
      Map.entry("protection_fire", "fire_protection"),
      Map.entry("protection_fall", "feather_falling"),
      Map.entry("protection_explosions", "blast_protection"),
      Map.entry("protection_projectile", "projectile_protection"),
      Map.entry("oxygen", "respiration"),
      Map.entry("water_worker", "aqua_affinity"),
      Map.entry("damage_all", "sharpness"),
      Map.entry("damage_undead", "smite"),
      Map.entry("damage_arthropods", "bane_of_arthropods"),
      Map.entry("loot_bonus_mobs", "looting"),
      Map.entry("dig_speed", "efficiency"),
      Map.entry("durability", "unbreaking"),
      Map.entry("loot_bonus_blocks", "fortune"),
      Map.entry("arrow_damage", "power"),
      Map.entry("arrow_knockback", "punch"),
      Map.entry("arrow_fire", "flame"),
      Map.entry("arrow_infinite", "infinity"),
      Map.entry("luck", "luck_of_the_sea"));
  private static final Map<String, String> RELEASED_ITEM_FLAGS = Map.of(
      "HIDE_ENCHANTS", "enchantments",
      "HIDE_ATTRIBUTES", "attribute-modifiers",
      "HIDE_UNBREAKABLE", "unbreakable",
      "HIDE_DESTROYS", "can-break",
      "HIDE_PLACED_ON", "can-place-on",
      "HIDE_ADDITIONAL_TOOLTIP", "additional-tooltip",
      "HIDE_DYE", "dyed-color",
      "HIDE_ARMOR_TRIM", "trim",
      "HIDE_STORED_ENCHANTS", "stored-enchantments");
  private static final Set<String> RELEASED_JOURNAL_ITEM_KEYS = Set.of(
      BUKKIT_TYPE, "v", "type", "amount", "damage", "meta");
  private static final Set<String> RELEASED_JOURNAL_META_KEYS = Set.of(
      BUKKIT_TYPE,
      "meta-type",
      "display-name",
      "lore",
      "custom-model-data",
      "enchants",
      "stored-enchants",
      "ItemFlags",
      "item-flags",
      "Unbreakable",
      "unbreakable",
      "Damage",
      "damage");

  @Override
  public String targetVersion() {
    return "7.0.0";
  }

  @Override
  public boolean migrate(final Context context) {
    final Path dataFolder = context.dataFolder();
    if (!Files.isDirectory(dataFolder)) {
      return false;
    }
    boolean changed = migrateReleasedQuestLayout(context);
    changed |= migrateGeneralConfig(context);
    try {
      for (final CategoryFiles.CategoryFolder category : CategoryFiles.discover(dataFolder)) {
        changed |= migrateFile(context, category.categoryFile());
        changed |= migrateFile(context, category.questsFile());
        changed |= migrateFile(context, category.actionsFile());
        changed |= migrateFile(context, category.conditionsFile());
        changed |= migrateFile(context, category.tagsFile());
        changed |= migrateFile(context, category.itemsFile());
        changed |= migrateConversationFolder(context, category.conversationsFolder());
      }
    } catch (final IOException exception) {
      throw failed(context, "Could not discover category files while upgrading 6.3 data.", exception);
    }
    return changed;
  }

  private boolean migrateReleasedQuestLayout(final Context context) {
    boolean changed = false;
    try {
      for (final CategoryFiles.CategoryFolder category : CategoryFiles.discover(context.dataFolder())) {
        final YamlConfig quests = YamlConfig.load(category.questsFile());
        final YamlConfig actions = YamlConfig.load(category.actionsFile());
        final boolean questsChanged = migrateQuestConfig(quests);
        final boolean actionsChanged = migrateActionsSection(actions, "actions");

        if (questsChanged) {
          YamlConfig.save(quests, category.questsFile());
        }
        if (actionsChanged) {
          YamlConfig.save(actions, category.actionsFile());
        }
        changed |= questsChanged || actionsChanged;
      }
    } catch (final Exception exception) {
      throw failed(context, "Could not upgrade the released 6.3 quest layout.", exception);
    }
    return changed;
  }

  private static boolean migrateGeneralConfig(final Context context) {
    final YamlConfig configuration = context.generalConfig();
    if (configuration == null) {
      return false;
    }
    boolean changed = migrateJournalItem(context, configuration);
    changed |= migrateDatabaseEnabled(configuration);
    changed |= migrateConsoleColors(configuration);
    final Change migrated = migrateValue(configuration.asMap());
    if (migrated.changed() && migrated.value() instanceof Map<?, ?> map) {
      configuration.replaceContents(map);
      changed = true;
    }
    return changed;
  }

  private static boolean migrateConsoleColors(final YamlConfig configuration) {
    boolean changed = false;
    for (final String path : List.of(
        "visual.colors.console.info.default",
        "visual.colors.console.info.data",
        "visual.colors.console.info.language",
        "visual.colors.console.warn.default",
        "visual.colors.console.severe.default",
        "visual.colors.console.debug.default")) {
      final Object color = configuration.get(path);
      if (!(color instanceof String)) {
        continue;
      }
      configuration.set(path, null);
      configuration.set(path + ".normal", color);
      changed = true;
    }
    return changed;
  }

  private boolean migrateConversationFolder(final Context context, final Path folder) {
    if (!Files.isDirectory(folder)) {
      return false;
    }
    boolean changed = false;
    try (Stream<Path> files = Files.list(folder)) {
      for (final Path file : files
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
          .sorted()
          .toList()) {
        changed |= migrateConversationFile(context, file);
      }
    } catch (final IOException exception) {
      throw failed(context, "Could not discover 6.3 conversation files.", exception);
    }
    return changed;
  }

  private boolean migrateConversationFile(final Context context, final Path file) {
    try {
      final YamlConfig configuration = YamlConfig.load(file);
      boolean changed = migrateConversationNpcs(configuration);
      final Change migrated = migrateValue(configuration.asMap());
      if (migrated.changed() && migrated.value() instanceof Map<?, ?> map) {
        configuration.replaceContents(map);
        changed = true;
      }
      if (changed) {
        YamlConfig.save(configuration, file);
      }
      return changed;
    } catch (final Exception exception) {
      throw failed(
          context,
          "Could not upgrade 6.3 conversation " + file.getFileName() + ".",
          exception);
    }
  }

  private static boolean migrateConversationNpcs(final YamlConfig configuration) {
    final Map<String, Object> root = new LinkedHashMap<>(configuration.asMap());
    final Map<String, Object> migratedNpcs = new LinkedHashMap<>();
    boolean changed = false;

    final Object configuredNpcs = root.get("npcs");
    if (configuredNpcs instanceof Map<?, ?> npcs) {
      for (final Map.Entry<?, ?> entry : npcs.entrySet()) {
        if (!(entry.getValue() instanceof Map<?, ?> npc)) {
          changed = true;
          continue;
        }
        final String originalKey = String.valueOf(entry.getKey());
        final Change normalized = normalizeConversationNpc(npc, originalKey, "");
        migratedNpcs.put(originalKey, normalized.value());
        changed |= normalized.changed();
      }
    }

    if (root.containsKey("npcID")) {
      changed |= addOldConversationNpc(migratedNpcs, root.get("npcID"));
      root.remove("npcID");
      changed = true;
    }
    if (root.containsKey("npcIDs")) {
      for (final Object id : values(root.get("npcIDs"))) {
        changed |= addOldConversationNpc(migratedNpcs, id);
      }
      root.remove("npcIDs");
      changed = true;
    }

    if (!migratedNpcs.isEmpty()) {
      root.put("npcs", migratedNpcs);
    } else if (configuredNpcs != null) {
      root.remove("npcs");
    }
    if (changed) {
      configuration.replaceContents(root);
    }
    return changed;
  }

  private static boolean addOldConversationNpc(
      final Map<String, Object> npcs,
      final Object rawId) {
    final Map<String, Object> npc = new LinkedHashMap<>();
    npc.put("type", "citizens");
    if (!putCanonicalNpcId(npc, rawId, "")) {
      return false;
    }
    final Object id = npc.containsKey("integerID")
        ? npc.get("integerID")
        : npc.containsKey("uuidID") ? npc.get("uuidID") : npc.get("stringID");
    npcs.putIfAbsent("citizens-" + id, npc);
    return true;
  }

  private static Change normalizeConversationNpc(
      final Map<?, ?> source,
      final String key,
      final String defaultType) {
    final Map<String, Object> normalized = copyMap(source);
    String type = firstNonBlank(
        source.get("type"), source.get("npcType"), source.get("PluginNPCType"), defaultType);
    final String name = firstNonBlank(
        source.get("name"), source.get("npcName"), source.get("displayName"));

    for (final String alias : List.of(
        "npcType", "PluginNPCType", "npcName", "displayName",
        "stringId", "PluginNPCStringID", "uuidId", "uuid", "PluginNPCUUID",
        "integerId", "PluginNPCID", "npcID", "id",
        "stringID", "uuidID", "integerID")) {
      normalized.remove(alias);
    }
    if (type.isBlank()) {
      final int separator = key.indexOf('-');
      if (separator > 0) {
        type = key.substring(0, separator);
      }
    }
    if (!type.isBlank()) {
      normalized.put("type", type);
    }
    if (!name.isBlank()) {
      normalized.put("name", name);
    }

    boolean idPresent = false;
    if (source.get("stringID") != null || source.get("stringId") != null
        || source.get("PluginNPCStringID") != null) {
      final String stringId = firstNonBlank(
          source.get("stringID"), source.get("stringId"), source.get("PluginNPCStringID"));
      if (!stringId.isBlank()) {
        normalized.put("stringID", stringId);
        idPresent = true;
      }
    } else if (source.get("uuidID") != null || source.get("uuidId") != null
        || source.get("uuid") != null || source.get("PluginNPCUUID") != null) {
      final String uuid = firstNonBlank(
          source.get("uuidID"), source.get("uuidId"), source.get("uuid"), source.get("PluginNPCUUID"));
      if (validUuid(uuid)) {
        normalized.put("uuidID", uuid);
        idPresent = true;
      }
    } else {
      final Object integer = firstPresent(
          source.get("integerID"), source.get("integerId"), source.get("PluginNPCID"),
          source.get("npcID"), source.get("id"));
      idPresent = putCanonicalNpcId(normalized, integer, "");
    }
    if (!idPresent) {
      final int separator = key.indexOf('-');
      final String keyId = separator < 0 ? key : key.substring(separator + 1);
      putCanonicalNpcId(normalized, keyId, "");
    }
    return new Change(normalized, !normalized.equals(copyMap(source)));
  }

  private static boolean putCanonicalNpcId(
      final Map<String, Object> target,
      final Object raw,
      final String forcedKind) {
    if (raw == null || raw.toString().isBlank()) {
      return false;
    }
    final String value = raw.toString();
    if ("string".equals(forcedKind)) {
      target.put("stringID", value);
      return true;
    }
    if ("uuid".equals(forcedKind) || validUuid(value)) {
      target.put("uuidID", value);
      return true;
    }
    try {
      target.put("integerID", Integer.parseInt(value));
    } catch (final NumberFormatException ignored) {
      target.put("stringID", value);
    }
    return true;
  }

  private static boolean validUuid(final String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (final IllegalArgumentException exception) {
      return false;
    }
  }

  private static Object firstPresent(final Object... values) {
    for (final Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String firstNonBlank(final Object... values) {
    for (final Object value : values) {
      if (value != null && !value.toString().isBlank()) {
        return value.toString();
      }
    }
    return "";
  }

  private boolean migrateFile(final Context context, final Path file) {
    if (!Files.isRegularFile(file)) {
      return false;
    }
    try {
      final YamlConfig configuration = YamlConfig.load(file);
      boolean changed = false;
      final Change migrated = migrateValue(configuration.asMap());
      if (migrated.changed() && migrated.value() instanceof Map<?, ?> map) {
        configuration.replaceContents(map);
        changed = true;
      }
      if (!changed) {
        return false;
      }
      YamlConfig.save(configuration, file);
      return true;
    } catch (final Exception exception) {
      throw failed(
          context,
          "Could not upgrade 6.3 values in " + file.getFileName() + ".",
          exception);
    }
  }

  private static boolean migrateJournalItem(
      final Context context,
      final YamlConfig configuration) {
    final Object configuredItem = configuration.get(JOURNAL_ITEM);
    if (!(configuredItem instanceof Map<?, ?> item) || !isReleasedItemStack(item)) {
      return false;
    }
    warnAboutUnsupportedJournalMetadata(context, item);
    configuration.set(JOURNAL_ITEM, portableJournalItem(item));
    return true;
  }

  private static Map<String, Object> portableJournalItem(final Map<?, ?> releasedItem) {
    final Map<String, Object> journal = new LinkedHashMap<>();
    final Object material = releasedItem.get("type");
    journal.put("material", material == null ? "ENCHANTED_BOOK" : material.toString());
    if (releasedItem.get("amount") instanceof Number amount) {
      journal.put("amount", Math.max(1, amount.intValue()));
    }
    if (firstPresent(releasedItem.get("damage"), releasedMetaValue(releasedItem, "Damage", "damage"))
        instanceof Number damage) {
      journal.put("damage", Math.max(0, damage.intValue()));
    }
    if (releasedItem.get("meta") instanceof Map<?, ?> meta) {
      if (meta.get("display-name") != null) {
        journal.put("display-name", releasedComponentText(meta.get("display-name")));
      }
      if (meta.get("lore") instanceof List<?> lore) {
        journal.put("lore", lore.stream().map(Version630Migration::releasedComponentText).toList());
      }
      if (meta.get("custom-model-data") instanceof Number modelData) {
        journal.put("custom-model-data", modelData.intValue());
      }
      final Map<String, Integer> enchantments = portableEnchantments(meta.get("enchants"));
      if (!enchantments.isEmpty()) {
        journal.put("enchantments", enchantments);
      }
      final Map<String, Integer> storedEnchantments = portableEnchantments(meta.get("stored-enchants"));
      if (!storedEnchantments.isEmpty()) {
        journal.put("stored-enchantments", storedEnchantments);
      }
      final List<String> hiddenComponents = portableHiddenComponents(
          firstPresent(meta.get("ItemFlags"), meta.get("item-flags")));
      if (!hiddenComponents.isEmpty()) {
        journal.put("hidden-components", hiddenComponents);
      }
      if (Boolean.TRUE.equals(firstPresent(meta.get("Unbreakable"), meta.get("unbreakable")))) {
        journal.put("unbreakable", true);
      }
      if (!enchantments.isEmpty() || !storedEnchantments.isEmpty()) {
        journal.put("glint", true);
      }
    }
    return journal;
  }

  private static Object releasedMetaValue(
      final Map<?, ?> releasedItem,
      final String... keys) {
    if (!(releasedItem.get("meta") instanceof Map<?, ?> meta)) {
      return null;
    }
    for (final String key : keys) {
      if (meta.get(key) != null) {
        return meta.get(key);
      }
    }
    return null;
  }

  private static Map<String, Integer> portableEnchantments(final Object value) {
    if (!(value instanceof Map<?, ?> enchantments)) {
      return Map.of();
    }
    final Map<String, Integer> result = new LinkedHashMap<>();
    for (final Map.Entry<?, ?> entry : enchantments.entrySet()) {
      if (!(entry.getValue() instanceof Number level) || level.intValue() <= 0) {
        continue;
      }
      String id = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
      id = RELEASED_ENCHANTMENT_NAMES.getOrDefault(id, id);
      if (!id.contains(":")) {
        id = "minecraft:" + id;
      }
      result.put(id, level.intValue());
    }
    return Map.copyOf(result);
  }

  private static List<String> portableHiddenComponents(final Object value) {
    final List<String> result = new ArrayList<>();
    for (final Object entry : values(value)) {
      final String component = RELEASED_ITEM_FLAGS.get(String.valueOf(entry).toUpperCase(Locale.ROOT));
      if (component != null && !result.contains(component)) {
        result.add(component);
      }
    }
    return List.copyOf(result);
  }

  private static void warnAboutUnsupportedJournalMetadata(
      final Context context,
      final Map<?, ?> releasedItem) {
    final List<String> unsupported = new ArrayList<>();
    for (final Object key : releasedItem.keySet()) {
      if (!RELEASED_JOURNAL_ITEM_KEYS.contains(String.valueOf(key))) {
        unsupported.add("item." + key);
      }
    }
    if (releasedItem.get("meta") instanceof Map<?, ?> meta) {
      for (final Object key : meta.keySet()) {
        if (!RELEASED_JOURNAL_META_KEYS.contains(String.valueOf(key))) {
          unsupported.add("meta." + key);
        }
      }
    }
    if (!unsupported.isEmpty()) {
      context.warnLogger().accept(
          "The 6.3 journal contains platform-only metadata that cannot be represented on every "
              + "supported platform: " + String.join(", ", unsupported)
              + ". The portable fields were migrated; the pre-migration backup retains the original item.",
          null);
    }
  }

  private static String releasedComponentText(final Object value) {
    final String text = value == null ? "" : value.toString();
    if (!text.startsWith("{") && !text.startsWith("[")) {
      return text;
    }
    try {
      return MINI_MESSAGE.serialize(JSON_COMPONENTS.deserialize(text));
    } catch (final RuntimeException ignored) {
      // Some old administrators used hand-written component-like values. Fall through to the
      // minimal released JSON reader so the migration still preserves their visible text.
    }
    final StringBuilder result = new StringBuilder();
    int offset = 0;
    while ((offset = text.indexOf("\"text\"", offset)) >= 0) {
      final int colon = text.indexOf(':', offset + 6);
      final int quote = colon < 0 ? -1 : text.indexOf('"', colon + 1);
      if (quote < 0) {
        break;
      }
      int end = quote + 1;
      boolean escaped = false;
      while (end < text.length()) {
        final char character = text.charAt(end);
        if (character == '"' && !escaped) {
          break;
        }
        escaped = character == '\\' && !escaped;
        if (character != '\\') {
          escaped = false;
        }
        end++;
      }
      if (end >= text.length()) {
        break;
      }
      result.append(unescapeJson(text.substring(quote + 1, end)));
      offset = end + 1;
    }
    return result.isEmpty() ? text : result.toString();
  }

  private static String unescapeJson(final String value) {
    return value
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\");
  }

  private static boolean nonEmptyMap(final Object value) {
    return value instanceof Map<?, ?> map && !map.isEmpty();
  }

  private static boolean migrateDatabaseEnabled(final YamlConfig configuration) {
    if (configuration.isBoolean("storage.database.enabled")) {
      return false;
    }
    final boolean complete = configuration.isString("storage.database.host")
        && configuration.isInt("storage.database.port")
        && configuration.isString("storage.database.database")
        && configuration.isString("storage.database.username")
        && configuration.isString("storage.database.password");
    if (!complete) {
      return false;
    }
    configuration.set("storage.database.enabled", true);
    return true;
  }

  private static Change migrateValue(final Object value) {
    if (value instanceof Map<?, ?> map) {
      if (map.containsKey("platform") && map.containsKey("data")) {
        return new Change(copyMap(map), false);
      }
      if (isReleasedLocation(map)) {
        return new Change(portableLocation(map), true);
      }
      if (isReleasedItemStack(map)) {
        return new Change(itemSelectionFor(map), true);
      }
      if (looksLikeReleasedItemSelection(map)) {
        return migrateItemSelectionMap(map);
      }
      boolean changed = false;
      final Map<String, Object> migrated = new LinkedHashMap<>();
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        final String key = String.valueOf(entry.getKey());
        if ("completionNPC".equals(key) && entry.getValue() instanceof Map<?, ?> npc) {
          final String selector = npcSelector(npc);
          if (!selector.isBlank()) {
            migrated.put(key, selector);
            changed = true;
            continue;
          }
        }
        if ("npcData".equals(key) && entry.getValue() instanceof Map<?, ?> npc) {
          final Change normalizedNpc = normalizeNpcData(npc);
          migrated.put(key, normalizedNpc.value());
          changed |= normalizedNpc.changed();
          continue;
        }
        final Change child = migrateValue(entry.getValue());
        migrated.put(key, child.value());
        changed |= child.changed();
      }
      return new Change(migrated, changed);
    }
    if (value instanceof List<?> list) {
      boolean changed = false;
      final List<Object> migrated = new ArrayList<>();
      for (final Object entry : list) {
        final Change child = migrateValue(entry);
        migrated.add(child.value());
        changed |= child.changed();
      }
      return new Change(migrated, changed);
    }
    return new Change(value, false);
  }

  private static Change migrateItemSelectionMap(final Map<?, ?> source) {
    final Map<String, Object> migrated = new LinkedHashMap<>();
    boolean changed = false;
    for (final Map.Entry<?, ?> entry : source.entrySet()) {
      final String key = String.valueOf(entry.getKey());
      if ("itemStacks".equals(key)) {
        final List<Map<String, Object>> exactItems = new ArrayList<>();
        for (final Object item : values(entry.getValue())) {
          if (item instanceof Map<?, ?> itemMap) {
            exactItems.add(exactItem(itemMap));
          }
        }
        if (!exactItems.isEmpty()) {
          migrated.put("exactItems", exactItems);
        }
        changed = true;
        continue;
      }
      if (("materials".equals(key) || "nqItems".equals(key))
          && entry.getValue() instanceof Map<?, ?> valuesByIndex) {
        migrated.put(key, new ArrayList<>(valuesByIndex.values()));
        changed = true;
        continue;
      }
      final Change child = migrateValue(entry.getValue());
      migrated.put(key, child.value());
      changed |= child.changed();
    }
    return new Change(migrated, changed);
  }

  private static Map<String, Object> portableLocation(final Map<?, ?> source) {
    final Map<String, Object> location = new LinkedHashMap<>();
    location.put("$type", "location");
    copy(source, location, "world");
    copy(source, location, "x");
    copy(source, location, "y");
    copy(source, location, "z");
    location.put("yaw", source.get("yaw") == null ? 0 : source.get("yaw"));
    location.put("pitch", source.get("pitch") == null ? 0 : source.get("pitch"));
    return location;
  }

  private static Map<String, Object> itemSelectionFor(final Map<?, ?> item) {
    final Map<String, Object> selection = new LinkedHashMap<>();
    selection.put("exactItems", List.of(exactItem(item)));
    final Object amount = item.get("amount");
    if (amount instanceof Number number && number.intValue() > 1) {
      selection.put("amount", number.intValue());
    }
    return selection;
  }

  private static Map<String, Object> exactItem(final Map<?, ?> item) {
    final Map<String, Object> exactItem = new LinkedHashMap<>();
    exactItem.put("platform", "paper");
    final Object material = item.get("type");
    if (material != null && !material.toString().isBlank()) {
      exactItem.put("material", material.toString().toLowerCase(Locale.ROOT));
    }
    exactItem.put("data", paperItem(item));
    return exactItem;
  }

  private static Map<String, Object> paperItem(final Map<?, ?> item) {
    final Map<String, Object> encoded = new LinkedHashMap<>();
    encoded.put(PAPER_ITEM_MARKER, PAPER_ITEM_TYPE);
    final Map<String, Object> serialized = copyMap(item);
    serialized.remove(BUKKIT_TYPE);
    encoded.put(PAPER_ITEM_DATA, serialized);
    return encoded;
  }

  private static String npcSelector(final Map<?, ?> npc) {
    final Object type = npc.get("type");
    final Object id = npcId(npc);
    if (type == null || type.toString().isBlank() || id == null || id.toString().isBlank()) {
      return "";
    }
    return type.toString().toLowerCase(Locale.ROOT) + ":" + id;
  }

  private static Change normalizeNpcData(final Map<?, ?> npc) {
    final Map<String, Object> normalized = copyMap(npc);
    if (normalized.containsKey("stringID")
        || normalized.containsKey("integerID")
        || normalized.containsKey("uuidID")
        || !normalized.containsKey("id")) {
      return new Change(normalized, false);
    }
    normalized.put("integerID", normalized.remove("id"));
    return new Change(normalized, true);
  }

  private static Object npcId(final Map<?, ?> npc) {
    if (npc.get("stringID") != null) {
      return npc.get("stringID");
    }
    if (npc.get("integerID") != null) {
      return npc.get("integerID");
    }
    if (npc.get("uuidID") != null) {
      return npc.get("uuidID");
    }
    return npc.get("id");
  }

  private static boolean isReleasedLocation(final Map<?, ?> map) {
    final String type = String.valueOf(map.get(BUKKIT_TYPE));
    return "Location".equals(type) || "org.bukkit.Location".equals(type);
  }

  private static boolean isReleasedItemStack(final Map<?, ?> map) {
    final String type = String.valueOf(map.get(BUKKIT_TYPE));
    return type.equals("ItemStack") || type.endsWith(".ItemStack");
  }

  private static boolean looksLikeReleasedItemSelection(final Map<?, ?> map) {
    return map.containsKey("itemStacks")
        || map.containsKey("materials")
        || map.containsKey("nqItems")
        || map.containsKey("any");
  }

  private static List<?> values(final Object value) {
    if (value instanceof Map<?, ?> map) {
      return new ArrayList<>(map.values());
    }
    if (value instanceof List<?> list) {
      return list;
    }
    return List.of();
  }

  private static Map<String, Object> copyMap(final Map<?, ?> source) {
    final Map<String, Object> copy = new LinkedHashMap<>();
    for (final Map.Entry<?, ?> entry : source.entrySet()) {
      copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
    }
    return copy;
  }

  private static Object copyValue(final Object value) {
    if (value instanceof Map<?, ?> map) {
      return copyMap(map);
    }
    if (value instanceof List<?> list) {
      return list.stream().map(Version630Migration::copyValue).toList();
    }
    return value;
  }

  private static void copy(final Map<?, ?> source, final Map<String, Object> target, final String key) {
    target.put(key, source.get(key));
  }

  private static IllegalStateException failed(
      final Context context,
      final String message,
      final Exception exception) {
    context.warnLogger().accept(message, exception);
    return new IllegalStateException(message, exception);
  }

  public boolean migrateQuestConfig(final YamlConfig configuration) {
    if (configuration == null) {
      return false;
    }
    boolean changed = false;
    final YamlConfig.Section quests = configuration.getConfigurationSection("quests");
    if (quests == null) {
      return false;
    }
    for (final String questName : quests.getKeys(false)) {
      final String questPath = "quests." + questName;
      changed |= migrateQuestLimits(configuration, questPath);
      changed |= migrateObjectivesSection(configuration, questPath + ".objectives");
      changed |= migrateActionsSection(configuration, questPath + ".rewards");
    }
    return changed;
  }

  private boolean migrateQuestLimits(final YamlConfig configuration, final String questPath) {
    boolean changed = false;
    final String oldMaxAccepts = questPath + ".maxAccepts";
    final String maxCompletions = questPath + ".limits.completions";
    if (configuration.contains(oldMaxAccepts)) {
      if (!configuration.contains(maxCompletions)) {
        configuration.set(maxCompletions, configuration.get(oldMaxAccepts));
      }
      configuration.set(oldMaxAccepts, null);
      changed = true;
    }

    final String cooldown = questPath + ".acceptCooldown";
    final Object oldCooldown = configuration.get(cooldown);
    if (oldCooldown instanceof Number) {
      configuration.set(cooldown, null);
      configuration.set(cooldown + ".complete", oldCooldown);
      changed = true;
    }
    return changed;
  }

  public boolean migrateActionsSection(
      final YamlConfig configuration,
      final String sectionPath) {
    if (configuration == null) {
      return false;
    }
    final YamlConfig.Section actions = configuration.getConfigurationSection(sectionPath);
    if (actions == null) {
      return false;
    }

    boolean changed = false;
    for (final String actionId : actions.getKeys(false)) {
      changed |= migrateAction(configuration, sectionPath + "." + actionId);
    }
    return changed;
  }

  private boolean migrateObjectivesSection(final YamlConfig configuration, final String sectionPath) {
    final YamlConfig.Section objectives = configuration.getConfigurationSection(sectionPath);
    if (objectives == null) {
      return false;
    }

    boolean changed = false;
    for (final String objectiveId : objectives.getKeys(false)) {
      changed |= migrateObjective(configuration, sectionPath + "." + objectiveId);
    }
    return changed;
  }

  private boolean migrateObjective(final YamlConfig configuration, final String objectivePath) {
    boolean changed = false;

    String objectiveType = configuration.getString(objectivePath + ".objectiveType", "");
    if ("CollectItems".equals(objectiveType)) {
      objectiveType = "PickupItems";
      configuration.set(objectivePath + ".objectiveType", objectiveType);
      changed = true;
    }

    if (configuration.contains(objectivePath + ".progressNeeded")) {
      configuration.set(
          objectivePath + ".progressNeededExpression",
          String.valueOf(configuration.getInt(objectivePath + ".progressNeeded", 1)));
      configuration.set(objectivePath + ".progressNeeded", null);
      changed = true;
    }

    changed |= switch (objectiveType) {
      case "BreakBlocks" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyMaterial("specifics.blockToBreak.material"));
      case "PlaceBlocks" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyMaterial("specifics.blockToPlace.material"));
      case "PickupItems", "DeliverItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToCollect.itemstack"));
      case "ConsumeItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToConsume.itemstack"));
      case "CraftItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToCraft.itemstack"));
      case "FishItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToFish.itemstack"));
      case "Smelt", "SmeltItems" -> migrateItemSelection(
          configuration,
          objectivePath,
          legacyItemStack("specifics.itemToSmelt.itemstack"));
      default -> false;
    };

    if ("DeliverItems".equals(objectiveType)) {
      changed |= migrateDeliverItemsNpc(configuration, objectivePath);
    }
    if ("TalkToNPC".equals(objectiveType)) {
      changed |= migrateTalkToNpc(configuration, objectivePath);
    }
    if ("PickupItems".equals(objectiveType)
        && configuration.contains(objectivePath + ".specifics.deductIfItemRemovedFromInventory")) {
      configuration.set(objectivePath + ".specifics.deductIfItemRemovedFromInventory", null);
      changed = true;
    }

    // Objective groups may be nested to any depth. Every nested objective used the same 6.3
    // representation as a root objective, so migrate the whole tree instead of only its first
    // level.
    changed |= migrateObjectivesSection(configuration, objectivePath + ".objectives");
    changed |= migrateActionsSection(configuration, objectivePath + ".rewards");
    return changed;
  }

  private boolean migrateAction(final YamlConfig configuration, final String actionPath) {
    boolean changed = false;
    final String actionType = configuration.getString(actionPath + ".actionType", "");
    final String oldActionPath = actionPath + ".specifics.action";
    final String actionsPath = actionPath + ".specifics.actions";
    if (("Action".equals(actionType) || "ActionChain".equals(actionType))
        && configuration.contains(oldActionPath)
        && !configuration.contains(actionsPath)) {
      configuration.set(actionsPath, configuration.get(oldActionPath));
      configuration.set(oldActionPath, null);
      changed = true;
    }
    if ("GiveItem".equals(actionType)) {
      changed |= migrateItemSelection(
          configuration,
          actionPath,
          legacyItemStack("specifics.item"),
          legacyItemStack("specifics.rewardItem"));
    }
    return changed;
  }

  private boolean migrateDeliverItemsNpc(final YamlConfig configuration, final String objectivePath) {
    final String recipientPath = objectivePath + ".specifics.recipientNPC";
    if (configuration.contains(recipientPath + ".type")) {
      return false;
    }

    final String citizensPath = objectivePath + ".specifics.recipientNPCID";
    final String armorStandPath = objectivePath + ".specifics.recipientArmorStandID";
    if (configuration.contains(citizensPath)) {
      configuration.set(recipientPath + ".type", "citizens");
      configuration.set(recipientPath + ".integerID", configuration.getInt(citizensPath));
      configuration.set(citizensPath, null);
      configuration.set(armorStandPath, null);
      return true;
    }
    if (configuration.contains(armorStandPath)) {
      configuration.set(recipientPath + ".type", "armorstand");
      configuration.set(recipientPath + ".uuidID", configuration.getString(armorStandPath));
      configuration.set(armorStandPath, null);
      configuration.set(citizensPath, null);
      return true;
    }
    return false;
  }

  private boolean migrateTalkToNpc(final YamlConfig configuration, final String objectivePath) {
    final String npcPath = objectivePath + ".specifics.npcToTalkTo";
    if (configuration.contains(npcPath + ".type")) {
      return false;
    }

    final String citizensPath = objectivePath + ".specifics.NPCtoTalkID";
    final String armorStandPath = objectivePath + ".specifics.ArmorStandToTalkUUID";
    if (configuration.contains(citizensPath)) {
      configuration.set(npcPath + ".type", "citizens");
      configuration.set(npcPath + ".integerID", configuration.getInt(citizensPath));
      configuration.set(citizensPath, null);
      configuration.set(armorStandPath, null);
      return true;
    }
    if (configuration.contains(armorStandPath)) {
      configuration.set(npcPath + ".type", "armorstand");
      configuration.set(npcPath + ".uuidID", configuration.getString(armorStandPath));
      configuration.set(armorStandPath, null);
      configuration.set(citizensPath, null);
      return true;
    }
    return false;
  }

  private boolean migrateItemSelection(
      final YamlConfig configuration,
      final String basePath,
      final LegacySelectionSource... sources) {
    final String nqItemPath = basePath + ".specifics.nqitem";
    boolean changed = false;
    final boolean hasNqItem = configuration.contains(nqItemPath);
    final String nqItemName = configuration.getString(nqItemPath, "");

    if (hasNqItem && nqItemName != null && !nqItemName.isBlank()) {
      appendSelectionValue(configuration, basePath, "nqItems", nqItemName);
      changed = true;
    }

    if (!hasNqItem || nqItemName == null || nqItemName.isBlank()) {
      for (final LegacySelectionSource source : sources) {
        final String sourcePath = basePath + "." + source.relativePath();
        if (!configuration.contains(sourcePath)) {
          continue;
        }
        final Object value = configuration.get(sourcePath);
        if (value != null) {
          appendSelectionValue(configuration, basePath, source.targetSection(), value);
        }
        changed = true;
      }
    }

    if (hasNqItem) {
      configuration.set(nqItemPath, null);
      changed = true;
    }
    for (final LegacySelectionSource source : sources) {
      final String sourcePath = basePath + "." + source.relativePath();
      if (configuration.contains(sourcePath)) {
        configuration.set(sourcePath, null);
        changed = true;
      }
    }

    if (changed && !configuration.contains(basePath + "." + ITEM_SELECTION_PATH + ".any")) {
      configuration.set(basePath + "." + ITEM_SELECTION_PATH + ".any", false);
    }
    return changed;
  }

  private void appendSelectionValue(
      final YamlConfig configuration,
      final String basePath,
      final String targetSection,
      final Object value) {
    final String sectionPath = basePath + "." + ITEM_SELECTION_PATH + "." + targetSection;
    configuration.set(sectionPath + "." + nextNumericKey(configuration, sectionPath), value);
  }

  private int nextNumericKey(final YamlConfig configuration, final String sectionPath) {
    final YamlConfig.Section section = configuration.getConfigurationSection(sectionPath);
    if (section == null) {
      return 1;
    }
    int highest = 0;
    for (final String key : section.getKeys(false)) {
      try {
        highest = Math.max(highest, Integer.parseInt(key));
      } catch (final NumberFormatException ignored) {
        // Non-numeric custom keys are left alone; migrated entries continue after numeric keys.
      }
    }
    return highest + 1;
  }

  private LegacySelectionSource legacyMaterial(final String relativePath) {
    return new LegacySelectionSource(relativePath, "materials");
  }

  private LegacySelectionSource legacyItemStack(final String relativePath) {
    return new LegacySelectionSource(relativePath, "itemStacks");
  }

  private record LegacySelectionSource(String relativePath, String targetSection) {}

  private record Change(Object value, boolean changed) {}
}
