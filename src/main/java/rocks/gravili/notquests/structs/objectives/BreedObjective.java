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
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.commands.arguments.EntityTypeSelector;
import rocks.gravili.notquests.structs.ActiveObjective;

public class BreedObjective extends Objective {
    private String entityToBreedType = "";

    public BreedObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreedMobs")
                .argument(EntityTypeSelector.of("entityType", main), ArgumentDescription.of("Type of Entity the player has to breed."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the player needs to breed this entity."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreedMobs Objective to a quest")
                .handler((context) -> {
                    final String entityType = context.get("entityType");
                    final int amount = context.get("amount");

                    BreedObjective breedObjective = new BreedObjective(main);
                    breedObjective.setEntityToBreedType(entityType);
                    breedObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(breedObjective, context);
                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breed.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%ENTITYTOBREED%", getEntityToBreedType());
    }

    public void setEntityToBreedType(final String entityToBreedType) {
        this.entityToBreedType = entityToBreedType;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.mobToBreed", getEntityToBreedType());
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final String getEntityToBreedType() {
        return entityToBreedType;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.entityToBreedType = configuration.getString(initialPath + ".specifics.mobToBreed");
    }
}
