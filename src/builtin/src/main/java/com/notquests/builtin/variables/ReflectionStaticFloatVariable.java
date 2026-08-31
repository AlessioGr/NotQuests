package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class ReflectionStaticFloatVariable {
    private ReflectionStaticFloatVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("ReflectionStaticFloat")
                .displayName("Static Float Reflection")
                .description("Reads or changes a static Java float field by reflection.")
                .singular("Float from static reflection")
                .plural("Floats from static reflection")
                .field("ClassPath", adapter.fields().text(() -> List.of("<Enter class path>")), "Full Java class path that owns the static field.")
                .field("Field", adapter.fields().text(() -> List.of("<Enter field name>")), "Static float field name to read or change.")
                .get(context -> number(adapter, context).floatValue())
                .set((newValue, context) -> ReflectionStaticFields.set(
                        adapter, context.text("ClassPath"), context.text("Field"), newValue.floatValue(), "ReflectionStaticFloat"))
                .register();
    }

    private static Number number(final NotQuestsAdapter adapter, final Variables.Context context) {
        final Object value = ReflectionStaticFields.get(
                adapter, context.text("ClassPath"), context.text("Field"), "ReflectionStaticFloat");
        return value instanceof Number number ? number : 0f;
    }
}
