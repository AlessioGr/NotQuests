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

package rocks.gravili.notquests.paper.structs.objectives.hooks.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.LocationArgument.locationArgument;

public class EscortNPCObjective extends Objective { //TODO: Add support for other NPC systems

    private int npcToEscortID = -1;
    private int npcToEscortToID = -1;
    private Location spawnLocation = null;

    public EscortNPCObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            return;
        }

        NQFlag spawnLocationCommandFlag = NQFlag.builder("spawnLocation")
                .withArgument(locationArgument())
                .withDescription(NQDescription.of("Spawn Location"))
                .build();

        manager.command(addObjectiveBuilder
                        .required("NPC to escort", NQArguments.integerArgument(), NQDescription.of("ID of the Citizens NPC the player has to escort."), (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            for (final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()) {
                                completions.add("" + npcID);
                            }
                            return completions;
                        })
                        .required("Destination NPC", NQArguments.integerArgument(), NQDescription.of("ID of the destination Citizens NPC where the player has to escort the NPC to escort to."), (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            try {
                                int npcToEscortID = context.get("NPC to escort");
                                for (final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()) {
                                    if (npcID != npcToEscortID) {
                                        completions.add(String.valueOf(npcID));
                                    }
                                }
                            } catch (Exception ignored) {

                            }
                            return completions;
                        })
                        .flag(spawnLocationCommandFlag)
                        .handler((context) -> {
                                    final int toEscortNPCID = context.get("NPC to escort");
                                    final int destinationNPCID = context.get("Destination NPC");

                                    final Location spawnLocation =
                                            context.flags().getValue(spawnLocationCommandFlag, null);

                                    if (toEscortNPCID == destinationNPCID) {
                                        context.sender().sendMessage(main.parse("<error>Error: Um... an NPC cannot themselves himself, to.. themselves?"));
                                        return;
                                    }

                                    final EscortNPCObjective escortNPCObjective = new EscortNPCObjective(main);

                                    escortNPCObjective.setSpawnLocation(spawnLocation);

                                    escortNPCObjective.setNpcToEscortID(toEscortNPCID);
                                    escortNPCObjective.setNpcToEscortToID(destinationNPCID);
                                    main.getObjectiveManager().addObjective(escortNPCObjective, context, level);
                                }));
    }

    public final Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(final Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        String toReturn = "";
        if (main.getIntegrationsManager().isCitizensEnabled()) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                final String mmNpcName =
                        main.getMiniMessage()
                                .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§", "&")));
                final String mmNpcDestinationName =
                        main.getMiniMessage()
                                .serialize(
                                        LegacyComponentSerializer.legacyAmpersand()
                                                .deserialize(npcDestination.getName()));

                toReturn =
                        main.getLanguageManager()
                                .getString(
                                        "chat.objectives.taskDescription.escortNPC.base",
                                        questPlayer,
                                        activeObjective,
                                        Map.of(
                                                "%NPCNAME%", mmNpcName,
                                                "%DESTINATIONNPCNAME%", mmNpcDestinationName));
            } else {
                toReturn = "    <GRAY>The target or destination NPC is currently not available!";
            }
        } else {
            toReturn += "    <RED>Error: Citizens plugin not installed. Contact an admin.";
        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.NPCToEscortID", getNpcToEscortID());
        configuration.set(initialPath + ".specifics.destinationNPCID", getNpcToEscortToID());
        if (getSpawnLocation() != null) {
            configuration.set(initialPath + ".specifics.spawnLocation", getSpawnLocation());
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        npcToEscortID = configuration.getInt(initialPath + ".specifics.NPCToEscortID");
        npcToEscortToID = configuration.getInt(initialPath + ".specifics.destinationNPCID");
        setSpawnLocation(configuration.getLocation(initialPath + ".specifics.spawnLocation", null));
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        if (main.getIntegrationsManager().isCitizensEnabled()) {
            main.getIntegrationsManager()
                    .getCitizensManager()
                    .handleEscortNPCObjectiveForActiveObjective(this, activeObjective.getActiveObjectiveHolder());
        }
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public final int getNpcToEscortID() {
        return npcToEscortID;
    }

    public void setNpcToEscortID(final int npcToEscortID) {
        this.npcToEscortID = npcToEscortID;
    }

    public final int getNpcToEscortToID() {
        return npcToEscortToID;
    }

    public void setNpcToEscortToID(final int npcToEscortToID) {
        this.npcToEscortToID = npcToEscortToID;
    }


}
