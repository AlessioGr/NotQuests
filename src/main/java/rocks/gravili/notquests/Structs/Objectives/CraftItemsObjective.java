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

public class CraftItemsObjective extends Objective {

    private ItemStack itemToCraft;

    public CraftItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("CraftItems")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be crafted."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be crafted."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new CraftItems Objective to a quest.")
                .handler((context) -> {
                    final int amount = context.get("amount");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToCraft;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToCraft = player.getInventory().getItemInMainHand();
                        } else {
                            final Audience audience = main.adventure().sender(context.getSender());
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        itemToCraft = new ItemStack(materialOrHand.material, 1);
                    }

                    CraftItemsObjective craftItemsObjective = new CraftItemsObjective(main);
                    craftItemsObjective.setItemToCraft(itemToCraft);
                    craftItemsObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(craftItemsObjective, context);
                }));
    }

    public void setItemToCraft(final ItemStack itemToCraft) {
        this.itemToCraft = itemToCraft;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final ItemStack getItemToCraft() {
        return itemToCraft;
    }

    public final long getAmountToCraft() {
        return super.getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        final String displayName;
        if (getItemToCraft().getItemMeta() != null) {
            displayName = getItemToCraft().getItemMeta().getDisplayName();
        } else {
            displayName = getItemToCraft().getType().name();
        }

        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.craftItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCRAFTTYPE%", "" + getItemToCraft().getType())
                    .replace("%ITEMTOCRAFTNAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "<RESET>)");
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.craftItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOCRAFTTYPE%", "" + getItemToCraft().getType())
                    .replace("%ITEMTOCRAFTNAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", "");
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemToCraft.itemstack", getItemToCraft());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        itemToCraft = configuration.getItemStack(initialPath + ".specifics.itemToCraft.itemstack");
    }
}
