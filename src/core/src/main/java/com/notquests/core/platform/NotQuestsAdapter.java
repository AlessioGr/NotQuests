package com.notquests.core.platform;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItem;
import com.notquests.core.items.SavedItems;
import com.notquests.core.managers.LanguageManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Public NotQuests adapter used by built-in registry entries and external registry packs.
 *
 * <p>Registry packs use this interface instead of talking to Paper/Bukkit directly. Platform
 * modules own the implementation and translate these portable registry entries into real commands,
 * config codecs, events, and runtime behavior.
 */
public interface NotQuestsAdapter {
    FieldFactories fields();

    Actions.Registry actions();

    Conditions.Registry conditions();

    Objectives.Registry objectives();

    Triggers.Registry triggers();

    Variables.Registry variables();

    String serverBrand();

    List<String> damageTypeIds();

    List<String> onlinePlayerNames();

    PlatformPlayer onlineQuestPlayer(String playerName);

    List<String> worldNames();

    boolean supportsWorldEditSelection();

    LocationRegion worldEditSelection(PlatformPlayer questPlayer);

    List<String> itemSelectionOptions();

    boolean itemMaterialExists(String materialId);

    boolean itemSelectionsAreSimilar(ItemSelection required, ItemSelection actual);

    List<String> entityTypeIds();

    List<String> particleTypeIds();

    List<String> soundTypeIds();

    List<String> soundCategoryIds();

    List<String> statisticIds();

    List<String> advancementIds();

    List<String> blockMaterialOptions();

    List<String> inventorySlotIds();

    List<String> enchantmentIds();

    boolean hasPermission(PlatformPlayer questPlayer, String permission);

    boolean supportsArbitraryPermissionChecks();

    boolean supportsPermissionMutation();

    boolean setPermission(PlatformPlayer questPlayer, String permission, boolean value);

    int playerStatistic(PlatformPlayer questPlayer, String statisticId);

    boolean setPlayerStatistic(PlatformPlayer questPlayer, String statisticId, int value);

    boolean hasAdvancement(PlatformPlayer questPlayer, String advancementId);

    boolean setAdvancement(PlatformPlayer questPlayer, String advancementId, boolean completed);

    String blockMaterial(NQLocation location);

    boolean setBlockMaterial(PlatformPlayer questPlayer, NQLocation location, String materialOrKeyword);

    List<ItemSelection> containerInventoryItems(NQLocation location);

    boolean addContainerInventoryItems(
            NQLocation location,
            List<SavedItems.ItemChoice> items,
            boolean dropOverflow);

    boolean removeContainerInventoryItems(NQLocation location, List<SavedItems.ItemChoice> items);

    boolean setContainerInventoryItems(NQLocation location, List<SavedItems.ItemChoice> items);

    List<String> inventoryItemEnchantments(PlatformPlayer questPlayer, String slotId);

    boolean supportsNpcAttachments();

    boolean supportsArmorStandAttachmentTools();

    List<String> npcSelectorOptions(boolean allowNone, boolean allowRightClickSelect);

    NpcSelection npcSelection(String npcSelector);

    List<String> nativeNpcQuestGiverTypes();

    boolean setNpcQuestGiver(NpcSelection selection, boolean enabled);

    boolean giveArmorStandTool(PlatformPlayer actor, ArmorStandToolItem tool);

    boolean giveNpcSelectionTool(
            PlatformPlayer actor,
            int selectionId,
            String displayName,
            List<String> lore);

    List<String> variableNames(VariableDataType type);

    VariableDataType variableType(String variableName);

    String variableSingular(String variableName);

    String variablePlural(String variableName);

    List<RegistryField.Definition> variableFields(String variableName);

    Object variableValue(String variableName, PlatformPlayer questPlayer, Object... objects);

    Object variableValue(
            String variableName,
            PlatformPlayer questPlayer,
            Map<String, String> stringArguments,
            Map<String, ?> numberArguments,
            Map<String, ?> booleanArguments);

    void warn(String message);

    void logInfo(String message);

    void broadcast(String miniMessage);

    void dispatchConsoleCommand(String command);

    void schedule(Duration delay, Runnable action);

    boolean isServerThread();

    <T> T callOnServerThread(Callable<T> action) throws Exception;

    boolean allowObjectiveUnlock(
            PlatformPlayer questPlayer,
            Quest quest,
            ActiveObjective objective,
            boolean triggerAcceptQuestTrigger);

    ItemSelection parseItemSelection(String input);

    ItemSelection parseItemSelection(Object input, PlatformPlayer questPlayer);

    NQLocation location(String worldName, double x, double y, double z);

    /**
     * Core-owned implementation of portable registry and suggestion behavior.
     *
     * <p>Concrete platform adapters implement only the raw native lists and effects. This keeps
     * saved-item keywords, variable lookup, translations, and registry access out of Paper and
     * NeoForge while retaining one explicit adapter contract for true platform capabilities.
     */
    abstract class Platform implements NotQuestsAdapter {
        private final NotQuestsPlugin plugin;
        private final NotQuestsAdapter registry;

        protected Platform(final NotQuestsPlugin plugin) {
            this.plugin = Objects.requireNonNull(plugin, "plugin");
            registry = plugin.createRegistryAdapter(
                    new NotQuestsRegistry.PlatformHooks(
                            plugin::warn,
                            this::broadcast,
                            this::dispatchConsoleCommand,
                            this::worldNames));
        }

        protected final NotQuestsPlugin plugin() {
            return plugin;
        }

        protected abstract List<String> itemMaterialIds();

        protected abstract boolean nativeItemSelectionsAreSimilar(
                ItemSelection required,
                ItemSelection actual);

        protected abstract ItemSelection heldItemSelection(PlatformPlayer questPlayer);

        protected abstract String nativeItemMaterialId(String input);

        protected abstract List<String> nativeEntityTypeIds();

        protected abstract List<String> blockMaterialIds();

        protected abstract List<String> nativeNpcSelectorIds();

        protected abstract NativeNpc nativeNpc(String npcType, NQNPCID npcId);

        protected abstract String heldBlockMaterial(PlatformPlayer questPlayer);

        protected abstract String itemSelectionBlockMaterial(List<SavedItems.ItemChoice> items);

        protected abstract boolean setNativeBlockMaterial(NQLocation location, String materialId);

        @Override
        public final FieldFactories fields() {
            return registry.fields();
        }

        @Override
        public final Actions.Registry actions() {
            return registry.actions();
        }

        @Override
        public final Conditions.Registry conditions() {
            return registry.conditions();
        }

        @Override
        public final Objectives.Registry objectives() {
            return registry.objectives();
        }

        @Override
        public final Triggers.Registry triggers() {
            return registry.triggers();
        }

        @Override
        public final Variables.Registry variables() {
            return registry.variables();
        }

        @Override
        public final List<String> itemSelectionOptions() {
            final ArrayList<String> options = new ArrayList<>(itemMaterialIds());
            options.addAll(plugin.savedItemNames());
            options.add("hand");
            options.add("any");
            return List.copyOf(options);
        }

        @Override
        public final boolean itemSelectionsAreSimilar(
                final ItemSelection required,
                final ItemSelection actual) {
            for (final SavedItems.ItemChoice requiredChoice : plugin.resolveItems(required)) {
                for (final SavedItems.ItemChoice actualChoice : plugin.resolveItems(actual)) {
                    if (ItemStackSelection.areSimilar(
                            requiredChoice.selection(), actualChoice.selection())
                            || nativeItemSelectionsAreSimilar(
                                    requiredChoice.selection(), actualChoice.selection())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public final boolean itemMaterialExists(final String materialId) {
            if (materialId == null || materialId.isBlank()) {
                return false;
            }
            final String expected = materialId.toLowerCase(Locale.ROOT);
            final String expectedWithoutNamespace = expected.startsWith("minecraft:")
                    ? expected.substring("minecraft:".length())
                    : expected;
            return itemMaterialIds().stream()
                    .filter(Objects::nonNull)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(value -> value.equals(expected)
                            || value.equals(expectedWithoutNamespace)
                            || value.equals("minecraft:" + expectedWithoutNamespace));
        }

        @Override
        public final ItemSelection parseItemSelection(final String input) {
            return parseItemSelection(input, null);
        }

        @Override
        public final ItemSelection parseItemSelection(
                final Object input,
                final PlatformPlayer questPlayer) {
            return SavedItems.parse(input, plugin::savedItem, new SavedItems.NativeItems() {
                @Override
                public ItemSelection itemInHand() {
                    return heldItemSelection(questPlayer);
                }

                @Override
                public String materialId(final String value) {
                    return nativeItemMaterialId(value);
                }
            });
        }

        @Override
        public final List<String> entityTypeIds() {
            final ArrayList<String> options = new ArrayList<>(nativeEntityTypeIds());
            options.add("any");
            return List.copyOf(options);
        }

        @Override
        public final List<String> blockMaterialOptions() {
            final ArrayList<String> options = new ArrayList<>(blockMaterialIds());
            options.addAll(plugin.savedItemNames());
            options.add("hand");
            options.add("any");
            return List.copyOf(options);
        }

        @Override
        public final boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final NQLocation location,
                final String materialOrKeyword) {
            if (location == null || materialOrKeyword == null || materialOrKeyword.isBlank()) {
                return false;
            }
            String material = materialOrKeyword;
            if (materialOrKeyword.equalsIgnoreCase("hand")) {
                material = heldBlockMaterial(questPlayer);
            } else if (materialOrKeyword.equalsIgnoreCase("any")) {
                final List<String> blocks = blockMaterialIds();
                material = blocks.isEmpty()
                        ? null
                        : blocks.get(ThreadLocalRandom.current().nextInt(blocks.size()));
            } else {
                final SavedItem savedItem = plugin.savedItem(materialOrKeyword);
                if (savedItem != null) {
                    material = itemSelectionBlockMaterial(plugin.resolveItems(savedItem.getItemSelection()));
                }
            }
            return material != null
                    && !material.isBlank()
                    && setNativeBlockMaterial(location, material);
        }

        @Override
        public final List<String> npcSelectorOptions(
                final boolean allowNone,
                final boolean allowRightClickSelect) {
            final ArrayList<String> options = new ArrayList<>();
            if (allowRightClickSelect) {
                options.add("rightClickSelect");
            }
            if (allowNone) {
                options.add("none");
            }
            options.addAll(nativeNpcSelectorIds());
            return List.copyOf(options);
        }

        @Override
        public final NpcSelection npcSelection(final String selector) {
            final NpcSelection armorStand = plugin.armorStandNpcSelection(selector);
            if (armorStand != null) {
                return armorStand;
            }
            final NpcAttachments.Selector parsed = NpcAttachments.Selector.parse(selector).orElse(null);
            if (parsed == null) {
                return null;
            }
            final NativeNpc npc = nativeNpc(parsed.type(), parsed.id());
            if (npc == null || npc.npcId() == null || npc.npcType().isBlank()) {
                return null;
            }
            return new NpcSelection(
                    NpcAttachments.selector(npc.npcType(), npc.npcId()),
                    NpcAttachments.formatAttachedNPC(npc.npcType(), npc.npcId(), npc.npcName()),
                    npc.npcType(),
                    npc.npcId(),
                    npc.npcName());
        }

        @Override
        public final List<String> variableNames(final VariableDataType type) {
            return registry.variableNames(type);
        }

        @Override
        public final VariableDataType variableType(final String variableName) {
            return registry.variableType(variableName);
        }

        @Override
        public final String variableSingular(final String variableName) {
            return registry.variableSingular(variableName);
        }

        @Override
        public final String variablePlural(final String variableName) {
            return registry.variablePlural(variableName);
        }

        @Override
        public final List<RegistryField.Definition> variableFields(final String variableName) {
            return registry.variableFields(variableName);
        }

        @Override
        public final Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            return registry.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public final Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Map<String, String> stringArguments,
                final Map<String, ?> numberArguments,
                final Map<String, ?> booleanArguments) {
            return registry.variableValue(
                    variableName,
                    questPlayer,
                    stringArguments,
                    numberArguments,
                    booleanArguments);
        }

        @Override
        public final void warn(final String message) {
            plugin.warn(message);
        }

        @Override
        public final void logInfo(final String message) {
            plugin.info(message);
        }

        @Override
        public final String resolveActionText(
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return plugin.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public final String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final ActiveObjective activeObjective,
                final Map<String, String> replacements) {
            return plugin.translate(
                    questPlayer,
                    translationKey,
                    replacements,
                    LanguageManager.missingString(translationKey));
        }
    }

    record ArmorStandToolItem(
            String materialId,
            int itemId,
            String questName,
            String displayName,
            List<String> loreLines) {
        public ArmorStandToolItem {
            materialId = materialId == null ? "" : materialId;
            questName = questName == null ? "" : questName;
            displayName = displayName == null ? "" : displayName;
            loreLines = loreLines == null ? List.of() : List.copyOf(loreLines);
        }
    }

    record NpcSelection(String selector, String label, String npcType, NQNPCID npcId, String npcName) {}

    /** Raw native NPC facts. Core owns selector and user-facing label formatting. */
    record NativeNpc(String npcType, NQNPCID npcId, String npcName) {
        public NativeNpc {
            npcType = npcType == null ? "" : npcType;
            npcName = npcName == null ? "" : npcName;
        }
    }

    /** A platform integration detected at runtime. Core owns how this fact is presented. */
    record Integration(String name, String version) {
        public Integration {
            name = name == null ? "" : name;
            version = version == null ? "" : version;
        }
    }

    String resolveActionText(Actions.Data action, PlatformPlayer questPlayer, String text, Object... objects);

    String objectiveTaskText(
            String translationKey,
            PlatformPlayer questPlayer,
            ActiveObjective activeObjective,
            Map<String, String> replacements);

}
