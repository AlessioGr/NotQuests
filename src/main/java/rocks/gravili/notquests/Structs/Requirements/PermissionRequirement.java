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

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class PermissionRequirement extends Requirement {

    private final NotQuests main;
    private final String requiredPermission;


    public PermissionRequirement(NotQuests main, final Quest quest, final int requirementID) {
        super(main, quest, requirementID, 1);
        this.main = main;

        this.requiredPermission = main.getDataManager().getQuestsData().getString("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.requiredPermission");
    }

    public PermissionRequirement(NotQuests main, final Quest quest, final int requirementID, long progressNeeded) {
        super(main, quest, requirementID, 1);
        this.main = main;

        this.requiredPermission = main.getDataManager().getQuestsData().getString("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.requiredPermission");
    }

    public PermissionRequirement(NotQuests main, final Quest quest, final int requirementID, String requiredPermission) {
        super(main, quest, requirementID, 1);
        this.main = main;
        this.requiredPermission = requiredPermission;
    }


    public final String getRequiredPermission() {
        return requiredPermission;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {

    }


    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.requiredPermission", getRequiredPermission());
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        final String requiredPermission = getRequiredPermission();

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!player.hasPermission(requiredPermission)) {
                return "\n§eYou need the following permission: §b" + requiredPermission + "§e.";
            }
        } else {
            return "\n§eYou need to be online.";
        }
        return "";
    }

    @Override
    public String getRequirementDescription() {
        return "§7-- Permission needed: " + getRequiredPermission();
    }
}
