package com.notquests.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Npc;
import com.notquests.paper.NotQuests;

public abstract class NQNPC implements Npc {
  protected final NotQuests main;
  private final String npcType;
  public NQNPC(final NotQuests main, final String npcType){
    this.main = main;
    this.npcType = npcType;
  }
  public abstract @Nullable String getName();
  public abstract NQNPCID getID();

  public final @NotNull String getNPCType() {
    return npcType;
  }

  @Override
  public final String npcType() {
    return getNPCType();
  }

  @Override
  public final NQNPCID npcId() {
    return getID();
  }

  @Override
  public final @Nullable String npcName() {
    return getName();
  }
}
