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

import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class MoneyVariable extends Variable<Double> {
  public MoneyVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      if (!main.getIntegrationsManager().isVaultEnabled()
          || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
        return 0D;
      } else {
        return main.getIntegrationsManager()
            .getVaultManager()
            .getEconomy()
            .getBalance(questPlayer.getPlayer(), questPlayer.getPlayer().getWorld().getName());
      }
    } else {
      return 0D;
    }
  }

  @Override
  public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      if (!main.getIntegrationsManager().isVaultEnabled()
          || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
        return false;
      } else {
        final double currentBalance =
            main.getIntegrationsManager()
                .getVaultManager()
                .getEconomy()
                .getBalance(questPlayer.getPlayer());
        if (newValue > currentBalance) {
          // player.sendMessage("Deposited " + (newValue-currentBalance));
          main.getIntegrationsManager()
              .getVaultManager()
              .getEconomy()
              .depositPlayer(questPlayer.getPlayer(), newValue - currentBalance);
        } else {
          // player.sendMessage("Withdraw " + (currentBalance - newValue));

          main.getIntegrationsManager()
              .getVaultManager()
              .getEconomy()
              .withdrawPlayer(questPlayer.getPlayer(), currentBalance - newValue);
        }
        return true;
      }
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
    return "Money";
  }

  @Override
  public String getSingular() {
    return "Money";
  }
}
