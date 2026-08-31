package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CategoryNamesTest {
    @Test
    void blankNamesUseDefaultCategory() {
        assertEquals("default", Category.canonical(""));
        assertEquals("default", Category.canonical(null));
        assertTrue(Category.isDefault("Default"));
    }

    @Test
    void joinsParentAndChildNames() {
        assertEquals("story.daily", Category.join("story", "daily"));
        assertEquals("daily", Category.join("", "daily"));
    }

    @Test
    void detectsTopLevelAndLeafNames() {
        assertTrue(Category.isTopLevel("story"));
        assertFalse(Category.isTopLevel("story.daily"));
        assertEquals("story", Category.parent("story.daily"));
        assertEquals("daily", Category.leaf("story.daily"));
    }
}
