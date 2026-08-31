package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerDatabaseSchemaTest {
    @Test
    void tableCreationStatementsCoverEveryRuntimeTable() {
        assertEquals(8, PlayerDatabase.TABLE_CREATION_STATEMENTS.size());
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_QUEST_PLAYER_PROFILE_DATA));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_QUEST_PLAYER_DATA));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_ACTIVE_QUESTS));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_FAILED_QUESTS));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_COMPLETED_QUESTS));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_ACTIVE_OBJECTIVES));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_ACTIVE_TRIGGERS));
        assertTrue(PlayerDatabase.TABLE_CREATION_STATEMENTS.contains(PlayerDatabase.CREATE_TAGS));
    }

    @Test
    void tagStatementsUseProfileScopedRuntimeShape() {
        assertTrue(PlayerDatabase.SELECT_TAGS.contains("PlayerUUID = ? AND Profile = ?"));
        assertTrue(PlayerDatabase.DELETE_TAGS.contains("PlayerUUID = ? AND Profile = ?"));
        assertTrue(PlayerDatabase.INSERT_TAG.contains("(PlayerUUID, TagIdentifier, TagValue, TagType, Profile)"));
    }
}
