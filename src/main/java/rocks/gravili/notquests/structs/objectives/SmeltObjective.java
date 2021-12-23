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

public class SmeltObjective extends Objective {

    private ItemStack itemToSmelt;
    private boolean smeltAnyItem = false;

    public SmeltObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("Smelt")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Output item of the smelting."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be smelted."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Smelt Objective to a quest.")
                .handler((context) -> {
                    final int amount = context.get("amount");

                    boolean smeltAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToSmelt;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToSmelt = player.getInventory().getItemInMainHand();
                        } else {
                            final Audience audience = main.adventure().sender(context.getSender());
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            smeltAnyItem = true;
                            itemToSmelt = null;
                        } else {
                            itemToSmelt = new ItemStack(Material.valueOf(materialOrHand.material), 1);
                        }
                    }

                    SmeltObjective smeltObjective = new SmeltObjective(main);
                    smeltObjective.setProgressNeeded(amount);
                    smeltObjective.setItemToSmelt(itemToSmelt);
                    smeltObjective.setSmeltAnyItem(smeltAnyItem);

                    main.getObjectiveManager().addObjective(smeltObjective, context);
                }));
    }

    public final boolean isSmeltAnyItem() {
        return smeltAnyItem;
    }

    public void setSmeltAnyItem(final boolean smeltAnyItem) {
        this.smeltAnyItem = smeltAnyItem;
    }

    public void setItemToSmelt(final ItemStack itemToSmelt) {
        this.itemToSmelt = itemToSmelt;
    }

    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

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
        if (!isSmeltAnyItem()) {
            if (getItemToSmelt().getItemMeta() != null) {
                displayName = getItemToSmelt().getItemMeta().getDisplayName();
            } else {
                displayName = getItemToSmelt().getType().name();
            }
        } else {
            displayName = "Any";
        }


        String itemType = isSmeltAnyItem() ? "Any" : getItemToSmelt().getType().name();


        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.smelt.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOSMELTTYPE%", "" + itemType)
                    .replace("%ITEMTOSMELTNAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "<RESET>)");
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.smelt.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTOSMELTTYPE%", "" + itemType)
                    .replace("%ITEMTOSMELTNAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", "");
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemToSmelt.itemstack", getItemToSmelt());
        configuration.set(initialPath + ".specifics.smeltAnyItem", isSmeltAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        itemToSmelt = configuration.getItemStack(initialPath + ".specifics.itemToSmelt.itemstack");
        smeltAnyItem = configuration.getBoolean(initialPath + ".specifics.smeltAnyItem");
    }
}
