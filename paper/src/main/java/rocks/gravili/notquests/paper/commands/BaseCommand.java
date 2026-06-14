package rocks.gravili.notquests.paper.commands;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;

public abstract class BaseCommand {
    protected final NotQuests notQuests;
    protected NQCommandBuilder builder;

    public BaseCommand(NotQuests notQuests, NQCommandBuilder builder) {
        this.notQuests = notQuests;
        this.builder = builder;
    }

    public abstract void apply(NQCommandManager commandManager);
}
