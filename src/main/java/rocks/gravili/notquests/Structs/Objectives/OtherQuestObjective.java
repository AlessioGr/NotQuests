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

package rocks.gravili.notquests.Structs.Objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class OtherQuestObjective extends Objective {
    private final NotQuests main;
    private final String otherQuestName;
    private final boolean countPreviousCompletions;


    public OtherQuestObjective(NotQuests main, final Quest quest, final int objectiveID, String otherQuestName, int amountOfCompletionsNeeded, boolean countPreviousCompletions) {
        super(main, quest, objectiveID, amountOfCompletionsNeeded);
        this.main = main;
        this.otherQuestName = otherQuestName;
        this.countPreviousCompletions = countPreviousCompletions;

    }

    public OtherQuestObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        otherQuestName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.otherQuestName");
        countPreviousCompletions = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.countPreviousCompletions");
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.otherQuest.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%OTHERQUESTNAME%", "" + getOtherQuest().getQuestName());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.otherQuestName", getOtherQuestName());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.countPreviousCompletions", isCountPreviousCompletions());
    }

    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    public final long getAmountOfCompletionsNeeded() {
        return super.getProgressNeeded();
    }

    public final boolean isCountPreviousCompletions() {
        return countPreviousCompletions;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("OtherQuest")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the other Quest the player has to complete."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the Quest needs to be completed."))
                .flag(
                        manager.flagBuilder("countPreviouslyCompletedQuests")
                                .withDescription(ArgumentDescription.of("Makes it so quests completed before this OtherQuest objective becomes active will be counted towards the progress too."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new OtherQuest Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final Quest otherQuest = context.get("otherquest");
                    final int amount = context.get("amount");
                    final boolean countPreviouslyCompletedQuests = context.flags().isPresent("countPreviouslyCompletedQuests");

                    OtherQuestObjective otherQuestObjective = new OtherQuestObjective(main, quest, quest.getObjectives().size() + 1, otherQuest.getQuestName(), amount, countPreviouslyCompletedQuests);

                    quest.addObjective(otherQuestObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "OtherQuest Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
