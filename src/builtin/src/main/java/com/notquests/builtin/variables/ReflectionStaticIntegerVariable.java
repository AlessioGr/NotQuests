package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class ReflectionStaticIntegerVariable {
    private ReflectionStaticIntegerVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("ReflectionStaticInteger")
                .displayName("Static Integer Reflection")
                .description("Reads or changes a static Java integer field by reflection.")
                .singular("Integer from static reflection")
                .plural("Integers from static reflection")
                .field("ClassPath", adapter.fields().text(() -> List.of("<Enter class path>")), "Full Java class path that owns the static field.")
                .field("Field", adapter.fields().text(() -> List.of("<Enter field name>")), "Static integer field name to read or change.")
                .get(context -> number(adapter, context).intValue())
                .set((newValue, context) -> ReflectionStaticFields.set(
                        adapter, context.text("ClassPath"), context.text("Field"), newValue.intValue(), "ReflectionStaticInteger"))
                .register();
    }

    private static Number number(final NotQuestsAdapter adapter, final Variables.Context context) {
        final Object value = ReflectionStaticFields.get(
                adapter, context.text("ClassPath"), context.text("Field"), "ReflectionStaticInteger");
        return value instanceof Number number ? number : 0;
    }
}
