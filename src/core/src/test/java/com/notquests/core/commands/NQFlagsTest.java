package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.*;

import java.util.List;

class NQFlagsTest {
    @Test
    void ownsSharedFlagNamesAndDescriptions() {
        final List<NQFlags.Flag> flags = List.of(
                NQFlags.NAMETAG_CONTAINS_ANY,
                NQFlags.NAMETAG_EQUALS,
                NQFlags.TASK_DESCRIPTION,
                NQFlags.SPEAKER_COLOR,
                NQFlags.MAX_DISTANCE,
                NQFlags.WORLD,
                NQFlags.APPLY_ON,
                NQFlags.TRIGGER_WORLD,
                NQFlags.WAIT_TIME_AFTER_COMPLETION,
                NQFlags.CATEGORY,
                NQFlags.FORCE,
                NQFlags.DELAY,
                NQFlags.PLAYER,
                NQFlags.ACTION_TARGET_PLAYER,
                NQFlags.IGNORE_ACTION_CONDITIONS,
                NQFlags.SILENT_ACTION_EXECUTION,
                NQFlags.VARIABLE_CHECK_PLAYER,
                NQFlags.CONVERSATION_DEMO,
                NQFlags.PRINT_TO_CONSOLE,
                NQFlags.NEGATE,
                NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED,
                NQFlags.LOCATION_X,
                NQFlags.LOCATION_Y,
                NQFlags.LOCATION_Z,
                NQFlags.GUI_ITEM_GLOW,
                NQFlags.HIDE_IN_NPC,
                NQFlags.HIDE_IN_ARMOR_STAND);

        assertEquals(27, flags.size());
        flags.forEach(flag -> {
            assertFalse(flag.name().isBlank());
            assertFalse(flag.description().textDescription().isBlank());
        });
    }
}
