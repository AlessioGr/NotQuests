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

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.BreakBlocksObjective;
import rocks.gravili.notquests.paper.structs.objectives.BreedObjective;
import rocks.gravili.notquests.paper.structs.objectives.CollectItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.ConditionObjective;
import rocks.gravili.notquests.paper.structs.objectives.ConsumeItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.CraftItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.DeliverItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.FishItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.InteractObjective;
import rocks.gravili.notquests.paper.structs.objectives.JumpObjective;
import rocks.gravili.notquests.paper.structs.objectives.KillMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.NumberVariableObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.OpenBuriedTreasureObjective;
import rocks.gravili.notquests.paper.structs.objectives.OtherQuestObjective;
import rocks.gravili.notquests.paper.structs.objectives.PlaceBlocksObjective;
import rocks.gravili.notquests.paper.structs.objectives.ReachLocationObjective;
import rocks.gravili.notquests.paper.structs.objectives.RunCommandObjective;
import rocks.gravili.notquests.paper.structs.objectives.ShearSheepObjective;
import rocks.gravili.notquests.paper.structs.objectives.SmeltObjective;
import rocks.gravili.notquests.paper.structs.objectives.SneakObjective;
import rocks.gravili.notquests.paper.structs.objectives.TalkToNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest.BetonQuestObjectiveStateChangeObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs.KillEliteMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn.JobsRebornReachJobLevelObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.projectkorra.ProjectKorraUseAbilityObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.slimefun.SlimefunResearchObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyNationReachTownCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyReachResidentCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.ultimatejobs.UltimateJobsReachJobLevelObjective;

public class ObjectiveManager {
  private final NotQuests main;

  private final HashMap<String, Class<? extends Objective>> objectives;

  public ObjectiveManager(final NotQuests main) {
    this.main = main;
    objectives = new HashMap<>();

    registerDefaultObjectives();
  }

  public void registerDefaultObjectives() {
    objectives.clear();
    registerObjective("Condition", ConditionObjective.class);
    registerObjective("BreakBlocks", BreakBlocksObjective.class);
    registerObjective("PlaceBlocks", PlaceBlocksObjective.class);
    registerObjective("CollectItems", CollectItemsObjective.class);
    registerObjective("FishItems", FishItemsObjective.class);

    registerObjective("TriggerCommand", TriggerCommandObjective.class);
    registerObjective("OtherQuest", OtherQuestObjective.class);
    registerObjective("KillMobs", KillMobsObjective.class);
    registerObjective("ConsumeItems", ConsumeItemsObjective.class);
    registerObjective("DeliverItems", DeliverItemsObjective.class);
    registerObjective("TalkToNPC", TalkToNPCObjective.class);
    registerObjective("EscortNPC", EscortNPCObjective.class);
    registerObjective("CraftItems", CraftItemsObjective.class);
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

    registerObjective("NumberVariable", NumberVariableObjective.class); //Special

    // Towny
    registerObjective("TownyReachResidentCount", TownyReachResidentCountObjective.class);
    registerObjective("TownyNationReachTownCount", TownyNationReachTownCountObjective.class);

    // Jobs
    registerObjective("JobsRebornReachJobLevel", JobsRebornReachJobLevelObjective.class);

    // ProjectKorra
    registerObjective("ProjectKorraUseAbility", ProjectKorraUseAbilityObjective.class);

    if (main.getIntegrationsManager().isBetonQuestEnabled()) {
      registerObjective(
          "BetonQuestObjectiveStateChange", BetonQuestObjectiveStateChangeObjective.class);
    }

    if (main.getIntegrationsManager().isUltimateJobsEnabled()) {
      registerObjective("UltimateJobsReachJobLevel", UltimateJobsReachJobLevelObjective.class);
    }

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
      Method commandHandler =
          objective.getMethod(
              "handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
      commandHandler.invoke(
          objective,
          main,
          main.getCommandManager().getPaperCommandManager(),
          main.getCommandManager()
              .getAdminEditAddObjectiveCommandBuilder()
              .literal(identifier)
              .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " objective")
              .flag(main.getCommandManager().taskDescription));
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

  public void addObjective(Objective objective, CommandContext<CommandSender> context) {

    final Quest quest = context.getOrDefault("quest", null);

    final String taskDescription =
        context.flags().getValue(main.getCommandManager().taskDescription, "");

    if (quest != null) {
      objective.setQuest(quest);
      objective.setObjectiveID(quest.getFreeObjectiveID());
      if(taskDescription != null && !taskDescription.isBlank()) {
        objective.setTaskDescription(taskDescription, true);
      }

      context
          .getSender()
          .sendMessage(
              main.parse(
                  "<success>"
                      + getObjectiveType(objective.getClass())
                      + " Objective successfully added to Quest <highlight>"
                      + quest.getQuestName()
                      + "</highlight>!"));

      quest.addObjective(objective, true);
    }
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
                  .literal(identifier)
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " objective"));

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
