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

package rocks.gravili.notquests.Structs.Requirements;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;


public class WorldTimeRequirement extends Requirement {

    private final NotQuests main;
    private final int minTime, maxTime;

    public WorldTimeRequirement(NotQuests main, final Quest quest, final int requirementID, long amountOfCompletionsNeeded) {
        super(main, quest, requirementID, 1);
        this.main = main;


        minTime = main.getDataManager().getQuestsConfig().getInt("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.minTime");
        maxTime = main.getDataManager().getQuestsConfig().getInt("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.maxTime");

    }

    public WorldTimeRequirement(NotQuests main, final Quest quest, final int requirementID, long amountOfCompletionsNeeded, int minTime, int maxTime) {
        super(main, quest, requirementID, 1);
        this.main = main;
        this.minTime = minTime;
        this.maxTime = maxTime;

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder) {
        manager.command(addRequirementBuilder.literal("WorldTime")
                .argument(IntegerArgument.<CommandSender>newBuilder("minTime").withMin(1), ArgumentDescription.of("Minimum world time (24-hour clock)"))
                .argument(IntegerArgument.<CommandSender>newBuilder("maxTime").withMin(1), ArgumentDescription.of("Maximum world time (24-hour clock)"))

                .meta(CommandMeta.DESCRIPTION, "Adds a new Time Requirement (24-hour-clock) to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int minTime = context.get("minTime");
                    final int maxTime = context.get("maxTime");

                    WorldTimeRequirement worldTimeRequirement = new WorldTimeRequirement(main, quest, quest.getRequirements().size() + 1, 1, minTime, maxTime);
                    quest.addRequirement(worldTimeRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "WorldTime Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    public final int getMinTime() {
        return minTime;
    }

    public final int getMaxTime() {
        return maxTime;
    }

    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        long currentTime = questPlayer.getPlayer().getWorld().getTime();

        if (currentTime >= 18000) {

            currentTime = currentTime / 1000 - 18;
        } else {

            currentTime = currentTime / 1000 + 6;
        }

        if (getMaxTime() >= getMinTime()) {
            if (currentTime <= getMaxTime() && currentTime >= getMinTime()) {
                return "";
            } else {
                return "\n§eCome back between §b" + getMinTime() + " §7and §b" + getMaxTime() + " §e(It's now " + currentTime + ")\n";
            }
        } else { //Maxtime is the next day
            if (currentTime <= getMinTime()) { //Chec for next day
                if (currentTime <= getMaxTime()) {
                    return "";
                } else {
                    return "\n§eCome back between §b" + getMinTime() + " §7and §b" + getMaxTime() + " §e(It's now " + currentTime + ")\n";
                }
            } else { //Check for current day
                if (currentTime >= getMinTime() && currentTime <= 24) {
                    return "";
                } else {
                    return "\n§eCome back between §b" + getMinTime() + " §7and §b" + getMaxTime() + " §e(It's now " + currentTime + ")\n";
                }
            }

        }


    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.minTime", getMinTime());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.maxTime", getMaxTime());

    }

    @Override
    public String getRequirementDescription() {
        return "§7-- World time: " + getMinTime() + " - " + getMaxTime();
    }
}
