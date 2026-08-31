package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class PredefinedProgressOrderTest {
    @Test
    void formatsProgressOrderForSharedCoreState() {
        assertEquals("", PredefinedProgressOrder.toConfigString(null));
        assertEquals("firstToLast", PredefinedProgressOrder.toConfigString(PredefinedProgressOrder.firstToLast()));
        assertEquals("lastToFirst", PredefinedProgressOrder.toConfigString(PredefinedProgressOrder.lastToFirst()));
        assertEquals(
                "custom 3,1,2",
                PredefinedProgressOrder.toConfigString(PredefinedProgressOrder.custom(new ArrayList<>(List.of("3", "1", "2")))));
    }
}
