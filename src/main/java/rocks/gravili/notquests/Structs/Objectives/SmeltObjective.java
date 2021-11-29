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

public class SmeltObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToSmelt;

    public SmeltObjective(NotQuests main, final Quest quest, final int objectiveID, int amountToSmelt, ItemStack itemToSmelt) {
        super(main, quest, objectiveID, amountToSmelt);
        this.main = main;
        this.itemToSmelt = itemToSmelt;
    }

    public SmeltObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        itemToSmelt = main.getDataManager().getQuestsConfig().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToSmelt.itemstack");

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("Smelt")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Output item of the smelting."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be smelted."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Smelt Objective to a quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int amount = context.get("amount");

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

                    SmeltObjective smeltObjective = new SmeltObjective(main, quest, quest.getObjectives().size() + 1, amount, itemStack);
                    quest.addObjective(smeltObjective, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Smelt Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToSmelt.itemstack", getAmountToSmelt());
    }

    public final ItemStack getItemToSmelt() {
        return itemToSmelt;
    }

    public final long getAmountToSmelt() {
        return super.getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        final String displayName;
        if (getItemToSmelt().getItemMeta() != null) {
            displayName = getItemToSmelt().getItemMeta().getDisplayName();
        } else {
            displayName = getItemToSmelt().getType().name();
        }

        return main.getLanguageManager().getString("chat.objectives.taskDescription.smelt.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ITEMTOSMELTTYPE%", "" + getItemToSmelt().getType())
                .replaceAll("%ITEMTOSMELTNAME%", "" + displayName);
    }
}
