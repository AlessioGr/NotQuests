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

package rocks.gravili.notquests.Structs.Conditions.hooks;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import me.ulrich.clans.api.PlayerAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;
import rocks.gravili.notquests.Structs.Conditions.Condition;

public class UltimateClansClanLevelCondition extends Condition {

    private final NotQuests main;
    private int minClanLevel = 1;


    public UltimateClansClanLevelCondition(final NotQuests main, final Object... objects) {
        super(main, objects);
        this.main = main;
    }

    public void setMinClanLevel(final int minClanLevel){
        this.minClanLevel = minClanLevel;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder, Command.Builder<CommandSender> objectiveAddConditionBuilder) {
        if (!main.isUltimateClansEnabled()) {
            return;
        }

        manager.command(addRequirementBuilder.literal("UltimateClansClanLevel")
                .argument(IntegerArgument.<CommandSender>newBuilder("minLevel").withMin(1), ArgumentDescription.of("Minimum clan level"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new UltimateClansClanLevel Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int minLevel = context.get("minLevel");

                    UltimateClansClanLevelCondition ultimateClansClanLevelRequirement = new UltimateClansClanLevelCondition(main, quest);
                    ultimateClansClanLevelRequirement.setMinClanLevel(minLevel);
                    quest.addRequirement(ultimateClansClanLevelRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "UltimateClansClanLevel Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));


        manager.command(objectiveAddConditionBuilder.literal("UltimateClansClanLevel")
                .argument(IntegerArgument.<CommandSender>newBuilder("minLevel").withMin(1), ArgumentDescription.of("Minimum clan level"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new UltimateClansClanLevel Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int minLevel = context.get("minLevel");

                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    UltimateClansClanLevelCondition ultimateClansClanLevelCondition = new UltimateClansClanLevelCondition(main, quest, objective);
                    ultimateClansClanLevelCondition.setMinClanLevel(minLevel);
                    objective.addCondition(ultimateClansClanLevelCondition, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "UltimateClansClanLevel Condition successfully added to Objective " + NotQuestColors.highlightGradient
                                    + objective.getObjectiveFinalName() + "</gradient>!</gradient>"));



                }));
    }

    public final long getMinClanLevel() {
        return minClanLevel;
    }



    @Override
    public String getConditionDescription() {

        return "§7-- Member of clan with min. level: " + getMinClanLevel() + "\n";
    }

    @Override
    public void save(String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.minClanLevel", getMinClanLevel());
    }

    @Override
    public void load(String initialPath) {
        minClanLevel = main.getDataManager().getQuestsConfig().getInt(initialPath + ".specifics.minClanLevel");
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!main.isUltimateClansEnabled()) {
                return "\n§eError: The server does not have UltimateClans enabled. Please ask the Owner to install UltimateClans for UltimateClans stuff to work.";
            } else {
                if (PlayerAPI.getInstance().getPlayerClan(player.getName()) != null && PlayerAPI.getInstance().getPlayerClan(player.getName()).getLevel() >= getMinClanLevel()) {
                    return "";

                }
                return "\n§eYou need to be in a Clan with at least level §b" + getMinClanLevel() + "§e.";

            }
        } else {
            return "\n§eError reading UltimateClans requirement...";

        }
    }
}
