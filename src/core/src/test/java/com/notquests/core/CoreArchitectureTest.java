package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class CoreArchitectureTest {
    private static final List<String> FORBIDDEN_PLATFORM_IMPORTS =
            List.of(
                    "org.bukkit.",
                    "io.papermc.",
                    "net.minecraft.",
                    "net.neoforged.",
                    "net.citizensnpcs.",
                    "de.oliver.",
                    "net.milkbowl.",
                    "me.clip.",
                    "com.sk89q.",
                    "com.gamingmesh.",
                    "io.lumine.",
                    "com.magmaguy.",
                    "com.notquests.paper.",
                    "com.notquests.neoforge.");

    @Test
    void platformPlayerCapabilitiesHaveNoHiddenDefaults() {
        final List<String> defaultMethods = java.util.stream.Stream.of(
                        NotQuestsPlatform.class,
                        PlatformPlayer.class,
                        NotQuestsAdapter.class,
                        ItemSelection.class,
                        Objectives.Progress.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(java.lang.reflect.Method::isDefault)
                .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName())
                .sorted()
                .toList();

        assertTrue(
                defaultMethods.isEmpty(),
                () -> "Every raw platform capability must be implemented explicitly by each adapter: "
                        + defaultMethods);
    }

    @Test
    void coreDoesNotDependOnPaperOrBukkit() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(CoreArchitectureTest::containsForbiddenPlatformImport)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "core must stay platform-neutral, but these files import platform code: " + offenders);
    }

    @Test
    void domainVocabularyLivesInStructsAndPlatformPlayerIsOnlyACapability() throws IOException {
        final Path source = Path.of("src/main/java/com/notquests/core");
        final List<String> structs = List.of(
                "Quest.java",
                "QuestPlayer.java",
                "ActiveObjective.java",
                "ActiveObjectives.java",
                "Category.java",
                "PredefinedProgressOrder.java");

        assertTrue(
                structs.stream().allMatch(file -> Files.isRegularFile(source.resolve("structs").resolve(file))),
                "Core quest/player/objective domain concepts must remain discoverable together in core.structs");
        assertTrue(
                structs.stream().noneMatch(file -> Files.exists(source.resolve("quests").resolve(file))),
                "The old split core.quests domain locations must not return");
        assertTrue(
                Files.isRegularFile(source.resolve("platform/PlatformPlayer.java"))
                        && !Files.exists(source.resolve("registry/runtime/PlatformPlayer.java"))
                        && !Files.exists(source.resolve("registry/runtime/QuestPlayer.java")),
                "PlatformPlayer is the adapter capability; QuestPlayer is core persisted domain state");

        final String domainPlayer = Files.readString(source.resolve("structs/QuestPlayer.java"));
        final String platformPlayer = Files.readString(source.resolve("platform/PlatformPlayer.java"));
        assertTrue(
                domainPlayer.contains("public final class QuestPlayer")
                        && !domainPlayer.contains("implements PlatformPlayer")
                        && platformPlayer.contains("public interface PlatformPlayer"),
                "Domain QuestPlayer state and the platform player capability must stay separate");
    }

    @Test
    void concreteDomainObjectsAndRegistryRemainTheConceptOwners() throws IOException {
        final Path core = Path.of("src/main/java/com/notquests/core");
        final String quest = Files.readString(core.resolve("structs/Quest.java"));
        final String questPlayer = Files.readString(core.resolve("structs/QuestPlayer.java"));
        final String activeQuest = Files.readString(core.resolve("structs/ActiveQuest.java"));
        final String activeObjectives = Files.readString(core.resolve("structs/ActiveObjectives.java"));
        final String registry = Files.readString(core.resolve("registry/NotQuestsRegistry.java"));

        assertTrue(
                List.of(
                                core.resolve("actions/Action.java"),
                                core.resolve("conditions/Condition.java"),
                                core.resolve("objectives/Objective.java"),
                                core.resolve("triggers/Trigger.java"))
                        .stream()
                        .allMatch(Files::isRegularFile),
                "Quest entries must be concrete domain concepts");
        assertTrue(
                !quest.contains("ConfiguredEntry")
                        && quest.contains("List<Action>")
                        && quest.contains("List<Condition>")
                        && quest.contains("List<Objective>")
                        && quest.contains("List<Trigger>"),
                "Quest must own concrete typed entries instead of one catch-all entry shape");
        assertTrue(
                !registry.contains("class Values")
                        && !registry.contains("record Values")
                        && !activeObjectives.contains("final Map<String, Object> values"),
                "Concrete domain objects must hide map storage from registry and runtime APIs");
        assertTrue(
                questPlayer.contains("List<ActiveQuest>")
                        && activeQuest.contains("List<ActiveObjective>")
                        && !Files.exists(core.resolve("commands/CommandQuestPlayer.java")),
                "Core QuestPlayer must own ActiveQuest/objective state without a fake command player");

        assertTrue(
                List.of("Actions", "Conditions", "Objectives", "Triggers", "Variables")
                        .stream()
                        .allMatch(name -> registry.contains("public static final class " + name)),
                "NotQuestsRegistry must directly contain every registered type family");
        assertTrue(
                List.of(
                                core.resolve("actions/ActionCatalog.java"),
                                core.resolve("conditions/ConditionCatalog.java"),
                                core.resolve("objectives/ObjectiveCatalog.java"),
                                core.resolve("triggers/TriggerCatalog.java"),
                                core.resolve("variables/VariableCatalog.java"))
                        .stream()
                        .noneMatch(Files::exists),
                "Registration must not be split back into separate catalog files");
        assertTrue(
                !Files.exists(core.resolve("registry/actions"))
                        && !Files.exists(core.resolve("registry/conditions"))
                        && !Files.exists(core.resolve("registry/objectives"))
                        && !Files.exists(core.resolve("registry/triggers"))
                        && !Files.exists(core.resolve("registry/variables")),
                "The fragmented registry type hierarchies must not return");
    }

    @Test
    void familiarCommandFrameworkNamesStayConsolidated() {
        final Path framework = Path.of("src/main/java/com/notquests/core/commands/framework");
        assertTrue(
                List.of(
                                "NQArgumentType.java",
                                "NQCommandBuilder.java",
                                "NQCommandContext.java",
                                "NQCommandSchema.java",
                                "NQFlag.java",
                                "NQFlags.java",
                                "NQSuggestionProvider.java")
                        .stream()
                        .allMatch(file -> Files.isRegularFile(framework.resolve(file))),
                "The command framework should retain the familiar NQ names");
        assertTrue(
                List.of(
                                "CommandArgumentInfo.java",
                                "CommandFlagInfo.java",
                                "CommandNodeInfo.java",
                                "CommandRoot.java",
                                "CommandRoots.java",
                                "CommandSuggestionResolver.java")
                        .stream()
                        .noneMatch(file -> Files.exists(framework.resolve(file))),
                "Tiny command schema fragments must remain folded into their owners");
    }

    @Test
    void smallCoreConceptsStayWithTheirRealOwners() throws IOException {
        final Path core = Path.of("src/main/java/com/notquests/core");
        final String plugin = Files.readString(core.resolve("NotQuestsPlugin.java"));
        final String migrations = Files.readString(core.resolve("migrations/ConfigurationMigrations.java"));
        final String npcAttachments = Files.readString(core.resolve("npc/NpcAttachments.java"));
        final String paper = Files.readString(
                Path.of("../paper/src/main/java/com/notquests/paper/NotQuests.java"));

        assertTrue(
                !Files.exists(core.resolve("NotQuestsMainAbstract.java"))
                        && !Files.exists(core.resolve("PluginStatus.java"))
                        && !Files.exists(core.resolve("PluginDisableReason.java"))
                        && plugin.contains("public static final class PluginStatus")
                        && plugin.contains("static final class DisableReason"),
                "Lifecycle and disable bookkeeping belong inside NotQuestsPlugin");
        assertTrue(
                paper.contains("class NotQuests implements NotQuestsPlatform")
                        && !paper.contains("NotQuestsMainAbstract"),
                "Paper should directly declare its parse/send leaves without a three-method abstract base");
        assertTrue(
                Files.isRegularFile(core.resolve("managers/tags/TagManager.java"))
                        && Files.isRegularFile(core.resolve("managers/tags/TagType.java"))
                        && !Files.exists(core.resolve("tags/Tags.java")),
                "Tag definitions, values, and persistence belong to TagManager");
        assertTrue(
                Files.isRegularFile(core.resolve("items/SavedItem.java"))
                        && Files.isRegularFile(core.resolve("items/ItemStackSelection.java"))
                        && Files.isRegularFile(core.resolve("items/SavedItems.java"))
                        && !Files.exists(core.resolve("items/NQItem.java"))
                        && !Files.exists(core.resolve("items/StoredItemSelection.java")),
                "Item concepts should retain the familiar SavedItem and ItemStackSelection names");
        assertTrue(
                migrations.contains("public interface Migration<C>")
                        && !Files.exists(core.resolve("migrations/CoreConfigMigrationManager.java"))
                        && !Files.exists(core.resolve("migrations/ConfigMigration.java")),
                "ConfigurationMigrations should own its tiny migration contract");
        assertTrue(
                npcAttachments.contains("public interface Npc")
                        && !Files.exists(core.resolve("npc/NpcInfo.java")),
                "The NPC shape used only by attachments belongs inside NpcAttachments");
    }

    @Test
    void dataManagerOwnsCorePersistenceWithoutAnInterfaceLayer() throws IOException {
        final Path core = Path.of("src/main/java/com/notquests/core");
        final Path dataManager = core.resolve("managers/DataManager.java");
        final Path playerDatabase = core.resolve("managers/PlayerDatabase.java");

        assertTrue(Files.isRegularFile(dataManager), "Configured and YAML runtime data belong to DataManager");
        assertTrue(Files.isRegularFile(playerDatabase), "Player SQL storage belongs to PlayerDatabase");
        assertTrue(
                !Files.exists(core.resolve("persistence/CoreYamlPersistence.java"))
                        && !Files.exists(core.resolve("persistence/NotQuestsPersistence.java"))
                        && !Files.exists(core.resolve("persistence/PlayerDatabase.java")),
                "The split persistence interface/implementation package must not return");

        final String source = Files.readString(dataManager);
        assertTrue(
                source.contains("public class DataManager")
                        && source.contains("public enum ReloadTarget")
                        && source.contains("public DataManager()")
                        && source.contains("public boolean saveConfiguredData()")
                        && source.contains("public boolean loadPlayerRuntime("),
                "DataManager must directly own configured and runtime YAML persistence, including its no-op state");
    }

    @Test
    void configurationAloneOwnsGeneralYamlAndLoadedRuntimeSettings() throws IOException {
        final Path core = Path.of("src/main/java/com/notquests/core");
        final Path configuration = core.resolve("managers/ConfigurationManager.java");

        assertTrue(Files.isRegularFile(configuration),
                "general.yml and runtime settings must have one familiar core ConfigurationManager owner");
        assertTrue(
                !Files.exists(core.resolve("NotQuestsSettings.java"))
                        && !Files.exists(core.resolve("config/GeneralConfig.java")),
                "Split settings/general-config owners and compatibility shims must not return");

        final String source = Files.readString(configuration);
        assertTrue(
                source.contains("public final class ConfigurationManager")
                        && source.contains("public Loaded open(")
                        && source.contains("public Shared load(")
                        && source.contains("public boolean save(")
                        && source.contains("public void loadFrom(")
                        && source.contains("public static Defaults ensure(")
                        && source.contains("public static Versions ensureVersion(")
                        && source.contains("copyMissingGeneral(")
                        && source.contains("setComments("),
                "ConfigurationManager must own the general.yml file, defaults/comments, versions, and loaded accessors");

        final List<String> forbiddenAdapterParsing = List.of(
                "ConfigurationManager.ensure(",
                "ConfigurationManager.ensureVersion(",
                "ConfigurationManager.packetMagic(",
                "configuration().loadFrom(",
                "loadGeneralConfig(",
                "readSharedGeneralConfig(",
                "loadSharedSettings(",
                "resolve(\"general.yml\")");
        final List<String> offenders;
        try (var paper = Files.walk(Path.of("../paper/src/main/java"));
                var neoforge = Files.walk(Path.of("../neoforge/src/main/java"))) {
            offenders = java.util.stream.Stream.concat(paper, neoforge)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbiddenAdapterParsing))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }
        assertTrue(
                offenders.isEmpty(),
                () -> "Platform adapters may consume loaded values but must not parse or orchestrate general.yml: "
                        + offenders);
    }

    @Test
    void releasedSixThreeConversionIsOneMigrationNotRuntimeMaintenance() throws IOException {
        final Path core = Path.of("src/main/java/com/notquests/core");
        final Path migrationFolder = core.resolve("migrations/v6_3_0");
        final List<Path> migrations;
        try (var files = Files.list(migrationFolder)) {
            migrations = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        assertTrue(
                migrations.size() == 1
                        && migrations.getFirst().getFileName().toString().equals("Version630Migration.java")
                        && !Files.exists(core.resolve("migrations/v7_0_0")),
                "All released-6.3 conversion belongs to one version-gated migration owner");

        final String database = Files.readString(core.resolve("managers/PlayerDatabase.java"));
        final String configuration = Files.readString(core.resolve("managers/ConfigurationManager.java"));
        final String dataManager = Files.readString(core.resolve("managers/DataManager.java"));
        final String conversations = Files.readString(core.resolve("conversation/ConversationManager.java"));
        final String itemSelections = Files.readString(core.resolve("items/ItemStackSelection.java"));
        final String plugin = Files.readString(core.resolve("NotQuestsPlugin.java"));
        final String paperCodec = Files.readString(Path.of(
                "../paper/src/main/java/com/notquests/paper/adapter/config/BukkitConfigurationValueCodec.java"));
        final String paperMain = Files.readString(Path.of(
                "../paper/src/main/java/com/notquests/paper/NotQuests.java"));

        assertTrue(
                !database.contains("ALTER TABLE")
                        && !database.contains("FOR_MIGRATION")
                        && !configuration.contains("legacyPlayerRuntime")
                        && !configuration.contains("firstString(")
                        && !plugin.contains("legacyPlayerRuntime")
                        && !dataManager.contains("map.get(\"data\")")
                        && !conversations.contains("PluginNPC")
                        && !conversations.contains("value(root, \"npcID\")")
                        && !conversations.contains("value(root, \"npcIDs\")")
                        && !itemSelections.contains("map.values()"),
                "Normal database/config startup must only understand the current schema");
        assertTrue(
                !paperCodec.contains("isLegacyLocation")
                        && !paperCodec.contains("serializedBytes")
                        && !paperMain.contains("legacyConfigured"),
                "Paper runtime codecs must only decode canonical v7 values");

        final Path events = Path.of("../paper/src/main/java/com/notquests/paper/events/notquests");
        final List<Path> deprecatedEvents;
        try (var files = Files.walk(events)) {
            deprecatedEvents = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("@Deprecated");
                        } catch (final IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    })
                    .toList();
        }
        assertTrue(deprecatedEvents.isEmpty(),
                () -> "Beta API aliases are not maintained: " + deprecatedEvents);
    }

    @Test
    void guiServiceOwnsGuiConfigurationTextAndBehavior() throws IOException {
        final Path coreGui = Path.of("src/main/java/com/notquests/core/gui");
        final Path paperGui = Path.of("../paper/src/main/java/com/notquests/paper/gui");

        assertTrue(
                Files.isRegularFile(coreGui.resolve("GuiService.java"))
                        && Files.isRegularFile(coreGui.resolve("GuiContext.java")),
                "GUI behavior and its domain context must remain discoverable in core.gui");
        assertTrue(
                !Files.exists(coreGui.resolve("CoreGuiBuilder.java"))
                        && !Files.exists(coreGui.resolve("GuiText.java")),
                "GUI construction and its one-owner text helper belong inside GuiService");
        assertTrue(
                !Files.exists(paperGui.resolve("PaperGuiContext.java"))
                        && !Files.exists(paperGui.resolve("GuiContext.java")),
                "GUI context is shared core state and must not be mirrored in Paper");

        final String service = Files.readString(coreGui.resolve("GuiService.java"));
        assertTrue(
                service.contains("public final class GuiService")
                        && service.contains("private interface Text")
                        && service.contains("propertyLines(button, \"conditions\")")
                        && service.contains("GuiAction.registryAction("),
                "GuiService must own text resolution and preserve registered action/condition behavior");
    }

    @Test
    void productionCodeDoesNotContainFakeUnsupportedPlatformMessages() throws IOException {
        final List<Path> roots = List.of(
                Path.of("src/main/java"),
                Path.of("../builtin/src/main/java"),
                Path.of("../paper/src/main/java"),
                Path.of("../neoforge/src/main/java"));
        final List<String> offenders = roots.stream()
                .filter(Files::exists)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (final IOException exception) {
                        throw new IllegalStateException("Could not scan " + root, exception);
                    }
                })
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> containsAny(path, List.of(
                        "not supported on this platform",
                        "unsupported on this platform",
                        "not available on this platform")))
                .map(Path::toString)
                .sorted()
                .toList();

        assertTrue(
                offenders.isEmpty(),
                () -> "Shared commands must be feature-complete or capability-gated, not fake parity at runtime: "
                        + offenders);
    }

    @Test
    void productionCodeDoesNotKeepDeprecatedOrLegacyShims() throws IOException {
        final List<Path> roots = List.of(
                Path.of("src/main/java"),
                Path.of("../builtin/src/main/java"),
                Path.of("../paper/src/main/java"),
                Path.of("../neoforge/src/main/java"));
        final List<String> offenders = roots.stream()
                .filter(Files::exists)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (final IOException exception) {
                        throw new IllegalStateException("Could not scan " + root, exception);
                    }
                })
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/migrations/"))
                .filter(path -> containsAny(path, List.of(
                        "@Deprecated",
                        "Legacy alias",
                        "Legacy spelling",
                        "legacy alias",
                        "legacy spelling")))
                .map(Path::toString)
                .sorted()
                .toList();

        assertTrue(
                offenders.isEmpty(),
                () -> "Production code should not keep deprecated or legacy shims outside migrations: "
                        + offenders);
    }

    @Test
    void coreEventEntrypointsDispatchTypedTriggerEventsDirectly() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, List.of("triggerPlayerEvent(")))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "Core event entrypoints should dispatch typed Event values directly, not hide them "
                        + "behind a string helper: " + offenders);
    }

    @Test
    void commandMessagesAreSpecificEnoughToBeUseful() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, List.of("Operation done!")))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "Command handlers should return specific user-facing messages, not generic completion text: "
                        + offenders);
    }

    @Test
    void productionCodeDoesNotUseCommandDumpEffectOrOperationNames() throws IOException {
        final List<Path> roots = List.of(
                Path.of("src/main/java"),
                Path.of("../builtin/src/main/java"),
                Path.of("../paper/src/main/java"),
                Path.of("../neoforge/src/main/java"));
        final List<String> offenders = roots.stream()
                .filter(Files::exists)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (final IOException exception) {
                        throw new IllegalStateException("Could not scan " + root, exception);
                    }
                })
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> containsAny(path, List.of(
                        "RegistryCommandRunner",
                        "CommandRunner",
                        "CommandEffects",
                        "CommandEffect",
                        "CommandOperations",
                        "CommandOperation",
                        "AdminEditOperation")))
                .map(Path::toString)
                .sorted()
                .toList();

        assertTrue(
                offenders.isEmpty(),
                () -> "Command behavior should be self-contained in core command feature files, "
                        + "not hidden behind runner/effects/operation dump names: " + offenders);
    }

    @Test
    void adapterLeavesAreNotCommandShapedArmorStandToolHandlers() throws IOException {
        final List<Path> roots = List.of(
                Path.of("src/main/java"),
                Path.of("../paper/src/main/java"),
                Path.of("../neoforge/src/main/java"));
        final List<String> offenders = roots.stream()
                .filter(Files::exists)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (final IOException exception) {
                        throw new IllegalStateException("Could not scan " + root, exception);
                    }
                })
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> containsAny(path, List.of(
                        "giveConversationArmorStandRemoveTool",
                        "giveArmorStandQuestTool")))
                .map(Path::toString)
                .sorted()
                .toList();

        assertTrue(
                offenders.isEmpty(),
                () -> "Adapters should receive one generic armor-stand tool item payload from core, "
                        + "not command-shaped quest/conversation tool methods: " + offenders);
    }

    private static boolean containsForbiddenPlatformImport(final Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::strip)
                    .filter(line -> line.startsWith("import "))
                    .anyMatch(line -> FORBIDDEN_PLATFORM_IMPORTS.stream().anyMatch(line::contains));
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }

    private static boolean containsAny(final Path path, final List<String> snippets) {
        try {
            final String source = Files.readString(path);
            return snippets.stream().anyMatch(source::contains);
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
