package com.notquests.core.managers;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.commands.PlayerTarget;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.conversation.Speaker;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.metadata.NQMetadataExporter;
import com.notquests.core.metadata.NQMetadataSchema.MetadataIndex;
import com.notquests.core.metadata.NQMetadataSchema.Type;
import com.notquests.core.metadata.NQMetadataSchema.Variable;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.Quest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CommandManager {
    public static final String ARMOR_STAND_EDIT_PERMISSION =
            "notquests.admin.armorstandeditingitems";
    public static final List<String> COMMAND_PLACEHOLDERS = List.of(
            "{PLAYER}",
            "{PLAYERUUID}",
            "{PLAYERX}",
            "{PLAYERY}",
            "{PLAYERZ}",
            "{WORLD}",
            "{QUEST}",
            "{{expression}}");

    /** Permissions exposed to native platform permission registries and their default access. */
    public static Map<String, Boolean> permissionDefaults() {
        final LinkedHashMap<String, Boolean> permissions = new LinkedHashMap<>();
        permissions.put("notquests.use", true);
        permissions.put("notquests.admin", false);
        permissions.put(ARMOR_STAND_EDIT_PERMISSION, false);
        permissions.put("notquests.user.profiles", false);
        return Map.copyOf(permissions);
    }

    private final NotQuestsPlugin plugin;
    private final NotQuestsAdapter adapter;
    private final Supplier<Path> dataFolder;

    public CommandManager(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        this(plugin, adapter, () -> Path.of("."));
    }

    public CommandManager(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final Supplier<Path> dataFolder) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.dataFolder = dataFolder == null ? () -> Path.of(".") : dataFolder;
    }

    public NotQuestsPlugin plugin() {
        return plugin;
    }

    public NotQuestsAdapter adapter() {
        return adapter;
    }

    public boolean openGui(
            final PlatformPlayer player,
            final String guiName,
            final String playerName,
            final GuiContext context) {
        return plugin.openGui(player, guiName, playerName, context);
    }

    public List<String> actionTypeIds() {
        return plugin.registry().actions().stream().map(Actions.Type::id).sorted().toList();
    }

    public List<Actions.Type> actionTypes() {
        return plugin.registry().actions();
    }

    public List<Conditions.Type> conditionTypes() {
        return plugin.registry().conditions();
    }

    public List<Objectives.Type> objectiveTypes() {
        return plugin.registry().objectives();
    }

    public List<Triggers.Type> triggerTypes() {
        return plugin.registry().triggers();
    }

    public List<Variables.Type> variableTypes() {
        return plugin.registry().variables();
    }

    public List<String> registrySummary() {
        return NotQuestsRegistry.summary(plugin.registry());
    }

    public NQCommandSchema.CommandIndex commandIndex(final String pluginVersion) {
        return new NQCommandSchema.CommandIndex(pluginVersion, commandInfos(
                pluginVersion,
                "",
                () -> Path.of(".")));
    }

    public MetadataIndex metadataIndex(
            final String pluginVersion,
            final String minecraftVersion) {
        final NQCommandSchema.CommandIndex commandIndex = commandIndex(pluginVersion);
        return new NQMetadataExporter().metadataIndex(new NQMetadataExporter.RuntimeMetadataSource() {
            @Override
            public String pluginVersion() {
                return pluginVersion;
            }

            @Override
            public String minecraftVersion() {
                return minecraftVersion;
            }

            @Override
            public NQCommandSchema.CommandIndex commandIndex() {
                return commandIndex;
            }

            @Override
            public List<Type> objectives() {
                return NQMetadataExporter.objectives(plugin.registry());
            }

            @Override
            public List<Type> actions() {
                return NQMetadataExporter.actions(plugin.registry());
            }

            @Override
            public List<Type> conditions() {
                return NQMetadataExporter.conditions(plugin.registry());
            }

            @Override
            public List<Type> triggers() {
                return NQMetadataExporter.triggers(plugin.registry());
            }

            @Override
            public List<Variable> variables() {
                return NQMetadataExporter.variables(plugin.registry());
            }
        });
    }

    public List<String> rootSummary(final String rootCommand) {
        final String command = rootCommand == null || rootCommand.isBlank() ? "notquests" : rootCommand;
        final boolean admin = NQCommandSchema.ADMIN.matches(command);
        final String canonicalRoot = admin ? NQCommandSchema.ADMIN.name() : NQCommandSchema.USER.name();
        final LinkedHashMap<String, Boolean> directChildren = new LinkedHashMap<>();
        for (final NQCommandSchema.CommandInfo info : commandInfos("development", "", dataFolder)) {
            if (!info.root().equals(canonicalRoot)) {
                continue;
            }
            if (info.segments().size() < 2) {
                continue;
            }
            final String token = info.segments().get(1).token();
            directChildren.merge(token, info.segments().size() > 2 || !info.flags().isEmpty(), Boolean::logicalOr);
        }

        final ArrayList<String> lines = new ArrayList<>();
        lines.add(admin
                ? "<main>NotQuests <unimportant>- available admin commands:"
                : "<main>NotQuests <unimportant>- available commands:");
        directChildren.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add(rootUsageLine(command, entry.getKey(), entry.getValue())));
        if (lines.size() == 1) {
            lines.add("<main>Use <highlight2>/" + command + " registry</highlight2> to list registered NotQuests type counts.");
        }
        return List.copyOf(lines);
    }

    private static String rootUsageLine(
            final String rootCommand,
            final String childToken,
            final boolean hasMore) {
        final String suggestion = "/" + rootCommand + " " + childToken + (hasMore ? " " : "");
        return "<hover:show_text:'<main>Click to insert this command shape.'>"
                + "<click:suggest_command:'" + miniMessageQuoted(suggestion) + "'>"
                + "<dark_gray>/</dark_gray><highlight2>" + rootCommand + "</highlight2> "
                + "<highlight>" + childToken + "</highlight>"
                + (hasMore ? " <dark_gray>...</dark_gray>" : "")
                + "</click></hover>";
    }

    public String versionMessage(final String version) {
        return versionMessage(version, "");
    }

    public String versionMessage(final String version, final String minecraftVersion) {
        final String nqVersion = blankDefault(version, "development");
        final String module = blankDefault(plugin.platformName(), "Unknown");
        final String serverVersion = blankDefault(minecraftVersion, "unknown");
        final String serverBrand = blankDefault(adapter.serverBrand(), module);
        final String javaVersion = blankDefault(System.getProperty("java.version"), "null");
        final String integrations = enabledIntegrations(false);
        final String plainIntegrations = enabledIntegrations(true);
        final String message = "<main>NotQuests version: <highlight>" + nqVersion
                + "\n<main>NotQuests module: <highlight>" + module
                + "\n<main>Server version: <highlight>" + serverVersion
                + "\n<main>Server Brand: <highlight>" + serverBrand
                + "\n<main>Java version: <highlight>" + javaVersion
                + "\n<main>Enabled integrations: <highlight>" + integrations;
        final String clipboard = "**NotQuests version:** " + nqVersion
                + "\n**NotQuests module:** " + module
                + "\n**Server version:** " + serverVersion
                + "\n**Server Brand:** " + serverBrand
                + "\n**Java version:** " + javaVersion
                + "\n**Enabled integrations:** " + plainIntegrations;
        return "<hover:show_text:'<main>Click to copy this information to your clipboard.'>"
                + "<click:copy_to_clipboard:'" + miniMessageQuoted(clipboard) + "'>"
                + message
                + "</click></hover>";
    }

    private String enabledIntegrations(final boolean plain) {
        final List<NotQuestsAdapter.Integration> integrations = plugin.enabledIntegrations();
        if (integrations == null || integrations.isEmpty()) {
            return "";
        }
        return integrations.stream()
                .map(integration -> plain
                        ? integration.name() + " (" + integration.version() + ")"
                        : integration.name() + " <unimportant>(" + integration.version()
                                + ")</unimportant>")
                .collect(Collectors.joining(
                        plain ? ", " : "<veryUnimportant>,</veryUnimportant> "));
    }

    public String objectivesMessage() {
        return NotQuestsRegistry.objectiveList(plugin.registry());
    }

    public String actionsMessage() {
        return NotQuestsRegistry.actionList(plugin.registry());
    }

    public String conditionsMessage() {
        return NotQuestsRegistry.conditionList(plugin.registry());
    }

    public String variablesMessage() {
        return NotQuestsRegistry.variableList(plugin.registry());
    }

    public String triggersMessage() {
        return NotQuestsRegistry.triggerList(plugin.registry());
    }

    private List<NQCommandSchema.CommandInfo> commandInfos(
            final String pluginVersion,
            final String minecraftVersion,
            final Supplier<Path> dataFolder) {
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        NotQuestsCommands.userCommands(
                        NQCommandSchema.USER.name(),
                        NQCommandSchema.USER.description(),
                        this,
                        NQCommandSchema.USER.aliases().toArray(String[]::new))
                .forEach(tree::register);
        NotQuestsCommands.adminCommands(
                        NQCommandSchema.ADMIN.name(),
                        NQCommandSchema.ADMIN.description(),
                        this,
                        () -> pluginVersion,
                        () -> minecraftVersion,
                        dataFolder,
                        NQCommandSchema.ADMIN.aliases().toArray(String[]::new))
                .forEach(tree::register);
        return tree.commandInfos();
    }

    public List<String> questsMessages() {
        final List<String> names = plugin.questManager().getQuestNames();
        if (names.isEmpty()) {
            return List.of("<unimportant>No NotQuests quests are loaded.");
        }
        final ArrayList<String> messages = new ArrayList<>();
        messages.add("<highlight>All Quests:</highlight>");
        for (int i = 0; i < names.size(); i++) {
            messages.add("<highlight>" + (i + 1) + ".</highlight> <main>" + names.get(i));
        }
        return List.copyOf(messages);
    }

    public List<String> placeholderMessages() {
        return List.of(
                "<highlight>All Placeholders (Case-sensitive):",
                "<highlight>1.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(0) + "</highlight2> <main>- Name of the player",
                "<highlight>2.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(1) + "</highlight2> <main>- UUID of the player",
                "<highlight>3.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(2) + "</highlight2> <main>- X coordinates of the player",
                "<highlight>4.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(3) + "</highlight2> <main>- Y coordinates of the player",
                "<highlight>5.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(4) + "</highlight2> <main>- Z coordinates of the player",
                "<highlight>6.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(5) + "</highlight2> <main>- World name of the player",
                "<highlight>6.</highlight> <highlight2>" + COMMAND_PLACEHOLDERS.get(6) + "</highlight2> <main>- Quest name (if relevant)");
    }

    public List<String> conditionTypeIds() {
        return plugin.registry().conditions().stream().map(Conditions.Type::id).sorted().toList();
    }

    public List<String> objectiveTypeIds() {
        return plugin.registry().objectives().stream().map(Objectives.Type::id).sorted().toList();
    }

    public List<String> variableIds() {
        return plugin.registry().variables().stream().map(Variables.Type::id).sorted().toList();
    }

    public String variableValueType(final String variableName) {
        final Variables.Type variable = plugin.registry().variables().stream()
                .filter(type -> type.id().equalsIgnoreCase(variableName))
                .findFirst()
                .orElse(null);
        return variable == null ? "" : variable.valueType();
    }

    public List<String> savedActionNames() {
        return plugin.savedActionNames();
    }

    public List<String> savedConditionNames() {
        return plugin.savedConditionNames();
    }

    public List<String> activeQuestNames(final PlatformPlayer questPlayer) {
        return plugin.activeQuestNames(questPlayer);
    }

    public List<String> questNames() {
        return plugin.questManager().getQuestNames();
    }

    public List<String> takeableQuestNames() {
        return plugin.questManager().getQuestNames().stream()
                .filter(questName -> {
                    final Quest quest = plugin.questManager().getQuest(questName);
                    return quest != null && quest.isTakeEnabled();
                })
                .toList();
    }

    public List<String> categoryNames() {
        return plugin.questManager().getCategoryNames();
    }

    public List<String> questNamesInCategory(final String categoryName) {
        return plugin.questManager().getQuestNamesInCategory(categoryName);
    }

    public List<String> conversationNames() {
        return plugin.conversationNames();
    }

    public List<String> conversationSpeakerNames(final String conversationName) {
        if (conversationName == null || conversationName.isBlank()) {
            return List.of();
        }
        return plugin.conversationManager().speakers(conversationName).stream()
                .map(Speaker::getSpeakerName)
                .toList();
    }

    public List<String> npcSelectorOptions(
            final boolean allowNone,
            final boolean allowRightClickSelect) {
        return adapter.npcSelectorOptions(allowNone, allowRightClickSelect);
    }

    public List<String> questObjectiveIds(final String questName) {
        return questObjectiveIds(questName, new int[0]);
    }

    public List<String> questObjectiveIds(final String questName, final int[] parentPath) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of();
        }
        final Objective parent = objectiveAt(questName, parentPath);
        if (parentPath != null && parentPath.length > 0 && parent == null) {
            return List.of();
        }
        final List<Objective> objectives = parent == null
                ? quest.getObjectives()
                : parent.getObjectives();
        return objectives.stream()
                .map(objective -> String.valueOf(objective.id()))
                .toList();
    }

    public List<String> questRequirementIds(final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of();
        }
        return quest.getRequirements().stream()
                .map(requirement -> String.valueOf(requirement.id()))
                .toList();
    }

    public List<String> questRewardIds(final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of();
        }
        return quest.getRewards().stream()
                .map(reward -> String.valueOf(reward.id()))
                .toList();
    }

    public List<String> questTriggerIds(final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of();
        }
        return quest.getTriggers().stream()
                .map(trigger -> String.valueOf(trigger.id()))
                .toList();
    }

    public List<String> tagNames() {
        return plugin.tagNames();
    }

    public List<String> triggerCommandNames() {
        return plugin.triggerCommandNames();
    }

    public List<String> itemSelectionOptions() {
        return adapter.itemSelectionOptions();
    }

    public List<String> savedItemNames() {
        return plugin.savedItemNames();
    }

    public List<String> entityTypeIds() {
        return adapter.entityTypeIds();
    }

    public List<String> enchantmentIds() {
        return adapter.enchantmentIds();
    }

    public List<String> onlinePlayerNames() {
        return adapter.onlinePlayerNames();
    }

    public List<String> worldNames() {
        return adapter.worldNames();
    }

    public boolean supportsWorldEditSelection() {
        return adapter.supportsWorldEditSelection();
    }

    public LocationRegion worldEditSelection(final PlatformPlayer questPlayer) {
        return adapter.worldEditSelection(questPlayer);
    }

    public boolean userCommandGuiEnabled() {
        return plugin.configuration().userCommandGuiEnabled();
    }

    public boolean debugEnabled() {
        return plugin.configuration().debugEnabled();
    }

    private Actions.Type actionType(final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private Conditions.Type conditionType(final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private Objectives.Type objectiveType(final String id) {
        return plugin.registry().objectives().stream()
                .filter(objective -> objective.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String stripMiniMessage(final String message) {
        return message == null ? "" : message.replaceAll("<[^>]+>", "");
    }

    private static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sanitizeFileName(final String value) {
        return value == null ? "" : value.replaceAll("[^0-9a-zA-Z-._]", "_");
    }

    private static String miniMessageQuoted(final String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String playerDisplayName(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return "";
        }
        return questPlayer.playerName() == null || questPlayer.playerName().isBlank()
                ? questPlayer.playerIdentifier()
                : questPlayer.playerName();
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }

    private static String unimportant(final Object value) {
        return "<unimportant>" + value + "</unimportant>";
    }

    private static CommandMessage missingQuest(final String questName) {
        return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist!");
    }

    private static CommandMessage missingCategory(final String categoryName) {
        return CommandMessage.error("<error>No Category found: " + categoryName);
    }

    private static String formatProgress(final double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private PlatformPlayer targetPlayer(final String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        final PlatformPlayer player = adapter.onlineQuestPlayer(playerName);
        return player != null && player.hasPlayer() ? player : null;
    }

    private PlayerTarget playerTarget(final String playerName) {
        final String lookup = playerName == null || playerName.isBlank() ? "unknown" : playerName;
        final PlatformPlayer online = targetPlayer(lookup);
        final String identifier = online == null || online.playerIdentifier() == null || online.playerIdentifier().isBlank()
                ? lookup
                : online.playerIdentifier();
        final String displayName = playerDisplayName(online).isBlank() ? lookup : playerDisplayName(online);
        return new PlayerTarget(
                lookup,
                plugin.activeQuestPlayer(identifier),
                online,
                identifier,
                displayName);
    }

    private String playerOnlineStatus(final String playerName) {
        return playerTarget(playerName).onlineStatus();
    }

    private Objective objectiveAt(
            final String questName,
            final int[] objectivePath) {
        if (objectivePath == null || objectivePath.length == 0) {
            return null;
        }
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return null;
        }
        Objective current = quest.getObjectiveFromID(objectivePath[0]);
        for (int i = 1; i < objectivePath.length && current != null; i++) {
            current = current.getObjectiveFromID(objectivePath[i]);
        }
        return current;
    }

    private static int[] parseObjectivePath(final String objectivePath) {
        if (objectivePath == null || objectivePath.isBlank()) {
            return new int[0];
        }
        final String[] tokens = objectivePath.split("\\.");
        final int[] parsed = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                parsed[i] = Integer.parseInt(tokens[i]);
            } catch (final NumberFormatException ignored) {
                return new int[0];
            }
        }
        return parsed;
    }

    private static String holderPath(final String questName, final int[] objectivePath) {
        if (questName == null || questName.isBlank() || objectivePath == null || objectivePath.length <= 1) {
            return questName == null ? "" : questName;
        }
        final StringBuilder builder = new StringBuilder(questName);
        for (int i = 0; i < objectivePath.length - 1; i++) {
            builder.append('.').append(objectivePath[i]);
        }
        return builder.toString();
    }

    private double progressNeeded(final Objective objective) {
        if (objective == null) {
            return 1;
        }
        final Objectives.Type type = objectiveType(objective.typeId());
        if (type != null) {
            for (final RegistryField.Definition field : type.fields()) {
                if (field.progressNeeded()) {
                    final double fieldValue = progressNeededValue(objective.data().value(field.name()));
                    if (fieldValue > 0) {
                        return fieldValue;
                    }
                }
            }
        }
        return 1;
    }

    private static double progressNeededValue(final Object amount) {
        if (amount instanceof Number number) {
            return number.doubleValue();
        }
        if (amount != null) {
            try {
                return Double.parseDouble(amount.toString());
            } catch (final NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

}
