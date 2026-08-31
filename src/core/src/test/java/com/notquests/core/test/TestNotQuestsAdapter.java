package com.notquests.core.test;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.util.ArrayList;
import java.util.Map;

public interface TestNotQuestsAdapter extends NotQuestsAdapter {
    @Override
    default java.util.List<String> nativeNpcQuestGiverTypes() {
        return java.util.List.of();
    }

    @Override
    default boolean itemMaterialExists(final String materialId) {
        return materialId != null && !materialId.isBlank();
    }

    @Override
    default ItemSelection parseItemSelection(final Object input, final PlatformPlayer questPlayer) {
        return parseItemSelection(input == null ? "" : String.valueOf(input));
    }

    @Override
    default boolean itemSelectionsAreSimilar(
            final ItemSelection required,
            final ItemSelection actual) {
        return ItemStackSelection.areSimilar(required, actual);
    }

    @Override
    default boolean supportsWorldEditSelection() {
        return false;
    }

    @Override
    default LocationRegion worldEditSelection(final PlatformPlayer questPlayer) {
        return null;
    }

    @Override
    default boolean supportsPermissionMutation() {
        return false;
    }

    default boolean supportsArbitraryPermissionChecks() {
        return true;
    }

    @Override
    default Object variableValue(
            final String variableName,
            final PlatformPlayer questPlayer,
            final Map<String, String> strings,
            final Map<String, ?> numbers,
            final Map<String, ?> booleans) {
        final ArrayList<Object> values = new ArrayList<>();
        for (final var field : variableFields(variableName)) {
            if (numbers != null && numbers.containsKey(field.name())) {
                values.add(String.valueOf(numbers.get(field.name())));
            } else if (booleans != null && booleans.containsKey(field.name())) {
                values.add(String.valueOf(booleans.get(field.name())));
            } else if (strings != null && strings.containsKey(field.name())) {
                values.add(strings.get(field.name()));
            } else {
                values.add("");
            }
        }
        return variableValue(variableName, questPlayer, values.toArray());
    }

    @Override
    default boolean allowObjectiveUnlock(
            final PlatformPlayer questPlayer,
            final com.notquests.core.structs.Quest quest,
            final com.notquests.core.structs.ActiveObjective objective,
            final boolean triggerAcceptQuestTrigger) {
        return true;
    }
}
