package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class NeoForgeRegistryTest {
    @Test
    void registersPortableBuiltInsIntoCoreRegistry() {
        final NotQuestsRegistry registry = portableRegistry();

        assertEquals(expectedPortableObjectives(), registry.objectives().stream()
                .map(Objectives.Type::id)
                .collect(Collectors.toCollection(java.util.TreeSet::new)));
        assertEquals(26, registry.actions().size());
        assertEquals(8, registry.conditions().size());
        assertEquals(8, registry.triggers().size());
        assertEquals(expectedPortableVariables(), registry.variables().stream()
                .map(Variables.Type::id)
                .collect(Collectors.toCollection(java.util.TreeSet::new)));
    }

    @Test
    void formatsExactCommandSummaryOutput() {
        final NotQuestsRegistry registry = portableRegistry();

        assertEquals(
                List.of(
                        "NotQuests registry:",
                        "- Objectives: " + expectedPortableObjectives().size(),
                        "- Actions: 26",
                        "- Conditions: 8",
                        "- Triggers: 8",
                        "- Variables: " + expectedPortableVariables().size()),
                NotQuestsRegistry.summary(registry));
    }

    @Test
    void formatsExactCommandListOutputs() {
        final NotQuestsRegistry registry = portableRegistry();

        final String objectiveOutput = NotQuestsRegistry.objectiveList(registry);
        for (final String objective : List.of(
                "Harvest",
                "PickupItems",
                "KillMobs",
                "Interact",
                "ReachLocation",
                "ShootArrow",
                "BrewItems",
                "SmithItems")) {
            assertTrue(
                    objectiveOutput.contains(objective),
                    "NeoForge objective output should contain " + objective + ", got: " + objectiveOutput);
        }
        assertEquals(
                oldList("All reward types", List.of(
                        "Action",
                        "Beam",
                        "Boolean",
                        "BroadcastMessage",
                        "Chat",
                        "CloseInventory",
                        "CompleteQuest",
                        "ConsoleCommand",
                        "FailQuest",
                        "GiveItem",
                        "GiveQuest",
                        "ItemStackList",
                        "List",
                        "Number",
                        "OpenGui",
                        "PlaySound",
                        "PlayerCommand",
                        "SendMessage",
                        "ShowActionBar",
                        "ShowTitle",
                        "SpawnMob",
                        "SpawnParticle",
                        "StartConversation",
                        "String",
                        "Teleport",
                        "TriggerCommand")),
                NotQuestsRegistry.actionList(registry));
        assertEquals(
                oldList("All requirement types", List.of(
                        "Boolean",
                        "CompletedObjective",
                        "Date",
                        "ItemStackList",
                        "List",
                        "Number",
                        "String",
                        "WorldTime")),
                NotQuestsRegistry.conditionList(registry));
        assertEquals(
                oldList("All variable types", expectedPortableVariables().stream().sorted().toList()),
                NotQuestsRegistry.variableList(registry));
        assertEquals(
                oldList("All trigger types", List.of("BEGIN", "COMPLETE", "DEATH", "DISCONNECT", "FAIL", "NPCDEATH", "WORLDENTER", "WORLDLEAVE")),
                NotQuestsRegistry.triggerList(registry));
    }

    private static NotQuestsRegistry portableRegistry() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        return registry;
    }

    private static String oldList(final String header, final List<String> names) {
        return "<highlight>" + header + ":\n<main>" + String.join("\n<main>", names);
    }

    private static Set<String> expectedPortableObjectives() {
        return Set.of(
                "BreakBlocks",
                "BreedMobs",
                "BrewItems",
                "Condition",
                "ConsumeItems",
                "CraftItems",
                "DeliverItems",
                "Die",
                "Enchant",
                "FeedMobs",
                "FishItems",
                "Harvest",
                "Interact",
                "Jump",
                "KillMobs",
                "MilkCow",
                "NumberVariable",
                "Objective",
                "OpenBuriedTreasure",
                "OtherQuest",
                "PickupItems",
                "PlaceBlocks",
                "ReachLocation",
                "RunCommand",
                "ShearSheep",
                "ShootArrow",
                "SmeltItems",
                "SmithItems",
                "Sneak",
                "TameMobs",
                "TalkToNPC",
                "TradeWithVillager",
                "TriggerCommand");
    }

    private static Set<String> expectedPortableVariables() {
        return Set.of(
                "ActiveQuests",
                "Advancement",
                "Block",
                "Chance",
                "Climbing",
                "CompletedObjectiveIDsOfQuest",
                "CompletedQuests",
                "Condition",
                "ContainerInventory",
                "CurrentBiome",
                "CurrentPositionX",
                "CurrentPositionY",
                "CurrentPositionZ",
                "CurrentWorld",
                "DayOfWeek",
                "DistanceToLocation",
                "EnderChest",
                "Experience",
                "ExperienceLevel",
                "False",
                "FlySpeed",
                "Flying",
                "FoodLevel",
                "GameMode",
                "Glowing",
                "Health",
                "InLava",
                "InWater",
                "Inventory",
                "ItemInInventoryEnchantments",
                "MaxHealth",
                "Name",
                "NearbyEntityCount",
                "Op",
                "Ping",
                "PlaytimeHours",
                "PlaytimeMinutes",
                "PlaytimeTicks",
                "QuestAbleToAccept",
                "QuestOnCooldown",
                "QuestPoints",
                "QuestReachedMaxAccepts",
                "QuestReachedMaxCompletions",
                "QuestReachedMaxFails",
                "RandomNumberBetweenRange",
                "ReflectionStaticBoolean",
                "ReflectionStaticDouble",
                "ReflectionStaticFloat",
                "ReflectionStaticInteger",
                "ReflectionStaticString",
                "Saturation",
                "Sleeping",
                "Sneaking",
                "Sprinting",
                "Statistic",
                "Swimming",
                "TagBoolean",
                "TagDouble",
                "TagFloat",
                "TagInteger",
                "TagString",
                "True",
                "WalkSpeed",
                "Weather");
    }
}
