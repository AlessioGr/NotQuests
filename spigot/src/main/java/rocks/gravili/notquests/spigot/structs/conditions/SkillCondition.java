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

package rocks.gravili.notquests.spigot.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.neostorm.neostorm.Api;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;

public class SkillCondition extends Condition {
    private String skill;

    public SkillCondition(final NotQuests main) {
        super(main);
    }

    public void setSkill(final String skill) {
        this.skill = skill;
    }
    public final long getSkillRequirement() {
        return this.getProgressNeeded();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.getIntegrationsManager().isVaultEnabled()) {
            return;
        }

        manager.command(builder.literal("Skill")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money needed"))
                .flag(
                        manager.flagBuilder("deductMoney")
                                .withDescription(ArgumentDescription.of("Makes it so the required money is deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new Money Requirement to a quest")
                .handler((context) -> {
                    int amount = context.get("amount");
                    String skill = context.get("skill");
                    SkillCondition skillCondition = new SkillCondition(main);
                    skillCondition.setProgressNeeded(amount);
                    skillCondition.setSkill(skill);
                    main.getConditionsManager().addCondition(skillCondition, context);
                }));
    }

    @Override
    public String getConditionDescription() {
        return "<GRAY>-- Skills needed: " + getSkillRequirement();
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        final long skillRequirementAmount = this.getSkillRequirement();
        final Player player = questPlayer.getPlayer();
        if (player == null) {
            return "<YELLOW>Error reading skill requirement...";
        }
        if (Api.getStats(player, this.skill) < skillRequirementAmount) {
            return String.valueOf(skillRequirementAmount - Api.getStats(player, this.skill));
        }
        return "";
    }
}
