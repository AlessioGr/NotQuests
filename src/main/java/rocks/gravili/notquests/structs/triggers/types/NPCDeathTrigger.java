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

package rocks.gravili.notquests.structs.triggers.types;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

public class NPCDeathTrigger extends Trigger {

    private int npcToDieID = -1;


    public NPCDeathTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(addTriggerBuilder.literal("NPCDEATH")
                .argument(IntegerArgument.<CommandSender>newBuilder("NPC").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        completions.add("" + npc.getId());
                    }
                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[NPC ID]", "[Amount of Deaths]");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC the player has to escort."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the NPC needs to die."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .meta(CommandMeta.DESCRIPTION, "Triggers when specified Citizens NPC dies.")
                .handler((context) -> {
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
