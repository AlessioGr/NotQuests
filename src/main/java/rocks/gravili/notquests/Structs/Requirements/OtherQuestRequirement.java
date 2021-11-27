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
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.CompletedQuest;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;


public class OtherQuestRequirement extends Requirement {

    private final NotQuests main;
    private final String otherQuestName;
    private final long amountOfCompletionsNeeded;


    public OtherQuestRequirement(NotQuests main, final Quest quest, final int requirementID, long amountOfCompletionsNeeded) {
        super(main, quest, requirementID, amountOfCompletionsNeeded);
        this.main = main;
        this.amountOfCompletionsNeeded = amountOfCompletionsNeeded;

        otherQuestName = main.getDataManager().getQuestsData().getString("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.otherQuestRequirememt");
    }

    public OtherQuestRequirement(NotQuests main, final Quest quest, final int requirementID, int amountOfCompletionsNeeded, String otherQuestName) {
        super(main, quest, requirementID, amountOfCompletionsNeeded);
        this.main = main;
        this.amountOfCompletionsNeeded = amountOfCompletionsNeeded;
        this.otherQuestName = otherQuestName;

    }


    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }



    public final long getAmountOfCompletionsNeeded() {
        return amountOfCompletionsNeeded;
    }

    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final Quest otherQuest = getOtherQuest();

        int otherQuestCompletedAmount = 0;

        for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(otherQuest)) {
                otherQuestCompletedAmount += 1;
            }
        }
        if (otherQuestCompletedAmount < getProgressNeeded()) {
            return "\n§eFinish the following quest: §b" + getOtherQuestName() + " §7(" + getProgressNeeded() + " times)\n";
        } else {
            return "";
        }
    }


    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.otherQuestRequirememt", getOtherQuestName());
    }

    @Override
    public String getRequirementDescription() {
        return "§7-- Finish Quest first: " + getOtherQuestName();
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder) {
        manager.command(addRequirementBuilder.literal("OtherQuest")
                .argument(QuestSelector.of("otherQuest", main), ArgumentDescription.of("Name of the other Quest the player has to complete."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of completions needed"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new OtherQuest Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final Quest otherQuest = context.get("otherQuest");
                    final int amount = context.get("amount");

                    OtherQuestRequirement otherQuestRequirement = new OtherQuestRequirement(main, quest, quest.getRequirements().size() + 1, amount, otherQuest.getQuestName());
                    quest.addRequirement(otherQuestRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "OtherQuest Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
