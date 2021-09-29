package notquests.notquests.Structs.Objectives.hooks;

import net.citizensnpcs.api.event.SpawnReason;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Objectives.ObjectiveType;
import notquests.notquests.Structs.Quest;

public class KillEliteMobsObjective extends Objective {

    private final NotQuests main;
    private final String eliteMobToKillContainsName;
    private final int minimumLevel, maximumLevel;
    private final SpawnReason spawnReason; //Optional. If null, any spawn reason will be used
    private final int minimumDamagePercentage; //How much damage the player has to do to the mob minimum
    private final int amountToKill;

    public KillEliteMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String eliteMobToKillContainsName, int minimumLevel, int maximumLevel, SpawnReason spawnReason, int minimumDamagePercentage, int amountToKill) {
        super(main, quest, objectiveID, ObjectiveType.KillEliteMobs, amountToKill);
        this.main = main;
        this.eliteMobToKillContainsName = eliteMobToKillContainsName;
        this.minimumLevel = minimumLevel;
        this.maximumLevel = maximumLevel;
        this.spawnReason = spawnReason;
        this.minimumDamagePercentage = minimumDamagePercentage;
        this.amountToKill = amountToKill;
    }

    public final String getEliteMobToKillContainsName() {
        return eliteMobToKillContainsName;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }

    public final int getMinimumLevel() {
        return minimumLevel;
    }

    public final int getMaximumLevel() {
        return maximumLevel;
    }

    public final SpawnReason getSpawnReason() {
        return spawnReason;
    }

    public final int getMinimumDamagePercentage() {
        return minimumDamagePercentage;
    }


}
