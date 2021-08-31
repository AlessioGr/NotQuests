package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

public class EscortNPCObjective extends Objective {

    private final NotQuests main;
    private final int npcToEscortID;
    private final int npcToEscortToID;

    public EscortNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int npcToEscortID, final int npcToEscortToID) {
        super(main, quest, objectiveID, ObjectiveType.EscortNPC, 1);
        this.main = main;
        this.npcToEscortID = npcToEscortID;
        this.npcToEscortToID = npcToEscortToID;
    }

    public final int getNpcToEscortID() {
        return npcToEscortID;
    }

    public final int getNpcToEscortToID() {
        return npcToEscortToID;
    }


}
