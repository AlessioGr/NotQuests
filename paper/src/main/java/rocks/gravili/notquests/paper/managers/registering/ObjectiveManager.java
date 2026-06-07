/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.objectives.*;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs.KillEliteMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn.JobsRebornReachJobLevelObjective;

import rocks.gravili.notquests.paper.structs.objectives.hooks.slimefun.SlimefunResearchObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyNationReachTownCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyReachResidentCountObjective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import static rocks.gravili.notquests.paper.commands.arguments.ObjectiveParser.objectiveParser;

public class ObjectiveManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Objective>> objectives;

    public ObjectiveManager(final NotQuests main) {
        this.main = main;
        objectives = new HashMap<>();

        registerDefaultObjectives();
    }

    public void registerDefaultObjectives() {
        main.getLogManager().info("Registering objectives...");

        objectives.clear();
        registerObjective("Condition", ConditionObjective.class);
        registerObjective("BreakBlocks", BreakBlocksObjective.class);
        registerObjective("PlaceBlocks", PlaceBlocksObjective.class);
        registerObjective("PickupItems", PickupItemsObjective.class);
        registerObjective("FishItems", FishItemsObjective.class);

        registerObjective("TriggerCommand", TriggerCommandObjective.class);
        registerObjective("OtherQuest", OtherQuestObjective.class);
        registerObjective("KillMobs", KillMobsObjective.class);
        registerObjective("ConsumeItems", ConsumeItemsObjective.class);
        registerObjective("DeliverItems", DeliverItemsObjective.class);
        registerObjective("TalkToNPC", TalkToNPCObjective.class);
        registerObjective("EscortNPC", EscortNPCObjective.class);
        registerObjective("CraftItems", CraftItemsObjective.class);
        registerObjective("Enchant", EnchantObjective.class);

        registerObjective(
                "KillEliteMobs", KillEliteMobsObjective.class); // TODO: only if EliteMobs enabled?
        registerObjective("ReachLocation", ReachLocationObjective.class);
        registerObjective("BreedMobs", BreedObjective.class);
        registerObjective("SlimefunResearch", SlimefunResearchObjective.class);
        registerObjective("RunCommand", RunCommandObjective.class);
        registerObjective("Interact", InteractObjective.class);
        registerObjective("Jump", JumpObjective.class);
        registerObjective("Sneak", SneakObjective.class);
        registerObjective("SmeltItems", SmeltObjective.class);
        registerObjective("OpenBuriedTreasure", OpenBuriedTreasureObjective.class);
        registerObjective("ShearSheep", ShearSheepObjective.class);
        registerObjective("MilkCow", MilkCowObjective.class);
        registerObjective("Objective", ObjectiveObjective.class);

        registerObjective("NumberVariable", NumberVariableObjective.class); //Special

        // Towny
        registerObjective("TownyReachResidentCount", TownyReachResidentCountObjective.class);
        registerObjective("TownyNationReachTownCount", TownyNationReachTownCountObjective.class);

        // Jobs
        registerObjective("JobsRebornReachJobLevel", JobsRebornReachJobLevelObjective.class);

        // registerObjectiveCommandCompletionHandler("KillMobs", this::eee);
    }

  /* public void registerObjectiveCommandCompletionHandler(final String identifier, final String commandCompletionHandler){
      main.getLogManager().info("Registering command completions for objective <highlight>" + identifier);
      objectiveCommandCompletionHandlers.put(identifier, commandCompletionHandler);

  }*/

    public void registerObjective(
            final String identifier, final Class<? extends Objective> objective) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering objective <highlight>" + identifier);
        }
        objectives.put(identifier, objective);

        try {
            Method commandHandler = objective.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, int.class);

            //Level 0
            final Command.Builder<CommandSender> objectivesBuilder = main.getCommandManager().getAdminEditCommandBuilder().literal("objectives", "o");
            final Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilder =
                    objectivesBuilder.literal("add");

            commandHandler.invoke(objective, main, main.getCommandManager().getPaperCommandManager(), adminEditAddObjectiveCommandBuilder
                    .literal(identifier, Description.of("Creates a new " + identifier + " objective"))
                    .flag(main.getCommandManager().taskDescription), 0);

            //Level 1
            final String objectiveIDIdentifier = "objectiveId";
            final Command.Builder<CommandSender> objectivesBuilderLevel1 = objectivesBuilder.literal("edit").required(objectiveIDIdentifier, objectiveParser(main, 0), Description.of(objectiveIDIdentifier));


            final Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilderLevel1 =
                    objectivesBuilderLevel1.literal("objectives", "o").literal("add");

            //Level 1
            commandHandler.invoke(objective, main, main.getCommandManager().getPaperCommandManager(), adminEditAddObjectiveCommandBuilderLevel1
                    .literal(identifier, Description.of("Creates a new " + identifier + " objective"))
                    .flag(main.getCommandManager().taskDescription), 1);


            final Command.Builder<CommandSender> objectivesBuilder2 = objectivesBuilderLevel1.literal("objectives", "");
            final String objectiveIDIdentifier2 = "objectiveId2";
            final int level2 = 2;
            final Command.Builder<CommandSender> objectivesBuilderLevel2 = objectivesBuilder2
                    .literal("edit")
                    .required(objectiveIDIdentifier2, objectiveParser(main, 1), Description.of(objectiveIDIdentifier2));


            final Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilderLevel2 =
                    objectivesBuilderLevel2.literal("objectives", "o").literal("add");

            //Level 2
            commandHandler.invoke(
                    objective,
                    main,
                    main.getCommandManager().getPaperCommandManager(),
                    adminEditAddObjectiveCommandBuilderLevel2
                            .literal(identifier, Description.of("Creates a new " + identifier + " objective"))
                            .flag(main.getCommandManager().taskDescription), 2);

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public final Class<? extends Objective> getObjectiveClass(@NotNull final String type) {
        return objectives.get(type);
    }

    public final String getObjectiveType(final Class<? extends Objective> objective) {
        for (final String objectiveType : objectives.keySet()) {
            if (objectives.get(objectiveType).equals(objective)) {
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

    public void addObjective(Objective objective, CommandContext<CommandSender> context, int level) {


        final ObjectiveHolder objectiveHolder = main.getCommandManager().getObjectiveHolderFromContextAndLevel(context, level);
        final Optional<Component> _taskDescription = context.flags().getValue(main.getCommandManager().taskDescription);
        objective.setObjectiveHolder(objectiveHolder);
        objective.setObjectiveID(objectiveHolder.getFreeObjectiveID());
        if(_taskDescription.isPresent()){
            String taskDescription = MiniMessage.builder().build().serialize(_taskDescription.get());
            if (!taskDescription.isBlank()) {
                objective.setTaskDescription(taskDescription, true);
            }
        }
        context.sender().sendMessage(main.parse("<success>" + getObjectiveType(objective.getClass()) + " Objective successfully added to Quest <highlight>" + objectiveHolder.getIdentifier() + "</highlight>!"));
        objectiveHolder.addObjective(objective, true);
    }

    public void updateVariableObjectives() {
        try {
            for (final Class<? extends Objective> objective : getObjectives()) {
                final String identifier = getObjectiveType(objective);

                final Method commandHandler =
                        objective.getMethod(
                                "handleCommands",
                                main.getClass(),
                                PaperCommandManager.class,
                                Command.Builder.class);
                if (identifier != null && objective == NumberVariableObjective.class) {

                    main.getLogManager()
                            .info("Re-registering objective " + identifier + " due to variable changes...");

                    commandHandler.invoke(
                            objective,
                            main,
                            main.getCommandManager().getPaperCommandManager(),
                            main.getCommandManager()
                                    .getAdminEditAddObjectiveCommandBuilder()
                                    .literal(identifier, Description.of("Creates a new " + identifier + " objective")));

                    //TODO Check if right? Why action stuff?
                    //TODO: Maybe remove everything below? Why is that there?
                    //TODO: I removed it for now.
          /*
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminEditAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
          );
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminEditObjectiveAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
          );
          commandHandler.invoke(
              objective,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminAddActionCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                  .flag(main.getCommandManager().categoryFlag)
          ); // For Actions.yml*/
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
