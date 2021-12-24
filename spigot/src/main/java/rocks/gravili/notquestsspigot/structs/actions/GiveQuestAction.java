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

package rocks.gravili.notquestsspigot.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.commands.arguments.QuestSelector;
import rocks.gravili.notquestsspigot.structs.Quest;

public class GiveQuestAction extends Action {

    private String questToGiveName = "";
    private boolean forceGive = false;


    public GiveQuestAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("GiveQuest")
                .argument(QuestSelector.of("quest to give", main), ArgumentDescription.of("Name of the Quest which should be given to the player."))
                .flag(
                        manager.flagBuilder("forceGive")
                                .withDescription(ArgumentDescription.of("Force-gives the Quest to the player, disregarding most Quest requirements/cooldowns/..."))
                )
                .meta(CommandMeta.DESCRIPTION, "Creates a new GiveQuest Action")
                .handler((context) -> {
                    final Quest foundQuest = context.get("quest to give");
                    final boolean forceGive = context.flags().isPresent("forceGive");


                    GiveQuestAction giveQuestAction = new GiveQuestAction(main);
                    giveQuestAction.setQuestToGiveName(foundQuest.getQuestName());
                    giveQuestAction.setForceGive(forceGive);

                    main.getActionManager().addAction(giveQuestAction, context);
                }));
    }

    public final String getQuestToGiveName() {
        return questToGiveName;
    }

    public void setQuestToGiveName(final String questName) {
        this.questToGiveName = questName;
    }

    public final boolean isForceGive() {
        return forceGive;
    }

    public void setForceGive(final boolean forceGive) {
        this.forceGive = forceGive;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        Quest foundQuest = main.getQuestManager().getQuest(getQuestToGiveName());
        if (foundQuest == null) {
            main.getLogManager().warn("Tried to execute GiveQuest action with null quest. Cannot find the following Quest: " + getQuestToGiveName());
            return;
        }
        if (!isForceGive()) {
            main.getQuestPlayerManager().acceptQuest(player, foundQuest, true, true);
        } else {
            main.getQuestPlayerManager().forceAcceptQuest(player.getUniqueId(), foundQuest);
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.quest", getQuestToGiveName());
        configuration.set(initialPath + ".specifics.forceGive", isForceGive());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.questToGiveName = configuration.getString(initialPath + ".specifics.quest");
        this.forceGive = configuration.getBoolean(initialPath + ".specifics.forceGive");
    }


    @Override
    public String getActionDescription() {
        return "Gives Quest: " + getQuestToGiveName();
    }
}
