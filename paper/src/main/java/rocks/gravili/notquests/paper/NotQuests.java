package rocks.gravili.notquests.paper;

import io.lumine.xikage.mythicmobs.skills.conditions.ConditionAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.gravili.notquests.paper.conversation.ConversationEvents;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.events.ArmorStandEvents;
import rocks.gravili.notquests.paper.events.InventoryEvents;
import rocks.gravili.notquests.paper.events.QuestEvents;
import rocks.gravili.notquests.paper.events.TriggerEvents;
import rocks.gravili.notquests.paper.events.notquests.NotQuestsFullyLoadedEvent;
import rocks.gravili.notquests.paper.managers.*;
import rocks.gravili.notquests.paper.managers.packets.PacketManager;
import rocks.gravili.notquests.paper.managers.registering.*;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.*;
import rocks.gravili.notquests.paper.structs.conditions.*;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class NotQuests {
    private static NotQuests instance;
    private final JavaPlugin main;

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
    private MessageManager messageManager;

    //Registering Managers
    private ObjectiveManager objectiveManager;
    private ConditionsManager conditionsManager;
    private ActionManager actionManager;
    private TriggerManager triggerManager;
    private IntegrationsManager integrationsManager;
    private VariablesManager variablesManager;

    public ArrayList<Action> allActions = new ArrayList<>(); //For bStats
    public ArrayList<Condition> allConditions = new ArrayList<>(); //For bStats

    //Metrics
    private Metrics metrics;

    public final JavaPlugin getMain(){
        return main;
    }

    public NotQuests(JavaPlugin main){
        this.main = main;
    }

    public void onLoad() {

        messageManager = new MessageManager(this);
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

        logManager.lateInit(); //To initialize adventure

        getLogManager().info("NotQuests (Paper) is starting...");

        //Create a new instance of the Util Manager which will be re-used everywhere
        utilManager = new UtilManager(this);


        //Create a new instance of the Performance Manager which will be re-used everywhere
        performanceManager = new PerformanceManager(this);


        actionsYMLManager = new ActionsYMLManager(this);
        conditionsYMLManager = new ConditionsYMLManager(this);


        integrationsManager.enableIntegrations();


        dataManager.loadStandardCompletions();


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
        variablesManager = new VariablesManager(this);

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

        dataManager.loadCategories(); //Categories need to be loaded before the condition & actions stuff, as they depend on them

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

        NotQuestsFullyLoadedEvent notQuestsFullyLoadedEvent = new NotQuestsFullyLoadedEvent(this);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(getMain(), () -> {
                Bukkit.getPluginManager().callEvent(notQuestsFullyLoadedEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(notQuestsFullyLoadedEvent);
        }
        getLogManager().info("NotQuests initial Loading has completed!");

    }

    public void setupBStats() {
        //bStats statistics
        final int pluginId = 12824; // <- Plugin ID (on bstats)
        metrics = new Metrics(main, pluginId);

        metrics.addCustomChart(new SingleLineChart("quests", new Callable<Integer>() {
            @Override
            public Integer call() {
                return getQuestManager().getAllQuests().size();
            }
        }));

        metrics.addCustomChart(new SingleLineChart("conversations", new Callable<Integer>() {
            @Override
            public Integer call() {
                return getConversationManager().getAllConversations().size();
            }
        }));

        metrics.addCustomChart(new AdvancedPie("ObjectiveTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
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


        metrics.addCustomChart(new AdvancedPie("ConditionTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> map = new HashMap<>();
                for (Condition condition : allConditions) {
                    String conditionType = condition.getConditionType();


                    if(condition instanceof NumberCondition numberCondition){
                        conditionType = numberCondition.getVariableName();
                    }else if(condition instanceof StringCondition stringCondition){
                        conditionType = stringCondition.getVariableName();
                    }else if(condition instanceof BooleanCondition booleanCondition){
                        conditionType = booleanCondition.getVariableName();
                    }else if(condition instanceof ListCondition listCondition){
                        conditionType = listCondition.getVariableName();
                    }

                    map.put(conditionType, map.getOrDefault(conditionType, 0) + 1);
                }

                return map;
            }
        }));

        metrics.addCustomChart(new AdvancedPie("AllActionTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> map = new HashMap<>();
                for (Action action : allActions) {
                    String actionType = action.getActionType();

                    if(action instanceof NumberAction numberAction){
                        actionType = numberAction.getVariableName();
                    }else if(action instanceof StringAction stringAction){
                        actionType = stringAction.getVariableName();
                    }else if(action instanceof BooleanAction booleanAction){
                        actionType = booleanAction.getVariableName();
                    }else if(action instanceof ListAction listAction){
                        actionType = listAction.getVariableName();
                    }


                    map.put(actionType, map.getOrDefault(actionType, 0) + 1);
                }
                return map;
            }
        }));

        metrics.addCustomChart(new AdvancedPie("TriggerTypes", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
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



        integrationsManager.onDisable();



        packetManager.terminate();

        /* This is kind of useful for compatibility with ServerUtils or Plugman.
         * If this is false, the plugin will try to load NPCs again if the Citizens plugin is reloaded or enabled.
         * Might not be necessary.
         */
        getDataManager().setAlreadyLoadedNPCs(false);

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

    public VariablesManager getVariablesManager(){
        return variablesManager;
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

    public final MessageManager getMessageManager(){
        return messageManager;
    }

    public final MiniMessage getMiniMessage(){
        return messageManager.getMiniMessage();
    }

    public final Component parse(String miniMessage){
        return getMiniMessage().deserialize(miniMessage);
    }

    public void sendMessage(CommandSender sender, String message){
        if(!message.isBlank() && sender != null){
            sender.sendMessage(parse(message));
        }
    }

    public void sendMessage(CommandSender sender, Component component){
        if(!PlainTextComponentSerializer.plainText().serialize(component).isBlank() && sender != null){
            sender.sendMessage(component);
        }
    }

}
