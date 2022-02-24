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

package rocks.gravili.notquests.spigot.managers.integrations;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import rocks.gravili.notquests.spigot.NotQuests;

import java.util.ArrayList;

public class ProjectKorraManager {
    private final NotQuests main;
    private final ArrayList<String> abilityCompletions;
    private ProjectKorra projectKorra;

    public ProjectKorraManager(final NotQuests main) {
        this.main = main;

        projectKorra = ProjectKorra.plugin;
        abilityCompletions = new ArrayList<>();
        for (Ability ability : CoreAbility.getAbilities()) {
            abilityCompletions.add(ability.getName());
        }
    }

    public ArrayList<String> getAbilityCompletions() {
        return abilityCompletions;
    }

    public final boolean isAbility(String abilityName) {
        return CoreAbility.getAbility(abilityName) != null;
    }
}
