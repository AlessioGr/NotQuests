package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.variables.NumberExpression;

import java.util.Map;

public final class EnchantObjective {
    private EnchantObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Enchant")
                .displayName("Enchant")
                .description("Counts matching items enchanted with a specific enchantment.")
                .field(
                        "enchantment",
                        adapter.fields().enchantment().config("specifics.enchantment"),
                        "Minecraft enchantment that must be applied.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Item that must be enchanted.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching enchantments required.")
                .flag(
                        "minLevel",
                        adapter.fields().optionalNumberExpression().config("specifics.minLevelExpression"),
                        "Minimum enchantment level that counts.")
                .flag(
                        "maxLevel",
                        adapter.fields().optionalNumberExpression().config("specifics.maxLevelExpression"),
                        "Maximum enchantment level that counts.")
                .flag(
                        "min",
                        adapter.fields().optionalNumberExpression().config("specifics.minLevelExpression"),
                        "Minimum enchantment level that counts.")
                .flag(
                        "max",
                        adapter.fields().optionalNumberExpression().config("specifics.maxLevelExpression"),
                        "Maximum enchantment level that counts.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final int minLevel = level(adapter, firstPresent(
                            objective.text("minLevel"), objective.text("min")), questPlayer, 0);
                    final int maxLevel = level(adapter, firstPresent(
                            objective.text("maxLevel"), objective.text("max")), questPlayer, 100);
                    return adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.enchant.base",
                            questPlayer,
                            activeObjective,
                            Map.of(
                                    "%ITEMTOENCHANTTYPE%",
                                    objective.itemSelection("materials").listedMaterials("main"),
                                    "%ITEMTOENCHANTNAME%",
                                    "",
                                    "%(%",
                                    "",
                                    "%)%",
                                    "",
                                    "%ENCHANTMENT%",
                                    enchantmentDescription(objective.text("enchantment"), minLevel, maxLevel)));
                })
                .onPlayerEnchantItem((event, objective) -> {
                    final int minLevel = level(adapter, firstPresent(
                            objective.text("minLevel"), objective.text("min")), objective.questPlayer(), 0);
                    final int maxLevel = level(adapter, firstPresent(
                            objective.text("maxLevel"), objective.text("max")), objective.questPlayer(), 100);
                    if (event.matches(objective.itemSelection("materials"))
                            && hasEnchantment(event.enchantments(), objective.text("enchantment"), minLevel, maxLevel)) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    private static String firstPresent(final String primary, final String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private static int level(
            final NotQuestsAdapter adapter,
            final String expression,
            final PlatformPlayer questPlayer,
            final int fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        try {
            return (int) new NumberExpression(adapter, expression).calculateValue(questPlayer);
        } catch (final RuntimeException ignored) {
            return fallback;
        }
    }

    private static String enchantmentDescription(
            final String enchantment,
            final int minLevel,
            final int maxLevel) {
        final String name = enchantment == null ? "" : enchantment;
        if (minLevel != 0 && maxLevel != 100) {
            return name + " (" + minLevel + "-" + maxLevel + ")";
        }
        if (minLevel != 0) {
            return name + " (> " + (minLevel - 1) + ")";
        }
        if (maxLevel != 100) {
            return name + " (< " + (maxLevel + 1) + ")";
        }
        return name;
    }

    private static boolean hasEnchantment(
            final Map<String, Integer> enchantments,
            final String configuredEnchantment,
            final int minLevel,
            final int maxLevel) {
        if (configuredEnchantment == null || configuredEnchantment.isBlank()) {
            return true;
        }
        for (final Map.Entry<String, Integer> enchantment :
                enchantments == null ? Map.<String, Integer>of().entrySet() : enchantments.entrySet()) {
            final String fullId = enchantment.getKey() == null ? "" : enchantment.getKey();
            final int namespace = fullId.indexOf(':');
            final String shortId = namespace < 0 ? fullId : fullId.substring(namespace + 1);
            final int level = enchantment.getValue() == null ? 0 : enchantment.getValue();
            if ((fullId.equalsIgnoreCase(configuredEnchantment)
                            || shortId.equalsIgnoreCase(configuredEnchantment))
                    && level >= minLevel
                    && level <= maxLevel) {
                return true;
            }
        }
        return false;
    }

}
