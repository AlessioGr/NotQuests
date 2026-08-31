package com.notquests.core.managers.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TagManagerValueTest {
    @Test
    void parsesStoredTagValuesByType() {
        assertEquals(true, TagManager.parse(TagType.BOOLEAN, "true"));
        assertEquals(7, TagManager.parse(TagType.INTEGER, "7"));
        assertEquals(1.5f, TagManager.parse(TagType.FLOAT, "1.5"));
        assertEquals(2.25d, TagManager.parse(TagType.DOUBLE, "2.25"));
        assertEquals("hello", TagManager.parse(TagType.STRING, "hello"));
        assertNull(TagManager.parse(TagType.STRING, null));
    }

    @Test
    void encodesSupportedTagValuesWithTheirType() {
        assertEquals(TagType.BOOLEAN, TagManager.encode(false).orElseThrow().type());
        assertEquals("12", TagManager.encode(12).orElseThrow().value());
        assertEquals(TagType.FLOAT, TagManager.encode(1.25f).orElseThrow().type());
        assertEquals(TagType.DOUBLE, TagManager.encode(1.25d).orElseThrow().type());
        assertEquals("text", TagManager.encode("text").orElseThrow().value());
    }

    @Test
    void rejectsUnsupportedOrWrongRuntimeTypes() {
        assertFalse(TagManager.encode(new Object()).isPresent());
        assertTrue(TagManager.matchesType("text", TagType.STRING));
        assertFalse(TagManager.matchesType("text", TagType.INTEGER));
        assertFalse(TagManager.matchesType(null, TagType.STRING));
    }
}
