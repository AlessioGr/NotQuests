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
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.structs.ActiveObjective;

public class BreakBlocksObjective extends Objective {
    private Material blockToBreak;
    private boolean deductIfBlockIsPlaced = true;


    public BreakBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreakBlocks")
                .argument(MaterialArgument.of("material"), ArgumentDescription.of("Material of the block which needs to be broken."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be broken"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsPlaced")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is placed"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreakBlocks Objective to a quest")
                .handler((context) -> {
                    final Material material = context.get("material");
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsPlaced = !context.flags().isPresent("doNotDeductIfBlockIsPlaced");

                    BreakBlocksObjective breakBlocksObjective = new BreakBlocksObjective(main)
                            .setBlockToBreak(material);
                    breakBlocksObjective.setBlockToBreak(material);
                    breakBlocksObjective.setProgressNeeded(amount);
                    breakBlocksObjective.setDeductIfBlockIsPlaced(deductIfBlockIsPlaced);

                    main.getObjectiveManager().addObjective(breakBlocksObjective, context);

                }));
    }

    public BreakBlocksObjective setBlockToBreak(final Material blockToBreak) {
        this.blockToBreak = blockToBreak;
        return this;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breakBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOBREAK%", getBlockToBreak().toString());
    }

    public void setDeductIfBlockIsPlaced(final boolean deductIfBlockIsPlaced) {
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.blockToBreak.material", getBlockToBreak().toString());
        configuration.set(initialPath + ".specifics.deductIfBlockPlaced", isDeductIfBlockPlaced());
    }


    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }


    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final long getAmountToBreak() {
        return super.getProgressNeeded();
    }

    public final boolean isDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        blockToBreak = Material.valueOf(configuration.getString(initialPath + ".specifics.blockToBreak.material"));
        deductIfBlockIsPlaced = configuration.getBoolean(initialPath + ".specifics.deductIfBlockPlaced", true);
    }
}
