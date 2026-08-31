package com.notquests.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class BuiltinConfigPathTest {
    private static NotQuestsRegistry registry;

    @BeforeAll
    static void registerBuiltins() {
        registry = new NotQuestsRegistry();
        BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    }

    @Test
    void actionFieldsUseCanonicalYamlPaths() {
        final Map<String, Set<String>> expected = Map.ofEntries(
                entry("Action", "specifics.actions", "specifics.amount", "specifics.ignoreConditions",
                        "specifics.minRandom", "specifics.maxRandom",
                        "specifics.onlyCountForRandomIfConditionsFulfilled", "specifics.executedActionDelay"),
                entry("Beam", "specifics.beamName", "specifics.remove", "specifics.location"),
                entry("Boolean", variablePaths("specifics.expression")),
                entry("BroadcastMessage", "specifics.message"),
                entry("Chat", "specifics.chatMessage"),
                entry("CompleteQuest", "specifics.quest"),
                entry("ConsoleCommand", "specifics.consoleCommand"),
                entry("FailQuest", "specifics.quest"),
                entry("GiveItem", "specifics.itemStackSelection", "specifics.nqitemamount"),
                entry("GiveQuest", "specifics.quest", "specifics.forceGive"),
                entry("ItemStackList", variablePaths("specifics.itemStack")),
                entry("List", variablePaths("specifics.expression")),
                entry("Number", variablePaths("specifics.expression")),
                entry("OpenGui", "specifics.guiName"),
                entry("PlaySound", "specifics.soundName", "specifics.stopOtherSounds", "specifics.worldName",
                        "specifics.locationX", "specifics.locationY", "specifics.locationZ", "specifics.volume",
                        "specifics.pitch", "specifics.playForEveryoneAtTheirLocation",
                        "specifics.playForEveryoneAtSetLocation", "specifics.soundCategory"),
                entry("PlayerCommand", "specifics.playerCommand"),
                entry("SendMessage", "specifics.message"),
                entry("SpawnMob", "specifics.mobToSpawn", "specifics.spawnLocation", "specifics.usePlayerLocation",
                        "specifics.amount", "specifics.spawnRadiusX", "specifics.spawnRadiusY", "specifics.spawnRadiusZ"),
                entry("StartConversation", "specifics.conversation", "specifics.endPrevious"),
                entry("String", variablePaths("specifics.newValue")),
                entry("TriggerCommand", "specifics.triggerName"));

        assertPaths(expected, registry.actions(), Actions.Type::id,
                type -> fields(type.fields(), type.flags()));
    }

    @Test
    void conditionFieldsUseCanonicalYamlPaths() {
        final Map<String, Set<String>> expected = Map.ofEntries(
                entry("Boolean", variablePaths("specifics.expression")),
                entry("CompletedObjective", "specifics.objectiveID"),
                entry("Date", "specifics.operation", "specifics.year", "specifics.month", "specifics.day",
                        "specifics.hours", "specifics.minutes", "specifics.seconds", "specifics.timeZone"),
                entry("ItemStackList", variablePaths("specifics.itemStack")),
                entry("List", variablePaths("specifics.expression")),
                entry("Number", variablePaths("specifics.expression")),
                entry("String", variablePaths("specifics.string")),
                entry("WorldTime", "specifics.minTime", "specifics.maxTime"));

        assertPaths(expected, registry.conditions(), Conditions.Type::id,
                type -> fields(type.fields(), type.flags()));
    }

    @Test
    void objectiveFieldsUseCanonicalYamlPaths() {
        final Map<String, Set<String>> expected = Map.ofEntries(
                entry("BreakBlocks", "specifics.itemStackSelection", "specifics.deductIfBlockPlaced"),
                entry("BreedMobs", "specifics.mobToBreed"),
                entry("Condition", "specifics.condition",
                        "specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                entry("ConsumeItems", "specifics.itemStackSelection"),
                entry("CraftItems", "specifics.itemStackSelection"),
                entry("DeliverItems", "specifics.itemStackSelection", "specifics.recipientNPC"),
                entry("Enchant", "specifics.enchantment", "specifics.itemStackSelection",
                        "specifics.minLevelExpression", "specifics.maxLevelExpression"),
                entry("FeedMobs", "specifics.mobToFeed"),
                entry("FishItems", "specifics.itemStackSelection"),
                entry("Interact", "specifics.locationToInteract", "specifics.leftClick", "specifics.rightClick",
                        "specifics.maxDistance", "specifics.cancelInteraction"),
                entry("KillMobs", "specifics.mobToKill", "extras.nameTagContainsAny", "extras.nameTagEquals"),
                entry("MilkCow", "specifics.cancelMilking"),
                entry("NumberVariable", "specifics.variableName", "specifics.operator", "specifics.additionalStrings",
                        "specifics.additionalNumbers", "specifics.additionalBooleans",
                        "specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                entry("Objective", "specifics.objectiveHolderName"),
                entry("OtherQuest", "specifics.otherQuestName", "specifics.countPreviousCompletions"),
                entry("PickupItems", "specifics.itemStackSelection", "specifics.deductIfItemDropped",
                        "specifics.deductIfItemPlaced"),
                entry("PlaceBlocks", "specifics.itemStackSelection", "specifics.deductIfBlockBroken"),
                entry("ReachLocation", "specifics.minLocation", "specifics.maxLocation", "specifics.locationName"),
                entry("RunCommand", "specifics.commandToRun", "specifics.ignoreCase", "specifics.cancelCommand"),
                entry("ShearSheep", "specifics.cancelShearing"),
                entry("SmeltItems", "specifics.itemStackSelection"),
                entry("TalkToNPC", "specifics.npcToTalkTo"),
                entry("TriggerCommand", "specifics.triggerName"));

        assertPaths(expected, registry.objectives(), Objectives.Type::id,
                type -> fields(type.fields(), type.flags()));
    }

    @Test
    void triggerFieldsUseCanonicalYamlPaths() {
        final Map<String, Set<String>> expected = Map.of(
                "DEATH", Set.of("amountNeeded"),
                "DISCONNECT", Set.of("amountNeeded"),
                "NPCDEATH", Set.of("specifics.npcToDie", "amountNeeded"),
                "WORLDENTER", Set.of("specifics.worldToEnter", "amountNeeded"),
                "WORLDLEAVE", Set.of("specifics.worldToLeave", "amountNeeded"));

        assertPaths(expected, registry.triggers(), Triggers.Type::id,
                Triggers.Type::fields);
    }

    @Test
    void invertedFlagsUsePositiveBooleanStorage() {
        assertInverted("BreakBlocks", "doNotDeductIfBlockIsPlaced", "specifics.deductIfBlockPlaced");
        assertInverted("PickupItems", "doNotDeductIfItemIsDropped", "specifics.deductIfItemDropped");
        assertInverted("PickupItems", "doNotDeductIfItemIsPlaced", "specifics.deductIfItemPlaced");
        assertInverted("PlaceBlocks", "doNotDeductIfBlockIsBroken", "specifics.deductIfBlockBroken");
    }

    @Test
    void enchantAliasesUseTheCanonicalExpressionPaths() {
        assertFieldPath(objective("Enchant"), "minLevel", "specifics.minLevelExpression");
        assertFieldPath(objective("Enchant"), "min", "specifics.minLevelExpression");
        assertFieldPath(objective("Enchant"), "maxLevel", "specifics.maxLevelExpression");
        assertFieldPath(objective("Enchant"), "max", "specifics.maxLevelExpression");
    }

    private static Set<String> variablePaths(final String valuePath) {
        return Set.of("specifics.variableName", "specifics.operator", valuePath, "specifics.additionalStrings",
                "specifics.additionalNumbers", "specifics.additionalBooleans");
    }

    private static Map.Entry<String, Set<String>> entry(final String type, final String... paths) {
        return Map.entry(type, Set.of(paths));
    }

    private static Map.Entry<String, Set<String>> entry(final String type, final Set<String> paths) {
        return Map.entry(type, paths);
    }

    private static List<RegistryField.Definition> fields(
            final List<RegistryField.Definition> fields,
            final List<RegistryField.Definition> flags) {
        return java.util.stream.Stream.concat(fields.stream(), flags.stream()).toList();
    }

    private static <T> void assertPaths(
            final Map<String, Set<String>> expected,
            final List<T> types,
            final Function<T, String> id,
            final Function<T, List<RegistryField.Definition>> fields) {
        for (final Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            final T type = types.stream().filter(candidate -> id.apply(candidate).equals(entry.getKey()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing registry type " + entry.getKey()));
            final Set<String> actual = fields.apply(type).stream()
                    .map(RegistryField.Definition::configPath)
                    .filter(path -> path != null && !path.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            assertTrue(actual.containsAll(entry.getValue()),
                    () -> entry.getKey() + " must use paths " + entry.getValue() + ", but had " + actual);
        }
    }

    private static void assertInverted(final String objectiveId, final String fieldName, final String configPath) {
        final RegistryField.Definition field = field(objective(objectiveId), fieldName);
        assertEquals(configPath, field.configPath());
        assertTrue(field.invertedBooleanConfig(), fieldName + " must invert the positive config value");
    }

    private static void assertFieldPath(
            final Objectives.Type objective,
            final String fieldName,
            final String configPath) {
        assertEquals(configPath, field(objective, fieldName).configPath());
    }

    private static RegistryField.Definition field(
            final Objectives.Type objective,
            final String fieldName) {
        return fields(objective.fields(), objective.flags()).stream()
                .filter(field -> field.name().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing field " + objective.id() + "." + fieldName));
    }

    private static Objectives.Type objective(final String id) {
        return registry.objectives().stream().filter(type -> type.id().equals(id)).findFirst().orElseThrow();
    }
}
