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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.neostorm.neostorm.Api;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GiveSkillLevelAction extends Action {

    private String nqSkill;
    private int nqLevelAmount;

    public GiveSkillLevelAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> builder,
            ActionFor actionFor) {
        manager.command(
                builder
                        .argument(
                                MiniMessageSelector.<CommandSender>newBuilder("Sending Message", main)
                                        .withPlaceholders()
                                        .build(),
                                ArgumentDescription.of("Message to broadcast"))
                        .argument(StringArgument.<CommandSender>newBuilder("Skill to level up").withSuggestionsProvider((context, lastString) -> {
                            ArrayList<String> completions = new ArrayList<>(Arrays.asList(Api.getStatTable()));
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Skill name]", "");
                            return completions;
                        }).build(), ArgumentDescription.of("The name of the skill"))
                        .handler(
                                (context) -> {
                                    String skillName = (String)context.get("skillName");
                                    int levelAmount = (int)context.get("amount");
                                    GiveSkillLevelAction giveSkillLevelAction = new GiveSkillLevelAction(main);
                                    giveSkillLevelAction.setNqSkill(skillName);
                                    giveSkillLevelAction.setNqItemAmount(levelAmount);
                                    if (skillName.equalsIgnoreCase("any")) {
                                        context.getSender().sendMessage(main.parse("<error>You cannot use <highlight>'any'</highlight> here!"));
                                    }
                                    else {
                                        main.getActionManager().addAction(giveSkillLevelAction, context);
                                    }
                                }));
    }

    public void setNqSkill(final String nqItemName) {
        this.nqSkill = nqItemName;
    }

    public void setNqItemAmount(final int nqItemAmount) {
        this.nqLevelAmount = nqItemAmount;
    }

    public final String getNqSkill() {
        return this.nqSkill;
    }

    public final int getNqLevelAmount() {
        return this.nqLevelAmount;
    }
    @Override
    public void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (this.getSkillReward() == null) {
            this.main.getLogManager().warn("Tried to give item reward with invalid reward item");
            return;
        }
        if (questPlayer.getPlayer() == null) {
            this.main.getLogManager().warn("Tried to give item reward with invalid player object");
            return;
        }
        final int newLevel = Api.getStats(questPlayer.getPlayer(), this.getNqSkill()) + this.getNqLevelAmount();
        Api.setNewLevel(questPlayer.getPlayer(), this.getNqSkill(), newLevel);
        Api.setSkillPoints(questPlayer.getPlayer(), this.getNqSkill(), Api.getSkillPoints(questPlayer.getPlayer(), this.getNqSkill()) + this.getNqLevelAmount());
        Api.setGoalExp(questPlayer.getPlayer(), this.getNqSkill(), newLevel);
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.nqSkill", getNqSkill());
        configuration.set(initialPath + "specifics.nqLevelAmount", getNqLevelAmount());
    }

    public final String getSkillReward() {
        return this.nqSkill;
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.nqSkill = configuration.getString(initialPath + ".specifics.nqSkill", getNqSkill());
        this.nqLevelAmount = configuration.getInt(initialPath + "specifics.nqLevelAmount", getNqLevelAmount());
    }

    @Override
    public String getActionDescription(QuestPlayer questPlayer, Object... objects) {
        return this.getSkillReward();
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        this.nqSkill = arguments.get(0);
    }
}
