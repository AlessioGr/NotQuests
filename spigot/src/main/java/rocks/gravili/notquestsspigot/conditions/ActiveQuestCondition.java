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

package rocks.gravili.notquestsspigot.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.commands.arguments.QuestSelector;
import rocks.gravili.notquestsspigot.structs.ActiveQuest;
import rocks.gravili.notquestsspigot.structs.Quest;
import rocks.gravili.notquestsspigot.structs.QuestPlayer;


public class ActiveQuestCondition extends Condition {

    private String otherQuestName = "";

    public ActiveQuestCondition(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder.literal("ActiveQuest")
                .argument(QuestSelector.of("otherQuest", main), ArgumentDescription.of("Name of the other Quest which needs to be active for the player."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ActiveQuest Requirement to a quest")
                .handler((context) -> {
                    final Quest otherQuest = context.get("otherQuest");

                    ActiveQuestCondition activeQuestCondition = new ActiveQuestCondition(main);
                    activeQuestCondition.setOtherQuestName(otherQuest.getQuestName());

                    main.getConditionsManager().addCondition(activeQuestCondition, context);
                }));
    }

    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public void setOtherQuestName(final String otherQuestName) {
        this.otherQuestName = otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final Quest otherQuest = getOtherQuest();

        if (otherQuest == null) {
            return "<RED>Error! Report this to an admin: The following Quest which should be active was not found: <AQUA>" + getOtherQuestName();

        }

        for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuest().equals(otherQuest)) {
                return "";
            }
        }
        return "<YELLOW>Following Quest needs to be active first: <AQUA>" + otherQuest.getQuestFinalName();

    }

    @Override
    public String getConditionDescription() {

        final Quest otherQuest = getOtherQuest();
        if (otherQuest != null) {
            return "<GRAY>-- Have active Quest: " + otherQuest.getQuestFinalName();
        } else {
            return "<GRAY>-- Have active Quest: " + getOtherQuestName();
        }

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.otherQuest", getOtherQuestName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        otherQuestName = configuration.getString(initialPath + ".specifics.otherQuest");
    }
}
