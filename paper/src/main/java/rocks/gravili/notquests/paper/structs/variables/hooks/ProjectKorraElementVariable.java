package rocks.gravili.notquests.paper.structs.variables.hooks;


import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class ProjectKorraElementVariable extends Variable<String[]> {
    public ProjectKorraElementVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String[] getValue(Player player, Object... objects) {
        if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
            return null;
        }
        if (player != null) {
            return main.getIntegrationsManager().getProjectKorraManager().getElements(player).toArray(new String[0]);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String[] newValue, Player player, Object... objects) {
        if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
            return false;
        }
        if (player != null) {
            main.getIntegrationsManager().getProjectKorraManager().setElements(player, new ArrayList<>(List.of(newValue)));
            return true;

        } else {
            return false;

        }
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return main.getIntegrationsManager().getProjectKorraManager().getAllElements();
    }

    @Override
    public String getPlural() {
        return "Elements";
    }

    @Override
    public String getSingular() {
        return "Element";
    }
}
