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

package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;

public class NPCDeathTrigger extends Trigger { //TODO: Add support for other NPC systems

    private int npcToDieID = -1;

    public NPCDeathTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(
                addTriggerBuilder
                        .required("NPC", integerParser(0), Description.of("ID of the Citizens NPC the player has to escort."), (context, lastString) -> {
                            final ArrayList<Suggestion> completions = new ArrayList<>();
                            for (final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()) {
                                completions.add(Suggestion.suggestion(String.valueOf(npcID)));
                            }
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[NPC ID]", "[Amount of Deaths]");

                            return CompletableFuture.completedFuture(completions);
                        })
                        .required("amount", integerParser(1), Description.of("Amount of times the NPC needs to die."))
                        .flag(main.getCommandManager().applyOn)
                        .flag(main.getCommandManager().triggerWorldString)
                        .commandDescription(Description.of("Triggers when specified Citizens NPC dies."))
                        .handler(
                                (context) -> {
                                    final int npcToDieID = context.get("NPC");

                                    NPCDeathTrigger npcDeathTrigger = new NPCDeathTrigger(main);
                                    npcDeathTrigger.setNpcToDieID(npcToDieID);

                                    main.getTriggerManager().addTrigger(npcDeathTrigger, context);
                                }));
    }

    public final int getNpcToDieID() {
        return npcToDieID;
    }

    public void setNpcToDieID(final int npcToDieID) {
        this.npcToDieID = npcToDieID;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.npcToDie", getNpcToDieID());
    }

    @Override
    public String getTriggerDescription() {
        return "NPC to die ID: <WHITE>" + getNpcToDieID();
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.npcToDieID = configuration.getInt(initialPath + ".specifics.npcToDie");
    }
}
