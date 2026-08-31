package com.notquests.core.metadata;

import com.notquests.core.commands.framework.NQCommandSchema.CommandIndex;
import com.notquests.core.commands.framework.NQCommandSchema;
import com.notquests.core.metadata.NQMetadataSchema.MetadataIndex;
import com.notquests.core.metadata.NQMetadataSchema.RegistryIndex;
import com.notquests.core.metadata.NQMetadataSchema.Field;
import com.notquests.core.metadata.NQMetadataSchema.Type;
import com.notquests.core.metadata.NQMetadataSchema.Variable;
import com.notquests.core.registry.NotQuestsRegistry.Variables.BooleanVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ItemStackListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.NumberVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.StringVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.util.List;
import java.util.Locale;

public final class NQMetadataExporter {
    public MetadataIndex metadataIndex(final RuntimeMetadataSource source) {
        return new MetadataIndex(
                source.pluginVersion(),
                source.minecraftVersion(),
                source.commandIndex(),
                new RegistryIndex(
                        source.objectives(),
                        source.actions(),
                        source.conditions(),
                        source.triggers(),
                        source.variables()));
    }

    public interface RuntimeMetadataSource {
        String pluginVersion();

        String minecraftVersion();

        CommandIndex commandIndex();

        List<Type> objectives();

        List<Type> actions();

        List<Type> conditions();

        List<Type> triggers();

        List<Variable> variables();
    }

    public static List<Type> objectives(final NotQuestsRegistry registry) {
        return registry.objectives().stream()
                .map(type -> typeInfo(type.id(), type.displayName(), type.description(), type.fields(), type.flags()))
                .toList();
    }

    public static List<Type> actions(final NotQuestsRegistry registry) {
        return registry.actions().stream()
                .map(type -> typeInfo(type.id(), type.displayName(), type.description(), type.fields(), type.flags()))
                .toList();
    }

    public static List<Type> conditions(final NotQuestsRegistry registry) {
        return registry.conditions().stream()
                .map(type -> typeInfo(type.id(), type.displayName(), type.description(), type.fields(), type.flags()))
                .toList();
    }

    public static List<Type> triggers(final NotQuestsRegistry registry) {
        return registry.triggers().stream()
                .map(type -> typeInfo(type.id(), type.displayName(), type.description(), type.fields(), List.of()))
                .toList();
    }

    public static List<Variable> variables(final NotQuestsRegistry registry) {
        return registry.variables().stream().map(NQMetadataExporter::variableInfo).toList();
    }

    public static Type typeInfo(
            final String id,
            final String displayName,
            final String description,
            final List<RegistryField.Definition> fields,
            final List<RegistryField.Definition> flags) {
        return new Type(
                id,
                displayName,
                id,
                description,
                null,
                false,
                fields.stream().map(field -> fieldInfo(field, false)).toList(),
                flags.stream().map(field -> fieldInfo(field, true)).toList());
    }

    private static Field fieldInfo(final RegistryField.Definition field, final boolean flag) {
        return new Field(
                field.name(),
                field.description(),
                argumentType(field),
                field.valueType(),
                !flag,
                flag);
    }

    private static String argumentType(final RegistryField.Definition field) {
        if (field.presenceFlag()) {
            return "PresenceFlag";
        }
        return field.valueType();
    }

    private static Variable variableInfo(final Variables.Type variable) {
        return new Variable(
                variable.id(),
                variable.displayName(),
                variable.id(),
                variable.description(),
                null,
                false,
                variable.valueType(),
                canSet(variable.handler()),
                variable.fields().stream()
                        .filter(field -> !field.presenceFlag()
                                && !field.valueType().toLowerCase(Locale.ROOT).contains("number")
                                && !field.valueType().toLowerCase(Locale.ROOT).contains("boolean"))
                        .map(RegistryField.Definition::name)
                        .toList(),
                variable.fields().stream()
                        .filter(field -> field.valueType().toLowerCase(Locale.ROOT).contains("number"))
                        .map(RegistryField.Definition::name)
                        .toList(),
                variable.fields().stream()
                        .filter(field -> !field.presenceFlag()
                                && field.valueType().toLowerCase(Locale.ROOT).contains("boolean"))
                        .map(RegistryField.Definition::name)
                        .toList(),
                variable.fields().stream()
                        .filter(RegistryField.Definition::presenceFlag)
                        .map(RegistryField.Definition::name)
                        .toList());
    }

    private static boolean canSet(final Object handler) {
        if (handler instanceof final Variables.BooleanVariableHandler booleanHandler) {
            return booleanHandler.canSet();
        }
        if (handler instanceof final Variables.NumberVariableHandler numberHandler) {
            return numberHandler.canSet();
        }
        if (handler instanceof final Variables.StringVariableHandler stringHandler) {
            return stringHandler.canSet();
        }
        if (handler instanceof final Variables.ListVariableHandler listHandler) {
            return listHandler.canSet();
        }
        return handler instanceof final Variables.ItemStackListVariableHandler itemHandler && itemHandler.canSet();
    }
}
