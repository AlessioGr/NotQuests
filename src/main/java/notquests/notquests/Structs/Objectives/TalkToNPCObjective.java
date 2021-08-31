package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

public class TalkToNPCObjective extends Objective {

    private final NotQuests main;
    private final int NPCtoTalkID;

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int NPCtoTalkID) {
        super(main, quest, objectiveID, ObjectiveType.TalkToNPC, 1);
        this.main = main;
        this.NPCtoTalkID = NPCtoTalkID;
    }


    public final int getNPCtoTalkID() {
        return NPCtoTalkID;
    }


}