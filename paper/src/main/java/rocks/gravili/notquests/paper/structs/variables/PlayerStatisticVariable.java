/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Statistic;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatisticVariable extends Variable<Integer> {
    public PlayerStatisticVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
        addRequiredString(StringVariableValueParser.of("Statistic", null, (context, input) -> {
            final ArrayList<String> suggestions = new ArrayList<>();
            for (final Statistic statistic : Statistic.values()) {
                if (statistic.getType() == Statistic.Type.UNTYPED) {
                    suggestions.add(statistic.name());
                }
            }
            suggestions.add("<Enter Statistic name>");
            return suggestions;
        }));
    }

    @Override
    public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            final String statisticName = getRequiredStringValue("Statistic");
            final Statistic statistic;
            try {
                statistic = Statistic.valueOf(statisticName);
            } catch (final IllegalArgumentException e) {
                main.getLogManager().severe("Tried to get statistic with name <highlight>" + statisticName + "</highlight2> when getting variable but it doesn't exist! This is not an error in NotQuests - you simply entered a statistic which does not exist. Please fix it!");
                return null;
            }

            return questPlayer.getPlayer().getStatistic(statistic);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            final String statisticName = getRequiredStringValue("Statistic");
            final Statistic statistic;
            try {
                statistic = Statistic.valueOf(statisticName);
            } catch (final IllegalArgumentException e) {
                main.getLogManager().severe("Tried to get statistic with name <highlight>" + statisticName + "</highlight2> when setting variable but it doesn't exist! This is not an error in NotQuests - you simply entered a statistic which does not exist. Please fix it!");
                return false;
            }
            questPlayer.getPlayer().setStatistic(statistic, newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Statistics";
    }

    @Override
    public String getSingular() {
        return "Statistic";
    }
}
