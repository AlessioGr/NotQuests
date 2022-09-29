package rocks.gravili.notquests.paper.commands.arguments.wrappers;

import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;

public class NQNPCResult {
  private final @Nullable NQNPC nqnpc;
  private final boolean none;
  private final boolean rightClickSelect;

  public NQNPCResult(final @Nullable NQNPC nqnpc, final boolean none, final boolean rightClickSelect) {
    this.nqnpc = nqnpc;
    this.none = none;
    this.rightClickSelect = rightClickSelect;
  }

  public final @Nullable NQNPC getNQNPC() {
    return nqnpc;
  }

  public final boolean isNone() {
    return none;
  }

  public final boolean isRightClickSelect() {
    return rightClickSelect;
  }

}
