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

package rocks.gravili.notquests.paper.managers.registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.*;
import rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs.KillEliteMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn.JobsRebornReachJobLevel;
import rocks.gravili.notquests.paper.structs.objectives.hooks.projectkorra.ProjectKorraUseAbilityObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.slimefun.SlimefunResearchObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyNationReachTownCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyReachResidentCountObjective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class ObjectiveManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;



    public ObjectiveManager(final NotQuests main){
        this.main = main;
        objectives = new HashMap<>();

        registerDefaultObjectives();

    }

    public void registerDefaultObjectives(){
        objectives.clear();
        registerObjective("BreakBlocks", BreakBlocksObjective.class);
        registerObjective("PlaceBlocks", PlaceBlocksObjective.class);
        registerObjective("CollectItems", CollectItemsObjective.class);
        registerObjective("TriggerCommand", TriggerCommandObjective.class);
        registerObjective("OtherQuest", OtherQuestObjective.class);
        registerObjective("KillMobs", KillMobsObjective.class);
        registerObjective("ConsumeItems", ConsumeItemsObjective.class);
        registerObjective("DeliverItems", DeliverItemsObjective.class);
        registerObjective("TalkToNPC", TalkToNPCObjective.class);
        registerObjective("EscortNPC", EscortNPCObjective.class);
        registerObjective("CraftItems", CraftItemsObjective.class);
        registerObjective("KillEliteMobs", KillEliteMobsObjective.class); //TODO: only if EliteMobs enabled?
        registerObjective("ReachLocation", ReachLocationObjective.class);
        registerObjective("BreedMobs", BreedObjective.class);
        registerObjective("SlimefunResearch", SlimefunResearchObjective.class);
        registerObjective("RunCommand", RunCommandObjective.class);
        registerObjective("Interact", InteractObjective.class);
        registerObjective("Jump", JumpObjective.class);
        registerObjective("Sneak", SneakObjective.class);
        registerObjective("SmeltItems", SmeltObjective.class);

        //Towny
        registerObjective("TownyReachResidentCount", TownyReachResidentCountObjective.class);
        registerObjective("TownyNationReachTownCount", TownyNationReachTownCountObjective.class);

        //Jobs
        registerObjective("JobsRebornReachJobLevel", JobsRebornReachJobLevel.class);

        //ProjectKorra
        registerObjective("ProjectKorraUseAbility", ProjectKorraUseAbilityObjective.class);


        //registerObjectiveCommandCompletionHandler("KillMobs", this::eee);
    }


   /* public void registerObjectiveCommandCompletionHandler(final String identifier, final String commandCompletionHandler){
        main.getLogManager().info("Registering command completions for objective <highlight>" + identifier);
        objectiveCommandCompletionHandlers.put(identifier, commandCompletionHandler);

    }*/

    public void registerObjective(final String identifier, final Class<? extends Objective> objective) {
        main.getLogManager().info("Registering objective <highlight>" + identifier);
        objectives.put(identifier, objective);

        try {
            Method commandHandler = objective.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
            commandHandler.invoke(objective, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddObjectiveCommandBuilder());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
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

    public final HashMap<String, Class<? extends Objective>> getObjectivesAndIdentifiers() {
        return objectives;
    }

    public final Collection<Class<? extends Objective>> getObjectives() {
        return objectives.values();
    }

    public final Collection<String> getObjectiveIdentifiers() {
        return objectives.keySet();
    }


    public void addObjective(Objective objective, CommandContext<CommandSender> context) {

        Quest quest = context.getOrDefault("quest", null);

        if (quest != null) {
            objective.setQuest(quest);
            objective.setObjectiveID(quest.getObjectives().size() + 1);
            context.getSender().sendMessage(main.parse(
                    "<success>" + getObjectiveType(objective.getClass()) + " Objective successfully added to Quest <highlight>"
                            + quest.getQuestName() + "</highlight>!"
            ));

            quest.addObjective(objective, true);
        }
    }
}
