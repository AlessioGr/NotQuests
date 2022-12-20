package rocks.gravili.notquests.paper.structs.variables.hooks;

import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class FloodgateIsFloodgatePlayerVariable extends Variable<Boolean> {
  public FloodgateIsFloodgatePlayerVariable(NotQuests main) {
    super(main);
    setCanSetValue(false);
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return main.getIntegrationsManager().getFloodgateManager().isPlayerOnFloodgate(questPlayer.getUniqueId());
    } else {
      return false;
    }
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Connected using Floodgate";
  }

  @Override
  public String getSingular() {
    return "Connected using Floodgate";
  }
}
