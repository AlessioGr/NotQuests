package rocks.gravili.notquests.spigot;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import rocks.gravili.notquests.Main;
import rocks.gravili.notquests.spigot.conditions.Condition;
import rocks.gravili.notquests.spigot.conversation.ConversationEvents;
import rocks.gravili.notquests.spigot.conversation.ConversationManager;
import rocks.gravili.notquests.spigot.events.ArmorStandEvents;
import rocks.gravili.notquests.spigot.events.InventoryEvents;
import rocks.gravili.notquests.spigot.events.QuestEvents;
import rocks.gravili.notquests.spigot.events.TriggerEvents;
import rocks.gravili.notquests.spigot.events.notquests.other.PlayerJumpEvent;
import rocks.gravili.notquests.spigot.managers.*;
import rocks.gravili.notquests.spigot.managers.packets.PacketManager;
import rocks.gravili.notquests.spigot.managers.registering.ActionManager;
import rocks.gravili.notquests.spigot.managers.registering.ConditionsManager;
import rocks.gravili.notquests.spigot.managers.registering.ObjectiveManager;
import rocks.gravili.notquests.spigot.managers.registering.TriggerManager;
import rocks.gravili.notquests.spigot.objectives.Objective;
import rocks.gravili.notquests.spigot.structs.Quest;
import rocks.gravili.notquests.spigot.structs.actions.Action;
import rocks.gravili.notquests.spigot.structs.triggers.Trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class NotQuests {
    private static NotQuests instance;
    private final Main main;

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

    public NotQuests(Main main){
        this.main = main;
    }

    public final Main getMain(){
        return main;
    }

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
    public void onEnable() {
        instance = this;

        // Initialize an audiences instance for the plugin
        this.adventure = BukkitAudiences.create(main);

        logManager.lateInit(); //To initialize adventure

        getLogManager().info("NotQuests (Spigot) is starting...");



        //Create a new instance of the Util Manager which will be re-used everywhere
        utilManager = new UtilManager(this);


        //Create a new instance of the Performance Manager which will be re-used everywhere
        performanceManager = new PerformanceManager(this);


        actionsYMLManager = new ActionsYMLManager(this);
        conditionsYMLManager = new ConditionsYMLManager(this);


        integrationsManager.enableIntegrations();


        dataManager.loadStandardCompletions();

        Bukkit.getServer().getPluginManager().registerEvents(PlayerJumpEvent.listener, main);


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
        main.getServer().getPluginManager().registerEvents(new QuestEvents(this), main);

        //Register the Event Listeners in InventoryEvents
        main.getServer().getPluginManager().registerEvents(new InventoryEvents(this), main);

        //Register the Event Listeners in TriggerEvents
        main.getServer().getPluginManager().registerEvents(new TriggerEvents(this), main);

        //Register the Event Listeners in ArmorStandEvents
        main.getServer().getPluginManager().registerEvents(new ArmorStandEvents(this), main);


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
        main.getServer().getPluginManager().registerEvents(new ConversationEvents(this, conversationManager), main);

        commandManager.setupAdminConversationCommands(conversationManager);

    }

    public void setupBStats() {
        //bStats statistics
        final int pluginId = 12824; // <- Plugin ID (on bstats)
        metrics = new Metrics(main, pluginId);

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
