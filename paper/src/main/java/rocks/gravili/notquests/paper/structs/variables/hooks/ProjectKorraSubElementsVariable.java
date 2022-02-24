package rocks.gravili.notquests.paper.structs.variables.hooks;


import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class ProjectKorraSubElementsVariable extends Variable<String[]> {
    public ProjectKorraSubElementsVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String[] getValue(QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
            return null;
        }
        if (questPlayer != null) {
            return main.getIntegrationsManager().getProjectKorraManager().getSubElements(questPlayer.getPlayer()).toArray(new String[0]);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
            return false;
        }
        if (questPlayer != null) {
            main.getIntegrationsManager().getProjectKorraManager().setSubElements(questPlayer.getPlayer(), new ArrayList<>(List.of(newValue)));
            return true;

        } else {
            return false;

        }
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return main.getIntegrationsManager().getProjectKorraManager().getAllSubElements();
    }

    @Override
    public String getPlural() {
        return "SubElements";
    }

    @Override
    public String getSingular() {
        return "SubElement";
    }
}
