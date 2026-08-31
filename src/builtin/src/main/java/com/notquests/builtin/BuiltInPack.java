package com.notquests.builtin;

import com.notquests.builtin.actions.ActionAction;
import com.notquests.builtin.actions.BeamAction;
import com.notquests.builtin.actions.BooleanAction;
import com.notquests.builtin.actions.BroadcastMessageAction;
import com.notquests.builtin.actions.ChatAction;
import com.notquests.builtin.actions.CloseInventoryAction;
import com.notquests.builtin.actions.CompleteQuestAction;
import com.notquests.builtin.actions.ConsoleCommandAction;
import com.notquests.builtin.actions.FailQuestAction;
import com.notquests.builtin.actions.GiveItemAction;
import com.notquests.builtin.actions.GiveQuestAction;
import com.notquests.builtin.actions.ItemStackListAction;
import com.notquests.builtin.actions.ListAction;
import com.notquests.builtin.actions.NumberAction;
import com.notquests.builtin.actions.OpenGuiAction;
import com.notquests.builtin.actions.PlaySoundAction;
import com.notquests.builtin.actions.PlayerCommandAction;
import com.notquests.builtin.actions.SendMessageAction;
import com.notquests.builtin.actions.ShowActionBarAction;
import com.notquests.builtin.actions.ShowTitleAction;
import com.notquests.builtin.actions.SpawnMobAction;
import com.notquests.builtin.actions.SpawnParticleAction;
import com.notquests.builtin.actions.StartConversationAction;
import com.notquests.builtin.actions.StringAction;
import com.notquests.builtin.actions.TeleportAction;
import com.notquests.builtin.actions.TriggerCommandAction;
import com.notquests.builtin.conditions.CompletedObjectiveCondition;
import com.notquests.builtin.conditions.DateCondition;
import com.notquests.builtin.conditions.VariableCondition;
import com.notquests.builtin.conditions.WorldTimeCondition;
import com.notquests.builtin.objectives.BreakBlocksObjective;
import com.notquests.builtin.objectives.BreedObjective;
import com.notquests.builtin.objectives.BrewItemsObjective;
import com.notquests.builtin.objectives.ConditionObjective;
import com.notquests.builtin.objectives.ConsumeItemsObjective;
import com.notquests.builtin.objectives.CraftItemsObjective;
import com.notquests.builtin.objectives.DeliverItemsObjective;
import com.notquests.builtin.objectives.DieObjective;
import com.notquests.builtin.objectives.EnchantObjective;
import com.notquests.builtin.objectives.FeedMobsObjective;
import com.notquests.builtin.objectives.FishItemsObjective;
import com.notquests.builtin.objectives.HarvestObjective;
import com.notquests.builtin.objectives.InteractObjective;
import com.notquests.builtin.objectives.JumpObjective;
import com.notquests.builtin.objectives.KillEliteMobsObjective;
import com.notquests.builtin.objectives.KillMobsObjective;
import com.notquests.builtin.objectives.MilkCowObjective;
import com.notquests.builtin.objectives.NumberVariableObjective;
import com.notquests.builtin.objectives.ObjectiveObjective;
import com.notquests.builtin.objectives.OpenBuriedTreasureObjective;
import com.notquests.builtin.objectives.OtherQuestObjective;
import com.notquests.builtin.objectives.PickupItemsObjective;
import com.notquests.builtin.objectives.PlaceBlocksObjective;
import com.notquests.builtin.objectives.ReachLocationObjective;
import com.notquests.builtin.objectives.RunCommandObjective;
import com.notquests.builtin.objectives.ShearSheepObjective;
import com.notquests.builtin.objectives.ShootArrowObjective;
import com.notquests.builtin.objectives.SlimefunResearchObjective;
import com.notquests.builtin.objectives.SmeltObjective;
import com.notquests.builtin.objectives.SmithItemsObjective;
import com.notquests.builtin.objectives.SneakObjective;
import com.notquests.builtin.objectives.TalkToNPCObjective;
import com.notquests.builtin.objectives.TameMobsObjective;
import com.notquests.builtin.objectives.TradeWithVillagerObjective;
import com.notquests.builtin.objectives.TriggerCommandObjective;
import com.notquests.builtin.triggers.BeginTrigger;
import com.notquests.builtin.triggers.CompleteTrigger;
import com.notquests.builtin.triggers.DeathTrigger;
import com.notquests.builtin.triggers.DisconnectTrigger;
import com.notquests.builtin.triggers.FailTrigger;
import com.notquests.builtin.triggers.NPCDeathTrigger;
import com.notquests.builtin.triggers.WorldEnterTrigger;
import com.notquests.builtin.triggers.WorldLeaveTrigger;
import com.notquests.builtin.variables.ActiveQuestsVariable;
import com.notquests.builtin.variables.AdvancementVariable;
import com.notquests.builtin.variables.BlockVariable;
import com.notquests.builtin.variables.BooleanTagVariable;
import com.notquests.builtin.variables.ChanceVariable;
import com.notquests.builtin.variables.CompletedObjectiveIDsOfQuestVariable;
import com.notquests.builtin.variables.CompletedQuestsVariable;
import com.notquests.builtin.variables.ConditionVariable;
import com.notquests.builtin.variables.ContainerInventoryVariable;
import com.notquests.builtin.variables.DayOfWeekVariable;
import com.notquests.builtin.variables.DistanceToLocationVariable;
import com.notquests.builtin.variables.DoubleTagVariable;
import com.notquests.builtin.variables.EnderChestVariable;
import com.notquests.builtin.variables.FalseVariable;
import com.notquests.builtin.variables.FloatTagVariable;
import com.notquests.builtin.variables.IntegerTagVariable;
import com.notquests.builtin.variables.InventoryVariable;
import com.notquests.builtin.variables.ItemInInventoryEnchantmentsVariable;
import com.notquests.builtin.variables.NearbyEntityCountVariable;
import com.notquests.builtin.variables.PermissionVariable;
import com.notquests.builtin.variables.PlayerClimbingVariable;
import com.notquests.builtin.variables.PlayerCurrentBiomeVariable;
import com.notquests.builtin.variables.PlayerCurrentPositionXVariable;
import com.notquests.builtin.variables.PlayerCurrentPositionYVariable;
import com.notquests.builtin.variables.PlayerCurrentPositionZVariable;
import com.notquests.builtin.variables.PlayerCurrentWorldVariable;
import com.notquests.builtin.variables.PlayerExperienceLevelVariable;
import com.notquests.builtin.variables.PlayerExperienceVariable;
import com.notquests.builtin.variables.PlayerFlySpeedVariable;
import com.notquests.builtin.variables.PlayerFlyingVariable;
import com.notquests.builtin.variables.PlayerFoodLevelVariable;
import com.notquests.builtin.variables.PlayerGameModeVariable;
import com.notquests.builtin.variables.PlayerGlowingVariable;
import com.notquests.builtin.variables.PlayerHealthVariable;
import com.notquests.builtin.variables.PlayerInLavaVariable;
import com.notquests.builtin.variables.PlayerInWaterVariable;
import com.notquests.builtin.variables.PlayerMaxHealthVariable;
import com.notquests.builtin.variables.PlayerNameVariable;
import com.notquests.builtin.variables.PlayerOpVariable;
import com.notquests.builtin.variables.PlayerPingVariable;
import com.notquests.builtin.variables.PlayerPlaytimeHoursVariable;
import com.notquests.builtin.variables.PlayerPlaytimeMinutesVariable;
import com.notquests.builtin.variables.PlayerPlaytimeTicksVariable;
import com.notquests.builtin.variables.PlayerSaturationVariable;
import com.notquests.builtin.variables.PlayerSleepingVariable;
import com.notquests.builtin.variables.PlayerSneakingVariable;
import com.notquests.builtin.variables.PlayerSprintingVariable;
import com.notquests.builtin.variables.PlayerStatisticVariable;
import com.notquests.builtin.variables.PlayerSwimmingVariable;
import com.notquests.builtin.variables.PlayerWalkSpeedVariable;
import com.notquests.builtin.variables.QuestAbleToAcceptVariable;
import com.notquests.builtin.variables.QuestOnCooldownVariable;
import com.notquests.builtin.variables.QuestPointsVariable;
import com.notquests.builtin.variables.QuestReachedMaxAcceptsVariable;
import com.notquests.builtin.variables.QuestReachedMaxCompletionsVariable;
import com.notquests.builtin.variables.QuestReachedMaxFailsVariable;
import com.notquests.builtin.variables.RandomNumberBetweenRangeVariable;
import com.notquests.builtin.variables.ReflectionStaticBooleanVariable;
import com.notquests.builtin.variables.ReflectionStaticDoubleVariable;
import com.notquests.builtin.variables.ReflectionStaticFloatVariable;
import com.notquests.builtin.variables.ReflectionStaticIntegerVariable;
import com.notquests.builtin.variables.ReflectionStaticStringVariable;
import com.notquests.builtin.variables.StringTagVariable;
import com.notquests.builtin.variables.TrueVariable;
import com.notquests.builtin.variables.WeatherVariable;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

/**
 * Platform-independent NotQuests registry entries.
 *
 * <p>This module intentionally stays free of Bukkit/Paper APIs. Built-ins and external registry
 * packs use the same {@link NotQuestsAdapter} contract; Paper is just one implementation of that
 * adapter.
 */
public final class BuiltInPack {
    private BuiltInPack() {}

    public static void register(final NotQuestsAdapter platform) {
        registerVariables(platform);
        registerActions(platform);
        registerConditions(platform);
        registerObjectives(platform);
        registerTriggers(platform);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        registerVariables(plugin, platform);
        registerActions(plugin, platform);
        registerConditions(plugin, platform);
        registerObjectives(plugin, platform);
        registerTriggers(platform);
    }

    public static void registerObjectives(final NotQuestsAdapter platform) {
        registerObjectives(NotQuestsPlugin.create(), platform);
    }

    public static void registerObjectives(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        registerGameplayObjectives(platform);
        registerCoreRuntimeObjectiveShapes(plugin, platform);
        registerNpcObjectiveShapes(plugin, platform);
        KillEliteMobsObjective.register(plugin, platform);
        SlimefunResearchObjective.register(plugin, platform);
    }

    public static void registerGameplayObjectives(final NotQuestsAdapter platform) {
        BreakBlocksObjective.register(platform);
        BreedObjective.register(platform);
        BrewItemsObjective.register(platform);
        ConsumeItemsObjective.register(platform);
        CraftItemsObjective.register(platform);
        DieObjective.register(platform);
        EnchantObjective.register(platform);
        FeedMobsObjective.register(platform);
        FishItemsObjective.register(platform);
        HarvestObjective.register(platform);
        InteractObjective.register(platform);
        JumpObjective.register(platform);
        KillMobsObjective.register(platform);
        MilkCowObjective.register(platform);
        OpenBuriedTreasureObjective.register(platform);
        PickupItemsObjective.register(platform);
        PlaceBlocksObjective.register(platform);
        ReachLocationObjective.register(platform);
        RunCommandObjective.register(platform);
        ShearSheepObjective.register(platform);
        ShootArrowObjective.register(platform);
        SneakObjective.register(platform);
        SmithItemsObjective.register(platform);
        SmeltObjective.register(platform);
        TameMobsObjective.register(platform);
        TradeWithVillagerObjective.register(platform);
    }

    public static void registerCoreRuntimeObjectiveShapes(final NotQuestsAdapter platform) {
        registerCoreRuntimeObjectiveShapes(NotQuestsPlugin.create(), platform);
    }

    public static void registerCoreRuntimeObjectiveShapes(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        ConditionObjective.register(plugin, platform);
        NumberVariableObjective.register(platform);
        ObjectiveObjective.register(platform);
        OtherQuestObjective.register(plugin, platform);
        TriggerCommandObjective.register(platform);
    }

    public static void registerNpcObjectiveShapes(final NotQuestsAdapter platform) {
        registerNpcObjectiveShapes(NotQuestsPlugin.create(), platform);
    }

    public static void registerNpcObjectiveShapes(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter platform) {
        DeliverItemsObjective.register(plugin, platform);
        TalkToNPCObjective.register(platform);
    }

    public static void registerVariables(final NotQuestsAdapter platform) {
        registerVariables(NotQuestsPlugin.create(), platform);
    }

    public static void registerVariables(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        TrueVariable.register(platform);
        FalseVariable.register(platform);
        ConditionVariable.register(plugin, platform);
        ChanceVariable.register(platform);
        RandomNumberBetweenRangeVariable.register(platform);
        DayOfWeekVariable.register(platform);
        PlayerNameVariable.register(platform);
        PlayerFlyingVariable.register(platform);
        PlayerSneakingVariable.register(platform);
        PlayerSprintingVariable.register(platform);
        PlayerSwimmingVariable.register(platform);
        PlayerHealthVariable.register(platform);
        PlayerMaxHealthVariable.register(platform);
        PlayerFoodLevelVariable.register(platform);
        PlayerSaturationVariable.register(platform);
        PlayerExperienceVariable.register(platform);
        PlayerExperienceLevelVariable.register(platform);
        PlayerPingVariable.register(platform);
        PlayerWalkSpeedVariable.register(platform);
        PlayerFlySpeedVariable.register(platform);
        PlayerGlowingVariable.register(platform);
        PlayerOpVariable.register(platform);
        PlayerSleepingVariable.register(platform);
        PlayerClimbingVariable.register(platform);
        PlayerInLavaVariable.register(platform);
        PlayerInWaterVariable.register(platform);
        PlayerGameModeVariable.register(platform);
        PlayerPlaytimeTicksVariable.register(platform);
        PlayerPlaytimeMinutesVariable.register(platform);
        PlayerPlaytimeHoursVariable.register(platform);
        PlayerStatisticVariable.register(platform);
        AdvancementVariable.register(platform);
        BlockVariable.register(platform);
        PlayerCurrentWorldVariable.register(platform);
        PlayerCurrentPositionXVariable.register(platform);
        PlayerCurrentPositionYVariable.register(platform);
        PlayerCurrentPositionZVariable.register(platform);
        PlayerCurrentBiomeVariable.register(platform);
        WeatherVariable.register(platform);
        DistanceToLocationVariable.register(platform);
        NearbyEntityCountVariable.register(platform);
        PermissionVariable.register(platform);
        QuestPointsVariable.register(plugin, platform);
        QuestAbleToAcceptVariable.register(plugin, platform);
        QuestOnCooldownVariable.register(plugin, platform);
        QuestReachedMaxAcceptsVariable.register(plugin, platform);
        QuestReachedMaxCompletionsVariable.register(plugin, platform);
        QuestReachedMaxFailsVariable.register(plugin, platform);
        ActiveQuestsVariable.register(plugin, platform);
        CompletedQuestsVariable.register(plugin, platform);
        CompletedObjectiveIDsOfQuestVariable.register(plugin, platform);
        InventoryVariable.register(plugin, platform);
        EnderChestVariable.register(plugin, platform);
        ContainerInventoryVariable.register(plugin, platform);
        ItemInInventoryEnchantmentsVariable.register(platform);
        BooleanTagVariable.register(plugin, platform);
        IntegerTagVariable.register(plugin, platform);
        FloatTagVariable.register(plugin, platform);
        DoubleTagVariable.register(plugin, platform);
        StringTagVariable.register(plugin, platform);
        ReflectionStaticDoubleVariable.register(platform);
        ReflectionStaticFloatVariable.register(platform);
        ReflectionStaticIntegerVariable.register(platform);
        ReflectionStaticBooleanVariable.register(platform);
        ReflectionStaticStringVariable.register(platform);
    }

    public static void registerActions(final NotQuestsAdapter platform) {
        registerActions(NotQuestsPlugin.create(), platform);
    }

    public static void registerActions(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        ConsoleCommandAction.register(platform);
        PlayerCommandAction.register(platform);
        ChatAction.register(platform);
        SendMessageAction.register(platform);
        BroadcastMessageAction.register(platform);
        ShowActionBarAction.register(platform);
        CloseInventoryAction.register(platform);
        GiveItemAction.register(plugin, platform);
        GiveQuestAction.register(plugin, platform);
        CompleteQuestAction.register(plugin, platform);
        FailQuestAction.register(plugin, platform);
        ActionAction.register(plugin, platform);
        TriggerCommandAction.register(plugin, platform);
        StartConversationAction.register(plugin, platform);
        ShowTitleAction.register(platform);
        TeleportAction.register(platform);
        SpawnParticleAction.register(platform);
        PlaySoundAction.register(platform);
        SpawnMobAction.register(plugin, platform);
        BeamAction.register(plugin, platform);
        OpenGuiAction.register(plugin, platform);
        NumberAction.register(plugin, platform);
        StringAction.register(plugin, platform);
        BooleanAction.register(plugin, platform);
        ListAction.register(plugin, platform);
        ItemStackListAction.register(plugin, platform);
    }

    public static void registerConditions(final NotQuestsAdapter platform) {
        registerConditions(NotQuestsPlugin.create(), platform);
    }

    public static void registerConditions(final NotQuestsPlugin plugin, final NotQuestsAdapter platform) {
        DateCondition.register(platform);
        WorldTimeCondition.register(platform);
        CompletedObjectiveCondition.register(plugin, platform);
        VariableCondition.register(platform);
    }

    public static void registerTriggers(final NotQuestsAdapter platform) {
        BeginTrigger.register(platform);
        CompleteTrigger.register(platform);
        DeathTrigger.register(platform);
        DisconnectTrigger.register(platform);
        FailTrigger.register(platform);
        NPCDeathTrigger.register(platform);
        WorldEnterTrigger.register(platform);
        WorldLeaveTrigger.register(platform);
    }
}
