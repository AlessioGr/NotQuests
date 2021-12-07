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

package rocks.gravili.notquests.Structs.Conditions;

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


public class WorldTimeCondition extends Condition {

    private final NotQuests main;
    private int minTime, maxTime;

    public WorldTimeCondition(NotQuests main, Object... objects) {
        super(main, objects);
        this.main = main;
    }



    public void setMinTime(final int minTime){
        this.minTime = minTime;
    }
    public void setMaxTime(final int maxTime){
        this.maxTime = maxTime;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder, Command.Builder<CommandSender> objectiveAddConditionBuilder) {
        manager.command(addRequirementBuilder.literal("WorldTime")
                .argument(IntegerArgument.<CommandSender>newBuilder("minTime").withMin(0).withMax(24), ArgumentDescription.of("Minimum world time (24-hour clock)"))
                .argument(IntegerArgument.<CommandSender>newBuilder("maxTime").withMin(0).withMax(24), ArgumentDescription.of("Maximum world time (24-hour clock)"))

                .meta(CommandMeta.DESCRIPTION, "Adds a new Time Requirement (24-hour-clock) to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int minTime = context.get("minTime");
                    final int maxTime = context.get("maxTime");

                    WorldTimeCondition worldTimeRequirement = new WorldTimeCondition(main, 1, quest);
                    worldTimeRequirement.setMinTime(minTime);
                    worldTimeRequirement.setMaxTime(maxTime);
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
    public String getConditionDescription() {
        return "§7-- World time: " + getMinTime() + " - " + getMaxTime();
    }

    @Override
    public void save(String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.minTime", getMinTime());
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.maxTime", getMaxTime());

    }

    @Override
    public void load(String initialPath) {
        minTime = main.getDataManager().getQuestsConfig().getInt(initialPath + ".specifics.minTime");
        maxTime = main.getDataManager().getQuestsConfig().getInt(initialPath + ".specifics.maxTime");

    }
}
