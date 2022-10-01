package rocks.gravili.notquests.paper.managers.npc;

import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public class NQNPCID {
  private final int integerID; /*-1 = null*/
  private final @Nullable UUID uuidID;

  private NQNPCID(final int integerID /*-1 = null*/, final @Nullable UUID uuidID) {
    this.integerID = integerID;
    this.uuidID = uuidID;
  }


  public final int getIntegerID() { /*-1 = null*/
    return integerID;
  }

  public final @Nullable UUID getUUIDID() {
    return uuidID;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof NQNPCID other)) {
      return false;
    }
    return
        (integerID == -1 && other.integerID == -1 && uuidID != null && other.uuidID != null && uuidID.equals(other.uuidID))
            || (uuidID == null && other.uuidID == null && integerID == other.integerID);
  }

  @Override
  public String toString() {
    return "NQNPCID{" +
        "integerID=" + integerID +
        ", uuidID=" + uuidID +
        '}';
  }

  public void saveToConfig(final FileConfiguration fileConfiguration, final String partialPath){
    if(integerID != -1){
      fileConfiguration.set(partialPath + ".integerID", getIntegerID());
    }else if(getUUIDID() != null) {
      fileConfiguration.set(partialPath + ".uuidID", getUUIDID().toString());
    }
  }

  public static @Nullable NQNPCID loadFromConfig(final FileConfiguration fileConfiguration, final String partialPath){
    if(fileConfiguration.isInt(partialPath + ".integerID")){
      return fromInteger(fileConfiguration.getInt(partialPath + ".integerID"));
    } else if(fileConfiguration.isString(partialPath + ".uuidID")){
      final String uuidString = fileConfiguration.getString(partialPath + ".uuidID");
      if(uuidString != null){
        return NQNPCID.fromUUID(UUID.fromString(uuidString));
      }
    }
    return null;
  }

  public static NQNPCID fromInteger(final int integerID) {
    return new NQNPCID(integerID, null);
  }

  public static NQNPCID fromUUID(final UUID uuidID) {
    return new NQNPCID(-1, uuidID);
  }

  public final String getEitherAsString(){
    if(uuidID != null){
      return uuidID.toString();
    }
    return ""+integerID;
  }
}
