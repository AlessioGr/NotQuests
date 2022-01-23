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

package rocks.gravili.notquests.paper.structs.objectives.hooks.citizens;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.location.LocationArgument;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EscortNPCObjective extends Objective {

    private int npcToEscortID = -1;
    private int npcToEscortToID = -1;
    private Location spawnLocation = null;

    public final Location getSpawnLocation(){
        return spawnLocation;
    }

    public void setSpawnLocation(final Location spawnLocation){
        this.spawnLocation = spawnLocation;
    }

    public EscortNPCObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            return;
        }

        CommandFlag<Location> spawnLocationCommandFlag = CommandFlag
                .newBuilder("spawnLocation")
                .withArgument(LocationArgument.of("spawnLocation"))
                .withDescription(ArgumentDescription.of("Spawn Location"))
                .build();

        manager.command(addObjectiveBuilder
                .argument(IntegerArgument.<CommandSender>newBuilder("NPC to escort").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        completions.add("" + npc.getId());
                    }
                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[NPC to escort ID]", "[Destination NPC ID]");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC the player has to escort."))
                .argument(IntegerArgument.<CommandSender>newBuilder("Destination NPC").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    try {
                        int npcToEscortID = context.get("NPC to escort");
                        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                            if (npc.getId() != npcToEscortID) {
                                completions.add("" + npc.getId());
                            }
                        }
                    } catch (Exception ignored) {

                    }

                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Destination NPC ID]", "");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the destination Citizens NPC where the player has to escort the NPC to escort to."))
                .flag(spawnLocationCommandFlag)
                .handler((context) -> {
                    final int toEscortNPCID = context.get("NPC to escort");
                    final int destinationNPCID = context.get("Destination NPC");

                    final Location spawnLocation = context.flags().getValue(spawnLocationCommandFlag, null);


                    if (toEscortNPCID == destinationNPCID) {
                        context.getSender().sendMessage(
                                main.parse(
                                        "<error>Error: Um... an NPC cannot themselves himself, to.. themselves?"
                                )
                        );
                        return;
                    }

                    EscortNPCObjective escortNPCObjective = new EscortNPCObjective(main);

                    escortNPCObjective.setSpawnLocation(spawnLocation);

                    escortNPCObjective.setNpcToEscortID(toEscortNPCID);
                    escortNPCObjective.setNpcToEscortToID(destinationNPCID);

                    main.getObjectiveManager().addObjective(escortNPCObjective, context);
                }));
    }

    public void setNpcToEscortID(final int npcToEscortID) {
        this.npcToEscortID = npcToEscortID;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (main.getIntegrationsManager().isCitizensEnabled()) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.escortNPC.base", player, Map.of(
                        "%EVENTUALCOLOR%", eventualColor,
                        "%NPCNAME%", npc.getName(),
                        "%DESTINATIONNPCNAME%", npcDestination.getName()
                ));
            } else {
                toReturn = "    <GRAY>" + eventualColor + "The target or destination NPC is currently not available!";
            }
        } else {
            toReturn += "    <RED>Error: Citizens plugin not installed. Contact an admin.";
        }
        return toReturn;
    }

    public void setNpcToEscortToID(final int npcToEscortToID) {
        this.npcToEscortToID = npcToEscortToID;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.NPCToEscortID", getNpcToEscortID());
        configuration.set(initialPath + ".specifics.destinationNPCID", getNpcToEscortToID());
        if(getSpawnLocation() != null){
            configuration.set(initialPath + ".specifics.spawnLocation", getSpawnLocation());
        }
    }


    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        if (main.getIntegrationsManager().isCitizensEnabled()) {
            main.getIntegrationsManager().getCitizensManager().handleEscortNPCObjectiveForActiveObjective(this, activeObjective.getActiveQuest());
        }
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final int getNpcToEscortID() {
        return npcToEscortID;
    }

    public final int getNpcToEscortToID() {
        return npcToEscortToID;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        npcToEscortID = configuration.getInt(initialPath + ".specifics.NPCToEscortID");
        npcToEscortToID = configuration.getInt(initialPath + ".specifics.destinationNPCID");
        setSpawnLocation( configuration.getLocation(initialPath + ".specifics.spawnLocation", null));
    }
}
