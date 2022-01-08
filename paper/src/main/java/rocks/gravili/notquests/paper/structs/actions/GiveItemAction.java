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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;

public class GiveItemAction extends Action {

    private ItemStack item = null;

    public GiveItemAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which the player should receive. If you use 'hand', the item you are holding in your main hand will be used."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new GiveItem Reward to a quest")
                .handler((context) -> {
                    final MaterialOrHand materialOrHand = context.get("material");
                    final int itemRewardAmount = context.get("amount");

                    ItemStack itemStack;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemStack = player.getInventory().getItemInMainHand().clone();
                            itemStack.setAmount(itemRewardAmount);
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            context.getSender().sendMessage(main.parse(
                                    "<error>You cannot use <highlight>'any'</highlight> here!"
                            ));
                            return;
                        }
                        itemStack = new ItemStack(Material.valueOf(materialOrHand.material), itemRewardAmount);
                    }

                    GiveItemAction giveItemAction = new GiveItemAction(main);
                    giveItemAction.setItem(itemStack);

                    main.getActionManager().addAction(giveItemAction, context);
                }));
    }

    public void setItem(final ItemStack item) {
        this.item = item;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (item == null) {
            main.getLogManager().warn("Tried to give item reward with invalid reward item");
            return;
        }
        if (player == null) {
            main.getLogManager().warn("Tried to give item reward with invalid player object");
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            player.getInventory().addItem(item);
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), () -> player.getInventory().addItem(item)); //TODO: Check if I can't just run it async if it already is async`?
        }


    }

    @Override
    public String getActionDescription() {
        return "Item: " + getItemReward();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.item", getItemReward());
    }


    public final ItemStack getItemReward() {
        return item;
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.item = configuration.getItemStack(initialPath + ".specifics.item");
    }
}