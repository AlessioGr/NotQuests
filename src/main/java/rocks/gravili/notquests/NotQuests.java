
/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.gravili.notquests.Conversation.ConversationEvents;
import rocks.gravili.notquests.Conversation.ConversationManager;
import rocks.gravili.notquests.Events.ArmorStandEvents;
import rocks.gravili.notquests.Events.InventoryEvents;
import rocks.gravili.notquests.Events.QuestEvents;
import rocks.gravili.notquests.Events.TriggerEvents;
import rocks.gravili.notquests.Events.notquests.other.PlayerJumpEvent;
import rocks.gravili.notquests.Managers.*;
import rocks.gravili.notquests.Managers.Packets.PacketManager;
import rocks.gravili.notquests.Managers.Registering.ActionManager;
import rocks.gravili.notquests.Managers.Registering.ConditionsManager;
import rocks.gravili.notquests.Managers.Registering.ObjectiveManager;
import rocks.gravili.notquests.Managers.Registering.TriggerManager;
import rocks.gravili.notquests.Structs.Actions.Action;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This is the entry point of NotQuests. All kinds of managers, commands and other shit is reistered here.
 *
 * @author Alessio Gravili
 */
public final class NotQuests extends JavaPlugin {

    private static NotQuests instance;

    //Managers
    private UtilManager utilManager;
    private LogManager logManager;
    private DataManager dataManager;
    private ActionsYMLManager actionsYMLManager;
    private ConditionsYMLManager conditionsYMLManager;
    private QuestManager questManager;
    private QuestPlayerManager questPlayerManager;
    private LanguageManager languageManager;
    private ArmorStandManager armorStandManager;
    private PerformanceManager performanceManager;
    private CommandManager commandManager;
    private ConversationManager conversationManager;
    private PacketManager packetManager;
    private UpdateManager updateManager;
    private GUIManager guiManager;
    private BackupManager backupManager;

    //Registering Managers
    private ObjectiveManager objectiveManager;
    private ConditionsManager conditionsManager;
    private ActionManager actionManager;
    private TriggerManager triggerManager;
    private IntegrationsManager integrationsManager;

    //Metrics
    private Metrics metrics;


    private BukkitAudiences adventure;


    @Override
    public void onLoad() {


        //Create a new instance of the Log Manager which will be re-used everywhere
        logManager = new LogManager(this);

        backupManager = new BackupManager(this);

        integrationsManager = new IntegrationsManager(this);


        //Create a new instance of the Data Manager which will be re-used everywhere
        dataManager = new DataManager(this);
        //Load general config first, because we'll need it for the integrations
        if (!dataManager.isAlreadyLoadedGeneral()) {
            dataManager.loadGeneralConfig();
        }

        if (packetManager == null) {
            packetManager = new PacketManager(this);
            packetManager.onLoad();
        }
    }

    public boolean isAdventureEnabled() {
        return this.adventure != null;
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    public static NotQuests getInstance() {
        return instance;
    }

    public Configuration getConfiguration() {
        return dataManager.getConfiguration();
    }

    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    @Override
    public void onEnable() {
        instance = this;

        // Initialize an audiences instance for the plugin
        this.adventure = BukkitAudiences.create(this);

        logManager.lateInit(); //To initialize adventure

        getLogManager().info("NotQuests is starting...");

        //PaperLib for paper-specific methods (like getting TPS)
        PaperLib.suggestPaper(this);


        //Create a new instance of the Util Manager which will be re-used everywhere
        utilManager = new UtilManager(this);


        //Create a new instance of the Performance Manager which will be re-used everywhere
        performanceManager = new PerformanceManager(this);


        actionsYMLManager = new ActionsYMLManager(this);
        conditionsYMLManager = new ConditionsYMLManager(this);


        integrationsManager.enableIntegrations();


        dataManager.loadStandardCompletions();

        Bukkit.getServer().getPluginManager().registerEvents(PlayerJumpEvent.listener, this);


        //Create a new instance of the Quest Manager which will be re-used everywhere
        questManager = new QuestManager(this);


        languageManager = new LanguageManager(this);

        updateManager = new UpdateManager(this);

        guiManager = new GUIManager(this);

        /*
         * Tell the Data Manager: Hey, NPCs have not been loaded yet. If this is set to false, the plugin will
         * try to load NPCs once Citizens is re-loaded or enabled
         */
        getDataManager().setAlreadyLoadedNPCs(false);

        //Create a new instance of the QuestPlayer Manager which will be re-used everywhere
        questPlayerManager = new QuestPlayerManager(this);

        //Create a new instance of the Armorstand Manager which will be re-used everywhere
        armorStandManager = new ArmorStandManager(this);

        armorStandManager.loadAllArmorStandsFromLoadedChunks();


        //Registering the nquestgiver Trait here has been commented out. I think I'm currently doing that somewhere else atm. So, this isn't needed at the moment.
        //net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));


        //Register the notquestsadmin command & tab completer. This command will be used by Admins
        commandManager = new CommandManager(this);
        commandManager.preSetupCommands();

        //Registering Managers
        objectiveManager = new ObjectiveManager(this);
        conditionsManager = new ConditionsManager(this);
        actionManager = new ActionManager(this);
        triggerManager = new TriggerManager(this);

        commandManager.setupCommands();


        //Register the Event Listeners in QuestEvents
        getServer().getPluginManager().registerEvents(new QuestEvents(this), this);

        //Register the Event Listeners in InventoryEvents
        getServer().getPluginManager().registerEvents(new InventoryEvents(this), this);

        //Register the Event Listeners in TriggerEvents
        getServer().getPluginManager().registerEvents(new TriggerEvents(this), this);

        //Register the Event Listeners in ArmorStandEvents
        getServer().getPluginManager().registerEvents(new ArmorStandEvents(this), this);


        integrationsManager.registerEvents();

        conditionsYMLManager.loadConditions();


        //Load actions first, as they are needed for triggers loading in dataManager.reloadData()
        actionsYMLManager.loadActions();

        //This finally starts loading all Config-, Quest-, and Player Data. Reload = Load
        dataManager.reloadData();

        //This registers all PlaceholderAPI placeholders, if loading is enabled
        if (getDataManager().isLoadingEnabled()) {
            integrationsManager.enableIntegrationsAfterDataLoad();

            //Update Checker
            updateManager.checkForPluginUpdates();

            conversationManager = new ConversationManager(this);

            setupBStats();


        }


        //Register the Event Listeners in ConversationEvents
        getServer().getPluginManager().registerEvents(new ConversationEvents(this, conversationManager), this);

        commandManager.setupAdminConversationCommands(conversationManager);

    }

    public void setupBStats() {
        //bStats statistics
        final int pluginId = 12824; // <- Plugin ID (on bstats)
        metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new SingleLineChart("quests", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getQuestManager().getAllQuests().size();
            }
        }));

        metrics.addCustomChart(new SingleLineChart("conversations", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getConversationManager().getAllConversations().size();
            }
        }));

        metrics.addCustomChart(new AdvancedPie("ObjectiveTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> valueMap = new HashMap<>();
                for (Quest quest : getQuestManager().getAllQuests()) {
                    for (Objective objective : quest.getObjectives()) {
                        String objectiveType = getObjectiveManager().getObjectiveType(objective.getClass());
                        valueMap.put(objectiveType, valueMap.getOrDefault(objectiveType, 0) + 1);
                    }
                }
                return valueMap;
            }
        }));


        metrics.addCustomChart(new AdvancedPie("RequirementTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> map = new HashMap<>();
                for (Quest quest : getQuestManager().getAllQuests()) {
                    for (Condition condition : quest.getRequirements()) {
                        String requirementType = getConditionsManager().getConditionType(condition.getClass());
                        map.put(requirementType, map.getOrDefault(requirementType, 0) + 1);
                    }
                }
                return map;
            }
        }));

        metrics.addCustomChart(new AdvancedPie("ActionTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> map = new HashMap<>();
                for (Quest quest : getQuestManager().getAllQuests()) {
                    for (Action action : quest.getRewards()) {
                        String actionType = action.getActionType();
                        map.put(actionType, map.getOrDefault(actionType, 0) + 1);
                    }
                    for (Objective objective : quest.getObjectives()) {
                        for (Action action : objective.getRewards()) {
                            String actionType = action.getActionType();
                            map.put(actionType, map.getOrDefault(actionType, 0) + 1);
                        }
                    }
                }
                for (Action action : getActionsYMLManager().getActionsAndIdentifiers().values()) {
                    String actionType = action.getActionType();
                    map.put(actionType, map.getOrDefault(actionType, 0) + 1);
                }
                return map;
            }
        }));

        metrics.addCustomChart(new AdvancedPie("TriggerTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> map = new HashMap<>();
                for (Quest quest : getQuestManager().getAllQuests()) {
                    for (Trigger trigger : quest.getTriggers()) {
                        String triggerType = getTriggerManager().getTriggerType(trigger.getClass());
                        map.put(triggerType, map.getOrDefault(triggerType, 0) + 1);
                    }
                }
                return map;
            }
        }));


        if (packetManager == null) {
            packetManager = new PacketManager(this);
        }

        packetManager.initialize();
    }




    /**
     * Called when the plugin is disabled or reloaded via ServerUtils / PlugMan
     */
    @Override
    public void onDisable() {
        getLogManager().info("NotQuests is shutting down...");

        //Save all kinds of data
        dataManager.saveData();



        /* This is kind of useful for compatibility with ServerUtils or Plugman.
         * If this is false, the plugin will try to load NPCs again if the Citizens plugin is reloaded or enabled.
         * Might not be necessary.
         */
        getDataManager().setAlreadyLoadedNPCs(false);

        integrationsManager.onDisable();


        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        packetManager.terminate();

    }


    /**
     * Returns an instance of the QuestManager which handles the saving and loading of configured Quests
     *
     * @return an instance of the Quest Manager
     */
    public QuestManager getQuestManager() {
        return questManager;
    }

    /**
     * Returns an instance of the QuestPlayer Manager which handles the saving and loading of player data
     *
     * @return an instance of the QuestPlayer Manager
     */
    public QuestPlayerManager getQuestPlayerManager() {
        return questPlayerManager;
    }

    /**
     * Returns an instance of the Data Manager which handles all kinds of MySQL & configuration loading and saving
     *
     * @return an instance of the Data Manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }

    public ActionsYMLManager getActionsYMLManager() {
        return actionsYMLManager;
    }

    public ConditionsYMLManager getConditionsYMLManager() {
        return conditionsYMLManager;
    }

    /**
     * Returns an instance of the bStats Metrics object
     *
     * @return bStats Metrics object
     */
    public Metrics getMetrics() {
        return metrics;
    }


    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public UtilManager getUtilManager() {
        return utilManager;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public PerformanceManager getPerformanceManager() {
        return performanceManager;
    }

    public ObjectiveManager getObjectiveManager() {
        return objectiveManager;
    }

    public ConditionsManager getConditionsManager() {
        return conditionsManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }


    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public ConversationManager getConversationManager() {
        return this.conversationManager;
    }


    public PacketManager getPacketManager() {
        return packetManager;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public IntegrationsManager getIntegrationsManager() {
        return integrationsManager;
    }
}

