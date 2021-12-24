package rocks.gravili.notquests.paper.managers.integrations;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import rocks.gravili.notquests.paper.NotQuests;

public class SlimefunManager {
    private final NotQuests main;
    private Slimefun slimefun;

    public SlimefunManager(final NotQuests main) {
        this.main = main;
        slimefun = Slimefun.instance();
    }

    public Slimefun getSlimefun() {
        return slimefun;
    }

}
