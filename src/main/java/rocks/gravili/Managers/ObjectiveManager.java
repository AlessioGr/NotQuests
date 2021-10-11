package rocks.gravili.Managers;

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Objectives.*;

import java.util.Collection;
import java.util.HashMap;

public class ObjectiveManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;


    private final HashMap<String, Void> objectiveCommandHandlers;
    private final HashMap<String, Void> objectiveCommandCompletionHandlers;


    public ObjectiveManager(final NotQuests main){
        this.main = main;
        objectives = new HashMap<>();

        objectiveCommandHandlers = new HashMap<>();
        objectiveCommandCompletionHandlers = new HashMap<>();

        registerDefaultObjectives();

    }

    public void registerDefaultObjectives(){
        objectives.clear();
        registerObjective("BreakBlocks", BreakBlocksObjective.class);
        registerObjective("CollectItems", CollectItemsObjective.class);
        registerObjective("TriggerCommand", TriggerCommandObjective.class);
        registerObjective("OtherQuest", OtherQuestObjective.class);
        registerObjective("KillMobs", KillMobsObjective.class);
        registerObjective("ConsumeItems", ConsumeItemsObjective.class);
        registerObjective("DeliverItems", DeliverItemsObjective.class);
        registerObjective("TalkToNPC", TalkToNPCObjective.class);
        registerObjective("EscortNPC", EscortNPCObjective.class);
        registerObjective("CraftItems", CraftItemsObjective.class);
        registerObjective("KillEliteMobs", KillMobsObjective.class); //TODO: only if EliteMobs enabled?
        registerObjective("ReachLocation", ReachLocationObjective.class);
        registerObjective("BreedMobs", BreedObjective.class);


        //registerObjectiveCommandCompletionHandler("KillMobs", this::eee);
    }

    public String eee(){
        return "";
    }

   /* public void registerObjectiveCommandCompletionHandler(final String identifier, final String commandCompletionHandler){
        main.getLogManager().info("Registering command completions for objective <AQUA>" + identifier);
        objectiveCommandCompletionHandlers.put(identifier, commandCompletionHandler);

    }*/

    public void registerObjective(final String identifier, final Class<? extends Objective> objective){
        main.getLogManager().info("Registering objective <AQUA>" + identifier);
        objectives.put(identifier, objective);
    }




    public final Class<? extends Objective> getObjectiveClass(final String type){
        return objectives.get(type);
    }
    public final String getObjectiveType(final Class<? extends Objective> objective){
        for(final String objectiveType : objectives.keySet()){
            if(objectives.get(objectiveType).equals(objective)) {
                return objectiveType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Objective>> getObjectivesAndIdentfiers(){
        return objectives;
    }

    public final Collection<Class<? extends Objective>> getObjectives(){
        return objectives.values();
    }

    public final Collection<String> getObjectiveIdentifiers(){
        return objectives.keySet();
    }
}
