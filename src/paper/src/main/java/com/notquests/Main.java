package com.notquests;

import org.bukkit.plugin.java.JavaPlugin;

import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.PaperBuiltins;

/** Paper plugin entrypoint. The actual NotQuests plugin lifecycle lives in core. */
public final class Main extends JavaPlugin {
    private static Main instance;
    private NotQuests notQuests;

    @Override
    public void onLoad() {
        instance = this;
        notQuests = new NotQuests(this);
        notQuests.addRegistryPack(new PaperBuiltins());
        notQuests.onLoad();
    }

    public NotQuests getNotQuests() {
        return notQuests;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        notQuests.onEnable();
    }

    @Override
    public void onDisable() {
        if (notQuests != null) {
            notQuests.onDisable();
        }
    }
}
