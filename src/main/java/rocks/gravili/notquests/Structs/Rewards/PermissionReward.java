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

package rocks.gravili.notquests.Structs.Rewards;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;


public class PermissionReward extends Reward {

    private final NotQuests main;
    private final String rewardedPermission;


    public PermissionReward(final NotQuests main, final Quest quest, final int rewardID) {
        super(main, quest, rewardID);
        this.main = main;

        this.rewardedPermission = main.getDataManager().getQuestsConfig().getString("quests." + getQuest().getQuestName() + ".rewards." + rewardID + ".specifics.rewardedPermission");
    }

    public PermissionReward(final NotQuests main, final Quest quest, final int rewardID, String rewardedPermission) {
        super(main, quest, rewardID);
        this.main = main;
        this.rewardedPermission = rewardedPermission;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRewardBuilder) {
        if (!main.isLuckpermsEnabled()) {
            return;
        }

        manager.command(addRewardBuilder.literal("Permission")
                .argument(StringArgument.<CommandSender>newBuilder("Permission").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Permission node]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter permission node>");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Permission node which the player will receive as a reward"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Permission Reward to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");


                    final String permissionNode = context.get("Permission");


                    PermissionReward permissionReward = new PermissionReward(main, quest, quest.getRewards().size() + 1, permissionNode);

                    quest.addReward(permissionReward);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Permission successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        if (!main.isLuckpermsEnabled()) {
            player.sendMessage("§cError: cannot give you the permission reward because Luckperms (needed for money giving to work) is not installed on the server.");
            return;
        }
        main.getLuckPermsManager().givePermission(player.getUniqueId(), rewardedPermission);


    }

    @Override
    public String getRewardDescription() {
        return "Permission: " + getRewardedPermission();
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".rewards." + getRewardID() + ".specifics.rewardedPermission", getRewardedPermission());
    }

    public final String getRewardedPermission() {
        return rewardedPermission;
    }
}