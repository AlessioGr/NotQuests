package com.notquests.core.managers.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.notquests.core.managers.tags.TagManager.Tag;

class TagManagerTagTest {
    @Test
    void normalizesTagNames() {
        final Tag tag = new Tag("Reputation", TagType.INTEGER);

        assertEquals("reputation", tag.tagName());
        assertEquals(TagType.INTEGER, tag.tagType());
    }

    @Test
    void rejectsInvalidTags() {
        assertThrows(IllegalArgumentException.class, () -> new Tag("", TagType.STRING));
        assertThrows(NullPointerException.class, () -> new Tag("name", null));
    }
}
