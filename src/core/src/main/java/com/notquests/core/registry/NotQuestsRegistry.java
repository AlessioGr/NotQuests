package com.notquests.core.registry;

import com.notquests.core.actions.Action;
import com.notquests.core.commands.framework.NQCommandContext;
import com.notquests.core.commands.framework.NQDescription;
import com.notquests.core.commands.framework.NQSuggestionProvider;
import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.managers.UtilManager;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;
import com.notquests.core.variables.NumberExpression;
import com.notquests.core.variables.VariableDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Owns portable type registration, parsing, callbacks, and generated command metadata. */
public final class NotQuestsRegistry {
    public interface Pack<Platform> {
        void register(Platform platform);

        default void variablesChanged(final Platform platform) {}

        default void variableValueChanged(
                final Platform platform, final String variableName, final PlatformPlayer questPlayer) {}
    }

    public static final class Packs<Platform> {
        private final List<Pack<Platform>> packs = new ArrayList<>();

        public void add(final Pack<Platform> pack) {
            packs.add(pack);
        }

        public List<Pack<Platform>> installed() {
            return List.copyOf(packs);
        }

        public void registerAll(final Platform platform) {
            for (final Pack<Platform> pack : packs) {
                pack.register(platform);
            }
        }

        public void refreshAfterVariableChange(final Platform platform) {
            for (final Pack<Platform> pack : packs) {
                pack.variablesChanged(platform);
            }
        }

        public void notifyAfterVariableValueChange(
                final Platform platform,
                final String variableName,
                final PlatformPlayer questPlayer) {
            for (final Pack<Platform> pack : packs) {
                pack.variableValueChanged(platform, variableName, questPlayer);
            }
        }
    }

    public static List<String> summary(final NotQuestsRegistry registry) {
        return List.of(
                "NotQuests registry:",
                "- Objectives: " + registry.objectives().size(),
                "- Actions: " + registry.actions().size(),
                "- Conditions: " + registry.conditions().size(),
                "- Triggers: " + registry.triggers().size(),
                "- Variables: " + registry.variables().size());
    }

    public static String objectiveList(final NotQuestsRegistry registry) {
        return "<highlight>All objective types:\n" + registry.objectives().stream()
                .map(Objectives.Type::id)
                .sorted()
                .map(id -> "<main>" + id)
                .collect(Collectors.joining("\n"));
    }

    public static String actionList(final NotQuestsRegistry registry) {
        return "<highlight>All reward types:\n" + registry.actions().stream()
                .map(Actions.Type::id)
                .sorted()
                .map(id -> "<main>" + id)
                .collect(Collectors.joining("\n"));
    }

    public static String conditionList(final NotQuestsRegistry registry) {
        return "<highlight>All requirement types:\n" + registry.conditions().stream()
                .map(Conditions.Type::id)
                .sorted()
                .map(id -> "<main>" + id)
                .collect(Collectors.joining("\n"));
    }

    public static String variableList(final NotQuestsRegistry registry) {
        return "<highlight>All variable types:\n" + registry.variables().stream()
                .map(Variables.Type::id)
                .sorted()
                .map(id -> "<main>" + id)
                .collect(Collectors.joining("\n"));
    }

    public static String triggerList(final NotQuestsRegistry registry) {
        return "<highlight>All trigger types:\n" + registry.triggers().stream()
                .sorted(Comparator.comparing(Triggers.Type::id))
                .map(Triggers.Type::id)
                .map(id -> "<main>" + id)
                .collect(Collectors.joining("\n"));
    }

    private final List<Actions.Type> actionTypes = new ArrayList<>();
    private final List<Conditions.Type> conditionTypes = new ArrayList<>();
    private final List<Objectives.Type> objectiveTypes = new ArrayList<>();
    private final List<Triggers.Type> triggerTypes = new ArrayList<>();
    private final List<Variables.Type> variableTypes = new ArrayList<>();

    public NotQuestsAdapter createAdapter(final PlatformHooks hooks) {
        return new CoreRegistryAdapter(this, hooks);
    }

    public void parseVariableAction(
            final PlatformHooks hooks,
            final Actions.Draft action,
            final List<String> arguments,
            final VariableDataType type) {
        new CoreRegistryAdapter(this, hooks).parseVariableAction(action, arguments, type);
    }

    public void executeVariableAction(
            final PlatformHooks hooks,
            final VariableDataType type,
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final Object... objects) {
        new CoreRegistryAdapter(this, hooks).executeVariableAction(type, action, questPlayer, objects);
    }

    public String variableActionDescription(
            final PlatformHooks hooks,
            final VariableDataType type,
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final Object... objects) {
        return new CoreRegistryAdapter(this, hooks).variableActionDescription(type, action, questPlayer, objects);
    }

    public List<Actions.Type> actions() {
        return List.copyOf(actionTypes);
    }

    public List<Conditions.Type> conditions() {
        return List.copyOf(conditionTypes);
    }

    public List<Objectives.Type> objectives() {
        return List.copyOf(objectiveTypes);
    }

    public List<Triggers.Type> triggers() {
        return List.copyOf(triggerTypes);
    }

    public List<Variables.Type> variables() {
        return List.copyOf(variableTypes);
    }

    public void registerActionType(final Actions.Type type) {
        actionTypes.removeIf(existing -> existing.id().equalsIgnoreCase(type.id()));
        actionTypes.add(type);
    }

    public void registerConditionType(final Conditions.Type type) {
        conditionTypes.removeIf(existing -> existing.id().equalsIgnoreCase(type.id()));
        conditionTypes.add(type);
    }

    public void registerObjectiveType(final Objectives.Type type) {
        objectiveTypes.removeIf(existing -> existing.id().equalsIgnoreCase(type.id()));
        objectiveTypes.add(type);
    }

    public void registerTriggerType(final Triggers.Type type) {
        triggerTypes.removeIf(existing -> existing.id().equalsIgnoreCase(type.id()));
        triggerTypes.add(type);
    }

    public void registerVariableType(final Variables.Type type) {
        variableTypes.removeIf(existing -> existing.id().equalsIgnoreCase(type.id()));
        variableTypes.add(type);
    }

    public record PlatformHooks(
            Consumer<String> warn,
            Consumer<String> broadcast,
            Consumer<String> consoleCommand,
            Supplier<List<String>> worldNames) {
        public PlatformHooks(
                final Consumer<String> warn,
                final Consumer<String> broadcast,
                final Consumer<String> consoleCommand) {
            this(warn, broadcast, consoleCommand, List::of);
        }

        public PlatformHooks {
            warn = warn == null ? ignored -> {} : warn;
            broadcast = broadcast == null ? ignored -> {} : broadcast;
            consoleCommand = consoleCommand == null ? ignored -> {} : consoleCommand;
            worldNames = worldNames == null ? List::of : worldNames;
        }
    }

    private static final class CoreRegistryAdapter implements NotQuestsAdapter {
        private final NotQuestsRegistry registry;
        private final PlatformHooks hooks;
        private final FieldFactories fields;

        private CoreRegistryAdapter(final NotQuestsRegistry registry, final PlatformHooks hooks) {
            this.registry = registry;
            this.hooks = Objects.requireNonNull(hooks, "hooks");
            this.fields = new RegistryFieldFactories(this::worldNames);
        }

        @Override
        public FieldFactories fields() {
            return fields;
        }

        @Override
        public Actions.Registry actions() {
            return id -> new ActionTypeBuilder(registry, id);
        }

        @Override
        public Conditions.Registry conditions() {
            return id -> new ConditionTypeBuilder(registry, id);
        }

        @Override
        public Objectives.Registry objectives() {
            return id -> new ObjectiveTypeBuilder(registry, id);
        }

        @Override
        public Triggers.Registry triggers() {
            return id -> new TriggerTypeBuilder(registry, id);
        }

        @Override
        public Variables.Registry variables() {
            return new VariableRegistration(registry);
        }

        @Override
        public String serverBrand() {
            return "Core";
        }

        @Override
        public List<String> damageTypeIds() {
            return List.of();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of();
        }

        @Override
        public PlatformPlayer onlineQuestPlayer(final String playerName) {
            return null;
        }

        @Override
        public List<String> worldNames() {
            final List<String> names = hooks.worldNames().get();
            return names == null ? List.of() : List.copyOf(names);
        }

        @Override
        public boolean supportsWorldEditSelection() {
            return false;
        }

        @Override
        public LocationRegion worldEditSelection(
                final PlatformPlayer questPlayer) {
            return null;
        }

        @Override
        public List<String> itemSelectionOptions() {
            return List.of("hand", "any");
        }

        @Override
        public boolean itemMaterialExists(final String materialId) {
            return false;
        }

        @Override
        public boolean itemSelectionsAreSimilar(
                final ItemSelection required,
                final ItemSelection actual) {
            return ItemStackSelection.areSimilar(required, actual);
        }

        @Override
        public List<String> entityTypeIds() {
            return List.of();
        }

        @Override
        public List<String> particleTypeIds() {
            return List.of();
        }

        @Override
        public List<String> soundTypeIds() {
            return List.of();
        }

        @Override
        public List<String> soundCategoryIds() {
            return List.of();
        }

        @Override
        public List<String> statisticIds() {
            return List.of();
        }

        @Override
        public List<String> advancementIds() {
            return List.of();
        }

        @Override
        public List<String> blockMaterialOptions() {
            return List.of();
        }

        @Override
        public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
            return 0;
        }

        @Override
        public boolean setPlayerStatistic(
                final PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            return false;
        }

        @Override
        public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
            return false;
        }

        @Override
        public boolean setAdvancement(
                final PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            return false;
        }

        @Override
        public String blockMaterial(final NQLocation location) {
            return "";
        }

        @Override
        public boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final NQLocation location,
                final String materialOrKeyword) {
            return false;
        }

        @Override
        public List<ItemSelection> containerInventoryItems(final NQLocation location) {
            return List.of();
        }

        @Override
        public boolean addContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            return false;
        }

        @Override
        public boolean removeContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return false;
        }

        @Override
        public boolean setContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return false;
        }

        @Override
        public List<String> enchantmentIds() {
            return List.of();
        }

        @Override
        public void warn(final String message) {
            hooks.warn().accept(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            hooks.broadcast().accept(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            hooks.consoleCommand().accept(command);
        }

        @Override
        public boolean supportsNpcAttachments() {
            return false;
        }

        @Override
        public List<String> nativeNpcQuestGiverTypes() {
            return List.of();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return false;
        }

        @Override
        public List<String> npcSelectorOptions(
                final boolean allowNone,
                final boolean allowRightClickSelect) {
            final ArrayList<String> options = new ArrayList<>();
            if (allowRightClickSelect) {
                options.add("rightClickSelect");
            }
            if (allowNone) {
                options.add("none");
            }
            return List.copyOf(options);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return null;
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return false;
        }

        @Override
        public boolean giveArmorStandTool(
                final PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return false;
        }

        @Override
        public boolean giveNpcSelectionTool(
                final PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            return false;
        }

        @Override
        public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
            return false;
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
        public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
            return List.of();
        }

        @Override
        public List<String> inventorySlotIds() {
            final ArrayList<String> slots = new ArrayList<>();
            slots.addAll(List.of("HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET"));
            for (int slot = 0; slot <= 35; slot++) {
                slots.add(String.valueOf(slot));
            }
            return List.copyOf(slots);
        }

        @Override
        public void schedule(final Duration delay, final Runnable action) {
            action.run();
        }

        @Override
        public boolean isServerThread() {
            return true;
        }

        @Override
        public <T> T callOnServerThread(final Callable<T> action) throws Exception {
            return action.call();
        }

        @Override
        public void logInfo(final String message) {
            hooks.warn().accept(message);
        }

        @Override
        public ItemSelection parseItemSelection(final String input) {
            return ItemStackSelection.parse(input);
        }

        @Override
        public ItemSelection parseItemSelection(final Object input, final PlatformPlayer questPlayer) {
            return parseItemSelection(input == null ? "" : String.valueOf(input));
        }

        @Override
        public NQLocation location(final String worldName, final double x, final double y, final double z) {
            return NQLocation.at(worldName, x, y, z);
        }

        @Override
        public List<String> variableNames(final VariableDataType type) {
            return registry.variables().stream()
                    .filter(variable -> type == null || variableType(variable) == type)
                    .map(Variables.Type::id)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        @Override
        public VariableDataType variableType(final String variableName) {
            final Variables.Type variable = variable(variableName);
            return variable == null ? null : variableType(variable);
        }

        @Override
        public String variableSingular(final String variableName) {
            final Variables.Type variable = variable(variableName);
            return variable == null ? cleanVariableName(variableName) : variable.singular();
        }

        @Override
        public String variablePlural(final String variableName) {
            final Variables.Type variable = variable(variableName);
            return variable == null ? cleanVariableName(variableName) : variable.plural();
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            final Variables.Type variable = variable(variableName);
            return variable == null ? List.of() : variable.fields();
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            final Variables.Type variable = variable(variableName);
            if (variable == null) {
                return null;
            }
            return switch (variableType(variable)) {
                case NUMBER -> ((Variables.NumberVariableHandler) variable.handler()).getValue(questPlayer, objects);
                case STRING -> ((Variables.StringVariableHandler) variable.handler()).getValue(questPlayer, objects);
                case BOOLEAN -> ((Variables.BooleanVariableHandler) variable.handler()).getValue(questPlayer, objects);
                case LIST -> ((Variables.ListVariableHandler) variable.handler()).getValue(questPlayer, objects);
                case ITEMSTACKLIST -> ((Variables.ItemStackListVariableHandler) variable.handler()).getValue(questPlayer, objects);
            };
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Map<String, String> stringArguments,
                final Map<String, ?> numberArguments,
                final Map<String, ?> booleanArguments) {
            final ArrayList<Object> values = new ArrayList<>();
            for (final RegistryField.Definition field : variableFields(variableName)) {
                if (numberArguments != null && numberArguments.containsKey(field.name())) {
                    values.add(numberExpressionValue(numberArguments.get(field.name()), questPlayer));
                } else if (booleanArguments != null && booleanArguments.containsKey(field.name())) {
                    values.add(booleanExpressionValue(booleanArguments.get(field.name()), questPlayer));
                } else if (stringArguments != null && stringArguments.containsKey(field.name())) {
                    values.add(stringArguments.get(field.name()));
                } else {
                    values.add("");
                }
            }
            return variableValue(variableName, questPlayer, values.toArray());
        }

        @Override
        public boolean allowObjectiveUnlock(
                final PlatformPlayer questPlayer,
                final Quest quest,
                final ActiveObjective objective,
                final boolean triggerAcceptQuestTrigger) {
            return true;
        }

        public void parseVariableAction(
                final Actions.Draft action,
                final List<String> arguments,
                final VariableDataType type) {
            if (arguments.size() < 3) {
                warn("Cannot parse " + type + " variable action: expected variable name, operator and value.");
                return;
            }
            int index = 0;
            final String variableName = arguments.get(index++);
            final Variables.Type variable = variable(variableName);
            if (variable == null) {
                warn("Cannot parse " + type + " variable action: unknown variable " + variableName + ".");
                return;
            }
            if (variableType(variable) != type) {
                warn("Cannot parse " + type + " variable action: " + variableName + " is a "
                        + variable.valueType() + " variable.");
                return;
            }

            action.setValue("variableName", variableName);
            final Map<String, String> stringArguments = new LinkedHashMap<>();
            final Map<String, String> numberArguments = new LinkedHashMap<>();
            final Map<String, String> booleanArguments = new LinkedHashMap<>();
            for (final RegistryField.Definition field : variable.fields()) {
                if (isVariableFlag(field)) {
                    booleanArguments.put(field.name(), "0");
                    continue;
                }
                if (index >= arguments.size() || arguments.get(index).startsWith("--")) {
                    warn("Cannot parse " + type + " variable action: missing value for variable field "
                            + field.name() + ".");
                    return;
                }
                final String value = arguments.get(index++);
                if (isNumberLike(field)) {
                    numberArguments.put(field.name(), value);
                } else if (isBooleanLike(field)) {
                    booleanArguments.put(field.name(), value);
                } else {
                    stringArguments.put(field.name(), value);
                }
            }

            if (index >= arguments.size()) {
                warn("Cannot parse " + type + " variable action: expected operator.");
                return;
            }
            final String operator = arguments.get(index++);
            action.setValue("operator", operator);
            if (type == VariableDataType.ITEMSTACKLIST) {
                booleanArguments.put(operator, "1");
            }
            if (type == VariableDataType.ITEMSTACKLIST) {
                if (index >= arguments.size()) {
                    warn("Cannot parse item-stack-list variable action: expected item.");
                    return;
                }
                final ItemSelection itemSelection = parseItemSelection(arguments.get(index++));
                if (index >= arguments.size() || arguments.get(index).startsWith("--")) {
                    warn("Cannot parse item-stack-list variable action: expected amount.");
                    return;
                }
                action.setValue("itemStack", itemSelection.withAmount(parseInteger(arguments.get(index++), 1)));
            } else {
                if (index >= arguments.size()) {
                    warn("Cannot parse " + type + " variable action: expected expression.");
                    return;
                }
                action.setValue("expression", arguments.get(index++));
            }

            while (index < arguments.size()) {
                final String token = arguments.get(index++);
                if (!token.startsWith("--")) {
                    continue;
                }
                final RegistryField.Definition flag = namedField(variable.fields(), token.substring(2));
                if (flag != null && isVariableFlag(flag)) {
                    booleanArguments.put(flag.name(), "1");
                }
            }
            action.setValue("additionalStrings", stringArguments);
            action.setValue("additionalNumbers", numberArguments);
            action.setValue("additionalBooleans", booleanArguments);
        }

        public void executeVariableAction(
                final VariableDataType type,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            final Variables.Type variable = variable(action.text("variableName"));
            if (variable == null) {
                warn("Cannot execute variable action: unknown variable " + action.text("variableName") + ".");
                return;
            }
            final Object[] arguments = combined(variableArguments(variable, action, questPlayer), objects);
            switch (type) {
                case NUMBER -> executeNumberVariableAction(variable, action, questPlayer, arguments);
                case STRING -> executeStringVariableAction(variable, action, questPlayer, arguments);
                case BOOLEAN -> executeBooleanVariableAction(variable, action, questPlayer, arguments);
                case LIST -> executeListVariableAction(variable, action, questPlayer, arguments);
                case ITEMSTACKLIST -> executeItemStackListVariableAction(variable, action, questPlayer, arguments);
            }
        }

        public String variableActionDescription(
                final VariableDataType type,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            final String expression = switch (type) {
                case NUMBER -> String.valueOf(new NumberExpression(this, action.text("expression"))
                        .calculateValue(questPlayer));
                case BOOLEAN -> String.valueOf(new NumberExpression(this, action.text("expression"))
                        .calculateBooleanValue(questPlayer));
                default -> action.text("expression");
            };
            return action.text("variableName") + ": " + action.text("operator") + " " + expression;
        }

        @Override
        public String resolveActionText(
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return text;
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final ActiveObjective activeObjective,
                final Map<String, String> replacements) {
            String text = DEFAULT_OBJECTIVE_TASK_TEXT.getOrDefault(translationKey, translationKey);
            if (replacements != null) {
                for (final Map.Entry<String, String> entry : replacements.entrySet()) {
                    text = text.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
                }
            }
            return text;
        }

        private static RegistryField.Definition namedField(final List<RegistryField.Definition> fields, final String name) {
            for (final RegistryField.Definition field : fields) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            return null;
        }

        private static boolean isNumberLike(final RegistryField.Definition field) {
            return field.valueType().toLowerCase(Locale.ROOT).contains("number")
                    || field.valueType().toLowerCase(Locale.ROOT).contains("integer");
        }

        private static boolean isBooleanLike(final RegistryField.Definition field) {
            return field.valueType().toLowerCase(Locale.ROOT).contains("boolean")
                    || field.presenceFlag();
        }

        private static boolean isVariableFlag(final RegistryField.Definition field) {
            return field.flag() || field.presenceFlag();
        }

        private static int parseInteger(final String value, final int fallback) {
            try {
                return Integer.parseInt(value);
            } catch (final NumberFormatException ignored) {
                return fallback;
            }
        }

        private Object[] variableArguments(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer) {
            final Map<String, String> stringArguments = stringMap(action.value("additionalStrings"));
            final Map<String, ?> numberArguments = objectMap(action.value("additionalNumbers"));
            final Map<String, ?> booleanArguments = objectMap(action.value("additionalBooleans"));
            final List<Object> values = new ArrayList<>();
            for (final RegistryField.Definition field : variable.fields()) {
                if (numberArguments.containsKey(field.name())) {
                    values.add(numberExpressionValue(numberArguments.get(field.name()), questPlayer));
                } else if (booleanArguments.containsKey(field.name())) {
                    values.add(booleanExpressionValue(booleanArguments.get(field.name()), questPlayer));
                } else if (stringArguments.containsKey(field.name())) {
                    values.add(stringArguments.get(field.name()));
                } else {
                    values.add("");
                }
            }
            return values.toArray();
        }

        private static Object[] combined(final Object[] first, final Object[] second) {
            if (second == null || second.length == 0) {
                return first;
            }
            final Object[] combined = new Object[first.length + second.length];
            System.arraycopy(first, 0, combined, 0, first.length);
            System.arraycopy(second, 0, combined, first.length, second.length);
            return combined;
        }

        private static Map<String, String> stringMap(final Object value) {
            if (!(value instanceof final Map<?, ?> map)) {
                return Map.of();
            }
            final Map<String, String> strings = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    strings.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            return strings;
        }

        private static Map<String, ?> objectMap(final Object value) {
            if (!(value instanceof final Map<?, ?> map)) {
                return Map.of();
            }
            final Map<String, Object> values = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    values.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return values;
        }

        private Variables.Type variable(final String id) {
            for (final Variables.Type variable : registry.variables()) {
                if (variable.id().equalsIgnoreCase(id)) {
                    return variable;
                }
            }
            return null;
        }

        private static String cleanVariableName(final String variableName) {
            return variableName == null ? "" : variableName;
        }

        private static VariableDataType variableType(final Variables.Type variable) {
            return switch (variable.valueType()) {
                case "number" -> VariableDataType.NUMBER;
                case "string" -> VariableDataType.STRING;
                case "boolean" -> VariableDataType.BOOLEAN;
                case "list" -> VariableDataType.LIST;
                case "itemStackList" -> VariableDataType.ITEMSTACKLIST;
                default -> throw new IllegalStateException("Unknown variable value type: " + variable.valueType());
            };
        }

        private void executeNumberVariableAction(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            if (!(variable.handler() instanceof final Variables.NumberVariableHandler handler)) {
                warn("Cannot execute number variable action: " + variable.id() + " is not a number variable.");
                return;
            }
            if (!handler.canSet()) {
                warn("Cannot execute number variable action: " + variable.id() + " cannot be changed.");
                return;
            }
            final Number current = handler.getValue(questPlayer, objects);
            final double expression = new NumberExpression(this, action.text("expression"))
                    .calculateValue(questPlayer);
            final double next = switch (action.text("operator").toLowerCase(Locale.ROOT)) {
                case "set" -> expression;
                case "add" -> current.doubleValue() + expression;
                case "deduct" -> current.doubleValue() - expression;
                case "multiply" -> current.doubleValue() * expression;
                case "divide" -> expression == 0 ? current.doubleValue() : current.doubleValue() / expression;
                default -> {
                    warn("Cannot execute number variable action: unknown operator " + action.text("operator") + ".");
                    yield current.doubleValue();
                }
            };
            handler.setValue(next, questPlayer, objects);
        }

        private void executeStringVariableAction(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            if (!(variable.handler() instanceof final Variables.StringVariableHandler handler)) {
                warn("Cannot execute string variable action: " + variable.id() + " is not a string variable.");
                return;
            }
            if (!handler.canSet()) {
                warn("Cannot execute string variable action: " + variable.id() + " cannot be changed.");
                return;
            }
            final String current = String.valueOf(handler.getValue(questPlayer, objects));
            final String next = action.text("operator").equalsIgnoreCase("append")
                    ? current + action.text("expression")
                    : action.text("expression");
            handler.setValue(next, questPlayer, objects);
        }

        private void executeBooleanVariableAction(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            if (!(variable.handler() instanceof final Variables.BooleanVariableHandler handler)) {
                warn("Cannot execute boolean variable action: " + variable.id() + " is not a boolean variable.");
                return;
            }
            if (!handler.canSet()) {
                warn("Cannot execute boolean variable action: " + variable.id() + " cannot be changed.");
                return;
            }
            boolean next = new NumberExpression(this, action.text("expression"))
                    .calculateBooleanValue(questPlayer);
            if (action.text("operator").equalsIgnoreCase("setNot")) {
                next = !next;
            }
            handler.setValue(next, questPlayer, objects);
        }

        private void executeListVariableAction(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            if (!(variable.handler() instanceof final Variables.ListVariableHandler handler)) {
                warn("Cannot execute list variable action: " + variable.id() + " is not a list variable.");
                return;
            }
            if (!handler.canSet()) {
                warn("Cannot execute list variable action: " + variable.id() + " cannot be changed.");
                return;
            }
            final List<String> current = handler.getValue(questPlayer, objects) == null
                    ? List.of()
                    : handler.getValue(questPlayer, objects);
            final List<String> expression = splitListExpression(action.text("expression"));
            final List<String> next = switch (action.text("operator").toLowerCase(Locale.ROOT)) {
                case "set" -> expression;
                case "add" -> {
                    final List<String> values = new ArrayList<>(expression);
                    values.addAll(current);
                    yield values;
                }
                case "remove" -> {
                    final List<String> values = new ArrayList<>(current);
                    values.removeAll(expression);
                    yield values;
                }
                case "clear" -> List.of();
                default -> {
                    warn("Cannot execute list variable action: unknown operator " + action.text("operator") + ".");
                    yield current;
                }
            };
            handler.setValue(List.copyOf(next), questPlayer, objects);
        }

        private void executeItemStackListVariableAction(
                final Variables.Type variable,
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            if (!(variable.handler() instanceof final Variables.ItemStackListVariableHandler handler)) {
                warn("Cannot execute item-stack-list variable action: " + variable.id()
                        + " is not an item-stack-list variable.");
                return;
            }
            if (!handler.canSet()) {
                warn("Cannot execute item-stack-list variable action: " + variable.id() + " cannot be changed.");
                return;
            }
            final List<ItemSelection> current = handler.getValue(questPlayer, objects) == null
                    ? List.of()
                    : handler.getValue(questPlayer, objects);
            final ItemSelection item = itemSelectionValue(action.value("itemStack"));
            if (item == null && !action.text("operator").equalsIgnoreCase("clear")) {
                warn("Cannot execute item-stack-list variable action: missing item stack.");
                return;
            }
            final List<ItemSelection> actionItems = item == null ? List.of() : List.of(item);
            final List<ItemSelection> next = switch (action.text("operator").toLowerCase(Locale.ROOT)) {
                case "set" -> List.of(item);
                case "add" -> {
                    final List<ItemSelection> values = new ArrayList<>();
                    values.add(item);
                    values.addAll(current);
                    yield values;
                }
                case "remove" -> removeItemSelection(current, item);
                case "clear" -> List.of();
                default -> {
                    warn("Cannot execute item-stack-list variable action: unknown operator "
                            + action.text("operator") + ".");
                    yield current;
                }
            };
            handler.setValue(
                    List.copyOf(next),
                    questPlayer,
                    combined(objects, new Object[] {itemStackListActionValues(action, actionItems)}));
        }

        private static Map<String, Object> itemStackListActionValues(
                final Actions.Data action,
                final List<ItemSelection> actionItems) {
            final Map<String, Object> values = new LinkedHashMap<>();
            values.putAll(objectMap(action.value("additionalBooleans")));
            values.put("itemStackActionItems", actionItems == null ? List.of() : List.copyOf(actionItems));
            return values;
        }

        private static List<ItemSelection> removeItemSelection(
                final List<ItemSelection> current,
                final ItemSelection removedItem) {
            int remaining = Math.max(1, removedItem.amount());
            final List<ItemSelection> values = new ArrayList<>();
            for (final ItemSelection selection : current) {
                if (remaining <= 0 || !overlaps(selection, removedItem)) {
                    values.add(selection);
                    continue;
                }
                final int amount = Math.max(1, selection.amount());
                if (amount > remaining) {
                    values.add(selection.withAmount(amount - remaining));
                    remaining = 0;
                } else {
                    remaining -= amount;
                }
            }
            return values;
        }

        private static boolean overlaps(final ItemSelection first, final ItemSelection second) {
            for (final String material : materialIds(first)) {
                if (second.includesMaterial(material)) {
                    return true;
                }
            }
            for (final String material : materialIds(second)) {
                if (first.includesMaterial(material)) {
                    return true;
                }
            }
            return false;
        }

        private static List<String> materialIds(final ItemSelection selection) {
            final String listed = selection == null ? "" : selection.listedMaterials("");
            if (listed == null || listed.isBlank()) {
                return List.of();
            }
            return Arrays.stream(listed.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .toList();
        }

        private static List<String> splitListExpression(final String expression) {
            if (expression == null || expression.isBlank()) {
                return List.of();
            }
            return Arrays.stream(expression.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .toList();
        }

        private static ItemSelection itemSelectionValue(final Object value) {
            if (value instanceof final ItemSelection itemSelection) {
                return itemSelection;
            }
            return value == null ? null : ItemStackSelection.parse(value.toString());
        }

        private double numberExpressionValue(final Object expression, final PlatformPlayer questPlayer) {
            if (expression instanceof final Number number) {
                return number.doubleValue();
            }
            if (expression instanceof final Boolean bool) {
                return bool ? 1d : 0d;
            }
            return new NumberExpression(this, String.valueOf(expression == null ? "0" : expression))
                    .calculateValue(questPlayer);
        }

        private boolean booleanExpressionValue(final Object expression, final PlatformPlayer questPlayer) {
            if (expression instanceof final Boolean bool) {
                return bool;
            }
            if (expression instanceof final Number number) {
                return number.doubleValue() >= 0.98d;
            }
            return new NumberExpression(this, String.valueOf(expression == null ? "0" : expression))
                    .calculateBooleanValue(questPlayer);
        }
    }

    private static final class RegisteredField<T> implements RegistryField<T> {
        private final String valueType;
        private final boolean presenceFlag;
        private final Supplier<List<String>> suggestions;
        private String configPath;
        private boolean progressNeeded;
        private boolean invertedBooleanConfig;

        private RegisteredField(final String valueType, final boolean presenceFlag) {
            this(valueType, presenceFlag, null);
        }

        private RegisteredField(
                final String valueType,
                final boolean presenceFlag,
                final Supplier<List<String>> suggestions) {
            this.valueType = valueType;
            this.presenceFlag = presenceFlag;
            this.suggestions = suggestions;
        }

        @Override
        public RegistryField<T> config(final String configPath) {
            this.configPath = configPath;
            return this;
        }

        @Override
        public RegistryField<T> progressNeeded() {
            this.progressNeeded = true;
            this.configPath = "progressNeededExpression";
            return this;
        }

        @Override
        public RegistryField<T> invertedBooleanConfig(final String configPath) {
            this.configPath = configPath;
            this.invertedBooleanConfig = true;
            return this;
        }

        private RegistryField.Definition toField(final String name, final String description, final boolean flag) {
            return new RegistryField.Definition(
                    requireText(name, "field name"),
                    requireText(description, "field description"),
                    valueType,
                    configPath,
                    progressNeeded,
                    flag,
                    presenceFlag,
                    invertedBooleanConfig,
                    suggestions);
        }
    }

    private static final class RegistryFieldFactories implements FieldFactories {
        private final Supplier<List<String>> worldNames;

        private RegistryFieldFactories(final Supplier<List<String>> worldNames) {
            this.worldNames = worldNames == null ? List::of : worldNames;
        }

        @Override
        public RegistryField<String> greedyText() {
            return new RegisteredField<>("greedyText", false);
        }

        @Override
        public RegistryField<String> text() {
            return new RegisteredField<>("text", false);
        }

        @Override
        public RegistryField<String> text(final Supplier<List<String>> suggestions) {
            return new RegisteredField<>("text", false, suggestions);
        }

        @Override
        public RegistryField<String> stringList(final Supplier<List<String>> suggestions) {
            return new RegisteredField<>("stringList", false, suggestions);
        }

        @Override
        public RegistryField<String> commandText() {
            return new RegisteredField<>("command", false);
        }

        @Override
        public RegistryField<String> conditionName() {
            return new RegisteredField<>("condition", false);
        }

        @Override
        public RegistryField<String> npcSelector() {
            return new RegisteredField<>("npcSelector", false);
        }

        @Override
        public RegistryField<String> numberExpression() {
            return new RegisteredField<>("numberExpression", false);
        }

        @Override
        public RegistryField<String> numberExpression(final boolean greedy) {
            return new RegisteredField<>(greedy ? "numberExpression" : "numberExpressionToken", false);
        }

        @Override
        public RegistryField<String> booleanExpression() {
            return new RegisteredField<>("booleanExpression", false);
        }

        @Override
        public RegistryField<String> optionalNumberExpression() {
            return new RegisteredField<>("optionalNumberExpression", false);
        }

        @Override
        public RegistryField<Integer> integer(final int fallback) {
            return new RegisteredField<>("integer", false);
        }

        @Override
        public RegistryField<Integer> storedInteger(final int fallback) {
            return new RegisteredField<>("integer", false);
        }

        @Override
        public RegistryField<Duration> duration(final Duration fallback) {
            return new RegisteredField<>("duration", false);
        }

        @Override
        public RegistryField<Double> storedNumber(final double fallback) {
            return new RegisteredField<>("number", false);
        }

        @Override
        public RegistryField<Double> doubleNumber(final double fallback) {
            return new RegisteredField<>("number", false);
        }

        @Override
        public RegistryField<Double> optionalDouble() {
            return new RegisteredField<>("optionalNumber", false);
        }

        @Override
        public RegistryField<NQLocation> storedLocation() {
            return new RegisteredField<>("location", false);
        }

        @Override
        public RegistryField<String> entityType() {
            return new RegisteredField<>("entityType", false);
        }

        @Override
        public RegistryField<String> enchantment() {
            return new RegisteredField<>("enchantment", false);
        }

        @Override
        public RegistryField<String> worldName(final String anyWorldValue) {
            return new RegisteredField<>("worldName", false, () -> {
                final LinkedHashSet<String> names = new LinkedHashSet<>();
                if (anyWorldValue != null && !anyWorldValue.isBlank()) {
                    names.add(anyWorldValue);
                }
                final List<String> platformNames = worldNames.get();
                if (platformNames != null) {
                    for (final String worldName : platformNames) {
                        if (worldName != null && !worldName.isBlank()) {
                            names.add(worldName);
                        }
                    }
                }
                return List.copyOf(names);
            });
        }

        @Override
        public RegistryField<Boolean> presenceFlag() {
            return new RegisteredField<>("presenceFlag", true);
        }

        @Override
        public RegistryField<ItemSelection> itemSelection() {
            return new RegisteredField<>("itemSelection", false);
        }

        @Override
        public RegistryField<Object> storedItemStack() {
            return new RegisteredField<>("itemStack", false);
        }

        @Override
        public RegistryField<Map<String, String>> stringMap() {
            return new RegisteredField<>("textMap", false);
        }

        @Override
        public RegistryField<Map<String, ?>> numberExpressionMap() {
            return new RegisteredField<>("numberExpressionMap", false);
        }
    }

    private static final class ActionTypeBuilder implements Actions.Builder {
        private final NotQuestsRegistry registry;
        private final String id;
        private String displayName;
        private String description;
        private final List<RegistryField.Definition> fields = new ArrayList<>();
        private final List<RegistryField.Definition> flags = new ArrayList<>();
        private Set<Conditions.Target> validTargets = Set.of(Conditions.Target.values());
        private Variables.Command variableCommand;
        private boolean typeLiteral = true;
        private Actions.SingleLineParser singleLineParser;
        private Actions.Executor executor;
        private Actions.DescriptionRenderer descriptionRenderer;

        private ActionTypeBuilder(final NotQuestsRegistry registry, final String id) {
            this.registry = registry;
            this.id = id;
        }

        @Override
        public Actions.Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Actions.Builder description(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public <V> Actions.Builder field(
                final String name, final RegistryField<V> type, final String description) {
            fields.add(toRegisteredField(type).toField(name, description, false));
            return this;
        }

        @Override
        public <V> Actions.Builder flag(
                final String name, final RegistryField<V> type, final String description) {
            flags.add(toRegisteredField(type).toField(name, description, true));
            return this;
        }

        @Override
        public Actions.Builder variableCommands(
                final VariableDataType variableType,
                final String variableNameField,
                final String operatorField,
                final List<String> operators,
                final String expressionField,
                final RegistryField<?> expressionType,
                final String operatorDescription,
                final String expressionDescription) {
            variableCommand = new Variables.Command(
                    Objects.requireNonNull(variableType, "variable type"),
                    requireText(variableNameField, "variable name field"),
                    requireText(operatorField, "operator field"),
                    List.copyOf(operators),
                    requireText(expressionField, "expression field"),
                    toRegisteredField(expressionType).toField(expressionField, expressionDescription, false),
                    requireText(operatorDescription, "operator description"),
                    requireText(expressionDescription, "expression description"));
            return withoutTypeLiteral();
        }

        @Override
        public Actions.Builder singleLine(final Actions.SingleLineParser parser) {
            this.singleLineParser = parser;
            return this;
        }

        @Override
        public Actions.Builder execute(final Actions.Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public Actions.Builder actionDescription(final Actions.DescriptionRenderer renderer) {
            this.descriptionRenderer = renderer;
            return this;
        }

        @Override
        public Actions.Builder withoutTypeLiteral() {
            this.typeLiteral = false;
            return this;
        }

        @Override
        public void register() {
            registry.registerActionType(new Actions.Type(
                    requireText(id, "action id"),
                    requireText(displayName, "action display name"),
                    requireText(description, "action description"),
                    List.copyOf(fields),
                    List.copyOf(flags),
                    variableCommand,
                    typeLiteral,
                    singleLineParser,
                    executor,
                    descriptionRenderer));
        }
    }

    private static final class ConditionTypeBuilder implements Conditions.Builder {
        private final NotQuestsRegistry registry;
        private final String id;
        private String displayName;
        private String description;
        private final List<RegistryField.Definition> fields = new ArrayList<>();
        private final List<RegistryField.Definition> flags = new ArrayList<>();
        private Set<Conditions.Target> validTargets = Set.of(Conditions.Target.values());
        private Variables.Command variableCommand;
        private Conditions.SingleLineParser singleLineParser;
        private Conditions.Checker checker;
        private Conditions.DescriptionRenderer descriptionRenderer;

        private ConditionTypeBuilder(final NotQuestsRegistry registry, final String id) {
            this.registry = registry;
            this.id = id;
        }

        @Override
        public Conditions.Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Conditions.Builder description(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public <V> Conditions.Builder field(
                final String name, final RegistryField<V> type, final String description) {
            fields.add(toRegisteredField(type).toField(name, description, false));
            return this;
        }

        @Override
        public <V> Conditions.Builder flag(
                final String name, final RegistryField<V> type, final String description) {
            flags.add(toRegisteredField(type).toField(name, description, true));
            return this;
        }

        @Override
        public Conditions.Builder validFor(final Conditions.Target... targets) {
            validTargets = Set.of(targets);
            return this;
        }

        @Override
        public Conditions.Builder variableCommands(
                final VariableDataType variableType,
                final String variableNameField,
                final String operatorField,
                final List<String> operators,
                final String expressionField,
                final RegistryField<?> expressionType,
                final String operatorDescription,
                final String expressionDescription) {
            variableCommand = new Variables.Command(
                    Objects.requireNonNull(variableType, "variable type"),
                    requireText(variableNameField, "variable name field"),
                    requireText(operatorField, "variable operator field"),
                    List.copyOf(Objects.requireNonNull(operators, "variable operators")),
                    requireText(expressionField, "variable expression field"),
                    toRegisteredField(expressionType).toField(expressionField, expressionDescription, false),
                    requireText(operatorDescription, "variable operator description"),
                    requireText(expressionDescription, "variable expression description"));
            return this;
        }

        @Override
        public Conditions.Builder singleLine(final Conditions.SingleLineParser parser) {
            this.singleLineParser = parser;
            return this;
        }

        @Override
        public Conditions.Builder check(final Conditions.Checker checker) {
            this.checker = checker;
            return this;
        }

        @Override
        public Conditions.Builder conditionDescription(final Conditions.DescriptionRenderer renderer) {
            this.descriptionRenderer = renderer;
            return this;
        }

        @Override
        public void register() {
            registry.registerConditionType(new Conditions.Type(
                    requireText(id, "condition id"),
                    requireText(displayName, "condition display name"),
                    requireText(description, "condition description"),
                    List.copyOf(fields),
                    List.copyOf(flags),
                    Set.copyOf(validTargets),
                    variableCommand,
                    singleLineParser,
                    checker,
                    descriptionRenderer));
        }
    }

    private static final class ObjectiveTypeBuilder implements Objectives.Builder {
        private final NotQuestsRegistry registry;
        private final String id;
        private String displayName;
        private String description;
        private final List<RegistryField.Definition> fields = new ArrayList<>();
        private final List<RegistryField.Definition> flags = new ArrayList<>();
        private Variables.Command variableCommand;
        private Objectives.TaskDescriptionRenderer taskDescriptionRenderer;
        private Objectives.ObjectiveLifecycleHandler unlockHandler;
        private Objectives.ObjectiveLoadHandler loadHandler;
        private Objectives.ObjectiveCompleteOrLockHandler completeOrLockHandler;
        private Objectives.ObjectiveRefreshHandler refreshHandler;
        private Objectives.ObjectiveEventHandler jumpHandler;
        private Objectives.ObjectiveEventHandler startSneakHandler;
        private Objectives.DeathObjectiveEventHandler deathHandler;
        private Objectives.BlockObjectiveEventHandler breakBlockHandler;
        private Objectives.BlockObjectiveEventHandler placeBlockHandler;
        private Objectives.HarvestBlockObjectiveEventHandler harvestBlockHandler;
        private Objectives.ItemObjectiveEventHandler pickupItemHandler;
        private Objectives.ItemObjectiveEventHandler dropItemHandler;
        private Objectives.ItemObjectiveEventHandler takeBrewedItemHandler;
        private Objectives.ItemObjectiveEventHandler takeSmithingResultHandler;
        private Objectives.EntityObjectiveEventHandler killEntityHandler;
        private Objectives.InteractionObjectiveEventHandler interactBlockHandler;
        private Objectives.MoveObjectiveEventHandler moveHandler;
        private Objectives.ProjectileHitObjectiveEventHandler projectileHitHandler;
        private Objectives.EntityObjectiveEventHandler breedEntityHandler;
        private Objectives.EntityObjectiveEventHandler feedEntityHandler;
        private Objectives.EntityObjectiveEventHandler tameEntityHandler;
        private Objectives.ItemObjectiveEventHandler consumeItemHandler;
        private Objectives.ItemObjectiveEventHandler fishItemHandler;
        private Objectives.ItemObjectiveEventHandler craftItemHandler;
        private Objectives.ItemObjectiveEventHandler takeSmeltedItemHandler;
        private Objectives.ItemObjectiveEventHandler tradeItemHandler;
        private Objectives.ShearSheepObjectiveEventHandler shearSheepHandler;
        private Objectives.MilkCowObjectiveEventHandler milkCowHandler;
        private Objectives.ObjectiveEventHandler openBuriedTreasureHandler;
        private Objectives.CommandObjectiveEventHandler runCommandHandler;
        private Objectives.EnchantObjectiveEventHandler enchantItemHandler;
        private Objectives.NpcInteractionObjectiveEventHandler npcInteractionHandler;

        private ObjectiveTypeBuilder(final NotQuestsRegistry registry, final String id) {
            this.registry = registry;
            this.id = id;
        }

        @Override
        public Objectives.Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Objectives.Builder description(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public <V> Objectives.Builder field(
                final String name, final RegistryField<V> type, final String description) {
            fields.add(toRegisteredField(type).toField(name, description, false));
            return this;
        }

        @Override
        public <V> Objectives.Builder flag(
                final String name, final RegistryField<V> type, final String description) {
            flags.add(toRegisteredField(type).toField(name, description, true));
            return this;
        }

        @Override
        public Objectives.Builder variableCommands(
                final VariableDataType variableType,
                final String variableNameField,
                final String operatorField,
                final List<String> operators,
                final String expressionField,
                final RegistryField<?> expressionType,
                final String operatorDescription,
                final String expressionDescription) {
            variableCommand = new Variables.Command(
                    Objects.requireNonNull(variableType, "variable type"),
                    requireText(variableNameField, "variable name field"),
                    requireText(operatorField, "operator field"),
                    List.copyOf(operators),
                    requireText(expressionField, "expression field"),
                    toRegisteredField(expressionType).toField(expressionField, expressionDescription, false),
                    requireText(operatorDescription, "operator description"),
                    requireText(expressionDescription, "expression description"));
            return this;
        }

        @Override
        public Objectives.Builder taskDescription(final Objectives.TaskDescriptionRenderer renderer) {
            this.taskDescriptionRenderer = renderer;
            return this;
        }

        @Override
        public Objectives.Builder onUnlock(final Objectives.ObjectiveLifecycleHandler handler) {
            this.unlockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder afterLoad(final Objectives.ObjectiveLoadHandler handler) {
            this.loadHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onCompleteOrLock(final Objectives.ObjectiveCompleteOrLockHandler handler) {
            this.completeOrLockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onRefresh(final Objectives.ObjectiveRefreshHandler handler) {
            this.refreshHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerJump(final Objectives.ObjectiveEventHandler handler) {
            this.jumpHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerStartSneak(final Objectives.ObjectiveEventHandler handler) {
            this.startSneakHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerDeath(final Objectives.DeathObjectiveEventHandler handler) {
            this.deathHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerBreakBlock(final Objectives.BlockObjectiveEventHandler handler) {
            this.breakBlockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerPlaceBlock(final Objectives.BlockObjectiveEventHandler handler) {
            this.placeBlockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerHarvestBlock(final Objectives.HarvestBlockObjectiveEventHandler handler) {
            this.harvestBlockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerPickupItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.pickupItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerDropItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.dropItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerTakeBrewedItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.takeBrewedItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerTakeSmithingResult(final Objectives.ItemObjectiveEventHandler handler) {
            this.takeSmithingResultHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerKillEntity(final Objectives.EntityObjectiveEventHandler handler) {
            this.killEntityHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerInteractBlock(final Objectives.InteractionObjectiveEventHandler handler) {
            this.interactBlockHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerMove(final Objectives.MoveObjectiveEventHandler handler) {
            this.moveHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerShootProjectileHit(final Objectives.ProjectileHitObjectiveEventHandler handler) {
            this.projectileHitHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerBreedEntity(final Objectives.EntityObjectiveEventHandler handler) {
            this.breedEntityHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerFeedEntity(final Objectives.EntityObjectiveEventHandler handler) {
            this.feedEntityHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerTameEntity(final Objectives.EntityObjectiveEventHandler handler) {
            this.tameEntityHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerConsumeItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.consumeItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerFishItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.fishItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerCraftItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.craftItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerTakeSmeltedItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.takeSmeltedItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerTradeItem(final Objectives.ItemObjectiveEventHandler handler) {
            this.tradeItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerShearSheep(final Objectives.ShearSheepObjectiveEventHandler handler) {
            this.shearSheepHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerMilkCow(final Objectives.MilkCowObjectiveEventHandler handler) {
            this.milkCowHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerOpenBuriedTreasure(final Objectives.ObjectiveEventHandler handler) {
            this.openBuriedTreasureHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerRunCommand(final Objectives.CommandObjectiveEventHandler handler) {
            this.runCommandHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerEnchantItem(final Objectives.EnchantObjectiveEventHandler handler) {
            this.enchantItemHandler = handler;
            return this;
        }

        @Override
        public Objectives.Builder onPlayerInteractNpc(final Objectives.NpcInteractionObjectiveEventHandler handler) {
            this.npcInteractionHandler = handler;
            return this;
        }

        @Override
        public void register() {
            registry.registerObjectiveType(new Objectives.Type(
                    requireText(id, "objective id"),
                    requireText(displayName, "objective display name"),
                    requireText(description, "objective description"),
                    List.copyOf(fields),
                    List.copyOf(flags),
                    variableCommand,
                    taskDescriptionRenderer,
                    unlockHandler,
                    loadHandler,
                    completeOrLockHandler,
                    refreshHandler,
                    jumpHandler,
                    startSneakHandler,
                    deathHandler,
                    breakBlockHandler,
                    placeBlockHandler,
                    harvestBlockHandler,
                    pickupItemHandler,
                    dropItemHandler,
                    takeBrewedItemHandler,
                    takeSmithingResultHandler,
                    killEntityHandler,
                    interactBlockHandler,
                    moveHandler,
                    projectileHitHandler,
                    breedEntityHandler,
                    feedEntityHandler,
                    tameEntityHandler,
                    consumeItemHandler,
                    fishItemHandler,
                    craftItemHandler,
                    takeSmeltedItemHandler,
                    tradeItemHandler,
                    shearSheepHandler,
                    milkCowHandler,
                    openBuriedTreasureHandler,
                    runCommandHandler,
                    enchantItemHandler,
                    npcInteractionHandler));
        }
    }

    private static final class TriggerTypeBuilder implements Triggers.Builder {
        private final NotQuestsRegistry registry;
        private final String id;
        private String displayName;
        private String description;
        private final List<RegistryField.Definition> fields = new ArrayList<>();
        private Function<Triggers.Description, String> descriptionRenderer;

        private TriggerTypeBuilder(final NotQuestsRegistry registry, final String id) {
            this.registry = registry;
            this.id = id;
        }

        @Override
        public Triggers.Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Triggers.Builder description(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public <V> Triggers.Builder field(
                final String name, final RegistryField<V> type, final String description) {
            fields.add(toRegisteredField(type).toField(name, description, false));
            return this;
        }

        @Override
        public Triggers.Builder triggerDescription(
                final Function<Triggers.Description, String> renderer) {
            this.descriptionRenderer = renderer;
            return this;
        }

        @Override
        public void register() {
            registry.registerTriggerType(new Triggers.Type(
                    requireText(id, "trigger id"),
                    requireText(displayName, "trigger display name"),
                    requireText(description, "trigger description"),
                    List.copyOf(fields),
                    descriptionRenderer));
        }
    }

    private static final class VariableRegistration implements Variables.Registry {
        private final NotQuestsRegistry registry;

        private VariableRegistration(final NotQuestsRegistry registry) {
            this.registry = registry;
        }

        @Override
        public Variables.BooleanVariableBuilder booleanVariable(final String identifier) {
            return new BooleanVariableBuilderImpl(registry, identifier);
        }

        @Override
        public Variables.StringVariableBuilder stringVariable(final String identifier) {
            return new StringVariableBuilderImpl(registry, identifier);
        }

        @Override
        public Variables.NumberVariableBuilder numberVariable(final String identifier) {
            return new NumberVariableBuilderImpl(registry, identifier);
        }

        @Override
        public Variables.ListVariableBuilder listVariable(final String identifier) {
            return new ListVariableBuilderImpl(registry, identifier);
        }

        @Override
        public Variables.ItemStackListVariableBuilder itemStackListVariable(final String identifier) {
            return new ItemStackListVariableBuilderImpl(registry, identifier);
        }
    }

    private abstract static class VariableBuilderBase<B> {
        protected final NotQuestsRegistry registry;
        protected final String identifier;
        protected String displayName;
        protected String description;
        protected String singular;
        protected String plural;
        protected final List<RegistryField.Definition> fields = new ArrayList<>();

        private VariableBuilderBase(final NotQuestsRegistry registry, final String identifier) {
            this.registry = registry;
            this.identifier = identifier;
        }

        protected B self() {
            @SuppressWarnings("unchecked")
            final B self = (B) this;
            return self;
        }

        public B displayName(final String displayName) {
            this.displayName = displayName;
            return self();
        }

        public B description(final String description) {
            this.description = description;
            return self();
        }

        public B singular(final String singular) {
            this.singular = singular;
            return self();
        }

        public B plural(final String plural) {
            this.plural = plural;
            return self();
        }

        public <V> B field(final String name, final RegistryField<V> type, final String description) {
            fields.add(toRegisteredField(type).toField(name, description, false));
            return self();
        }
    }

    private static final class BooleanVariableBuilderImpl
            extends VariableBuilderBase<Variables.BooleanVariableBuilder>
            implements Variables.BooleanVariableBuilder {
        private BooleanGetter getter;
        private BooleanContextGetter contextGetter;
        private BooleanSetter setter;
        private BooleanContextSetter contextSetter;
        private BooleanPossibleValues possibleValues;
        private BooleanContextPossibleValues contextPossibleValues;

        private BooleanVariableBuilderImpl(final NotQuestsRegistry registry, final String identifier) {
            super(registry, identifier);
        }

        @Override
        public Variables.BooleanVariableBuilder get(final BooleanGetter getter) {
            this.getter = getter;
            return this;
        }

        @Override
        public Variables.BooleanVariableBuilder get(final BooleanContextGetter getter) {
            this.contextGetter = getter;
            return this;
        }

        @Override
        public Variables.BooleanVariableBuilder set(final BooleanSetter setter) {
            this.setter = setter;
            return this;
        }

        @Override
        public Variables.BooleanVariableBuilder set(final BooleanContextSetter setter) {
            this.contextSetter = setter;
            return this;
        }

        @Override
        public Variables.BooleanVariableBuilder possibleValues(final BooleanPossibleValues possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        @Override
        public Variables.BooleanVariableBuilder possibleValues(final BooleanContextPossibleValues possibleValues) {
            this.contextPossibleValues = possibleValues;
            return this;
        }

        @Override
        public void register() {
            final List<RegistryField.Definition> variableFields = List.copyOf(fields);
            registry.registerVariableType(new Variables.Type(
                    requireText(identifier, "variable id"),
                    requireText(displayName, "variable display name"),
                    requireText(description, "variable description"),
                    requireText(singular, "variable singular"),
                    requireText(plural, "variable plural"),
                    List.copyOf(fields),
                    new Variables.BooleanVariableHandler() {
                        @Override
                        public Boolean getValue(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextGetter != null) {
                                return contextGetter.get(variableContext(questPlayer, variableFields, objects));
                            }
                            return Objects.requireNonNull(getter, "boolean variable getter")
                                    .get(questPlayer, objects);
                        }

                        @Override
                        public boolean canSet() {
                            return setter != null || contextSetter != null;
                        }

                        @Override
                        public boolean setValue(
                                final Boolean newValue, final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextSetter != null) {
                                return contextSetter.set(newValue, variableContext(questPlayer, variableFields, objects));
                            }
                            return setter != null && setter.set(newValue, questPlayer, objects);
                        }

                        @Override
                        public List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextPossibleValues != null) {
                                return contextPossibleValues.values(variableContext(questPlayer, variableFields, objects));
                            }
                            return possibleValues == null
                                    ? Variables.BooleanVariableHandler.super.possibleValues(questPlayer, objects)
                                    : possibleValues.values(questPlayer, objects);
                        }
                    },
                    "boolean"));
        }
    }

    private static final class StringVariableBuilderImpl
            extends VariableBuilderBase<Variables.StringVariableBuilder>
            implements Variables.StringVariableBuilder {
        private StringGetter getter;
        private StringContextGetter contextGetter;
        private StringSetter setter;
        private StringContextSetter contextSetter;
        private StringPossibleValues possibleValues;
        private StringContextPossibleValues contextPossibleValues;

        private StringVariableBuilderImpl(final NotQuestsRegistry registry, final String identifier) {
            super(registry, identifier);
        }

        @Override
        public Variables.StringVariableBuilder get(final StringContextGetter getter) {
            this.contextGetter = getter;
            return this;
        }

        @Override
        public Variables.StringVariableBuilder get(final StringGetter getter) {
            this.getter = getter;
            return this;
        }

        @Override
        public Variables.StringVariableBuilder set(final StringContextSetter setter) {
            this.contextSetter = setter;
            return this;
        }

        @Override
        public Variables.StringVariableBuilder set(final StringSetter setter) {
            this.setter = setter;
            return this;
        }

        @Override
        public Variables.StringVariableBuilder possibleValues(final StringContextPossibleValues possibleValues) {
            this.contextPossibleValues = possibleValues;
            return this;
        }

        @Override
        public Variables.StringVariableBuilder possibleValues(final StringPossibleValues possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        @Override
        public void register() {
            registry.registerVariableType(new Variables.Type(
                    requireText(identifier, "variable id"),
                    requireText(displayName, "variable display name"),
                    requireText(description, "variable description"),
                    requireText(singular, "variable singular"),
                    requireText(plural, "variable plural"),
                    List.copyOf(fields),
                    new Variables.StringVariableHandler() {
                        @Override
                        public String getValue(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextGetter != null) {
                                return contextGetter.get(variableContext(questPlayer, fields, objects));
                            }
                            return Objects.requireNonNull(getter, "string variable getter")
                                    .get(questPlayer, objects);
                        }

                        @Override
                        public boolean canSet() {
                            return setter != null || contextSetter != null;
                        }

                        @Override
                        public boolean setValue(
                                final String newValue, final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextSetter != null) {
                                return contextSetter.set(newValue, variableContext(questPlayer, fields, objects));
                            }
                            return setter != null && setter.set(newValue, questPlayer, objects);
                        }

                        @Override
                        public List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextPossibleValues != null) {
                                return contextPossibleValues.values(variableContext(questPlayer, fields, objects));
                            }
                            return possibleValues == null
                                    ? Variables.StringVariableHandler.super.possibleValues(questPlayer, objects)
                                    : possibleValues.values(questPlayer, objects);
                        }
                    },
                    "string"));
        }
    }

    private static final class NumberVariableBuilderImpl
            extends VariableBuilderBase<Variables.NumberVariableBuilder>
            implements Variables.NumberVariableBuilder {
        private NumberGetter getter;
        private NumberContextGetter contextGetter;
        private NumberSetter setter;
        private NumberContextSetter contextSetter;
        private NumberPossibleValues possibleValues;
        private NumberContextPossibleValues contextPossibleValues;

        private NumberVariableBuilderImpl(final NotQuestsRegistry registry, final String identifier) {
            super(registry, identifier);
        }

        @Override
        public Variables.NumberVariableBuilder get(final NumberGetter getter) {
            this.getter = getter;
            return this;
        }

        @Override
        public Variables.NumberVariableBuilder get(final NumberContextGetter getter) {
            this.contextGetter = getter;
            return this;
        }

        @Override
        public Variables.NumberVariableBuilder set(final NumberSetter setter) {
            this.setter = setter;
            return this;
        }

        @Override
        public Variables.NumberVariableBuilder set(final NumberContextSetter setter) {
            this.contextSetter = setter;
            return this;
        }

        @Override
        public Variables.NumberVariableBuilder possibleValues(final NumberPossibleValues possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        @Override
        public Variables.NumberVariableBuilder possibleValues(final NumberContextPossibleValues possibleValues) {
            this.contextPossibleValues = possibleValues;
            return this;
        }

        @Override
        public void register() {
            registry.registerVariableType(new Variables.Type(
                    requireText(identifier, "variable id"),
                    requireText(displayName, "variable display name"),
                    requireText(description, "variable description"),
                    requireText(singular, "variable singular"),
                    requireText(plural, "variable plural"),
                    List.copyOf(fields),
                    new Variables.NumberVariableHandler() {
                        @Override
                        public Number getValue(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextGetter != null) {
                                return contextGetter.get(variableContext(questPlayer, fields, objects));
                            }
                            return Objects.requireNonNull(getter, "number variable getter").get(questPlayer, objects);
                        }

                        @Override
                        public boolean canSet() {
                            return setter != null || contextSetter != null;
                        }

                        @Override
                        public boolean setValue(
                                final Number newValue, final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextSetter != null) {
                                return contextSetter.set(newValue, variableContext(questPlayer, fields, objects));
                            }
                            return setter != null && setter.set(newValue, questPlayer, objects);
                        }

                        @Override
                        public List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextPossibleValues != null) {
                                return contextPossibleValues.values(variableContext(questPlayer, fields, objects));
                            }
                            return possibleValues == null
                                    ? Variables.NumberVariableHandler.super.possibleValues(questPlayer, objects)
                                    : possibleValues.values(questPlayer, objects);
                        }
                    },
                    "number"));
        }
    }

    private static final class ListVariableBuilderImpl
            extends VariableBuilderBase<Variables.ListVariableBuilder>
            implements Variables.ListVariableBuilder {
        private ListGetter getter;
        private ListContextGetter contextGetter;
        private ListSetter setter;
        private ListContextSetter contextSetter;
        private ListPossibleValues possibleValues;
        private ListContextPossibleValues contextPossibleValues;

        private ListVariableBuilderImpl(final NotQuestsRegistry registry, final String identifier) {
            super(registry, identifier);
        }

        @Override
        public Variables.ListVariableBuilder get(final ListContextGetter getter) {
            this.contextGetter = getter;
            return this;
        }

        @Override
        public Variables.ListVariableBuilder get(final ListGetter getter) {
            this.getter = getter;
            return this;
        }

        @Override
        public Variables.ListVariableBuilder set(final ListContextSetter setter) {
            this.contextSetter = setter;
            return this;
        }

        @Override
        public Variables.ListVariableBuilder set(final ListSetter setter) {
            this.setter = setter;
            return this;
        }

        @Override
        public Variables.ListVariableBuilder possibleValues(final ListContextPossibleValues possibleValues) {
            this.contextPossibleValues = possibleValues;
            return this;
        }

        @Override
        public Variables.ListVariableBuilder possibleValues(final ListPossibleValues possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        @Override
        public void register() {
            registry.registerVariableType(new Variables.Type(
                    requireText(identifier, "variable id"),
                    requireText(displayName, "variable display name"),
                    requireText(description, "variable description"),
                    requireText(singular, "variable singular"),
                    requireText(plural, "variable plural"),
                    List.copyOf(fields),
                    new Variables.ListVariableHandler() {
                        @Override
                        public List<String> getValue(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextGetter != null) {
                                return contextGetter.get(variableContext(questPlayer, fields, objects));
                            }
                            return Objects.requireNonNull(getter, "list variable getter")
                                    .get(questPlayer, objects);
                        }

                        @Override
                        public boolean canSet() {
                            return setter != null;
                        }

                        @Override
                        public boolean setValue(
                                final List<String> newValue, final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextSetter != null) {
                                return contextSetter.set(newValue, variableContext(questPlayer, fields, objects));
                            }
                            return setter != null && setter.set(newValue, questPlayer, objects);
                        }

                        @Override
                        public List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextPossibleValues != null) {
                                return contextPossibleValues.values(variableContext(questPlayer, fields, objects));
                            }
                            return possibleValues == null
                                    ? Variables.ListVariableHandler.super.possibleValues(questPlayer, objects)
                                    : possibleValues.values(questPlayer, objects);
                        }
                    },
                    "list"));
        }
    }

    private static final class ItemStackListVariableBuilderImpl
            extends VariableBuilderBase<Variables.ItemStackListVariableBuilder>
            implements Variables.ItemStackListVariableBuilder {
        private ItemStackListGetter getter;
        private ItemStackListContextGetter contextGetter;
        private ItemStackListSetter setter;
        private ItemStackListContextSetter contextSetter;
        private ItemStackListPossibleValues possibleValues;
        private ItemStackListContextPossibleValues contextPossibleValues;

        private ItemStackListVariableBuilderImpl(final NotQuestsRegistry registry, final String identifier) {
            super(registry, identifier);
        }

        @Override
        public Variables.ItemStackListVariableBuilder get(final ItemStackListGetter getter) {
            this.getter = getter;
            return this;
        }

        @Override
        public Variables.ItemStackListVariableBuilder get(final ItemStackListContextGetter getter) {
            this.contextGetter = getter;
            return this;
        }

        @Override
        public Variables.ItemStackListVariableBuilder set(final ItemStackListSetter setter) {
            this.setter = setter;
            return this;
        }

        @Override
        public Variables.ItemStackListVariableBuilder set(final ItemStackListContextSetter setter) {
            this.contextSetter = setter;
            return this;
        }

        @Override
        public Variables.ItemStackListVariableBuilder possibleValues(final ItemStackListPossibleValues possibleValues) {
            this.possibleValues = possibleValues;
            return this;
        }

        @Override
        public Variables.ItemStackListVariableBuilder possibleValues(final ItemStackListContextPossibleValues possibleValues) {
            this.contextPossibleValues = possibleValues;
            return this;
        }

        @Override
        public void register() {
            final List<RegistryField.Definition> variableFields = List.copyOf(fields);
            registry.registerVariableType(new Variables.Type(
                    requireText(identifier, "variable id"),
                    requireText(displayName, "variable display name"),
                    requireText(description, "variable description"),
                    requireText(singular, "variable singular"),
                    requireText(plural, "variable plural"),
                    List.copyOf(fields),
                    new Variables.ItemStackListVariableHandler() {
                        @Override
                        public List<ItemSelection> getValue(
                                final PlatformPlayer questPlayer,
                                final Object... objects) {
                            if (contextGetter != null) {
                                return contextGetter.get(variableContext(questPlayer, variableFields, objects));
                            }
                            return Objects.requireNonNull(getter, "item-stack-list variable getter")
                                    .get(questPlayer, objects);
                        }

                        @Override
                        public boolean canSet() {
                            return setter != null || contextSetter != null;
                        }

                        @Override
                        public boolean setValue(
                                final List<ItemSelection> newValue,
                                final PlatformPlayer questPlayer,
                                final Object... objects) {
                            if (contextSetter != null) {
                                return contextSetter.set(
                                        newValue,
                                        variableContext(questPlayer, variableFields, objects));
                            }
                            return setter != null && setter.set(newValue, questPlayer, objects);
                        }

                        @Override
                        public List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                            if (contextPossibleValues != null) {
                                return contextPossibleValues.values(variableContext(questPlayer, variableFields, objects));
                            }
                            return possibleValues == null
                                    ? Variables.ItemStackListVariableHandler.super.possibleValues(questPlayer, objects)
                                    : possibleValues.values(questPlayer, objects);
                        }
                    },
                    "itemStackList"));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> RegisteredField<T> toRegisteredField(final RegistryField<T> field) {
        if (field instanceof RegisteredField<?> registeredField) {
            return (RegisteredField<T>) registeredField;
        }
        throw new IllegalArgumentException("Registry field was not created by this registry adapter.");
    }

        private static Variables.Context variableContext(
                final PlatformPlayer questPlayer,
                final List<RegistryField.Definition> fields,
                final Object... objects) {
            final Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < fields.size() && i < objects.length; i++) {
                values.put(fields.get(i).name(), objects[i]);
            }
            for (int i = fields.size(); i < objects.length; i++) {
                if (objects[i] instanceof final Map<?, ?> extraValues) {
                    for (final Map.Entry<?, ?> entry : extraValues.entrySet()) {
                        if (entry.getKey() != null) {
                            values.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                }
            }
            return new Variables.Context(questPlayer, values);
        }

    private static String requireText(final String value, final String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private static <T> void replace(
            final List<T> existing,
            final Function<T, String> identifier,
            final String id) {
        existing.removeIf(entry -> identifier.apply(entry).equalsIgnoreCase(id));
    }

    private static final Map<String, String> DEFAULT_OBJECTIVE_TASK_TEXT = Map.ofEntries(
            Map.entry("chat.objectives.taskDescription.breakBlocks.base", "Break Blocks: <main>%BLOCKTOBREAK%"),
            Map.entry("chat.objectives.taskDescription.breed.base", "Breed Mobs: <main>%ENTITYTOBREED%"),
            Map.entry("chat.objectives.taskDescription.feedMobs.base", "Feed Mobs: <main>%ENTITYTOFEED%"),
            Map.entry("chat.objectives.taskDescription.tameMobs.base", "Tame Mobs: <main>%ENTITYTOTAME%"),
            Map.entry("chat.objectives.taskDescription.harvest.base", "Harvest: <main>%CROPTOHARVEST%"),
            Map.entry("chat.objectives.taskDescription.pickupItems.base", "Pickup Items: <main>%ITEMTOPICKUPTYPE% %(%%ITEMTOPICKUPNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.consumeItems.base", "Consume Items: <main>%ITEMTOCONSUMETYPE% %(%%ITEMTOCONSUMENAME%%)%"),
            Map.entry("chat.objectives.taskDescription.craftItems.base", "Craft Items: <main>%ITEMTOCRAFTTYPE% %(%%ITEMTOCRAFTNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.deliverItems.base", "Deliver Items: <main>%ITEMTODELIVERTYPE% %(%%ITEMTODELIVERNAME%%(%"),
            Map.entry("chat.objectives.taskDescription.deliverItems.deliver-to-npc", "      <GRAY>Deliver it to <WHITE>%NPCNAME%"),
            Map.entry("chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available", "      <GRAY>The delivery NPC is currently not available!"),
            Map.entry("chat.objectives.taskDescription.tradeWithVillager.base", "Trade with Villager: <main>%ITEMTOTRADETYPE% %(%%ITEMTOTRADENAME%%)%"),
            Map.entry("chat.objectives.taskDescription.enchant.base", "Enchant Items with %ENCHANTMENT%: <main>%ITEMTOENCHANTTYPE% %(%%ITEMTOENCHANTNAME%%(%"),
            Map.entry("chat.objectives.taskDescription.fishItems.base", "Fish Items: <main>%ITEMTOFISHTYPE% %(%%ITEMTOFISHNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.killMobs.base", "Kill mobs: <main>%MOBTOKILL%"),
            Map.entry("chat.objectives.taskDescription.otherQuest.base", "Complete Quest: <main>%OTHERQUESTNAME%"),
            Map.entry("chat.objectives.taskDescription.placeBlocks.base", "Place Blocks: <main>%BLOCKTOPLACE%"),
            Map.entry("chat.objectives.taskDescription.reachLocation.base", "Reach Location: <main>%LOCATIONNAME%"),
            Map.entry("chat.objectives.taskDescription.talkToNPC.base", "Talk to <main>%NAME%"),
            Map.entry("chat.objectives.taskDescription.talkToNPC.npc-not-available", "    <GRAY>The target NPC is currently not available!"),
            Map.entry("chat.objectives.taskDescription.triggerCommand.base", "Goal: <main>%TRIGGERNAME%"),
            Map.entry("chat.objectives.taskDescription.runCommand.base", "Run command: <main>%COMMANDTORUN%"),
            Map.entry("chat.objectives.taskDescription.shootArrow.base", "Shoot Arrow: <main>%COORDINATES%</main> in world <main>%WORLDNAME%</main> within <main>%RADIUS%</main> blocks"),
            Map.entry("chat.objectives.taskDescription.interact.base", "%INTERACTTYPE% Location: <main>%COORDINATES% <main>in world %WORLDNAME%"),
            Map.entry("chat.objectives.taskDescription.jump.base", "Jump <main>%AMOUNTOFJUMPS%</main> times"),
            Map.entry("chat.objectives.taskDescription.sneak.base", "Sneak <main>%AMOUNTOFSNEAKS%</main> times"),
            Map.entry("chat.objectives.taskDescription.die.base", "Die: <main>%DAMAGETYPE%"),
            Map.entry("chat.objectives.taskDescription.smelt.base", "Smelt Items: <main>%ITEMTOSMELTTYPE% %(%%ITEMTOSMELTNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.smeltItems.base", "Smelt Items: <main>%ITEMTOSMELTTYPE% %(%%ITEMTOSMELTNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.brewItems.base", "Brew Items: <main>%ITEMTOBREWTYPE% %(%%ITEMTOBREWNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.smithItems.base", "Smith Items: <main>%ITEMTOSMITHTYPE% %(%%ITEMTOSMITHNAME%%)%"),
            Map.entry("chat.objectives.taskDescription.objective.base", "%OBJECTIVEHOLDERNAME%"),
            Map.entry("chat.objectives.taskDescription.openBuriedTreasure.base", "Open buried treasures"),
            Map.entry("chat.objectives.taskDescription.shearSheep.base", "Shear <main>%AMOUNTOFSHEEP%</main> sheep"),
            Map.entry("chat.objectives.taskDescription.milkCow.base", "Milk <main>%AMOUNTOFCOWS%</main> cows"));

    /** Owns action registration, action data contracts, and command parsing. */
    public static final class Actions {
        public enum Target {
            QUEST,
            OBJECTIVE,
            SAVED_ACTION,
            INLINE
        }

        public record Type(
                String id,
                String displayName,
                String description,
                List<RegistryField.Definition> fields,
                List<RegistryField.Definition> flags,
                Variables.Command variableCommand,
                boolean typeLiteral,
                SingleLineParser singleLineParser,
                Executor executor,
                DescriptionRenderer descriptionRenderer) {}

        public interface Data {
            Object value(String name);

            void copyTo(Draft action);

            default <V> V value(final String name, final Class<V> type) {
                final Object value = value(name);
                return value == null ? null : type.cast(value);
            }

            String text(String name);

            int integer(String name);

            int integer(String name, int fallback);

            default double number(final String name, final double fallback) {
                return fallback;
            }

            default boolean flag(final String name) {
                return false;
            }

            default ItemSelection itemSelection(final String name) {
                return null;
            }

            default Duration duration(final String name, final Duration fallback) {
                return fallback;
            }

            default NQLocation location(final String name) {
                return null;
            }
        }

        public interface Draft {
            void setValue(String name, Object value);
        }

        @FunctionalInterface
        public interface Registry {
            Builder action(String identifier);
        }

        public interface Builder {
            Builder displayName(String displayName);

            Builder description(String description);

            <V> Builder field(String name, RegistryField<V> type, String description);

            <V> Builder flag(String name, RegistryField<V> type, String description);

            Builder variableCommands(
                    VariableDataType variableType,
                    String variableNameField,
                    String operatorField,
                    List<String> operators,
                    String expressionField,
                    RegistryField<?> expressionType,
                    String operatorDescription,
                    String expressionDescription);

            Builder singleLine(SingleLineParser parser);

            Builder execute(Executor executor);

            Builder actionDescription(DescriptionRenderer renderer);

            Builder withoutTypeLiteral();

            void register();
        }

        @FunctionalInterface
        public interface SingleLineParser {
            void parse(Draft action, List<String> arguments);
        }

        @FunctionalInterface
        public interface Executor {
            void execute(Data action, PlatformPlayer questPlayer, Object... objects);
        }

        @FunctionalInterface
        public interface DescriptionRenderer {
            String render(Data action, PlatformPlayer questPlayer, Object... objects);
        }

        public static Action parse(
                final NotQuestsAdapter adapter,
                final Actions.Type actionType,
                final String rawInput) {
            return parse(adapter, actionType, rawInput, null);
        }

        public static Action parse(
                final NotQuestsAdapter adapter,
                final Actions.Type actionType,
                final String rawInput,
                final PlatformPlayer questPlayer) {
            final List<String> tokens = tokenize(rawInput);
            final Action data = new Action(0, actionType.id(), null);
            if (actionType.variableCommand() != null) {
                readVariableAction(
                        adapter,
                        actionType,
                        actionType.variableCommand(),
                        new ParseCursor(tokens),
                        data,
                        questPlayer);
                return data;
            }
            final boolean hasItemSelectionField = actionType.fields().stream()
                    .anyMatch(field -> "itemSelection".equals(field.valueType()));
            if (actionType.singleLineParser() != null && !hasItemSelectionField) {
                actionType.singleLineParser()
                        .parse(data, actionType.variableCommand() == null ? tokensBeforeFlags(tokens) : tokens);
                parseFlags(adapter, actionType, data, new ParseCursor(tokens), questPlayer);
                return data;
            }

            final ParseCursor cursor = new ParseCursor(tokens);

            for (final RegistryField.Definition field : actionType.fields()) {
                if (cursor.finished()) {
                    break;
                }
                if (cursor.peekFlag()) {
                    break;
                }
                data.setValue(field.name(), readValue(adapter, cursor, field, false, questPlayer));
            }

            parseFlags(adapter, actionType, data, cursor, questPlayer);
            return data;
        }

        private static void readVariableAction(
                final NotQuestsAdapter adapter,
                final Actions.Type actionType,
                final Variables.Command command,
                final ParseCursor cursor,
                final Action data,
                final PlatformPlayer questPlayer) {
            final String variableName = required(cursor, command.variableNameField());
            if (adapter.variableType(variableName) != command.variableType()) {
                throw new IllegalArgumentException(
                        "Variable '" + variableName + "' is not a "
                                + command.variableType().name().toLowerCase(Locale.ROOT) + " variable.");
            }
            data.setValue(command.variableNameField(), variableName);

            final LinkedHashMap<String, String> stringArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> numberArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> booleanArguments = new LinkedHashMap<>();
            final List<RegistryField.Definition> variableFields = adapter.variableFields(variableName);
            for (final RegistryField.Definition variableField : variableFields) {
                if (variableField.flag() || variableField.presenceFlag()) {
                    booleanArguments.put(variableField.name(), "0");
                    continue;
                }
                final Object value = readValue(adapter, cursor, variableField, false, questPlayer);
                final String rawValue = value == null ? "" : value.toString();
                if (variableField.valueType().contains("number") || variableField.valueType().contains("integer")) {
                    numberArguments.put(variableField.name(), rawValue);
                } else if (variableField.valueType().contains("boolean")) {
                    booleanArguments.put(variableField.name(), rawValue);
                } else {
                    stringArguments.put(variableField.name(), rawValue);
                }
            }

            final String operator = required(cursor, command.operatorField());
            data.setValue(command.operatorField(), operator);
            if (command.variableType() == VariableDataType.ITEMSTACKLIST) {
                final ItemSelection selection = (ItemSelection) readValue(
                        adapter, cursor, command.expressionType(), false, questPlayer);
                final int amount = cursor.finished() || cursor.peekFlag()
                        ? 1
                        : Integer.parseInt(cursor.next());
                data.setValue(command.expressionField(), selection == null ? null : selection.withAmount(amount));
                data.setValue("amount", amount);
            } else {
                data.setValue(command.expressionField(), readValue(
                        adapter, cursor, command.expressionType(), false, questPlayer));
            }

            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final String flagName = token.substring(2);
                final RegistryField.Definition variableFlag = namedField(variableFields, flagName);
                if (variableFlag != null && (variableFlag.flag() || variableFlag.presenceFlag())) {
                    booleanArguments.put(variableFlag.name(), "1");
                    continue;
                }
                final RegistryField.Definition actionFlag = flag(actionType, flagName);
                if (actionFlag != null) {
                    if (actionFlag.presenceFlag()) {
                        data.setValue(actionFlag.name(), true);
                    } else {
                        data.setValue(actionFlag.name(), readValue(
                                adapter, cursor, actionFlag, true, questPlayer));
                    }
                }
            }
            data.setValue("additionalStrings", stringArguments);
            data.setValue("additionalNumbers", numberArguments);
            data.setValue("additionalBooleans", booleanArguments);
        }

        private static void parseFlags(
                final NotQuestsAdapter adapter,
                final Actions.Type actionType,
                final Action data,
                final ParseCursor cursor,
                final PlatformPlayer questPlayer) {
            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final RegistryField.Definition flag = flag(actionType, token.substring(2));
                if (flag == null) {
                    continue;
                }
                if (flag.presenceFlag()) {
                    data.setValue(flag.name(), true);
                } else {
                    data.setValue(flag.name(), readValue(adapter, cursor, flag, true, questPlayer));
                }
            }
        }

        public static List<String> tokenize(final String input) {
            final String text = input == null ? "" : input;
            final List<String> tokens = new ArrayList<>();
            final StringBuilder current = new StringBuilder();
            boolean quoted = false;
            char quote = 0;
            boolean escaping = false;
            for (int i = 0; i < text.length(); i++) {
                final char character = text.charAt(i);
                if (escaping) {
                    current.append(character);
                    escaping = false;
                    continue;
                }
                if (character == '\\') {
                    escaping = true;
                    continue;
                }
                if (quoted) {
                    if (character == quote) {
                        quoted = false;
                    } else {
                        current.append(character);
                    }
                    continue;
                }
                if (character == '"' || character == '\'') {
                    quoted = true;
                    quote = character;
                    continue;
                }
                if (Character.isWhitespace(character)) {
                    addToken(tokens, current);
                    continue;
                }
                current.append(character);
            }
            addToken(tokens, current);
            return tokens;
        }

        private static Object readValue(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field,
                final boolean flagValue,
                final PlatformPlayer questPlayer) {
            return switch (field.valueType()) {
                case "greedyText" -> readRest(cursor, true);
                case "command" -> normalizeCommandText(readRest(cursor, false));
                case "integer" -> Integer.parseInt(required(cursor, field));
                case "number", "optionalNumber" -> Double.parseDouble(required(cursor, field));
                case "duration" -> UtilManager.parseDuration(required(cursor, field));
                case "location" -> readLocation(adapter, cursor, field);
                case "itemSelection" -> readItemSelection(adapter, required(cursor, field), questPlayer);
                default -> required(cursor, field);
            };
        }

        private static String normalizeCommandText(final String command) {
            if (command == null || command.isBlank()) {
                return "";
            }
            return command.startsWith("/") ? command : "/" + command;
        }

        private static NQLocation readLocation(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field) {
            final String worldName = required(cursor, field);
            final double x = Double.parseDouble(required(cursor, field));
            final double y = Double.parseDouble(required(cursor, field));
            final double z = Double.parseDouble(required(cursor, field));
            return adapter.location(worldName, x, y, z);
        }

        private static ItemSelection readItemSelection(
                final NotQuestsAdapter adapter,
                final String input,
                final PlatformPlayer questPlayer) {
            return adapter.parseItemSelection(input, questPlayer);
        }

        private static String readRest(final ParseCursor cursor, final boolean stopAtNextFlag) {
            final List<String> values = new ArrayList<>();
            while (!cursor.finished() && (!stopAtNextFlag || !cursor.peekFlag())) {
                values.add(cursor.next());
            }
            return String.join(" ", values);
        }

        private static String required(final ParseCursor cursor, final RegistryField.Definition field) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for action field '" + field.name() + "'.");
            }
            return cursor.next();
        }

        private static String required(final ParseCursor cursor, final String fieldName) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for action field '" + fieldName + "'.");
            }
            return cursor.next();
        }

        private static RegistryField.Definition flag(final Actions.Type actionType, final String name) {
            final String normalized = name.toLowerCase(Locale.ROOT);
            for (final RegistryField.Definition flag : actionType.flags()) {
                if (flag.name().equalsIgnoreCase(normalized)) {
                    return flag;
                }
            }
            return null;
        }

        private static RegistryField.Definition namedField(
                final List<RegistryField.Definition> fields,
                final String name) {
            for (final RegistryField.Definition field : fields) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            return null;
        }

        private static List<String> tokensBeforeFlags(final List<String> tokens) {
            final List<String> values = new ArrayList<>();
            for (final String token : tokens) {
                if (token.startsWith("--")) {
                    break;
                }
                values.add(token);
            }
            return values;
        }

        private static void addToken(final List<String> tokens, final StringBuilder current) {
            if (!current.isEmpty()) {
                tokens.add(current.toString());
                current.setLength(0);
            }
        }

        private static final class ParseCursor {
            private final List<String> tokens;
            private int index;

            private ParseCursor(final List<String> tokens) {
                this.tokens = tokens;
            }

            private boolean finished() {
                return index >= tokens.size();
            }

            private boolean peekFlag() {
                return !finished() && tokens.get(index).startsWith("--");
            }

            private String next() {
                return tokens.get(index++);
            }
        }
    }

    /** Owns condition registration, condition data contracts, and command parsing. */
    public static final class Conditions {
        private static final String ADDITIONAL_STRINGS = "additionalStrings";
        private static final String ADDITIONAL_NUMBERS = "additionalNumbers";
        private static final String ADDITIONAL_BOOLEANS = "additionalBooleans";
        public enum Target {
            QUEST,
            OBJECTIVE_UNLOCK,
            OBJECTIVE_PROGRESS,
            OBJECTIVE_COMPLETE,
            SAVED_CONDITION,
            INLINE,
            ACTION
        }

        public record Type(
                String id,
                String displayName,
                String description,
                List<RegistryField.Definition> fields,
                List<RegistryField.Definition> flags,
                Set<Target> validTargets,
                Variables.Command variableCommand,
                SingleLineParser singleLineParser,
                Checker checker,
                DescriptionRenderer descriptionRenderer) {}

        public interface Data {
            void copyTo(Draft condition);

            default Object value(final String name) {
                return null;
            }

            default <V> V value(final String name, final Class<V> type) {
                final Object value = value(name);
                return value == null ? null : type.cast(value);
            }

            String text(String name);

            int integer(String name);

            int integer(String name, int fallback);

            default boolean flag(final String name) {
                return Boolean.TRUE.equals(value(name));
            }
        }

        public interface Draft {
            void setValue(String name, Object value);
        }

        @FunctionalInterface
        public interface Registry {
            Builder condition(String identifier);
        }

        public interface Builder {
            Builder displayName(String displayName);

            Builder description(String description);

            <V> Builder field(String name, RegistryField<V> type, String description);

            <V> Builder flag(String name, RegistryField<V> type, String description);

            Builder validFor(Target... targets);

            Builder variableCommands(
                    VariableDataType variableType,
                    String variableNameField,
                    String operatorField,
                    List<String> operators,
                    String expressionField,
                    RegistryField<?> expressionType,
                    String operatorDescription,
                    String expressionDescription);

            Builder singleLine(SingleLineParser parser);

            Builder check(Checker checker);

            Builder conditionDescription(DescriptionRenderer renderer);

            void register();
        }

        @FunctionalInterface
        public interface Checker {
            String check(Data condition, PlatformPlayer questPlayer);
        }

        @FunctionalInterface
        public interface DescriptionRenderer {
            String render(Data condition, PlatformPlayer questPlayer, Object... objects);
        }

        @FunctionalInterface
        public interface SingleLineParser {
            void parse(Draft condition, List<String> arguments);
        }

        public static Condition parse(
                final NotQuestsAdapter adapter,
                final Conditions.Type conditionType,
                final String rawInput) {
            return parse(adapter, conditionType, rawInput, null);
        }

        public static Condition parse(
                final NotQuestsAdapter adapter,
                final Conditions.Type conditionType,
                final String rawInput,
                final PlatformPlayer questPlayer) {
            final List<String> tokens = Actions.tokenize(rawInput);
            final Condition data = new Condition(0, conditionType.id(), null);
            final ParseCursor cursor = new ParseCursor(tokens);

            if (conditionType.variableCommand() != null) {
                readVariableCondition(adapter, conditionType, conditionType.variableCommand(), cursor, data, questPlayer);
                return data;
            }

            if (conditionType.singleLineParser() != null) {
                conditionType.singleLineParser().parse(data, tokensBeforeFlags(tokens));
                readFlags(adapter, conditionType, new ParseCursor(tokens), data, questPlayer);
                return data;
            }

            for (final RegistryField.Definition field : conditionType.fields()) {
                if (cursor.finished() || cursor.peekFlag()) {
                    break;
                }
                data.setValue(field.name(), readValue(adapter, cursor, field, questPlayer));
            }

            readFlags(adapter, conditionType, cursor, data, questPlayer);
            return data;
        }

        private static void readVariableCondition(
                final NotQuestsAdapter adapter,
                final Conditions.Type conditionType,
                final Variables.Command command,
                final ParseCursor cursor,
                final Condition data,
                final PlatformPlayer questPlayer) {
            final String variableName = required(cursor, command.variableNameField());
            if (adapter.variableType(variableName) != command.variableType()) {
                throw new IllegalArgumentException(
                        "Variable '" + variableName + "' is not a " + command.variableType().name().toLowerCase() + " variable.");
            }
            data.setValue(command.variableNameField(), variableName);

            final LinkedHashMap<String, String> stringArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> numberArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> booleanArguments = new LinkedHashMap<>();
            final List<RegistryField.Definition> variableFields = adapter.variableFields(variableName);
            for (final RegistryField.Definition variableField : variableFields) {
                if (isVariableFlag(variableField)) {
                    booleanArguments.put(variableField.name(), "0");
                    continue;
                }
                if (cursor.finished() || cursor.peekFlag()) {
                    throw new IllegalArgumentException("Missing value for variable field '" + variableField.name() + "'.");
                }
                final Object value = readValue(adapter, cursor, variableField, questPlayer);
                final String rawValue = value == null ? "" : value.toString();
                if (isNumberField(variableField)) {
                    numberArguments.put(variableField.name(), rawValue);
                } else if (isBooleanField(variableField)) {
                    booleanArguments.put(variableField.name(), rawValue);
                } else {
                    stringArguments.put(variableField.name(), rawValue);
                }
            }
            data.setValue(ADDITIONAL_STRINGS, stringArguments);
            data.setValue(ADDITIONAL_NUMBERS, numberArguments);
            data.setValue(ADDITIONAL_BOOLEANS, booleanArguments);

            data.setValue(command.operatorField(), required(cursor, command.operatorField()));
            final Object expression = readValue(adapter, cursor, command.expressionType(), questPlayer);
            if (command.variableType() == VariableDataType.ITEMSTACKLIST) {
                final int amount = Integer.parseInt(required(cursor, "amount"));
                data.setValue("amount", amount);
                data.setValue(command.expressionField(), expression instanceof final ItemSelection itemSelection
                        ? itemSelection.withAmount(amount)
                        : expression);
            } else {
                data.setValue(command.expressionField(), expression);
            }

            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final String flagName = token.substring(2);
                final RegistryField.Definition variableFlag = namedField(variableFields, flagName);
                if (variableFlag != null && isVariableFlag(variableFlag)) {
                    booleanArguments.put(variableFlag.name(), "1");
                    continue;
                }
                final RegistryField.Definition conditionFlag = flag(conditionType, flagName);
                if (conditionFlag == null) {
                    continue;
                }
                if (conditionFlag.presenceFlag()) {
                    data.setValue(conditionFlag.name(), true);
                } else {
                    data.setValue(conditionFlag.name(), readValue(adapter, cursor, conditionFlag, questPlayer));
                }
            }
        }

        private static void readFlags(
                final NotQuestsAdapter adapter,
                final Conditions.Type conditionType,
                final ParseCursor cursor,
                final Condition data,
                final PlatformPlayer questPlayer) {
            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final RegistryField.Definition flag = flag(conditionType, token.substring(2));
                if (flag == null) {
                    continue;
                }
                if (flag.presenceFlag()) {
                    data.setValue(flag.name(), true);
                } else {
                    data.setValue(flag.name(), readValue(adapter, cursor, flag, questPlayer));
                }
            }
        }

        private static Object readValue(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field,
                final PlatformPlayer questPlayer) {
            return switch (field.valueType()) {
                case "greedyText" -> readRest(cursor, true);
                case "command" -> readRest(cursor, false);
                case "integer" -> Integer.parseInt(required(cursor, field));
                case "number", "optionalNumber", "numberExpression", "numberExpressionToken" -> required(cursor, field);
                case "duration" -> UtilManager.parseDuration(required(cursor, field));
                case "location" -> readLocation(adapter, cursor, field);
                case "itemSelection" -> readItemSelection(adapter, required(cursor, field), questPlayer);
                default -> required(cursor, field);
            };
        }

        private static NQLocation readLocation(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field) {
            final String worldName = required(cursor, field);
            final double x = Double.parseDouble(required(cursor, field));
            final double y = Double.parseDouble(required(cursor, field));
            final double z = Double.parseDouble(required(cursor, field));
            return adapter.location(worldName, x, y, z);
        }

        private static ItemSelection readItemSelection(
                final NotQuestsAdapter adapter,
                final String input,
                final PlatformPlayer questPlayer) {
            return adapter.parseItemSelection(input, questPlayer);
        }

        private static String readRest(final ParseCursor cursor, final boolean stopAtNextFlag) {
            final ArrayList<String> values = new ArrayList<>();
            while (!cursor.finished() && (!stopAtNextFlag || !cursor.peekFlag())) {
                values.add(cursor.next());
            }
            return String.join(" ", values);
        }

        private static String required(final ParseCursor cursor, final RegistryField.Definition field) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for condition field '" + field.name() + "'.");
            }
            return cursor.next();
        }

        private static String required(final ParseCursor cursor, final String name) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for condition field '" + name + "'.");
            }
            return cursor.next();
        }

        private static boolean isNumberField(final RegistryField.Definition field) {
            return field.valueType().contains("number") || field.valueType().contains("integer");
        }

        private static boolean isBooleanField(final RegistryField.Definition field) {
            return field.valueType().contains("boolean") || field.valueType().contains("presence");
        }

        private static boolean isVariableFlag(final RegistryField.Definition field) {
            return field.flag() || field.presenceFlag();
        }

        private static RegistryField.Definition flag(final Conditions.Type conditionType, final String name) {
            for (final RegistryField.Definition flag : conditionType.flags()) {
                if (flag.name().equalsIgnoreCase(name)) {
                    return flag;
                }
            }
            return null;
        }

        private static RegistryField.Definition namedField(
                final List<RegistryField.Definition> fields,
                final String name) {
            for (final RegistryField.Definition field : fields) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            return null;
        }

        private static List<String> tokensBeforeFlags(final List<String> tokens) {
            final ArrayList<String> values = new ArrayList<>();
            for (final String token : tokens) {
                if (token.startsWith("--")) {
                    break;
                }
                values.add(token);
            }
            return List.copyOf(values);
        }

        private static final class ParseCursor {
            private final List<String> tokens;
            private int index;

            private ParseCursor(final List<String> tokens) {
                this.tokens = tokens;
            }

            private boolean finished() {
                return index >= tokens.size();
            }

            private boolean peekFlag() {
                return !finished() && tokens.get(index).startsWith("--");
            }

            private String next() {
                return tokens.get(index++);
            }
        }
    }

    /** Owns objective registration, runtime events, objective data, and command parsing. */
    public static final class Objectives {
        public static final class Kinds {
            public static final String OBJECTIVE_GROUP = "Objective";
            public static final String OBJECTIVE_GROUP_HOLDER_NAME = "objectiveHolderName";
            public static final String OTHER_QUEST = "OtherQuest";
            public static final String OTHER_QUEST_TARGET = "otherQuest";

            private Kinds() {}

            public static boolean isObjectiveGroup(final String typeId) {
                return OBJECTIVE_GROUP.equals(typeId);
            }

            public static boolean isOtherQuest(final String typeId) {
                return OTHER_QUEST.equals(typeId);
            }

            public static String objectiveGroupHolderName(
                    final String typeId,
                    final Function<String, String> value) {
                if (!isObjectiveGroup(typeId) || value == null) {
                    return "";
                }
                final String holderName = value.apply(OBJECTIVE_GROUP_HOLDER_NAME);
                return holderName == null ? "" : holderName;
            }

            public static boolean otherQuestTargets(
                    final String typeId,
                    final Function<String, String> value,
                    final String questIdentifier) {
                if (!isOtherQuest(typeId) || value == null || questIdentifier == null) {
                    return false;
                }
                final String target = value.apply(OTHER_QUEST_TARGET);
                return target != null && target.equalsIgnoreCase(questIdentifier);
            }
        }

        private static final String ADDITIONAL_STRINGS = "additionalStrings";
        private static final String ADDITIONAL_NUMBERS = "additionalNumbers";
        private static final String ADDITIONAL_BOOLEANS = "additionalBooleans";
        public record Type(
                String id,
                String displayName,
                String description,
                List<RegistryField.Definition> fields,
                List<RegistryField.Definition> flags,
                Variables.Command variableCommand,
                TaskDescriptionRenderer taskDescriptionRenderer,
                ObjectiveLifecycleHandler unlockHandler,
                ObjectiveLoadHandler loadHandler,
                ObjectiveCompleteOrLockHandler completeOrLockHandler,
                ObjectiveRefreshHandler refreshHandler,
                ObjectiveEventHandler jumpHandler,
                ObjectiveEventHandler startSneakHandler,
                DeathObjectiveEventHandler deathHandler,
                BlockObjectiveEventHandler breakBlockHandler,
                BlockObjectiveEventHandler placeBlockHandler,
                HarvestBlockObjectiveEventHandler harvestBlockHandler,
                ItemObjectiveEventHandler pickupItemHandler,
                ItemObjectiveEventHandler dropItemHandler,
                ItemObjectiveEventHandler takeBrewedItemHandler,
                ItemObjectiveEventHandler takeSmithingResultHandler,
                EntityObjectiveEventHandler killEntityHandler,
                InteractionObjectiveEventHandler interactBlockHandler,
                MoveObjectiveEventHandler moveHandler,
                ProjectileHitObjectiveEventHandler projectileHitHandler,
                EntityObjectiveEventHandler breedEntityHandler,
                EntityObjectiveEventHandler feedEntityHandler,
                EntityObjectiveEventHandler tameEntityHandler,
                ItemObjectiveEventHandler consumeItemHandler,
                ItemObjectiveEventHandler fishItemHandler,
                ItemObjectiveEventHandler craftItemHandler,
                ItemObjectiveEventHandler takeSmeltedItemHandler,
                ItemObjectiveEventHandler tradeItemHandler,
                ShearSheepObjectiveEventHandler shearSheepHandler,
                MilkCowObjectiveEventHandler milkCowHandler,
                ObjectiveEventHandler openBuriedTreasureHandler,
                CommandObjectiveEventHandler runCommandHandler,
                EnchantObjectiveEventHandler enchantItemHandler,
                NpcInteractionObjectiveEventHandler npcInteractionHandler) {}

        public interface Data {
            void copyTo(Draft objective);

            default Object value(final String name) {
                return null;
            }

            default <V> V value(final String name, final Class<V> type) {
                final Object value = value(name);
                return type.isInstance(value) ? type.cast(value) : null;
            }

            String text(String name);

            boolean flag(String name);

            default int integer(final String name, final int fallback) {
                return fallback;
            }

            default double number(final String name, final double fallback) {
                return fallback;
            }

            default NQLocation location(final String name) {
                return null;
            }

            ItemSelection itemSelection(String name);
        }

        public interface Draft {
            void setValue(String name, Object value);
        }

        public interface Progress extends Data {
            double currentProgress();

            double progressNeeded();

            int childObjectiveCount();

            PlatformPlayer questPlayer();

            void setProgress(double progress, boolean capAtZero);

            void addProgress(double amount);

            void removeProgress(double amount, boolean capAtZero);
        }

        @FunctionalInterface
        public interface Registry {
            Builder objective(String identifier);
        }

        public interface Builder {
            Builder displayName(String displayName);

            Builder description(String description);

            <V> Builder field(String name, RegistryField<V> type, String description);

            <V> Builder flag(String name, RegistryField<V> type, String description);

            Builder variableCommands(
                    VariableDataType variableType,
                    String variableNameField,
                    String operatorField,
                    List<String> operators,
                    String expressionField,
                    RegistryField<?> expressionType,
                    String operatorDescription,
                    String expressionDescription);

            Builder taskDescription(TaskDescriptionRenderer renderer);

            Builder onUnlock(ObjectiveLifecycleHandler handler);

            Builder afterLoad(ObjectiveLoadHandler handler);

            Builder onCompleteOrLock(ObjectiveCompleteOrLockHandler handler);

            Builder onRefresh(ObjectiveRefreshHandler handler);

            Builder onPlayerJump(ObjectiveEventHandler handler);

            Builder onPlayerStartSneak(ObjectiveEventHandler handler);

            Builder onPlayerDeath(DeathObjectiveEventHandler handler);

            Builder onPlayerBreakBlock(BlockObjectiveEventHandler handler);

            Builder onPlayerPlaceBlock(BlockObjectiveEventHandler handler);

            Builder onPlayerHarvestBlock(HarvestBlockObjectiveEventHandler handler);

            Builder onPlayerPickupItem(ItemObjectiveEventHandler handler);

            Builder onPlayerDropItem(ItemObjectiveEventHandler handler);

            Builder onPlayerTakeBrewedItem(ItemObjectiveEventHandler handler);

            Builder onPlayerTakeSmithingResult(ItemObjectiveEventHandler handler);

            Builder onPlayerKillEntity(EntityObjectiveEventHandler handler);

            Builder onPlayerInteractBlock(InteractionObjectiveEventHandler handler);

            Builder onPlayerMove(MoveObjectiveEventHandler handler);

            Builder onPlayerShootProjectileHit(ProjectileHitObjectiveEventHandler handler);

            Builder onPlayerBreedEntity(EntityObjectiveEventHandler handler);

            Builder onPlayerFeedEntity(EntityObjectiveEventHandler handler);

            Builder onPlayerTameEntity(EntityObjectiveEventHandler handler);

            Builder onPlayerConsumeItem(ItemObjectiveEventHandler handler);

            Builder onPlayerFishItem(ItemObjectiveEventHandler handler);

            Builder onPlayerCraftItem(ItemObjectiveEventHandler handler);

            Builder onPlayerTakeSmeltedItem(ItemObjectiveEventHandler handler);

            Builder onPlayerTradeItem(ItemObjectiveEventHandler handler);

            Builder onPlayerShearSheep(ShearSheepObjectiveEventHandler handler);

            Builder onPlayerMilkCow(MilkCowObjectiveEventHandler handler);

            Builder onPlayerOpenBuriedTreasure(ObjectiveEventHandler handler);

            Builder onPlayerRunCommand(CommandObjectiveEventHandler handler);

            Builder onPlayerEnchantItem(EnchantObjectiveEventHandler handler);

            Builder onPlayerInteractNpc(NpcInteractionObjectiveEventHandler handler);

            void register();
        }

        @FunctionalInterface
        public interface TaskDescriptionRenderer {
            String render(Data objective, PlatformPlayer questPlayer, ActiveObjective activeObjective);
        }

        @FunctionalInterface
        public interface ObjectiveLifecycleHandler {
            void handle(Progress objective, PlatformPlayer questPlayer, boolean loading);
        }

        @FunctionalInterface
        public interface ObjectiveLoadHandler {
            void handle(Data objective);
        }

        @FunctionalInterface
        public interface ObjectiveCompleteOrLockHandler {
            void handle(Progress objective, PlatformPlayer questPlayer, boolean loading, boolean completed);
        }

        @FunctionalInterface
        public interface ObjectiveRefreshHandler {
            void handle(ObjectiveRefresh refresh, Progress objective);
        }

        public record ObjectiveRefresh(
                boolean variableValueChanged,
                String changedVariableName,
                boolean questCompleted,
                String completedQuestName) {
            public static ObjectiveRefresh periodic() {
                return new ObjectiveRefresh(false, "", false, "");
            }

            public static ObjectiveRefresh variableValueChanged(final String variableName) {
                return new ObjectiveRefresh(true, variableName == null ? "" : variableName, false, "");
            }

            public static ObjectiveRefresh questCompleted(final String questName) {
                return new ObjectiveRefresh(false, "", true, questName == null ? "" : questName);
            }

            public boolean matchesVariable(final String variableName) {
                return changedVariableName.equalsIgnoreCase(variableName == null ? "" : variableName);
            }

            public boolean matchesCompletedQuest(final String questName) {
                return completedQuestName.equalsIgnoreCase(questName == null ? "" : questName);
            }
        }

        @FunctionalInterface
        public interface ObjectiveEventHandler {
            void handle(Progress objective);
        }

        @FunctionalInterface
        public interface BlockObjectiveEventHandler {
            void handle(BlockEvent event, Progress objective);
        }

        public interface BlockEvent {
            String materialId();

            boolean matches(ItemSelection selection);
        }

        @FunctionalInterface
        public interface ItemObjectiveEventHandler {
            void handle(ItemEvent event, Progress objective);
        }

        public interface ItemEvent {
            String materialId();

            int amount();

            boolean matches(ItemSelection selection);
        }

        @FunctionalInterface
        public interface HarvestBlockObjectiveEventHandler {
            void handle(HarvestBlockEvent event, Progress objective);
        }

        public interface HarvestBlockEvent extends BlockEvent {
            boolean fullyGrownHarvestable();

            boolean playerPlaced();
        }

        @FunctionalInterface
        public interface EntityObjectiveEventHandler {
            void handle(EntityEvent event, Progress objective);
        }

        public interface EntityEvent {
            String entityTypeId();

            String plainCustomName();

            boolean selfAttributedDeath();
        }

        @FunctionalInterface
        public interface InteractionObjectiveEventHandler {
            void handle(InteractionEvent event, Progress objective);
        }

        public interface InteractionEvent {
            NQLocation location();

            boolean leftClick();

            boolean rightClick();

            void cancel();
        }

        @FunctionalInterface
        public interface ShearSheepObjectiveEventHandler {
            void handle(ShearSheepEvent event, Progress objective);
        }

        public interface ShearSheepEvent {
            void cancel();
        }

        @FunctionalInterface
        public interface MilkCowObjectiveEventHandler {
            void handle(MilkCowEvent event, Progress objective);
        }

        public interface MilkCowEvent {
            void cancel();
        }

        @FunctionalInterface
        public interface MoveObjectiveEventHandler {
            void handle(MoveEvent event, Progress objective);
        }

        public interface MoveEvent {
            NQLocation to();
        }

        @FunctionalInterface
        public interface ProjectileHitObjectiveEventHandler {
            void handle(ProjectileHitEvent event, Progress objective);
        }

        public interface ProjectileHitEvent {
            NQLocation location();
        }

        @FunctionalInterface
        public interface CommandObjectiveEventHandler {
            void handle(CommandEvent event, Progress objective);
        }

        public interface CommandEvent {
            String command();

            void cancel();
        }

        @FunctionalInterface
        public interface EnchantObjectiveEventHandler {
            void handle(EnchantEvent event, Progress objective);
        }

        public interface EnchantEvent extends ItemEvent {
            Map<String, Integer> enchantments();
        }

        @FunctionalInterface
        public interface NpcInteractionObjectiveEventHandler {
            void handle(NpcInteractionEvent event, Progress objective);
        }

        public interface NpcInteractionEvent {
            PlatformPlayer questPlayer();

            String npcSelector();

            String npcName();
        }

        @FunctionalInterface
        public interface DeathObjectiveEventHandler {
            void handle(DeathEvent event, Progress objective);
        }

        public interface DeathEvent {
            String damageTypeId();
        }

        public static Objective parse(
                final NotQuestsAdapter adapter,
                final Objectives.Type objectiveType,
                final String rawInput) {
            return parse(adapter, objectiveType, rawInput, null);
        }

        public static Objective parse(
                final NotQuestsAdapter adapter,
                final Objectives.Type objectiveType,
                final String rawInput,
                final PlatformPlayer questPlayer) {
            final ParseCursor cursor = new ParseCursor(Actions.tokenize(rawInput));
            final Objective data = new Objective(0, objectiveType.id(), null);

            if (objectiveType.variableCommand() != null) {
                readVariableObjective(adapter, objectiveType, objectiveType.variableCommand(), cursor, data, questPlayer);
                applyObjectiveSemantics(objectiveType, data);
                return data;
            }

            for (final RegistryField.Definition field : objectiveType.fields()) {
                if (cursor.finished() || cursor.peekFlag() || field.presenceFlag()) {
                    break;
                }
                data.setValue(field.name(), readValue(adapter, cursor, field, questPlayer));
            }

            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final RegistryField.Definition field = namedField(objectiveType, token.substring(2));
                if (field == null) {
                    continue;
                }
                if (field.presenceFlag()) {
                    data.setValue(field.name(), true);
                } else {
                    data.setValue(field.name(), readValue(adapter, cursor, field, questPlayer));
                }
            }
            applyObjectiveSemantics(objectiveType, data);
            return data;
        }

        private static void readVariableObjective(
                final NotQuestsAdapter adapter,
                final Objectives.Type objectiveType,
                final Variables.Command command,
                final ParseCursor cursor,
                final Objective data,
                final PlatformPlayer questPlayer) {
            final String variableName = required(cursor, command.variableNameField());
            if (adapter.variableType(variableName) != command.variableType()) {
                throw new IllegalArgumentException(
                        "Variable '" + variableName + "' is not a " + command.variableType().name().toLowerCase() + " variable.");
            }
            data.setValue(command.variableNameField(), variableName);

            final LinkedHashMap<String, String> stringArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> numberArguments = new LinkedHashMap<>();
            final LinkedHashMap<String, String> booleanArguments = new LinkedHashMap<>();
            final List<RegistryField.Definition> variableFields = adapter.variableFields(variableName);
            for (final RegistryField.Definition variableField : variableFields) {
                if (isVariableFlag(variableField)) {
                    booleanArguments.put(variableField.name(), "0");
                    continue;
                }
                if (cursor.finished() || cursor.peekFlag()) {
                    throw new IllegalArgumentException("Missing value for variable field '" + variableField.name() + "'.");
                }
                final Object value = readValue(adapter, cursor, variableField, questPlayer);
                final String rawValue = value == null ? "" : value.toString();
                if (isNumberField(variableField)) {
                    numberArguments.put(variableField.name(), rawValue);
                } else if (isBooleanField(variableField)) {
                    booleanArguments.put(variableField.name(), rawValue);
                } else {
                    stringArguments.put(variableField.name(), rawValue);
                }
            }
            data.setValue(command.operatorField(), required(cursor, command.operatorField()));
            data.setValue(command.expressionField(), readValue(adapter, cursor, command.expressionType(), questPlayer));

            while (!cursor.finished()) {
                final String token = cursor.next();
                if (!token.startsWith("--")) {
                    continue;
                }
                final String flagName = token.substring(2);
                final RegistryField.Definition variableFlag = namedField(variableFields, flagName);
                if (variableFlag != null && isVariableFlag(variableFlag)) {
                    booleanArguments.put(variableFlag.name(), "1");
                    continue;
                }
                final RegistryField.Definition objectiveFlag = namedField(objectiveType, flagName);
                if (objectiveFlag == null) {
                    continue;
                }
                if (objectiveFlag.presenceFlag()) {
                    data.setValue(objectiveFlag.name(), true);
                } else {
                    data.setValue(objectiveFlag.name(), readValue(adapter, cursor, objectiveFlag, questPlayer));
                }
            }
            data.setValue(ADDITIONAL_STRINGS, stringArguments);
            data.setValue(ADDITIONAL_NUMBERS, numberArguments);
            data.setValue(ADDITIONAL_BOOLEANS, booleanArguments);
        }

        private static void applyObjectiveSemantics(
                final Objectives.Type objectiveType,
                final Objective data) {
            if (!"NumberVariable".equalsIgnoreCase(objectiveType.id())) {
                return;
            }
            if (!"moreThan".equalsIgnoreCase(data.text("operator"))) {
                return;
            }
            final String amount = data.text("amount");
            if (!amount.isBlank()) {
                data.setValue("amount", addOne(amount));
            }
        }

        private static String addOne(final String expression) {
            try {
                final double value = Double.parseDouble(expression);
                final double increased = value + 1;
                return increased == Math.rint(increased)
                        ? String.valueOf((long) increased)
                        : String.valueOf(increased);
            } catch (final NumberFormatException ignored) {
                return expression + "+1";
            }
        }

        private static Object readValue(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field,
                final PlatformPlayer questPlayer) {
            return switch (field.valueType()) {
                case "greedyText" -> readRest(cursor, true);
                case "command" -> normalizeCommandText(readRest(cursor, true));
                case "integer" -> Integer.parseInt(required(cursor, field));
                case "number", "optionalNumber" -> Double.parseDouble(required(cursor, field));
                case "numberExpression", "numberExpressionToken", "optionalNumberExpression" -> required(cursor, field);
                case "duration" -> UtilManager.parseDuration(required(cursor, field));
                case "location" -> readLocation(adapter, cursor, field);
                case "itemSelection" -> readItemSelection(adapter, required(cursor, field), questPlayer);
                default -> required(cursor, field);
            };
        }

        private static NQLocation readLocation(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field) {
            final String worldName = required(cursor, field);
            if ("none".equalsIgnoreCase(worldName)) {
                return null;
            }
            final double x = Double.parseDouble(required(cursor, field));
            final double y = Double.parseDouble(required(cursor, field));
            final double z = Double.parseDouble(required(cursor, field));
            return adapter.location(worldName, x, y, z);
        }

        private static ItemSelection readItemSelection(
                final NotQuestsAdapter adapter,
                final String input,
                final PlatformPlayer questPlayer) {
            return adapter.parseItemSelection(input, questPlayer);
        }

        private static String readRest(final ParseCursor cursor, final boolean stopAtNextFlag) {
            final ArrayList<String> values = new ArrayList<>();
            while (!cursor.finished() && (!stopAtNextFlag || !cursor.peekFlag())) {
                values.add(cursor.next());
            }
            return String.join(" ", values);
        }

        private static String normalizeCommandText(final String command) {
            if (command == null || command.isBlank()) {
                return "";
            }
            return command.startsWith("/") ? command : "/" + command;
        }

        private static String required(final ParseCursor cursor, final RegistryField.Definition field) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for objective field '" + field.name() + "'.");
            }
            return cursor.next();
        }

        private static String required(final ParseCursor cursor, final String fieldName) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for objective field '" + fieldName + "'.");
            }
            return cursor.next();
        }

        private static RegistryField.Definition namedField(
                final Objectives.Type objectiveType,
                final String name) {
            for (final RegistryField.Definition field : objectiveType.flags()) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            for (final RegistryField.Definition field : objectiveType.fields()) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            return null;
        }

        private static RegistryField.Definition namedField(
                final List<RegistryField.Definition> fields,
                final String name) {
            for (final RegistryField.Definition field : fields) {
                if (field.name().equalsIgnoreCase(name)) {
                    return field;
                }
            }
            return null;
        }

        private static boolean isNumberField(final RegistryField.Definition field) {
            return field.valueType().contains("number") || field.valueType().contains("integer");
        }

        private static boolean isBooleanField(final RegistryField.Definition field) {
            return field.valueType().contains("boolean") || field.valueType().contains("presence");
        }

        private static boolean isVariableFlag(final RegistryField.Definition field) {
            return field.flag() || field.presenceFlag();
        }

        private static final class ParseCursor {
            private final List<String> tokens;
            private int index;

            private ParseCursor(final List<String> tokens) {
                this.tokens = tokens;
            }

            private boolean finished() {
                return index >= tokens.size();
            }

            private boolean peekFlag() {
                return !finished() && tokens.get(index).startsWith("--");
            }

            private String next() {
                return tokens.get(index++);
            }
        }
    }

    /** Owns trigger registration metadata and description rendering. */
    public static final class Triggers {
      public record Type(
          String id,
          String displayName,
          String description,
          List<RegistryField.Definition> fields,
          Function<Description, String> descriptionRenderer) {}

      public interface Description {
        String text(String name);

        int integer(String name, int fallback);
      }

      public interface Data extends Description {
        Object value(String name);

        void copyTo(Draft trigger);
      }

      public interface Draft {
        void setValue(String name, Object value);
      }

      @FunctionalInterface
      public interface Registry {
        Builder trigger(String identifier);
      }

      public interface Builder {
        Builder displayName(String displayName);

        Builder description(String description);

        <V> Builder field(String name, RegistryField<V> type, String description);

        Builder triggerDescription(Function<Description, String> renderer);

        void register();
      }
    }

    /** Owns variable registration, handlers, context, flags, suggestions, and parsing. */
    public static final class Variables {
        public record Command(
                VariableDataType variableType,
                String variableNameField,
                String operatorField,
                List<String> operators,
                String expressionField,
                RegistryField.Definition expressionType,
                String operatorDescription,
                String expressionDescription) {}

        public record Type(
                String id,
                String displayName,
                String description,
                String singular,
                String plural,
                List<RegistryField.Definition> fields,
                Object handler,
                String valueType) {}

        public static final class Context {
            private final PlatformPlayer questPlayer;
            private final Map<String, Object> values;

            public Context(final PlatformPlayer questPlayer, final Map<String, Object> values) {
                this.questPlayer = questPlayer;
                this.values = values == null ? Map.of() : Map.copyOf(values);
            }

            public PlatformPlayer questPlayer() {
                return questPlayer;
            }

            public Object value(final String name) {
                return values.get(name);
            }

            public String text(final String name) {
                final Object value = value(name);
                return value == null ? "" : String.valueOf(value);
            }

            public double number(final String name, final double fallback) {
                final Object value = value(name);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                if (value instanceof String string && !string.isBlank()) {
                    return Double.parseDouble(string);
                }
                return fallback;
            }

            public boolean bool(final String name, final boolean fallback) {
                final Object value = value(name);
                if (value instanceof Boolean bool) {
                    return bool;
                }
                if (value instanceof Number number) {
                    return number.doubleValue() != 0d;
                }
                if (value instanceof String string && !string.isBlank()) {
                    return string.equalsIgnoreCase("true")
                            || string.equalsIgnoreCase("yes")
                            || string.equals("1");
                }
                return fallback;
            }

            public NQLocation location(final String name) {
                final Object value = value(name);
                return value instanceof NQLocation location ? location : null;
            }
        }

        public record Flag(String name, NQDescription description) {
            public Flag {
                name = Objects.requireNonNull(name, "name");
                description = description == null ? NQDescription.EMPTY : description;
            }

            public static Flag of(final String name, final NQDescription description) {
                return new Flag(name, description);
            }
        }

        @FunctionalInterface
        public interface SuggestionProvider extends NQSuggestionProvider<NQCommandContext> {}

        public interface Registry {
            Variables.BooleanVariableBuilder booleanVariable(String identifier);

            Variables.StringVariableBuilder stringVariable(String identifier);

            Variables.NumberVariableBuilder numberVariable(String identifier);

            Variables.ListVariableBuilder listVariable(String identifier);

            Variables.ItemStackListVariableBuilder itemStackListVariable(String identifier);
        }

        public interface BooleanVariableHandler {
            Boolean getValue(PlatformPlayer questPlayer, Object... objects);

            default boolean canSet() {
                return false;
            }

            default boolean setValue(final Boolean newValue, final PlatformPlayer questPlayer, final Object... objects) {
                return false;
            }

            default List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                return List.of("true", "false");
            }
        }

        public interface StringVariableHandler {
            String getValue(PlatformPlayer questPlayer, Object... objects);

            default boolean canSet() {
                return false;
            }

            default boolean setValue(final String newValue, final PlatformPlayer questPlayer, final Object... objects) {
                return false;
            }

            default List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                return List.of();
            }
        }

        public interface NumberVariableHandler {
            Number getValue(PlatformPlayer questPlayer, Object... objects);

            default boolean canSet() {
                return false;
            }

            default boolean setValue(final Number newValue, final PlatformPlayer questPlayer, final Object... objects) {
                return false;
            }

            default List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                return List.of();
            }
        }

        public interface ListVariableHandler {
            List<String> getValue(PlatformPlayer questPlayer, Object... objects);

            default boolean canSet() {
                return false;
            }

            default boolean setValue(final List<String> newValue, final PlatformPlayer questPlayer, final Object... objects) {
                return false;
            }

            default List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                return List.of();
            }
        }

        public interface ItemStackListVariableHandler {
            List<ItemSelection> getValue(PlatformPlayer questPlayer, Object... objects);

            default boolean canSet() {
                return false;
            }

            default boolean setValue(
                    final List<ItemSelection> newValue,
                    final PlatformPlayer questPlayer,
                    final Object... objects) {
                return false;
            }

            default List<String> possibleValues(final PlatformPlayer questPlayer, final Object... objects) {
                return List.of();
            }
        }

        public interface BooleanVariableBuilder {
            Variables.BooleanVariableBuilder displayName(String displayName);
            Variables.BooleanVariableBuilder description(String description);
            Variables.BooleanVariableBuilder singular(String singular);
            Variables.BooleanVariableBuilder plural(String plural);
            <V> Variables.BooleanVariableBuilder field(String name, RegistryField<V> type, String description);
            Variables.BooleanVariableBuilder get(BooleanGetter getter);
            Variables.BooleanVariableBuilder get(BooleanContextGetter getter);
            Variables.BooleanVariableBuilder set(BooleanSetter setter);
            Variables.BooleanVariableBuilder set(BooleanContextSetter setter);
            Variables.BooleanVariableBuilder possibleValues(BooleanPossibleValues possibleValues);
            Variables.BooleanVariableBuilder possibleValues(BooleanContextPossibleValues possibleValues);
            void register();

            @FunctionalInterface interface BooleanGetter { Boolean get(PlatformPlayer questPlayer, Object... objects); }
            @FunctionalInterface interface BooleanContextGetter { Boolean get(Context context); }
            @FunctionalInterface interface BooleanSetter { boolean set(Boolean value, PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface BooleanContextSetter { boolean set(Boolean value, Context context); }
            @FunctionalInterface interface BooleanPossibleValues { List<String> values(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface BooleanContextPossibleValues { List<String> values(Context context); }
        }

        public interface StringVariableBuilder {
            Variables.StringVariableBuilder displayName(String displayName);
            Variables.StringVariableBuilder description(String description);
            Variables.StringVariableBuilder singular(String singular);
            Variables.StringVariableBuilder plural(String plural);
            <V> Variables.StringVariableBuilder field(String name, RegistryField<V> type, String description);
            Variables.StringVariableBuilder get(StringContextGetter getter);
            Variables.StringVariableBuilder get(StringGetter getter);
            Variables.StringVariableBuilder set(StringContextSetter setter);
            Variables.StringVariableBuilder set(StringSetter setter);
            Variables.StringVariableBuilder possibleValues(StringContextPossibleValues possibleValues);
            Variables.StringVariableBuilder possibleValues(StringPossibleValues possibleValues);
            void register();

            @FunctionalInterface interface StringContextGetter { String get(Context context); }
            @FunctionalInterface interface StringGetter { String get(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface StringContextSetter { boolean set(String value, Context context); }
            @FunctionalInterface interface StringSetter { boolean set(String value, PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface StringContextPossibleValues { List<String> values(Context context); }
            @FunctionalInterface interface StringPossibleValues { List<String> values(PlatformPlayer player, Object... objects); }
        }

        public interface NumberVariableBuilder {
            Variables.NumberVariableBuilder displayName(String displayName);
            Variables.NumberVariableBuilder description(String description);
            Variables.NumberVariableBuilder singular(String singular);
            Variables.NumberVariableBuilder plural(String plural);
            <V> Variables.NumberVariableBuilder field(String name, RegistryField<V> type, String description);
            Variables.NumberVariableBuilder get(NumberContextGetter getter);
            Variables.NumberVariableBuilder get(NumberGetter getter);
            Variables.NumberVariableBuilder set(NumberContextSetter setter);
            Variables.NumberVariableBuilder set(NumberSetter setter);
            Variables.NumberVariableBuilder possibleValues(NumberContextPossibleValues possibleValues);
            Variables.NumberVariableBuilder possibleValues(NumberPossibleValues possibleValues);
            void register();

            @FunctionalInterface interface NumberContextGetter { Number get(Context context); }
            @FunctionalInterface interface NumberGetter { Number get(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface NumberContextSetter { boolean set(Number value, Context context); }
            @FunctionalInterface interface NumberSetter { boolean set(Number value, PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface NumberContextPossibleValues { List<String> values(Context context); }
            @FunctionalInterface interface NumberPossibleValues { List<String> values(PlatformPlayer player, Object... objects); }
        }

        public interface ListVariableBuilder {
            Variables.ListVariableBuilder displayName(String displayName);
            Variables.ListVariableBuilder description(String description);
            Variables.ListVariableBuilder singular(String singular);
            Variables.ListVariableBuilder plural(String plural);
            <V> Variables.ListVariableBuilder field(String name, RegistryField<V> type, String description);
            Variables.ListVariableBuilder get(ListContextGetter getter);
            Variables.ListVariableBuilder get(ListGetter getter);
            Variables.ListVariableBuilder set(ListContextSetter setter);
            Variables.ListVariableBuilder set(ListSetter setter);
            Variables.ListVariableBuilder possibleValues(ListContextPossibleValues possibleValues);
            Variables.ListVariableBuilder possibleValues(ListPossibleValues possibleValues);
            void register();

            @FunctionalInterface interface ListGetter { List<String> get(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ListContextGetter { List<String> get(Context context); }
            @FunctionalInterface interface ListSetter { boolean set(List<String> value, PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ListContextSetter { boolean set(List<String> value, Context context); }
            @FunctionalInterface interface ListPossibleValues { List<String> values(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ListContextPossibleValues { List<String> values(Context context); }
        }

        public interface ItemStackListVariableBuilder {
            Variables.ItemStackListVariableBuilder displayName(String displayName);
            Variables.ItemStackListVariableBuilder description(String description);
            Variables.ItemStackListVariableBuilder singular(String singular);
            Variables.ItemStackListVariableBuilder plural(String plural);
            <V> Variables.ItemStackListVariableBuilder field(String name, RegistryField<V> type, String description);
            Variables.ItemStackListVariableBuilder get(ItemStackListGetter getter);
            Variables.ItemStackListVariableBuilder get(ItemStackListContextGetter getter);
            Variables.ItemStackListVariableBuilder set(ItemStackListSetter setter);
            Variables.ItemStackListVariableBuilder set(ItemStackListContextSetter setter);
            Variables.ItemStackListVariableBuilder possibleValues(ItemStackListPossibleValues possibleValues);
            Variables.ItemStackListVariableBuilder possibleValues(ItemStackListContextPossibleValues possibleValues);
            void register();

            @FunctionalInterface interface ItemStackListGetter { List<ItemSelection> get(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ItemStackListContextGetter { List<ItemSelection> get(Context context); }
            @FunctionalInterface interface ItemStackListSetter { boolean set(List<ItemSelection> value, PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ItemStackListContextSetter { boolean set(List<ItemSelection> value, Context context); }
            @FunctionalInterface interface ItemStackListPossibleValues { List<String> values(PlatformPlayer player, Object... objects); }
            @FunctionalInterface interface ItemStackListContextPossibleValues { List<String> values(Context context); }
        }

        public static Object[] parse(
                final NotQuestsAdapter adapter,
                final Variables.Type variable,
                final String rawInput) {
            return parse(adapter, variable, rawInput, null);
        }

        public static Object[] parse(
                final NotQuestsAdapter adapter,
                final Variables.Type variable,
                final String rawInput,
                final PlatformPlayer questPlayer) {
            final ParseCursor cursor = new ParseCursor(Actions.tokenize(rawInput));
            final List<Object> values = new ArrayList<>();
            for (final RegistryField.Definition field : variable.fields()) {
                values.add(readValue(adapter, cursor, field, questPlayer));
            }
            if (!cursor.finished()) {
                throw new IllegalArgumentException("Unexpected extra variable argument: " + cursor.next());
            }
            return values.toArray();
        }

        private static Object readValue(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field,
                final PlatformPlayer questPlayer) {
            return switch (field.valueType()) {
                case "greedyText", "command" -> readRest(cursor);
                case "integer" -> Integer.parseInt(required(cursor, field));
                case "number", "optionalNumber" -> Double.parseDouble(required(cursor, field));
                case "numberExpression", "numberExpressionToken", "optionalNumberExpression" -> required(cursor, field);
                case "duration" -> UtilManager.parseDuration(required(cursor, field));
                case "location" -> readLocation(adapter, cursor, field);
                case "itemSelection" -> readItemSelection(adapter, required(cursor, field), questPlayer);
                default -> required(cursor, field);
            };
        }

        private static NQLocation readLocation(
                final NotQuestsAdapter adapter,
                final ParseCursor cursor,
                final RegistryField.Definition field) {
            final String worldName = required(cursor, field);
            final double x = Double.parseDouble(required(cursor, field));
            final double y = Double.parseDouble(required(cursor, field));
            final double z = Double.parseDouble(required(cursor, field));
            return adapter.location(worldName, x, y, z);
        }

        private static ItemSelection readItemSelection(
                final NotQuestsAdapter adapter,
                final String input,
                final PlatformPlayer questPlayer) {
            return adapter.parseItemSelection(input, questPlayer);
        }

        private static String readRest(final ParseCursor cursor) {
            final ArrayList<String> values = new ArrayList<>();
            while (!cursor.finished()) {
                values.add(cursor.next());
            }
            return String.join(" ", values);
        }

        private static String required(final ParseCursor cursor, final RegistryField.Definition field) {
            if (cursor.finished()) {
                throw new IllegalArgumentException("Missing value for variable field '" + field.name() + "'.");
            }
            return cursor.next();
        }

        private static final class ParseCursor {
            private final List<String> tokens;
            private int index;

            private ParseCursor(final List<String> tokens) {
                this.tokens = tokens;
            }

            private boolean finished() {
                return index >= tokens.size();
            }

            private String next() {
                return tokens.get(index++);
            }
        }
    }
}
