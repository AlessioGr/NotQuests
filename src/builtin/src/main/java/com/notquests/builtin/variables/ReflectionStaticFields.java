package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.lang.reflect.Field;

final class ReflectionStaticFields {
    private ReflectionStaticFields() {}

    static Object get(final NotQuestsAdapter adapter, final String classPath, final String fieldName, final String variableId) {
        try {
            return field(classPath, fieldName).get(null);
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            adapter.warn("Reflection in " + variableId + " failed: " + exception.getMessage());
            return null;
        }
    }

    static boolean set(
            final NotQuestsAdapter adapter,
            final String classPath,
            final String fieldName,
            final Object value,
            final String variableId) {
        try {
            field(classPath, fieldName).set(null, value);
            return true;
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            adapter.warn("Reflection in " + variableId + " failed: " + exception.getMessage());
            return false;
        }
    }

    private static Field field(final String classPath, final String fieldName)
            throws ClassNotFoundException, NoSuchFieldException {
        final Class<?> foundClass = Class.forName(classPath);
        final Field field = foundClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
