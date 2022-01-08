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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;


public class ConditionCondition extends Condition {

    private Condition condition = null;


    public ConditionCondition(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder
                .argument(StringArgument.<CommandSender>newBuilder("Condition Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition Identifier (name)]", "");

                            return new ArrayList<>(main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Condition Identifier"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Condition Condition to a quest (checks for another condition from conditions.yml)")
                .handler((context) -> {

                    final String conditionIdentifier = context.get("Condition Identifier");

                    final Condition foundCondition = main.getConditionsYMLManager().getCondition(conditionIdentifier);

                    if (foundCondition != null) {

                        ConditionCondition conditionCondition = new ConditionCondition(main);
                        conditionCondition.setCondition(foundCondition);

                        main.getConditionsManager().addCondition(conditionCondition, context);
                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Condition with the name <highlight>" + conditionIdentifier + "</highlight> does not exist!"));
                    }


                }));
    }

    public final Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        if (condition == null) {
            return "<warn>Error: ConditionCondition cannot be checked because the condition was not found. Report this to the server owner.";
        }

        return condition.check(questPlayer);
    }

    @Override
    public String getConditionDescription() {
        if (condition != null) {
            return "<unimportant>-- Complete Condition: <highlight>" + condition.getConditionName();
        } else {
            return "<unimportant>-- Complete Condition: Condition not found.";
        }

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if (getCondition() != null) {
            configuration.set(initialPath + ".specifics.condition", getCondition().getConditionName());
        } else {
            main.getLogManager().warn("Error: cannot save Condition for condition condition, because it's null. Configuration path: " + initialPath);
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        String conditionName = configuration.getString(initialPath + ".specifics.condition");
        this.condition = main.getConditionsYMLManager().getCondition(conditionName);
        if (condition == null) {
            main.getLogManager().warn("Error: ConditionCondition cannot find the condition with name " + conditionName + ". Condition Path: " + initialPath);
        }
    }
}
