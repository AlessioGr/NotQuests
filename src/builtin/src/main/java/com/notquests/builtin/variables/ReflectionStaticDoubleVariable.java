package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class ReflectionStaticDoubleVariable {
    private ReflectionStaticDoubleVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("ReflectionStaticDouble")
                .displayName("Static Double Reflection")
                .description("Reads or changes a static Java double field by reflection.")
                .singular("Double from static reflection")
                .plural("Doubles from static reflection")
                .field("ClassPath", adapter.fields().text(() -> List.of("<Enter class path>")), "Full Java class path that owns the static field.")
                .field("Field", adapter.fields().text(() -> List.of("<Enter field name>")), "Static double field name to read or change.")
                .get(context -> number(adapter, context).doubleValue())
                .set((newValue, context) -> ReflectionStaticFields.set(
                        adapter, context.text("ClassPath"), context.text("Field"), newValue.doubleValue(), "ReflectionStaticDouble"))
                .register();
    }

    private static Number number(final NotQuestsAdapter adapter, final Variables.Context context) {
        final Object value = ReflectionStaticFields.get(
                adapter, context.text("ClassPath"), context.text("Field"), "ReflectionStaticDouble");
        return value instanceof Number number ? number : 0d;
    }
}
