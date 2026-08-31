package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

class PlayerDatabaseRowsTest {
    @Test
    void questPlayerDataDefaultsMissingProfileColumn() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final PlayerDatabase.QuestPlayerDataRow row = PlayerDatabase.QuestPlayerDataRow.read(
                resultSet(Map.of("PlayerUUID", uuid.toString(), "QuestPoints", 25L)),
                null);

        assertEquals(uuid, row.playerUuid());
        assertEquals(25L, row.questPoints());
        assertEquals("default", row.profile());
    }

    @Test
    void questPlayerDataDefaultsBlankProfileColumn() throws Exception {
        final PlayerDatabase.QuestPlayerDataRow row = PlayerDatabase.QuestPlayerDataRow.read(
                resultSet(Map.of("QuestPoints", 5L, "Profile", "")),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertEquals("default", row.profile());
    }

    @Test
    void activeObjectiveTracksNullProgressNeeded() throws Exception {
        final PlayerDatabase.ActiveObjectiveReadRow row = PlayerDatabase.ActiveObjectiveReadRow.read(
                resultSet(Map.of(
                        "ObjectiveType", "BreakBlocks",
                        "QuestName", "MyQuest.1",
                        "CurrentProgress", 3.0D,
                        "ObjectiveID", 2,
                        "HasBeenCompleted", true,
                        "ProgressNeeded", NullValue.INSTANCE)));

        assertEquals("BreakBlocks", row.objectiveType());
        assertEquals("MyQuest.1", row.holderPath());
        assertEquals(3.0D, row.currentProgress());
        assertEquals(2, row.objectiveId());
        assertTrue(row.completed());
        assertTrue(row.progressNeededNull());
    }

    @Test
    void activeTriggerReadsCoreRuntimeColumns() throws Exception {
        final PlayerDatabase.ActiveTriggerReadRow row = PlayerDatabase.ActiveTriggerReadRow.read(
                resultSet(Map.of("TriggerType", "BEGIN", "CurrentProgress", 7L, "TriggerID", 4)));

        assertEquals("BEGIN", row.triggerType());
        assertEquals(7L, row.currentProgress());
        assertEquals(4, row.triggerId());
    }

    private static ResultSet resultSet(final Map<String, Object> values) {
        final InvocationHandler handler = new InvocationHandler() {
            private boolean lastWasNull;

            @Override
            public Object invoke(final Object proxy, final java.lang.reflect.Method method, final Object[] args)
                    throws Throwable {
                final String methodName = method.getName();
                if (methodName.equals("findColumn")) {
                    final String column = (String) args[0];
                    if (!values.containsKey(column)) {
                        throw new SQLException("No such column: " + column);
                    }
                    return 1;
                }
                if (methodName.equals("wasNull")) {
                    return lastWasNull;
                }
                if (methodName.equals("toString")) {
                    return values.toString();
                }
                final Object value = values.get((String) args[0]);
                lastWasNull = value == null || value == NullValue.INSTANCE;
                final Object normalized = lastWasNull ? null : value;
                return switch (methodName) {
                    case "getString" -> normalized == null ? null : normalized.toString();
                    case "getLong" -> normalized == null ? 0L : ((Number) normalized).longValue();
                    case "getDouble" -> normalized == null ? 0D : ((Number) normalized).doubleValue();
                    case "getInt" -> normalized == null ? 0 : ((Number) normalized).intValue();
                    case "getBoolean" -> normalized != null && (Boolean) normalized;
                    default -> throw new UnsupportedOperationException(methodName);
                };
            }
        };
        return (ResultSet) Proxy.newProxyInstance(
                PlayerDatabaseRowsTest.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                handler);
    }

    private enum NullValue {
        INSTANCE
    }
}
