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

package rocks.gravili.notquests.paper.structs.variables.hooks;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

public class UltimateClansClanLevelVariable extends Variable<Integer> {
    public UltimateClansClanLevelVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Integer getValue(QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isUltimateClansEnabled()) {
            return 0;
        }
        if (questPlayer != null) {
            return main.getIntegrationsManager().getUltimateClansManager().getClanLevel(questPlayer.getPlayer());
        } else {
            return 0;
        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isUltimateClansEnabled()) {
            return false;
        }
        if (questPlayer != null) {
            main.getIntegrationsManager().getUltimateClansManager().setClanLevel(questPlayer.getPlayer(), newValue);
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
        return "Clan Level";
    }

    @Override
    public String getSingular() {
        return "Clan Levels";
    }
}
