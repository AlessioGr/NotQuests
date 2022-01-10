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

package rocks.gravili.notquests.paper.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class CompletedQuestCondition extends Condition {

    private String otherQuestName = "";
    private long minimumTimeAfterCompletion = -1; //time in minutes. -1 or smaller => no cooldown.

    public CompletedQuestCondition(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder
                .argument(QuestSelector.of("otherQuest", main), ArgumentDescription.of("Name of the other Quest the player has to complete."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of completions needed"))
                .flag(main.getCommandManager().minimumTimeAfterCompletion)
                .meta(CommandMeta.DESCRIPTION, "Adds a new CompletedQuest Requirement to a quest")
                .handler((context) -> {

                    final Quest otherQuest = context.get("otherQuest");
                    final int amount = context.get("amount");

                    long minimumWaitTimeAfterQuestCompletion = -1;
                    if (context.flags().contains(main.getCommandManager().minimumTimeAfterCompletion)) {
                        minimumWaitTimeAfterQuestCompletion = context.flags().getValue(main.getCommandManager().minimumTimeAfterCompletion, -1L);
                    }

                    CompletedQuestCondition completedQuestCondition = new CompletedQuestCondition(main);
                    completedQuestCondition.setProgressNeeded(amount);
                    completedQuestCondition.setOtherQuestName(otherQuest.getQuestName());
                    completedQuestCondition.setMinimumTimeAfterCompletion(minimumWaitTimeAfterQuestCompletion);

                    main.getConditionsManager().addCondition(completedQuestCondition, context);


                }));
    }

    public final long getMinimumTimeAfterCompletion() {
        return minimumTimeAfterCompletion;
    }

    public void setOtherQuestName(final String otherQuestName) {
        this.otherQuestName = otherQuestName;
    }

    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }


    public final long getAmountOfCompletionsNeeded() {
        return getProgressNeeded();
    }

    public void setMinimumTimeAfterCompletion(final long minimumTimeAfterCompletion) {
        this.minimumTimeAfterCompletion = minimumTimeAfterCompletion;
    }

    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        final Quest otherQuest = getOtherQuest();

        if (otherQuest == null) {
            return "<YELLOW> Cannot check CompletedQuest Condition because the specified Quest is null. Report this to the server owner.";
        }

        int otherQuestCompletedAmount = 0;
        long mostRecentAcceptTime = 0;

        for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.getQuest().equals(otherQuest)) {
                otherQuestCompletedAmount += 1;
                if (completedQuest.getTimeCompleted() > mostRecentAcceptTime) {
                    mostRecentAcceptTime = completedQuest.getTimeCompleted();
                }
            }
        }
        if (otherQuestCompletedAmount < getProgressNeeded()) {
            return "<YELLOW>Finish the following quest: <highlight>" + otherQuest.getQuestFinalName() + " <GRAY>(" + getProgressNeeded() + " times)";
        } else {
            //Now check minimum time after completion
            if (getMinimumTimeAfterCompletion() > 1) {
                final long acceptTimeDifference = System.currentTimeMillis() - mostRecentAcceptTime;
                final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);


                final long timeToWaitInMinutes = getMinimumTimeAfterCompletion() - acceptTimeDifferenceMinutes;
                final double timeToWaitInHours = Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
                final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;

                if (acceptTimeDifferenceMinutes >= getMinimumTimeAfterCompletion()) {
                    return "";
                } else {
                    if (timeToWaitInMinutes < 60) {
                        return "<YELLOW>You have to wait another <highlight>" + timeToWaitInMinutes + " minutes</highlight>.";
                    } else {
                        if (timeToWaitInHours < 24) {
                            if (timeToWaitInHours == 1) {
                                return "<YELLOW>You have to wait another <highlight>" + timeToWaitInHours + " hour</highlight>.";

                            } else {
                                return "<YELLOW>You have to wait another <highlight>" + timeToWaitInHours + " hours</highlight>.";
                            }
                        } else {
                            if (timeToWaitInDays == 1) {
                                return "<YELLOW>You have to wait another <highlight>" + timeToWaitInDays + " day</highlight>.";

                            } else {
                                return "<YELLOW>You have to wait another <highlight>" + timeToWaitInDays + " days</highlight>.";
                            }
                        }
                    }
                }
            }


            return "";
        }
    }

    @Override
    public String getConditionDescription() {
        String waitItemAddition = "";
        if (getMinimumTimeAfterCompletion() > 0) {
            final double timeToWaitInHours = Math.round((getMinimumTimeAfterCompletion() / 60f) * 10) / 10.0;
            final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;
            if (getMinimumTimeAfterCompletion() < 60) {
                waitItemAddition = " <GRAY>(and wait " + getMinimumTimeAfterCompletion() + " minutes after completion";
            } else {
                if (timeToWaitInHours < 24) {
                    if (timeToWaitInHours == 1) {
                        waitItemAddition = " <GRAY>(and wait " + timeToWaitInHours + " hour after completion";
                    } else {
                        waitItemAddition = " <GRAY>(and wait " + timeToWaitInHours + " hours after completion";
                    }
                } else {
                    if (timeToWaitInDays == 1) {
                        waitItemAddition = " <GRAY>(and wait " + timeToWaitInDays + " day after completion";
                    } else {
                        waitItemAddition = " <GRAY>(and wait " + timeToWaitInDays + " days after completion";
                    }
                }
            }

        }

        final Quest otherQuest = getOtherQuest();
        if (otherQuest != null) {
            return "<GRAY>-- Finish Quest first: " + otherQuest.getQuestFinalName() + waitItemAddition;
        } else {
            return "<GRAY>-- Finish Quest first: " + getOtherQuestName() + waitItemAddition;
        }

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.otherQuest", getOtherQuestName());
        configuration.set(initialPath + ".specifics.waitTimeAfterCompletion", getMinimumTimeAfterCompletion());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        otherQuestName = configuration.getString(initialPath + ".specifics.otherQuest", "");
        if (otherQuestName.isBlank()) { //Converter
            otherQuestName = configuration.getString(initialPath + ".specifics.otherQuestRequirememt", "");
            if (!otherQuestName.isBlank()) {
                configuration.set(initialPath + ".specifics.otherQuestRequirememt", null);
                configuration.set(initialPath + ".specifics.otherQuest", otherQuestName);
            }
        }

        minimumTimeAfterCompletion = configuration.getLong(initialPath + ".specifics.waitTimeAfterCompletion", -1L);
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        otherQuestName = arguments.get(0);

        setProgressNeeded(Long.parseLong(arguments.get(1)));

        if(arguments.size() >= 3){
            minimumTimeAfterCompletion = Long.parseLong(arguments.get(2));
        }else{
            minimumTimeAfterCompletion = -1L;
        }
    }
}
