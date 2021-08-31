package notquests.notquests.Structs.Requirements;

public class Requirement {
    private final RequirementType requirementType;
    private final long progressNeeded;

    public Requirement(RequirementType requirementType, long progressNeeded) {
        this.requirementType = requirementType;
        this.progressNeeded = progressNeeded;
    }

    public final RequirementType getRequirementType() {
        return requirementType;
    }

    public final long getProgressNeeded() {
        return progressNeeded;
    }
}
