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
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class BreedObjective extends Objective {
    private final NotQuests main;
    private final String entityToBreedType;

    public BreedObjective(NotQuests main, Quest quest, int objectiveID, int progressNeeded, String entityToBreedType) {
        super(main, quest, objectiveID, progressNeeded);
        this.main = main;
        this.entityToBreedType = entityToBreedType;
    }

    public BreedObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        this.main = main;
        final String questName = quest.getQuestName();

        this.entityToBreedType = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.mobToBreed");

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breed.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ENTITYTOBREED%", getEntityToBreedType());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.mobToBreed", getEntityToBreedType());

    }

    public final String getEntityToBreedType() {
        return entityToBreedType;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreedMobs")
                .argument(MaterialArgument.of("material"))
                .argument(IntegerArgument.of("amount"))
                .flag(
                        manager.flagBuilder("deductIfBlockIsPlaced")
                                .withDescription(ArgumentDescription.of("Determines if Quest progress should be removed if a block is placed"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreedMobs Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                }));
    }
}
