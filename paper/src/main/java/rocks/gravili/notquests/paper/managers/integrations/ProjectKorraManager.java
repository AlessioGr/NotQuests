package rocks.gravili.notquests.paper.managers.integrations;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

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

    public final ArrayList<String> getElements(Player player) {
        ArrayList<String> elements = new ArrayList<>();
        for(Element element : BendingPlayer.getBendingPlayer(player).getElements()){
            elements.add(element.getName());
        }
        return elements;
    }

    public void setElements(Player player, ArrayList<String> elements) {
        BendingPlayer.getBendingPlayer(player).getElements().clear();
        ArrayList<Element> elements1 = new ArrayList<>();

        for(String elementString : elements){
            elements1.add(Element.getElement(elementString));
        }
        BendingPlayer.getBendingPlayer(player).getElements().addAll(elements1);
    }

    public final ArrayList<String> getAllElements() {
        ArrayList<String> allElementsStringList = new ArrayList<>();
        for(Element element : Element.getAllElements()){
            allElementsStringList.add(element.getName());
        }
        return allElementsStringList;
    }

}
