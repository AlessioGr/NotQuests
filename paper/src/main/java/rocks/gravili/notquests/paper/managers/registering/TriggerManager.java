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
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;
import rocks.gravili.notquests.paper.structs.triggers.types.BeginTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.CompleteTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.DeathTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.DisconnectTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.FailTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.NPCDeathTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.WorldEnterTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.WorldLeaveTrigger;

public class TriggerManager {
  private final NotQuests main;

  private final HashMap<String, Class<? extends Trigger>> triggers;

  public TriggerManager(final NotQuests main) {
    this.main = main;
    triggers = new HashMap<>();

    registerDefaultTriggers();
  }

  public void registerDefaultTriggers() {
    main.getLogManager().info("Registering triggers...");

    triggers.clear();
    registerTrigger("BEGIN", BeginTrigger.class);
    registerTrigger("COMPLETE", CompleteTrigger.class);
    registerTrigger("DEATH", DeathTrigger.class);
    registerTrigger("DISCONNECT", DisconnectTrigger.class);
    registerTrigger("FAIL", FailTrigger.class);
    registerTrigger("NPCDEATH", NPCDeathTrigger.class);
    registerTrigger("WORLDENTER", WorldEnterTrigger.class);
    registerTrigger("WORLDLEAVE", WorldLeaveTrigger.class);
  }

  public void registerTrigger(final String identifier, final Class<? extends Trigger> trigger) {
    if (main.getConfiguration().isVerboseStartupMessages()) {
      main.getLogManager().info("Registering trigger <highlight>" + identifier);
    }
    triggers.put(identifier, trigger);

    try {
      Method commandHandler =
          trigger.getMethod(
              "handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
      commandHandler.invoke(
          trigger,
          main,
          main.getCommandManager().getPaperCommandManager(),
          main.getCommandManager()
              .getAdminEditAddTriggerCommandBuilder()
              .literal(identifier)
              .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " trigger"));
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public final Class<? extends Trigger> getTriggerClass(@NotNull final String type) {
    return triggers.get(type);
  }

  public final String getTriggerType(final Class<? extends Trigger> trigger) {
    for (final String triggerType : triggers.keySet()) {
      if (triggers.get(triggerType).equals(trigger)) {
        return triggerType;
      }
    }
    return null;
  }

  public final HashMap<String, Class<? extends Trigger>> getTriggersAndIdentifiers() {
    return triggers;
  }

  public final Collection<Class<? extends Trigger>> getTriggers() {
    return triggers.values();
  }

  public final Collection<String> getTriggerIdentifiers() {
    return triggers.keySet();
  }

  public void addTrigger(Trigger trigger, CommandContext<CommandSender> context) {
    Quest quest = context.getOrDefault("quest", null);

    final Action action = context.get("action");

    int applyOn = 0;
    if (context.flags().contains(main.getCommandManager().applyOn)) {
      applyOn = context.flags().getValue(main.getCommandManager().applyOn, 0);
    }
    final String worldString =
        context.flags().getValue(main.getCommandManager().triggerWorldString, "ALL");

    int amount = 1;
    if (context.contains("amount")) {
      amount = context.get("amount");
    }

    if (quest != null) {
      trigger.setQuest(quest);
      trigger.setAction(action);
      trigger.setApplyOn(applyOn);
      trigger.setWorldName(worldString);
      trigger.setTriggerID(quest.getFreeTriggerID());
      trigger.setAmountNeeded(amount);

      quest.addTrigger(trigger, true);

      context
          .getSender()
          .sendMessage(
              main.parse(
                  "<success>"
                      + getTriggerType(trigger.getClass())
                      + " Trigger successfully added to Quest <highlight>"
                      + quest.getIdentifier()
                      + "</highlight>!"));
    }
  }
}
