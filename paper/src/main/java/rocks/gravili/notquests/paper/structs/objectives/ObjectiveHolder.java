package rocks.gravili.notquests.paper.structs.objectives;

import java.util.ArrayList;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.structs.PredefinedProgressOrder;

public abstract class ObjectiveHolder {
  protected PredefinedProgressOrder predefinedProgressOrder;
  private final ArrayList<Objective> objectives;

  private String objectiveHolderDescription = "";

  public ObjectiveHolder(){
    objectives = new ArrayList<>();
  }

  public final PredefinedProgressOrder getPredefinedProgressOrder(){
    return this.predefinedProgressOrder;
  }

  public final ArrayList<Objective> getObjectives(){
    return this.objectives;
  }

  public final int getFreeObjectiveID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getObjectiveFromID(i) == null) {
        return i;
      }
    }
    return getObjectives().size() + 1;
  }

  public abstract void setPredefinedProgressOrder(final PredefinedProgressOrder predefinedProgressOrder,
      final boolean save);


  public abstract FileConfiguration getConfig();

  public abstract String getInitialConfigPath();

  public abstract String getIdentifier();

  public abstract void saveConfig();

  public abstract void clearObjectives();

  public abstract Objective getObjectiveFromID(final int objectiveID);

  public abstract void removeObjective(final Objective objective);


  public abstract void addObjective(Objective objective, boolean save);

  public abstract String getDisplayNameOrIdentifier();

  public String getObjectiveHolderDescription(){
    return objectiveHolderDescription;
  }

  public void setObjectiveHolderDescription(final String objectiveHolderDescription){
    this.objectiveHolderDescription = objectiveHolderDescription;
  }


}
