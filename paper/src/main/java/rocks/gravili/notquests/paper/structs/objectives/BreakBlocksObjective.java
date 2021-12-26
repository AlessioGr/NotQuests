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

package rocks.gravili.notquests.paper.structs.objectives;

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
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;

public class BreakBlocksObjective extends Objective {
    private String blockToBreak;
    private boolean deductIfBlockIsPlaced = true;


    public BreakBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreakBlocks")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the block which needs to be broken."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be broken"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsPlaced")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is placed"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreakBlocks Objective to a quest")
                .handler((context) -> {

                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsPlaced = !context.flags().isPresent("doNotDeductIfBlockIsPlaced");

                    final MaterialOrHand materialOrHand = context.get("material");
                    final String materialToBreak;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            materialToBreak = player.getInventory().getItemInMainHand().getType().name();
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        materialToBreak = materialOrHand.material;
                    }

                    BreakBlocksObjective breakBlocksObjective = new BreakBlocksObjective(main);
                    breakBlocksObjective.setBlockToBreak(materialToBreak);
                    breakBlocksObjective.setProgressNeeded(amount);
                    breakBlocksObjective.setDeductIfBlockIsPlaced(deductIfBlockIsPlaced);

                    main.getObjectiveManager().addObjective(breakBlocksObjective, context);

                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breakBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOBREAK%", getBlockToBreak());
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.blockToBreak.material", getBlockToBreak());
        configuration.set(initialPath + ".specifics.deductIfBlockPlaced", isDeductIfBlockPlaced());
    }

    public void setDeductIfBlockIsPlaced(final boolean deductIfBlockIsPlaced) {
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    public final String getBlockToBreak() {
        return blockToBreak;
    }


    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public void setBlockToBreak(final String blockToBreak) {
        this.blockToBreak = blockToBreak;
    }

    public final long getAmountToBreak() {
        return super.getProgressNeeded();
    }

    public final boolean isDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        blockToBreak = configuration.getString(initialPath + ".specifics.blockToBreak.material");
        deductIfBlockIsPlaced = configuration.getBoolean(initialPath + ".specifics.deductIfBlockPlaced", true);
    }
}
