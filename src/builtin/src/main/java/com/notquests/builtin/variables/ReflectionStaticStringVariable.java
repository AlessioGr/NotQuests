package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class ReflectionStaticStringVariable {
    private ReflectionStaticStringVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("ReflectionStaticString")
                .displayName("Static String Reflection")
                .description("Reads or changes a static Java text field by reflection.")
                .singular("String from static reflection")
                .plural("Strings from static reflection")
                .field("ClassPath", adapter.fields().text(() -> List.of("<Enter class path>")), "Full Java class path that owns the static field.")
                .field("Field", adapter.fields().text(() -> List.of("<Enter field name>")), "Static text field name to read or change.")
                .get(context -> {
                    final Object value = ReflectionStaticFields.get(
                            adapter, context.text("ClassPath"), context.text("Field"), "ReflectionStaticString");
                    return value instanceof String string ? string : "";
                })
                .set((newValue, context) -> ReflectionStaticFields.set(
                        adapter, context.text("ClassPath"), context.text("Field"), newValue, "ReflectionStaticString"))
                .register();
    }
}
