package rocks.gravili.Managers;

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Objectives.*;
import rocks.gravili.Structs.Objectives.*;

import java.util.Collection;
import java.util.HashMap;

public class ObjectiveManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;

    public ObjectiveManager(final NotQuests main){
        this.main = main;
        objectives = new HashMap<>();


    }

    public void registerDefaultObjectives(){
        objectives.clear();
        objectives.put("BreakBlocks", BreakBlocksObjective.class);
        objectives.put("CollectItems", CollectItemsObjective.class);
        objectives.put("TriggerCommand", TriggerCommandObjective.class);
        objectives.put("OtherQuest", OtherQuestObjective.class);
        objectives.put("KillMobs", KillMobsObjective.class);
        objectives.put("ConsumeItems", ConsumeItemsObjective.class);
        objectives.put("DeliverItems", DeliverItemsObjective.class);
        objectives.put("TalkToNPC", TalkToNPCObjective.class);
        objectives.put("EscortNPC", EscortNPCObjective.class);
        objectives.put("CraftItems", CraftItemsObjective.class);
        objectives.put("KillEliteMobs", KillMobsObjective.class); //TODO: only if EliteMobs enabled?
        objectives.put("ReachLocation", ReachLocationObjective.class);
    }


    public void registerObjective(final Class<? extends Objective> objective, final String identifier){
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
