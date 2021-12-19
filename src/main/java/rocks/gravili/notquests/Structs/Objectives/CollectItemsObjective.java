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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.Commands.newCMDs.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;

public class CollectItemsObjective extends Objective {

    private ItemStack itemToCollect;
    private boolean deductIfItemIsDropped = true;

    public CollectItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("CollectItems")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be collected."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be collected."))
                .flag(
                        manager.flagBuilder("doNotDeductIfItemIsDropped")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the item is dropped."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new CollectItems Objective to a quest")
                .handler((context) -> {
                    final int amount = context.get("amount");
                    final boolean deductIfItemIsDropped = !context.flags().isPresent("doNotDeductIfItemIsDropped");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToCollect;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToCollect = player.getInventory().getItemInMainHand();
                        } else {
                            final Audience audience = main.adventure().sender(context.getSender());
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        itemToCollect = new ItemStack(materialOrHand.material, 1);
                    }

                    CollectItemsObjective collectItemsObjective = new CollectItemsObjective(main);
                    collectItemsObjective.setItemToCollect(itemToCollect);
                    collectItemsObjective.setProgressNeeded(amount);
                    collectItemsObjective.setDeductIfItemIsDropped(deductIfItemIsDropped);

                    main.getObjectiveManager().addObjective(collectItemsObjective, context);
                }));
    }

    public void setItemToCollect(final ItemStack itemToCollect) {
        this.itemToCollect = itemToCollect;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        final String displayName;
        if (getItemToCollect().getItemMeta() != null) {
            displayName = getItemToCollect().getItemMeta().getDisplayName();
        } else {
            displayName = getItemToCollect().getType().name();
        }


        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCOLLECTTYPE%", "" + getItemToCollect().getType())
                    .replace("%ITEMTOCOLLECTNAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "<RESET>)");
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCOLLECTTYPE%", "" + getItemToCollect().getType())
                    .replace("%ITEMTOCOLLECTNAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", "");
        }

    }

    public void setDeductIfItemIsDropped(final boolean deductIfItemIsDropped) {
        this.deductIfItemIsDropped = deductIfItemIsDropped;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemToCollect.itemstack", getItemToCollect());
        configuration.set(initialPath + ".specifics.deductIfItemDropped", deductIfItemIsDropped);
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        itemToCollect = configuration.getItemStack(initialPath + ".specifics.itemToCollect.itemstack");
        deductIfItemIsDropped = configuration.getBoolean(initialPath + ".specifics.deductIfItemDropped", true);
    }

    public final ItemStack getItemToCollect() {
        return itemToCollect;
    }

    public final long getAmountToCollect() {
        return super.getProgressNeeded();
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final boolean isDeductIfItemIsDropped() {
        return deductIfItemIsDropped;
    }
}
