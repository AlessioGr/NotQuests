package com.notquests.paper;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.items.ItemStackSelection;
import com.notquests.paper.adapter.config.BukkitConfigurationValueCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PaperItems {
    private PaperItems() {}

    public static boolean isEmpty(final ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }

    /** Converts core item selections to and from Bukkit item objects. */
    public static final class Selection extends ItemStackSelection {
        private final boolean takeAmountFromFirstItem;

        /** Kept for callers that create a mutable native selection from the Paper plugin. */
        public Selection(final NotQuests ignored) {
            super(1);
            this.takeAmountFromFirstItem = true;
        }

        public Selection(final int amount) {
            super(amount);
            this.takeAmountFromFirstItem = false;
        }

        public void addItemStack(@Nullable final ItemStack itemStack) {
            if (itemStack == null || itemStack.getType().isAir()) {
                return;
            }
            final ItemStack copy = itemStack.clone();
            if (takeAmountFromFirstItem && empty()) {
                setAmount(copy.getAmount());
            }
            final Map<String, Object> exactItem = new LinkedHashMap<>();
            exactItem.put("platform", "paper");
            exactItem.put("material", copy.getType().getKey().toString());
            exactItem.put("data", BukkitConfigurationValueCodec.toYamlValue(copy));
            addMaterialName(copy.getType().getKey().getKey());
            addExactItem(exactItem);
        }

        public void addMaterial(final Material material) {
            if (material != null && !material.isAir()) {
                addMaterialName(material.getKey().getKey());
            }
        }

        public @Nullable ItemStack toFirstItemStack() {
            if (any()) {
                return null;
            }
            final List<ItemStack> exactItems = getItemStacks();
            if (!exactItems.isEmpty()) {
                return exactItems.getFirst();
            }
            for (final String materialId : materialIds()) {
                final Material material = Material.matchMaterial(materialId);
                if (material != null && !material.isAir()) {
                    return new ItemStack(material, amount());
                }
            }
            return null;
        }

        public @NonNull ArrayList<ItemStack> toItemStackList() {
            final ArrayList<ItemStack> items = new ArrayList<>();
            if (any()) {
                return items;
            }
            items.addAll(getItemStacks());
            for (final String materialId : materialIds()) {
                final Material material = Material.matchMaterial(materialId);
                if (material != null && !material.isAir()
                        && items.stream().noneMatch(item -> item.getType() == material)) {
                    items.add(new ItemStack(material, amount()));
                }
            }
            return items;
        }

        public List<ItemStack> getItemStacks() {
            final ArrayList<ItemStack> items = new ArrayList<>();
            for (final Map<String, Object> exactItem : exactItems()) {
                if (!(exactItem.get("platform") instanceof String platform)
                        || !"paper".equalsIgnoreCase(platform)) {
                    continue;
                }
                final Object encoded = exactItem.containsKey("data") ? exactItem.get("data") : exactItem;
                final Object decoded = BukkitConfigurationValueCodec.fromYamlValue(encoded);
                if (decoded instanceof ItemStack itemStack) {
                    items.add(itemStack.clone());
                }
            }
            return List.copyOf(items);
        }
    }
}
