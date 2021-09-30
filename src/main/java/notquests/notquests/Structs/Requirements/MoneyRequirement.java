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

package notquests.notquests.Structs.Requirements;

import notquests.notquests.NotQuests;

public class MoneyRequirement extends Requirement {

    private final NotQuests main;
    private final long moneyRequirement;
    private final boolean deductMoney;


    public MoneyRequirement(final NotQuests main, final long moneyRequirement, final boolean deductMoney) {
        super(RequirementType.Money, moneyRequirement);
        this.main = main;
        this.moneyRequirement = moneyRequirement;
        this.deductMoney = deductMoney;

    }


    public final long getMoneyRequirement() {
        return moneyRequirement;
    }


    public final boolean isDeductMoney() {
        return deductMoney;
    }

}
