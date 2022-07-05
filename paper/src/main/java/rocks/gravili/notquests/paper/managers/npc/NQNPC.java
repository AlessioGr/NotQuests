package rocks.gravili.notquests.paper.managers.npc;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;

public abstract class NQNPC {
  protected final NotQuests main;
  private final String npcType;
  public NQNPC(final NotQuests main, final String npcType){
    this.main = main;
    this.npcType = npcType;
  }
  public abstract @Nullable String getName();
  public abstract @NotNull int getID();

  public final @NotNull String getNPCType() {
    return npcType;
  }

  public void saveToConfig(final FileConfiguration fileConfiguration, final String partialPath){
    fileConfiguration.set(partialPath + ".type", getNPCType());
    fileConfiguration.set(partialPath + ".id", getID());
    fileConfiguration.set(partialPath + ".name", getName());
  }

  public static NQNPC fromConfig(final NotQuests main, final FileConfiguration fileConfiguration, final String partialPath){
    final String type = fileConfiguration.getString(partialPath + ".type");
    final int id = fileConfiguration.getInt(partialPath + ".id");
    final String name = fileConfiguration.getString(partialPath + ".name");
    if(type == null){
      return null;
    }
    if(type.equals("Citizens")){
      return new CitizensNPC(main, id);
    }
    return null;
  }

  //DO NOT PERSIST? Or do persist? idk. Adds trait for citizens npc
  public abstract void bindToConversation(final Conversation conversation);
}
