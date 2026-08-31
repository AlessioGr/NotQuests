package com.notquests.core.commands;

import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.QuestPlayer;

public record PlayerTarget(
        String lookup,
        QuestPlayer questPlayer,
        PlatformPlayer platformPlayer,
        String identifier,
        String displayName) {
    public boolean online() {
        return platformPlayer != null;
    }

    public String onlineStatus() {
        return online() ? "<green>(online)</green>" : "<red>(offline)</red>";
    }
}
