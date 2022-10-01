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

package rocks.gravili.notquests.paper.managers.integrations;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

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

  public final ArrayList<String> getElements(Player player) {
    ArrayList<String> elements = new ArrayList<>();
    for (Element element : BendingPlayer.getBendingPlayer(player).getElements()) {
      elements.add(element.getName());
    }
    return elements;
  }

  public void setElements(Player player, ArrayList<String> elements) {
    BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
    bendingPlayer.getElements().clear();

    for (String elementString : elements) {
      bendingPlayer.addElement(Element.getElement(elementString));
    }
  }

  public final ArrayList<String> getAllElements() {
    ArrayList<String> allElementsStringList = new ArrayList<>();
    for (Element element : Element.getAllElements()) {
      allElementsStringList.add(element.getName());
    }
    return allElementsStringList;
  }

  public void setSubElements(Player player, ArrayList<String> subElements) {
    BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
    bendingPlayer.getSubElements().clear();

    for (String subElementString : subElements) {
      for (Element.SubElement subElement : Element.getAllSubElements()) {
        if (subElement.getName().equalsIgnoreCase(subElementString)) {
          bendingPlayer.addSubElement(subElement);
        }
      }
    }
  }

  public final ArrayList<String> getAllSubElements() {
    final ArrayList<String> allSubElementsStringList = new ArrayList<>();
    for (final Element.SubElement subElement : Element.getAllSubElements()) {
      allSubElementsStringList.add(subElement.getName());
    }
    return allSubElementsStringList;
  }

  public final ArrayList<String> getSubElements(final Player player) {
    final ArrayList<String> subElements = new ArrayList<>();
    for (final Element.SubElement subElement : BendingPlayer.getBendingPlayer(player).getSubElements()) {
      subElements.add(subElement.getName());
    }
    return subElements;
  }

  public final boolean isBender(final Player player) {
    return BendingPlayer.getBendingPlayer(player).isBender();
  }
}
