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

package rocks.gravili.notquests.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.commands.NotQuestColors;
import rocks.gravili.notquests.commands.newcmds.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.commands.newcmds.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.structs.ActiveObjective;

public class ConsumeItemsObjective extends Objective {

    private ItemStack itemToConsume;
    private boolean consumeAnyItem = false;

    public ConsumeItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("ConsumeItems")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be consumed."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be consumed."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ConsumeItems Objective to a quest.")
                .handler((context) -> {
                    final int amount = context.get("amount");

                    boolean consumeAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToConsume;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToConsume = player.getInventory().getItemInMainHand();
                        } else {
                            final Audience audience = main.adventure().sender(context.getSender());
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            consumeAnyItem = true;
                            itemToConsume = null;
                        } else {
                            itemToConsume = new ItemStack(Material.valueOf(materialOrHand.material), 1);
                        }
                    }

                    ConsumeItemsObjective consumeItemsObjective = new ConsumeItemsObjective(main);
                    consumeItemsObjective.setItemToConsume(itemToConsume);
                    consumeItemsObjective.setConsumeAnyItem(consumeAnyItem);
                    consumeItemsObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(consumeItemsObjective, context);

                }));
    }

    public final boolean isConsumeAnyItem() {
        return consumeAnyItem;
    }

    public void setConsumeAnyItem(final boolean consumeAnyItem) {
        this.consumeAnyItem = consumeAnyItem;
    }

    public void setItemToConsume(final ItemStack itemToConsume) {
        this.itemToConsume = itemToConsume;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final ItemStack getItemToConsume() {
        return itemToConsume;
    }

    public final long getAmountToConsume() {
        return super.getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        final String displayName;
        if (!isConsumeAnyItem()) {
            if (getItemToConsume().getItemMeta() != null) {
                displayName = getItemToConsume().getItemMeta().getDisplayName();
            } else {
                displayName = getItemToConsume().getType().name();
            }
        } else {
            displayName = "Any";
        }


        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.consumeItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCONSUMETYPE%", "" + getItemToConsume().getType())
                    .replace("%ITEMTOCONSUMENAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "<RESET>)");
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.consumeItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCONSUMETYPE%", "" + getItemToConsume().getType())
                    .replace("%ITEMTOCONSUMENAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", ")");
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemToConsume.itemstack", getItemToConsume());
        configuration.set(initialPath + ".specifics.consumeAnyItem", isConsumeAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        itemToConsume = configuration.getItemStack(initialPath + ".specifics.itemToConsume.itemstack");
        consumeAnyItem = configuration.getBoolean(initialPath + ".specifics.consumeAnyItem", false);
    }
}
