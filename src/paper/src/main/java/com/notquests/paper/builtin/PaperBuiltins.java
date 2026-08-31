package com.notquests.paper.builtin;

import com.notquests.builtin.BuiltInPack;
import com.notquests.core.registry.NotQuestsRegistry.Pack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.*;
import com.notquests.paper.builtin.objectives.*;
import com.notquests.paper.builtin.variables.hooks.*;

public final class PaperBuiltins implements Pack<NotQuests> {
    @Override
    public void register(final NotQuests main) {
        BuiltInPack.register(main.getCorePlugin(), main.getRegistryAdapter());
        if (main.getCorePlugin().integrationEnabled("BetonQuest")) {
            BetonQuestFireEventAction.register(main, main.getRegistryAdapter());
            BetonQuestFireInlineEventAction.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("Citizens")) {
            EscortNPCObjective.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("Towny")) {
            TownyReachResidentCountObjective.register(main, main.getRegistryAdapter());
            TownyNationReachTownCountObjective.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("Jobs")) {
            JobsRebornReachJobLevelObjective.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("BetonQuest")) {
            BetonQuestObjectiveStateChangeObjective.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("PlaceholderAPI")) {
            PlaceholderAPINumberVariable.register(main, main.getRegistryAdapter());
            PlaceholderAPIStringVariable.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("Towny")) {
            TownyNationTownCountVariable.register(main, main.getRegistryAdapter());
            TownyTownResidentCountVariable.register(main, main.getRegistryAdapter());
            TownyTownPlotCountVariable.register(main, main.getRegistryAdapter());
            TownyNationNameVariable.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("Vault")) {
            MoneyVariable.register(main, main.getRegistryAdapter());
        }

        if (main.getCorePlugin().integrationEnabled("Floodgate")) {
            FloodgateIsFloodgatePlayerVariable.register(main, main.getRegistryAdapter());
        }
        if (main.getCorePlugin().integrationEnabled("BetonQuest")) {
            BetonQuestConditionVariable.register(main, main.getRegistryAdapter());
        }
    }
}
