package com.notquests.builtin.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.ActiveObjectives;

import java.util.HashMap;
import java.util.Map;

class BuiltinObjectiveBehaviorTest {
    @Test
    void brewItemsCountsOnlySelectedFreshlyBrewedItems() {
        final var brewItems = objective("BrewItems");
        final TestObjective progress = new TestObjective().itemSelection("materials", ItemStackSelection.parse("potion"));

        brewItems.takeBrewedItemHandler().handle(new ItemEvent("potion", 1), progress);
        brewItems.takeBrewedItemHandler().handle(new ItemEvent("glass_bottle", 1), progress);

        assertEquals(1, progress.progress());
    }

    @Test
    void itemResultObjectivesOnlyCountSelectedMaterials() {
        final TestObjective smithing =
                new TestObjective().itemSelection("materials", ItemStackSelection.parse("netherite_sword"));
        objective("SmithItems").takeSmithingResultHandler().handle(new ItemEvent("netherite_sword", 1), smithing);
        objective("SmithItems").takeSmithingResultHandler().handle(new ItemEvent("iron_sword", 1), smithing);
        assertEquals(1, smithing.progress());

        final TestObjective trade =
                new TestObjective().itemSelection("materials", ItemStackSelection.parse("emerald"));
        objective("TradeWithVillager").tradeItemHandler().handle(new ItemEvent("emerald", 1), trade);
        objective("TradeWithVillager").tradeItemHandler().handle(new ItemEvent("diamond", 1), trade);
        assertEquals(1, trade.progress());
    }

    @Test
    void tameMobsSupportsSpecificEntityOrAny() {
        final var tameMobs = objective("TameMobs");
        final TestObjective wolfOnly = new TestObjective().text("entityType", "wolf");
        tameMobs.tameEntityHandler().handle(new EntityEvent("wolf", "", false), wolfOnly);
        tameMobs.tameEntityHandler().handle(new EntityEvent("cat", "", false), wolfOnly);
        assertEquals(1, wolfOnly.progress());

        final TestObjective any = new TestObjective().text("entityType", "any");
        tameMobs.tameEntityHandler().handle(new EntityEvent("wolf", "", false), any);
        tameMobs.tameEntityHandler().handle(new EntityEvent("cat", "", false), any);
        assertEquals(2, any.progress());
    }

    @Test
    void interactCountsConfiguredClicksOnMatchingLocation() {
        final var interact = objective("Interact");
        final NQLocation target = NQLocation.at("world", 10, 64, -5);

        final TestObjective defaultClicks = new TestObjective()
                .location("locationToInteract", target)
                .withInteger("maxDistance", 1);
        interact.interactBlockHandler().handle(new InteractionEvent(target, false, true), defaultClicks);
        interact.interactBlockHandler().handle(new InteractionEvent(target, true, false), defaultClicks);
        assertEquals(2, defaultClicks.progress());

        final TestObjective leftClickOnly = new TestObjective()
                .location("locationToInteract", target)
                .flag("leftClick", true)
                .withInteger("maxDistance", 1);
        interact.interactBlockHandler().handle(new InteractionEvent(target, true, false), leftClickOnly);
        interact.interactBlockHandler().handle(new InteractionEvent(target, false, true), leftClickOnly);
        assertEquals(1, leftClickOnly.progress());
    }

    @Test
    void shootArrowCountsTargetRadius() {
        final var shootArrow = objective("ShootArrow");

        final TestObjective radius = new TestObjective()
                .location("targetLocation", NQLocation.at("world", 10, 64, -5))
                .withNumber("radius", 3);
        shootArrow.projectileHitHandler().handle(new ProjectileHitEvent(NQLocation.at("world", 13, 64, -5)), radius);
        shootArrow.projectileHitHandler().handle(new ProjectileHitEvent(NQLocation.at("world", 14, 64, -5)), radius);
        shootArrow.projectileHitHandler().handle(new ProjectileHitEvent(NQLocation.at("other", 10, 64, -5)), radius);
        assertEquals(1, radius.progress());
    }

    @Test
    void harvestCountsFullyGrownNonPlayerPlacedCropAliases() {
        final var harvest = objective("Harvest");
        final TestObjective carrots = new TestObjective()
                .itemSelection("crops", ItemStackSelection.parse("carrot"));

        harvest.harvestBlockHandler().handle(new HarvestEvent("carrots", true, false), carrots);
        harvest.harvestBlockHandler().handle(new HarvestEvent("carrots", false, false), carrots);
        harvest.harvestBlockHandler().handle(new HarvestEvent("carrots", true, true), carrots);

        assertEquals(1, carrots.progress());
    }

    @Test
    void harvestRulesMatchPaperAndNeoForgePlacedBlockBehavior() {
        assertEquals(true, ActiveObjectives.isFullyGrownHarvestable("wheat", false, true));
        assertEquals(false, ActiveObjectives.isFullyGrownHarvestable("wheat", false, false));
        assertEquals(false, ActiveObjectives.isFullyGrownHarvestable("pumpkin_stem", false, true));
        assertEquals(true, ActiveObjectives.isFullyGrownHarvestable("melon", false, false));
        assertEquals(false, ActiveObjectives.isFullyGrownHarvestable("sugar_cane", false, false));
        assertEquals(true, ActiveObjectives.isFullyGrownHarvestable("sugar_cane", true, false));
        assertEquals(true, ActiveObjectives.shouldTrackPlayerPlacedHarvestBlock("melon", false));
        assertEquals(false, ActiveObjectives.shouldTrackPlayerPlacedHarvestBlock("wheat", false));
    }

    @Test
    void npcObjectivesExposeTaskDescriptionRenderers() {
        assertEquals(
                "Talk to <main>Citizens:12",
                objective("TalkToNPC")
                        .taskDescriptionRenderer()
                        .render(new TestObjective().text("npc", "Citizens:12"), null, null));
        final String deliverItemsText = objective("DeliverItems")
                .taskDescriptionRenderer()
                .render(new TestObjective()
                        .itemSelection("materials", ItemStackSelection.parse("apple"))
                        .text("npc", "Citizens:12"), null, null);
        assertTrue(deliverItemsText.contains("Deliver Items: <main>apple"));
        assertTrue(deliverItemsText.contains("Deliver it to <WHITE>Citizens:12"));
    }

    @Test
    void sharedObjectiveTaskDescriptionsDoNotLeakRawPlaceholders() {
        assertTaskDescription("BreedMobs", new TestObjective().text("entityType", "cow"), "Breed Mobs: <main>cow");
        assertTaskDescription("FeedMobs", new TestObjective().text("entityType", "cow"), "Feed Mobs: <main>cow");
        assertTaskDescription("TameMobs", new TestObjective().text("entityType", "wolf"), "Tame Mobs: <main>wolf");
        assertTaskDescription("ShearSheep", new TestObjective().text("amount", "3"), "Shear <main>3</main> sheep");
        assertTaskDescription("MilkCow", new TestObjective().text("amount", "2"), "Milk <main>2</main> cows");
        assertTaskDescription(
                "ShootArrow",
                new TestObjective()
                        .location("targetLocation", NQLocation.at("minecraft:overworld", 4, 5, 6))
                        .withNumber("radius", 7),
                "Shoot Arrow: <main>X: 4.0 Y: 5.0 Z: 6.0</main> in world <main>minecraft:overworld</main> within <main>7.0</main> blocks");
        assertTaskDescription(
                "Interact",
                new TestObjective()
                        .location("locationToInteract", NQLocation.at("minecraft:overworld", 4, 5, 6))
                        .flag("rightClick", true),
                "Right-Click Location: <main>X: 4.0 Y: 5.0 Z: 6.0 <main>in world minecraft:overworld");
        assertTaskDescription("OtherQuest", new TestObjective().text("otherQuest", "DailyQuest"), "Complete Quest: <main>DailyQuest");
        assertTaskDescription("Objective", new TestObjective().text("objectiveHolderName", "Find the Key"), "Find the Key");
    }

    @Test
    void itemObjectiveTaskDescriptionsRemoveUnusedNameWrappers() {
        assertTaskDescription(
                "PickupItems",
                new TestObjective().itemSelection("materials", ItemStackSelection.parse("apple")),
                "Pickup Items: <main>apple ");
        assertTaskDescription(
                "SmeltItems",
                new TestObjective().itemSelection("materials", ItemStackSelection.parse("iron_ingot")),
                "Smelt Items: <main>iron_ingot ");
        assertTaskDescription(
                "Enchant",
                new TestObjective()
                        .itemSelection("materials", ItemStackSelection.parse("book"))
                        .text("enchantment", "sharpness"),
                "Enchant Items with sharpness: <main>book ");
    }

    private static void assertTaskDescription(
            final String objectiveId,
            final TestObjective objective,
            final String expected) {
        final String rendered = objective(objectiveId).taskDescriptionRenderer().render(objective, null, null);
        assertEquals(expected, rendered);
        assertFalse(rendered.matches(".*%[A-Z()]+%.*"), rendered);
    }

    private static Objectives.Type objective(final String id) {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        BuiltInPack.registerObjectives(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        return registry.objectives().stream()
                .filter(objective -> objective.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private record ItemEvent(String materialId, int amount) implements Objectives.ItemEvent {
        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record InteractionEvent(NQLocation location, boolean leftClick, boolean rightClick)
            implements Objectives.InteractionEvent {
        @Override
        public void cancel() {}
    }

    private record ProjectileHitEvent(NQLocation location) implements Objectives.ProjectileHitEvent {}

    private record HarvestEvent(String materialId, boolean fullyGrownHarvestable, boolean playerPlaced)
            implements Objectives.HarvestBlockEvent {
        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record EntityEvent(
            String entityTypeId,
            String plainCustomName,
            boolean selfAttributedDeath) implements Objectives.EntityEvent {}

    private static final class TestObjective implements Objectives.Progress {
        private final Map<String, Object> values = new HashMap<>();
        private double progress;

        TestObjective text(final String name, final String value) {
            values.put(name, value);
            return this;
        }

        TestObjective flag(final String name, final boolean value) {
            values.put(name, value);
            return this;
        }

        TestObjective withInteger(final String name, final int value) {
            values.put(name, value);
            return this;
        }

        TestObjective withNumber(final String name, final double value) {
            values.put(name, value);
            return this;
        }

        TestObjective location(final String name, final NQLocation value) {
            values.put(name, value);
            return this;
        }

        TestObjective itemSelection(final String name, final ItemSelection value) {
            values.put(name, value);
            return this;
        }

        double progress() {
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
        public com.notquests.core.platform.PlatformPlayer questPlayer() {
            return null;
        }

        @Override
        public void copyTo(final Objectives.Draft objective) {
            values.forEach(objective::setValue);
        }

        @Override
        public void addProgress(final double amount) {
            progress += amount;
        }

        @Override
        public void removeProgress(final double amount, final boolean capAtZero) {
            progress = capAtZero ? Math.max(0, progress - amount) : progress - amount;
        }

        @Override
        public void setProgress(final double progress, final boolean capAtZero) {
            this.progress = capAtZero ? Math.max(0, progress) : progress;
        }

        @Override
        public String text(final String name) {
            final Object value = values.get(name);
            return value == null ? "" : value.toString();
        }

        @Override
        public boolean flag(final String name) {
            return Boolean.TRUE.equals(values.get(name));
        }

        @Override
        public int integer(final String name, final int fallback) {
            final Object value = values.get(name);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        @Override
        public double number(final String name, final double fallback) {
            final Object value = values.get(name);
            return value instanceof Number number ? number.doubleValue() : fallback;
        }

        @Override
        public NQLocation location(final String name) {
            final Object value = values.get(name);
            return value instanceof NQLocation location ? location : null;
        }

        @Override
        public ItemSelection itemSelection(final String name) {
            final Object value = values.get(name);
            return value instanceof ItemSelection selection ? selection : null;
        }
    }
}
