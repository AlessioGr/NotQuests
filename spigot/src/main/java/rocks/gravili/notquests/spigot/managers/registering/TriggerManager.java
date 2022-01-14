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

package rocks.gravili.notquests.spigot.managers.registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.spigot.structs.triggers.Trigger;
import rocks.gravili.notquests.spigot.structs.triggers.types.*;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.structs.Quest;
import rocks.gravili.notquests.spigot.structs.actions.Action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class TriggerManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Trigger>> triggers;


    public TriggerManager(final NotQuests main) {
        this.main = main;
        triggers = new HashMap<>();

        registerDefaultTriggers();

    }

    public void registerDefaultTriggers() {
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
        main.getLogManager().info("Registering trigger <AQUA>" + identifier);
        triggers.put(identifier, trigger);

        try {
            Method commandHandler = trigger.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
            commandHandler.invoke(trigger, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddTriggerCommandBuilder());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Trigger> getTriggerClass(final String type) {
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
        Audience audience = main.adventure().sender(context.getSender());

        Quest quest = context.getOrDefault("quest", null);

        final Action action = context.get("action");


        int applyOn = 0;
        if (context.flags().contains(main.getCommandManager().applyOn)) {
            applyOn = context.flags().getValue(main.getCommandManager().applyOn, 0);
        }
        final String worldString = context.flags().getValue(main.getCommandManager().triggerWorldString, "ALL");

        int amount = 1;
        if (context.contains("amount")) {
            amount = context.get("amount");
        }

        if (quest != null) {
            trigger.setQuest(quest);
            trigger.setAction(action);
            trigger.setApplyOn(applyOn);
            trigger.setWorldName(worldString);
            trigger.setTriggerID(quest.getTriggers().size() + 1);
            trigger.setAmountNeeded(amount);

            quest.addTrigger(trigger, true);

            audience.sendMessage(MiniMessage.miniMessage().deserialize(
                    NotQuestColors.successGradient + getTriggerType(trigger.getClass()) + " Trigger successfully added to Quest " + NotQuestColors.highlightGradient
                            + quest.getQuestName() + "</gradient>!</gradient>"
            ));
        }
    }
}
