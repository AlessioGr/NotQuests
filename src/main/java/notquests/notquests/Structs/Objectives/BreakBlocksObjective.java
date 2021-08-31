package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;

public class BreakBlocksObjective extends Objective {

    private final NotQuests main;
    private final Material blockToBreak;
    private final int amountToBreak;
    private final boolean deductIfBlockIsPlaced;

    public BreakBlocksObjective(NotQuests main, final Quest quest, final int objectiveID, Material blockToBreak, int amountToBreak, boolean deductIfBlockIsPlaced) {
        super(main, quest, objectiveID, ObjectiveType.BreakBlocks, amountToBreak);
        this.main = main;
        this.blockToBreak = blockToBreak;
        this.amountToBreak = amountToBreak;
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final int getAmountToBreak() {
        return amountToBreak;
    }

    public final boolean willDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }
}
