package notquests.notquests.Structs.Requirements;

import notquests.notquests.NotQuests;

public class PermissionRequirement extends Requirement {

    private final NotQuests main;
    private final String requiredPermission;


    public PermissionRequirement(NotQuests main, String requiredPermission) {
        super(RequirementType.Permission, 1);
        this.main = main;
        this.requiredPermission = requiredPermission;

    }


    public final String getRequiredPermission() {
        return requiredPermission;
    }


}
