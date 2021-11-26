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
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

public class EscortNPCObjective extends Objective {

    private final NotQuests main;
    private final int npcToEscortID;
    private final int npcToEscortToID;

    public EscortNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int npcToEscortID, final int npcToEscortToID) {
        super(main, quest, objectiveID, 1);
        this.main = main;
        this.npcToEscortID = npcToEscortID;
        this.npcToEscortToID = npcToEscortToID;
    }

    public EscortNPCObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();
        this.main = main;

        npcToEscortID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCToEscortID");
        npcToEscortToID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.destinationNPCID");

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (main.isCitizensEnabled()) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.escortNPC.base", player)
                        .replaceAll("%EVENTUALCOLOR%", eventualColor)
                        .replaceAll("%NPCNAME%", "" + npc.getName())
                        .replaceAll("%DESTINATIONNPCNAME%", "" + npcDestination.getName());
            } else {
                toReturn = "    §7" + eventualColor + "The target or destination NPC is currently not available!";
            }
        } else {
            toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
        }
        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.NPCToEscortID", getNpcToEscortID());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.destinationNPCID", getNpcToEscortToID());
    }

    public final int getNpcToEscortID() {
        return npcToEscortID;
    }

    public final int getNpcToEscortToID() {
        return npcToEscortToID;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("EscortNPC")
                .argument(IntegerArgument.<CommandSender>newBuilder("NPC to escort").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        completions.add("" + npc.getId());
                    }
                    final List<String> allArgs = context.getRawInput();
                    final Audience audience = main.adventure().sender(context.getSender());
                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[NPC to escort ID]", "[Destination NPC ID]");

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Destination NPC ID]", "");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the destination Citizens NPC where the player has to escort the NPC to escort to."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new EscortNPC Objective to a quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final int toEscortNPCID = context.get("NPC to escort");
                    final int destinationNPCID = context.get("Destination NPC");

                    if (toEscortNPCID == destinationNPCID) {
                        audience.sendMessage(
                                MiniMessage.miniMessage().parse(
                                        NotQuestColors.errorGradient + "Error: Um... an NPC cannot themselves himself, to.. themselves?"
                                )
                        );
                        return;
                    }

                    EscortNPCObjective escortNPCObjective = new EscortNPCObjective(main, quest, quest.getObjectives().size() + 1, toEscortNPCID, destinationNPCID);
                    quest.addObjective(escortNPCObjective, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "EscortNPC Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));


                }));
    }
}
