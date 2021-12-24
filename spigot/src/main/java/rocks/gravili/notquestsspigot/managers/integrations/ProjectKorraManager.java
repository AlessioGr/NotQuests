package rocks.gravili.notquestsspigot.managers.integrations;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import rocks.gravili.notquestsspigot.NotQuests;

import java.util.ArrayList;

public class ProjectKorraManager {
    private final NotQuests main;
    private final ArrayList<String> abilityCompletions;
    private ProjectKorra projectKorra;

    public ProjectKorraManager(final NotQuests main) {
        this.main = main;

        projectKorra = ProjectKorra.plugin;
        abilityCompletions = new ArrayList<>();
        for (Ability ability : CoreAbility.getAbilities()) {
            abilityCompletions.add(ability.getName());
        }
    }

    public ArrayList<String> getAbilityCompletions() {
        return abilityCompletions;
    }

    public final boolean isAbility(String abilityName) {
        return CoreAbility.getAbility(abilityName) != null;
    }
}
