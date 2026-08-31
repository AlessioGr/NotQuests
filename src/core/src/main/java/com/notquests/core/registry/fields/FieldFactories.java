package com.notquests.core.registry.fields;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface FieldFactories {
    RegistryField<String> greedyText();

    RegistryField<String> text();

    RegistryField<String> text(Supplier<List<String>> suggestions);

    RegistryField<String> stringList(Supplier<List<String>> suggestions);

    RegistryField<String> commandText();

    RegistryField<String> conditionName();

    RegistryField<String> npcSelector();

    RegistryField<String> numberExpression();

    RegistryField<String> numberExpression(boolean greedy);

    RegistryField<String> booleanExpression();

    RegistryField<String> optionalNumberExpression();

    RegistryField<Integer> integer(int fallback);

    RegistryField<Integer> storedInteger(int fallback);

    RegistryField<Duration> duration(Duration fallback);

    RegistryField<Double> storedNumber(double fallback);

    RegistryField<Double> doubleNumber(double fallback);

    RegistryField<Double> optionalDouble();

    RegistryField<NQLocation> storedLocation();

    RegistryField<String> entityType();

    RegistryField<String> enchantment();

    RegistryField<String> worldName(String anyWorldValue);

    RegistryField<Boolean> presenceFlag();

    RegistryField<ItemSelection> itemSelection();

    RegistryField<Object> storedItemStack();

    RegistryField<Map<String, String>> stringMap();

    RegistryField<Map<String, ?>> numberExpressionMap();
}
