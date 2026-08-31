package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class ReflectionStaticBooleanVariable {
    private ReflectionStaticBooleanVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("ReflectionStaticBoolean")
                .displayName("Static Boolean Reflection")
                .description("Reads or changes a static Java boolean field by reflection.")
                .singular("Boolean from static reflection")
                .plural("Booleans from static reflection")
                .field("ClassPath", adapter.fields().text(() -> List.of("<Enter class path>")), "Full Java class path that owns the static field.")
                .field("Field", adapter.fields().text(() -> List.of("<Enter field name>")), "Static boolean field name to read or change.")
                .get(context -> Boolean.TRUE.equals(value(adapter, context)))
                .set((newValue, context) -> ReflectionStaticFields.set(
                        adapter, context.text("ClassPath"), context.text("Field"), newValue, "ReflectionStaticBoolean"))
                .register();
    }

    private static Object value(final NotQuestsAdapter adapter, final Variables.Context context) {
        return ReflectionStaticFields.get(
                adapter, context.text("ClassPath"), context.text("Field"), "ReflectionStaticBoolean");
    }
}
