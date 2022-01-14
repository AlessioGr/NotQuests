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

package rocks.gravili.notquests.spigot.structs.objectives;

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
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.spigot.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;

public class PlaceBlocksObjective extends Objective {

    private String blockToPlace;
    private boolean deductIfBlockIsBroken = true;

    public PlaceBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("PlaceBlocks")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the block which needs to be place."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be placed"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsBroken")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is broken"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new PlaceBlocks Objective to a quest")
                .handler((context) -> {
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsBroken = !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    final MaterialOrHand materialOrHand = context.get("material");
                    final String materialToPlace;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            materialToPlace = player.getInventory().getItemInMainHand().getType().name();
                        } else {
                            final Audience audience = main.adventure().sender(context.getSender());
                            audience.sendMessage(MiniMessage.miniMessage().deserialize(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        materialToPlace = materialOrHand.material;
                    }

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main);
                    placeBlocksObjective.setBlockToPlace(materialToPlace);
                    placeBlocksObjective.setDeductIfBlockIsBroken(deductIfBlockIsBroken);
                    placeBlocksObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(placeBlocksObjective, context);
                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.placeBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOPLACE%", getBlockToPlace());
    }

    public void setDeductIfBlockIsBroken(final boolean deductIfBlockIsBroken) {
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.blockToPlace.material", getBlockToPlace());
        configuration.set(initialPath + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        blockToPlace = configuration.getString(initialPath + ".specifics.blockToPlace.material");
        deductIfBlockIsBroken = configuration.getBoolean(initialPath + ".specifics.deductIfBlockBroken", true);
    }

    public final String getBlockToPlace() {
        return blockToPlace;
    }


    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }

    public void setBlockToPlace(final String blockToPlace) {
        this.blockToPlace = blockToPlace;
    }

    public final long getAmountToPlace() {
        return super.getProgressNeeded();
    }

    public final boolean isDeductIfBlockBroken() {
        return deductIfBlockIsBroken;
    }
}
