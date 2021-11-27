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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.Commands.newCMDs.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class ItemReward extends Reward {

    private final NotQuests main;
    private final ItemStack item;

    public ItemReward(final NotQuests main, final Quest quest, final int rewardID) {
        super(main, quest, rewardID);
        this.main = main;

        this.item = main.getDataManager().getQuestsConfig().getItemStack("quests." + getQuest().getQuestName() + ".rewards." + rewardID + ".specifics.rewardItem");
    }

    public ItemReward(final NotQuests main, final Quest quest, final int rewardID, ItemStack item) {
        super(main, quest, rewardID);
        this.main = main;
        this.item = item;
    }

    @Override
    public void giveReward(final Player player, final Quest quest) {

        if (Bukkit.isPrimaryThread()) {
            player.getInventory().addItem(item);
        } else {
            Bukkit.getScheduler().runTask(main, () -> player.getInventory().addItem(item)); //TODO: Check if I can't just run it async if it already is async`?
        }


    }

    @Override
    public String getRewardDescription() {
        return "Item: " + getItemReward();
    }


    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".rewards." + getRewardID() + ".specifics.rewardItem", getItemReward());
    }

    public final ItemStack getItemReward() {
        return item;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRewardBuilder) {
        manager.command(addRewardBuilder.literal("Item")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which the player should receive. If you use 'hand', the item you are holding in your main hand will be used."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Item Reward to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");


                    final MaterialOrHand materialOrHand = context.get("material");
                    final int itemRewardAmount = context.get("amount");

                    ItemStack itemStack;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemStack = player.getInventory().getItemInMainHand().clone();
                            itemStack.setAmount(itemRewardAmount);
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        itemStack = new ItemStack(materialOrHand.material, itemRewardAmount);
                    }

                    ItemReward itemReward = new ItemReward(main, quest, quest.getRewards().size() + 1, itemStack);

                    quest.addReward(itemReward);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Item Reward successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}