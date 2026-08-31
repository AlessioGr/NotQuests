package com.notquests.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.actions.BroadcastMessageAction;
import com.notquests.builtin.objectives.DeliverItemsObjective;
import com.notquests.builtin.objectives.EnchantObjective;
import com.notquests.builtin.objectives.TalkToNPCObjective;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BuiltinParityTest {
    @Test
    void broadcastMessageResolvesActionPlaceholdersBeforeBroadcasting() {
        final String[] broadcast = new String[1];
        final Object[] resolvedObjects = new Object[1];
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = actionTextAdapter(
                registry.createAdapter(new NotQuestsRegistry.PlatformHooks(
                        null,
                        message -> broadcast[0] = message,
                        null)),
                resolvedObjects);
        BroadcastMessageAction.register(adapter);
        final Action action = new Action(0, "SendMessage", null);
        action.setValue("message", "Hello %PLAYER% from %QUEST%");
        final PlatformPlayer player = player("Alex");

        action(registry, "BroadcastMessage").executor().execute(action, player, "DailyQuest");

        assertEquals("Hello Alex from DailyQuest", broadcast[0]);
        assertEquals("DailyQuest", resolvedObjects[0]);
    }

    @Test
    void enchantEvaluatesLevelExpressionsPerPlayerAndRendersConfiguredRanges() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.variables()
                .numberVariable("PlayerEnchantLevel")
                .displayName("Player Enchant Level")
                .description("Test level derived from the current player.")
                .singular("level")
                .plural("levels")
                .get((player, objects) -> player.playerName().equals("Alex") ? 2 : 5)
                .register();
        EnchantObjective.register(adapter);
        final Objectives.Type enchant = objective(registry, "Enchant");
        final PlatformPlayer lowLevelPlayer = player("Alex");
        final PlatformPlayer highLevelPlayer = player("Sam");
        final TestObjective lowLevelObjective = enchantObjective(lowLevelPlayer)
                .text("minLevel", "PlayerEnchantLevel")
                .text("maxLevel", "PlayerEnchantLevel + 2");
        final TestObjective highLevelObjective = enchantObjective(highLevelPlayer)
                .text("minLevel", "PlayerEnchantLevel")
                .text("maxLevel", "PlayerEnchantLevel + 2");
        final TestEnchantEvent lowLevelEvent = new TestEnchantEvent("book", "sharpness", 3);
        final TestEnchantEvent highLevelEvent = new TestEnchantEvent("book", "sharpness", 3);

        enchant.enchantItemHandler().handle(lowLevelEvent, lowLevelObjective);
        enchant.enchantItemHandler().handle(highLevelEvent, highLevelObjective);

        assertEquals(1, lowLevelObjective.progress());
        assertEquals(0, highLevelObjective.progress());
        assertEquals(
                "Enchant Items with sharpness (2-4): <main>book ",
                enchant.taskDescriptionRenderer().render(lowLevelObjective, lowLevelPlayer, null));
        assertEquals(
                "Enchant Items with sharpness (5-7): <main>book ",
                enchant.taskDescriptionRenderer().render(highLevelObjective, highLevelPlayer, null));
        assertEquals(
                "Enchant Items with sharpness (> 2): <main>book ",
                enchant.taskDescriptionRenderer().render(
                        enchantObjective(lowLevelPlayer).text("minLevel", "3"), lowLevelPlayer, null));
        assertEquals(
                "Enchant Items with sharpness (< 5): <main>book ",
                enchant.taskDescriptionRenderer().render(
                        enchantObjective(lowLevelPlayer).text("maxLevel", "4"), lowLevelPlayer, null));
    }

    @Test
    void npcObjectiveDescriptionsPreferNamesThenIdsThenSelectors() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = npcAdapter(registry.createAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null)));
        DeliverItemsObjective.register(plugin, adapter);
        TalkToNPCObjective.register(adapter);
        final Objectives.Type deliverItems = objective(registry, "DeliverItems");
        final Objectives.Type talkToNpc = objective(registry, "TalkToNPC");

        assertEquals(
                "Talk to <main>Guide",
                talkToNpc.taskDescriptionRenderer().render(
                        new TestObjective(null).text("npc", "citizens:12"), null, null));
        assertEquals(
                "Talk to <main>13",
                talkToNpc.taskDescriptionRenderer().render(
                        new TestObjective(null).text("npc", "citizens:13"), null, null));
        assertEquals(
                "Talk to <main>custom:missing",
                talkToNpc.taskDescriptionRenderer().render(
                        new TestObjective(null).text("npc", "custom:missing"), null, null));

        final String deliverText = deliverItems.taskDescriptionRenderer().render(
                new TestObjective(null)
                        .itemSelection("materials", ItemStackSelection.parse("apple"))
                        .text("npc", "citizens:12"),
                null,
                null);
        assertEquals("Deliver Items: <main>apple \n      <GRAY>Deliver it to <WHITE>Guide", deliverText);
    }

    private static TestObjective enchantObjective(final PlatformPlayer player) {
        return new TestObjective(player)
                .itemSelection("materials", ItemStackSelection.parse("book"))
                .text("enchantment", "sharpness");
    }

    private static Actions.Type action(
            final NotQuestsRegistry registry,
            final String id) {
        return registry.actions().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Objectives.Type objective(
            final NotQuestsRegistry registry,
            final String id) {
        return registry.objectives().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static NotQuestsAdapter actionTextAdapter(
            final NotQuestsAdapter delegate,
            final Object[] resolvedObjects) {
        return proxy(delegate, (method, arguments) -> {
            if (!method.getName().equals("resolveActionText")) {
                return Invocation.unhandled();
            }
            final PlatformPlayer player = (PlatformPlayer) arguments[1];
            final Object[] objects = (Object[]) arguments[3];
            resolvedObjects[0] = objects[0];
            return Invocation.handled(((String) arguments[2])
                    .replace("%PLAYER%", player.playerName())
                    .replace("%QUEST%", String.valueOf(objects[0])));
        });
    }

    private static NotQuestsAdapter npcAdapter(final NotQuestsAdapter delegate) {
        return proxy(delegate, (method, arguments) -> {
            if (!method.getName().equals("npcSelection")) {
                return Invocation.unhandled();
            }
            final String selector = (String) arguments[0];
            if (selector.equals("citizens:12")) {
                return Invocation.handled(new NotQuestsAdapter.NpcSelection(
                        selector, "Guide", "citizens", NQNPCID.fromInteger(12), "Guide"));
            }
            if (selector.equals("citizens:13")) {
                return Invocation.handled(new NotQuestsAdapter.NpcSelection(
                        selector, "13", "citizens", NQNPCID.fromInteger(13), ""));
            }
            return Invocation.handled(null);
        });
    }

    private static NotQuestsAdapter proxy(
            final NotQuestsAdapter delegate,
            final AdapterInvocation adapterInvocation) {
        return (NotQuestsAdapter) Proxy.newProxyInstance(
                NotQuestsAdapter.class.getClassLoader(),
                new Class<?>[] {NotQuestsAdapter.class},
                (proxy, method, arguments) -> {
                    final Invocation invocation = adapterInvocation.invoke(method, arguments);
                    if (invocation.handled()) {
                        return invocation.value();
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (final InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    @FunctionalInterface
    private interface AdapterInvocation {
        Invocation invoke(Method method, Object[] arguments);
    }

    private record Invocation(boolean handled, Object value) {
        private static Invocation handled(final Object value) {
            return new Invocation(true, value);
        }

        private static Invocation unhandled() {
            return new Invocation(false, null);
        }
    }

    private static PlatformPlayer player(final String name) {
        return (PlatformPlayer) Proxy.newProxyInstance(
                PlatformPlayer.class.getClassLoader(),
                new Class<?>[] {PlatformPlayer.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "hasPlayer" -> true;
                    case "playerName", "playerIdentifier" -> name;
                    case "currentWorldTimeTicks" -> 0L;
                    case "toString" -> "TestPlayer[" + name + "]";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(final Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return 0;
    }

    private static final class TestObjective implements Objectives.Progress {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final PlatformPlayer player;
        private double progress;

        private TestObjective(final PlatformPlayer player) {
            this.player = player;
        }

        private TestObjective text(final String name, final String value) {
            values.put(name, value);
            return this;
        }

        private TestObjective itemSelection(final String name, final ItemSelection value) {
            values.put(name, value);
            return this;
        }

        private double progress() {
            return progress;
        }

        @Override
        public double currentProgress() {
            return progress;
        }

        @Override
        public double progressNeeded() {
            return 1;
        }

        @Override
        public int childObjectiveCount() {
            return 0;
        }

        @Override
        public PlatformPlayer questPlayer() {
            return player;
        }

        @Override
        public void copyTo(final Objectives.Draft objective) {
            values.forEach(objective::setValue);
        }

        @Override
        public String text(final String name) {
            final Object value = values.get(name);
            return value == null ? "" : String.valueOf(value);
        }

        @Override
        public boolean flag(final String name) {
            return false;
        }

        @Override
        public ItemSelection itemSelection(final String name) {
            final Object value = values.get(name);
            return value instanceof ItemSelection selection ? selection : null;
        }

        @Override
        public void setProgress(final double progress, final boolean capAtZero) {
            this.progress = capAtZero ? Math.max(0, progress) : progress;
        }

        @Override
        public void addProgress(final double amount) {
            progress += amount;
        }

        @Override
        public void removeProgress(final double amount, final boolean capAtZero) {
            progress = capAtZero ? Math.max(0, progress - amount) : progress - amount;
        }
    }

    private static final class TestEnchantEvent implements Objectives.EnchantEvent {
        private final String material;
        private final String enchantment;
        private final int level;

        private TestEnchantEvent(final String material, final String enchantment, final int level) {
            this.material = material;
            this.enchantment = enchantment;
            this.level = level;
        }

        @Override
        public String materialId() {
            return material;
        }

        @Override
        public int amount() {
            return 1;
        }

        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(material);
        }

        @Override
        public Map<String, Integer> enchantments() {
            return Map.of(enchantment, level);
        }
    }
}
