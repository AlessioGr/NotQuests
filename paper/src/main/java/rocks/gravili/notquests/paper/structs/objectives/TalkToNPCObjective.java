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
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCSelector;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class TalkToNPCObjective extends Objective {

    private NQNPC npcToTalkTo;

    public TalkToNPCObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder,
        final int level) {
        manager.command(addObjectiveBuilder
                .argument(NQNPCSelector.of("NPC", main, false, true), ArgumentDescription.of("NPC to whom you should talk."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");

                    final NQNPCResult nqNPCResult = context.get("NPC");

                    if (nqNPCResult.isRightClickSelect()) {//Armor Stands
                        if (context.getSender() instanceof final Player player) {
                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                (nqNPC) -> {
                                    final TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main);
                                    talkToNPCObjective.setObjectiveHolder(quest);
                                    talkToNPCObjective.setObjectiveID(quest.getFreeObjectiveID());
                                    talkToNPCObjective.setNPCtoTalkTo(nqNPC);

                                    main.getObjectiveManager().addObjective(talkToNPCObjective, context, level);
                                },
                                player,
                                "<success>You have been given an item with which you can add the TalkToNPC Objective to an NPC by rightclicking the NPC. Check your inventory!",
                                "<LIGHT_PURPLE>Add TalkToNPC Objective to NPC",
                                "<WHITE>Right-click an NPC to add the following objective to it:",
                                "<YELLOW>TalkToNPC <WHITE>Objective of Quest <highlight>" + quest.getIdentifier()  + "</highlight>."
                            );

                        } else {
                            context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }
                    }else {
                        final NQNPC nqNPC = nqNPCResult.getNQNPC();

                        final TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main);
                        talkToNPCObjective.setNPCtoTalkTo(nqNPC);

                        main.getObjectiveManager().addObjective(talkToNPCObjective, context, level);
                    }



                }));

    }

    @Override
    public String getTaskDescriptionInternal(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        final String toReturn;
        if (npcToTalkTo != null) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.base", questPlayer, activeObjective, Map.of(
                "%NAME%", npcToTalkTo.getName() != null ? npcToTalkTo.getName() : npcToTalkTo.getID().getEitherAsString()
            ));
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.npc-not-available", questPlayer, activeObjective);

        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        npcToTalkTo.saveToConfig(configuration, initialPath + ".specifics.npcToTalkTo");
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        npcToTalkTo = NQNPC.fromConfig(main, configuration, initialPath + ".specifics.npcToTalkTo");

        if (npcToTalkTo == null) {
            npcToTalkTo = main.getNPCManager().getOrCreateNQNpc("citizens", NQNPCID.fromInteger(configuration.getInt(initialPath + ".specifics.NPCtoTalkID", -1)));
            try{
                if(npcToTalkTo == null){
                    final String armorStandUUIDString = configuration.getString(initialPath + ".specifics.ArmorStandToTalkUUID", "");
                    npcToTalkTo = main.getNPCManager().getOrCreateNQNpc("armorstand", NQNPCID.fromUUID(UUID.fromString(armorStandUUIDString)));
                }
            }catch (Exception e){
                main.getLogManager().warn("Some error happened when reading/converting NqNPC (which was null) for DeliverItemsObjective (Objective Holder: <highlight>%s</highlight>, config path: <highlight>%s</highlight>)", getObjectiveHolder().getIdentifier(), initialPath);
                if(main.getConfiguration().debug){
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final NQNPC getNPCtoTalkTo() {
        return npcToTalkTo;
    }

    public void setNPCtoTalkTo(final NQNPC npcToTalkTo) {
        this.npcToTalkTo = npcToTalkTo;
    }
}