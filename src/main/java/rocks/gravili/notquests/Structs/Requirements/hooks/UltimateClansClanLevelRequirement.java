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

package rocks.gravili.notquests.Structs.Requirements.hooks;

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
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;
import rocks.gravili.notquests.Structs.Requirements.Requirement;

public class UltimateClansClanLevelRequirement extends Requirement {

    private final NotQuests main;
    private final long minClanLevel;


    public UltimateClansClanLevelRequirement(final NotQuests main, final Quest quest, final int requirementID, final long minClanLevel) {
        super(main, quest, requirementID, minClanLevel);
        this.main = main;
        this.minClanLevel = minClanLevel;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder) {
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

                    UltimateClansClanLevelRequirement ultimateClansClanLevelRequirement = new UltimateClansClanLevelRequirement(main, quest, quest.getRequirements().size() + 1, minLevel);
                    quest.addRequirement(ultimateClansClanLevelRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "UltimateClansClanLevel Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    public final long getMinClanLevel() {
        return minClanLevel;
    }

    @Override
    public void save() {

    }

    @Override
    public String getRequirementDescription() {

        return "§7-- Member of clan with min. level: " + getMinClanLevel() + "\n";
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
