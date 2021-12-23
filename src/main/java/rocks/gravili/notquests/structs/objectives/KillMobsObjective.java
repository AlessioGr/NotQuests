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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.commands.NotQuestColors;
import rocks.gravili.notquests.commands.arguments.EntityTypeSelector;
import rocks.gravili.notquests.structs.ActiveObjective;

public class KillMobsObjective extends Objective {

    private String mobToKillType;
    private String nameTagContainsAny = "";
    private String nameTagEquals = "";

    public KillMobsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("KillMobs")
                .argument(EntityTypeSelector.of("entityType", main), ArgumentDescription.of("Type of Entity the player has to kill."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of kills needed"))
                .flag(main.getCommandManager().nametag_equals)
                .flag(main.getCommandManager().nametag_containsany)
                .meta(CommandMeta.DESCRIPTION, "Adds a new KillMobs Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final String entityType = context.get("entityType");
                    final int amountToKill = context.get("amount");

                    final String[] a = context.flags().getValue(main.getCommandManager().nametag_equals, new String[]{""});
                    final String[] b = context.flags().getValue(main.getCommandManager().nametag_containsany, new String[]{""});
                    final String nametag_equals = String.join(" ", a);
                    final String nametag_containsany = String.join(" ", b);

                    KillMobsObjective killMobsObjective = new KillMobsObjective(main);

                    killMobsObjective.setMobToKillType(entityType);
                    killMobsObjective.setProgressNeeded(amountToKill);

                    //Add flags
                    killMobsObjective.setNameTagEquals(nametag_equals);
                    killMobsObjective.setNameTagContainsAny(nametag_containsany);


                    main.getObjectiveManager().addObjective(killMobsObjective, context);


                    if (!nametag_equals.isBlank()) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.mainGradient + "With nametag_equals flag:  " + NotQuestColors.highlightGradient
                                        + nametag_equals + "</gradient>!</gradient>"
                        ));
                    }
                    if (!nametag_containsany.isBlank()) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.mainGradient + "With nametag_containsany flag:  " + NotQuestColors.highlightGradient
                                        + nametag_containsany + "</gradient>!</gradient>"
                        ));
                    }

                }));
    }


    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.killMobs.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%MOBTOKILL%", "" + getMobToKill());
    }

    public void setMobToKillType(final String mobToKillType) {
        this.mobToKillType = mobToKillType;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.mobToKill", getMobToKill());

        //Extra args
        if (!getNameTagContainsAny().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagContainsAny", getNameTagContainsAny());
        }
        if (!getNameTagEquals().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagEquals", getNameTagEquals());
        }
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final String getMobToKill() {
        return mobToKillType;
    }

    public final long getAmountToKill() {
        return super.getProgressNeeded();
    }


    //Extra args
    public final String getNameTagContainsAny() {
        return nameTagContainsAny;
    }

    public void setNameTagContainsAny(final String nameTagContainsAny) {
        this.nameTagContainsAny = nameTagContainsAny;
    }

    public final String getNameTagEquals() {
        return nameTagEquals;
    }

    public void setNameTagEquals(final String nameTagEquals) {
        this.nameTagEquals = nameTagEquals;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        mobToKillType = configuration.getString(initialPath + ".specifics.mobToKill");

        //Extras
        final String nameTagContains = configuration.getString(initialPath + ".extras.nameTagContainsAny", "");
        if (!nameTagContains.isBlank()) {
            setNameTagContainsAny(nameTagContains);
        }

        final String nameTagEquals = configuration.getString(initialPath + ".extras.nameTagEquals", "");
        if (!nameTagEquals.isBlank()) {
            setNameTagEquals(nameTagEquals);
        }
    }
}
