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

package rocks.gravili.notquests.Structs.Objectives.hooks;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

public class TownyReachResidentCountObjective extends Objective {

    private final NotQuests main;
    private final boolean countPreviousResidents;


    public TownyReachResidentCountObjective(NotQuests main, final Quest quest, final int objectiveID, int progressNeeded, boolean countPreviousResidents) {
        super(main, quest, objectiveID, progressNeeded);
        this.main = main;
        this.countPreviousResidents = countPreviousResidents;
    }
    public TownyReachResidentCountObjective(NotQuests main, final Quest quest, final int objectiveID, int progressNeeded) {
        super(main, quest, objectiveID, progressNeeded);
        this.main = main;
        countPreviousResidents = main.getDataManager().getQuestsConfig().getBoolean("quests." + quest.getQuestName() + ".objectives." + objectiveID + ".specifics.countPreviousResidents");
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.isTownyEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("TownyReachResidentCount")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Minimum amounts of residents"))
                .flag(
                        manager.flagBuilder("doNotCountPreviousResidents")
                                .withDescription(ArgumentDescription.of("Makes it so only additional residents from the time of unlocking this Objective will count (and previous/existing counts will not count, so it starts from zero)"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new TownyReachResidentCount Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    int amount = context.get("amount");
                    final boolean countPreviousResidents = !context.flags().isPresent("doNotCountPreviousResidents");


                    TownyReachResidentCountObjective townyReachResidentCountObjective = new TownyReachResidentCountObjective(main, quest, quest.getObjectives().size() + 1, amount, countPreviousResidents);

                    quest.addObjective(townyReachResidentCountObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "TownyReachResidentCount objective successfully added to Quest " + NotQuestColors.highlightGradient + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    public final long getAmountOfResidentsToReach(){
        return getProgressNeeded();
    }

    public final boolean isCountPreviousResidents(){
        return countPreviousResidents;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.townyReachResidentCount.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%AMOUNT%", "" + getAmountOfResidentsToReach());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.countPreviousResidents", isCountPreviousResidents());

    }

    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {
        if(!isCountPreviousResidents()){
            return;
        }

        final Player player = activeObjective.getQuestPlayer().getPlayer();
        if(player == null){
            return;
        }
        Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

        if(resident == null){
            return;
        }

        Town town = resident.getTownOrNull();

        if(town == null){
            return;
        }

        activeObjective.addProgress(town.getNumResidents(), -1);
    }
}
