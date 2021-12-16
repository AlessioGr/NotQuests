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

package rocks.gravili.notquests.Structs.Actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.List;


public class GrantPermissionAction extends Action {

    private String rewardedPermission = "";


    public GrantPermissionAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        if (!main.isLuckpermsEnabled()) {
            return;
        }

        manager.command(builder.literal("GrantPermission")
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
                .meta(CommandMeta.DESCRIPTION, "Adds a new GrantPermission Reward to a quest")
                .handler((context) -> {
                    final String permissionNode = context.get("Permission");

                    GrantPermissionAction grantPermissionAction = new GrantPermissionAction(main);
                    grantPermissionAction.setRewardedPermissionNode(permissionNode);

                    main.getActionManager().addAction(grantPermissionAction, context);

                }));
    }

    public void setRewardedPermissionNode(final String rewardedPermission) {
        this.rewardedPermission = rewardedPermission;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (rewardedPermission.isBlank()) {
            main.getLogManager().warn("Tried to give permission reward, but the rewarded permission node is empty.");
            return;
        }
        if (!main.isLuckpermsEnabled()) {
            player.sendMessage("§cError: cannot give you the permission reward because Luckperms (needed for money giving to work) is not installed on the server.");
            return;
        }
        main.getLuckPermsManager().givePermission(player.getUniqueId(), rewardedPermission);


    }

    @Override
    public String getActionDescription() {
        return "Permission: " + getRewardedPermission();
    }


    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.permission", getRewardedPermission());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.rewardedPermission = configuration.getString(initialPath + ".specifics.permission");
    }

    public final String getRewardedPermission() {
        return rewardedPermission;
    }
}