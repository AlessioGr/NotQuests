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

package rocks.gravili.notquests.Structs.Objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.Commands.newCMDs.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class CollectItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCollect;
    private final boolean deductIfItemIsDropped;

    public CollectItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToCollect, int amountToCollect, boolean deductIfItemIsDropped) {
        super(main, quest, objectiveID, amountToCollect);
        this.main = main;
        this.itemToCollect = itemToCollect;
        this.deductIfItemIsDropped = deductIfItemIsDropped;
    }

    public CollectItemsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
        deductIfItemIsDropped = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfItemDropped", true);

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ITEMTOCOLLECTTYPE%", "" + getItemToCollect().getType())
                .replaceAll("%ITEMTOCOLLECTNAME%", "" + getItemToCollect().getItemMeta().getDisplayName());
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("CollectItems")
                .argument(MaterialOrHandArgument.of("material"), ArgumentDescription.of("Material of the item which needs to be collected."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be collected"))
                .flag(
                        manager.flagBuilder("doNotDeductIfItemIsDropped")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the item is dropped"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreakBlocks Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int amount = context.get("amount");
                    final boolean deductIfItemIsDropped = !context.flags().isPresent("doNotDeductIfItemIsDropped");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemStack;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemStack = player.getInventory().getItemInMainHand();
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        itemStack = new ItemStack(materialOrHand.material, 1);
                    }


                    CollectItemsObjective collectItemsObjective = new CollectItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amount, deductIfItemIsDropped);

                    quest.addObjective(collectItemsObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "CollectItems Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    public final ItemStack getItemToCollect() {
        return itemToCollect;
    }

    public final long getAmountToCollect() {
        return super.getProgressNeeded();
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToCollect.itemstack", getItemToCollect());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.deductIfItemDropped", deductIfItemIsDropped);

    }

    public final boolean isDeductIfItemIsDropped() {
        return deductIfItemIsDropped;
    }
}
