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
