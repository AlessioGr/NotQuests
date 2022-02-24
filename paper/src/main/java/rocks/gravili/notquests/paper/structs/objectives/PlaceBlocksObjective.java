/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

public class PlaceBlocksObjective extends Objective {

    private String blockToPlace;
    private boolean deductIfBlockIsBroken = true;
    private String nqItemName = "";


    public PlaceBlocksObjective(NotQuests main) {
        super(main);
    }

    public void setNQItem(final String nqItemName){
        this.nqItemName = nqItemName;
    }
    public final String getNQItem(){
        return nqItemName;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the block which needs to be place."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be placed"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsBroken")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is broken"))
                )
                .handler((context) -> {
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsBroken = !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    final MaterialOrHand materialOrHand = context.get("material");
                    final String materialToPlace;

                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            materialToPlace = player.getInventory().getItemInMainHand().getType().name();
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (!materialOrHand.material.equalsIgnoreCase("any")) {
                            materialToPlace = main.getItemsManager().getMaterial(materialOrHand.material).name();
                        }else{
                            materialToPlace = "any";
                        }
                    }

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main);

                    if(!materialOrHand.material.equalsIgnoreCase("any") && main.getItemsManager().getItem(materialOrHand.material) != null){
                        placeBlocksObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        placeBlocksObjective.setBlockToPlace(materialToPlace);
                    }


                    placeBlocksObjective.setDeductIfBlockIsBroken(deductIfBlockIsBroken);
                    placeBlocksObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(placeBlocksObjective, context);
                }));
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.placeBlocks.base", questPlayer, Map.of(
                "%BLOCKTOPLACE%", getBlockToPlace()
        ));
    }

    public void setDeductIfBlockIsBroken(final boolean deductIfBlockIsBroken) {
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
        }else {
            configuration.set(initialPath + ".specifics.blockToPlace.material", getBlockToPlace());
        }

        configuration.set(initialPath + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

        if(nqItemName.isBlank()){
            blockToPlace = configuration.getString(initialPath + ".specifics.blockToPlace.material");
        }

        deductIfBlockIsBroken = configuration.getBoolean(initialPath + ".specifics.deductIfBlockBroken", true);
    }

    public final String getBlockToPlace() {
        if(!nqItemName.isBlank()){
            return main.getItemsManager().getItem(nqItemName).getItemStack().getType().name();
        }else {
            return blockToPlace;
        }
    }


    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
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
