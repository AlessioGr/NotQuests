package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

import java.util.UUID;

public class TalkToNPCObjective extends Objective {

    private final NotQuests main;
    private final int NPCtoTalkID;

    private final UUID armorStandUUID;

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int NPCtoTalkID) {
        super(main, quest, objectiveID, ObjectiveType.TalkToNPC, 1);
        this.main = main;
        this.NPCtoTalkID = NPCtoTalkID;
        this.armorStandUUID = null;
    }

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final UUID armorStandUUID) {
        super(main, quest, objectiveID, ObjectiveType.TalkToNPC, 1);
        this.main = main;
        this.NPCtoTalkID = -1;
        this.armorStandUUID = armorStandUUID;
    }


    public final int getNPCtoTalkID() {
        return NPCtoTalkID;
    }

    public final UUID getArmorStandUUID() {
        return armorStandUUID;
    }


}