package com.notquests.paper.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.LinkedHashMap;
import java.util.Map;

class BukkitConfigurationValueCodecTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @SuppressWarnings("deprecation")
    void roundTripsTheCompleteCanonicalItem() {
        final ItemStack original = new ItemStack(Material.ENCHANTED_BOOK, 3);
        final ItemMeta meta = original.getItemMeta();
        meta.setCustomModelData(73);
        original.setItemMeta(meta);

        final Object encoded = BukkitConfigurationValueCodec.toYamlValue(original);
        final ItemStack restored = assertInstanceOf(
                ItemStack.class,
                BukkitConfigurationValueCodec.fromYamlValue(encoded));

        assertEquals(Material.ENCHANTED_BOOK, restored.getType());
        assertEquals(3, restored.getAmount());
        assertEquals(73, restored.getItemMeta().getCustomModelData());
    }

    @Test
    void releasedBukkitMapsAreNotDecodedByTheNormalRuntimeCodec() {
        final Map<String, Object> released = new LinkedHashMap<>();
        released.put("==", "org.bukkit.inventory.ItemStack");
        released.put("type", "STONE");

        final Map<?, ?> unchanged = assertInstanceOf(
                Map.class,
                BukkitConfigurationValueCodec.fromYamlValue(released));

        assertEquals("org.bukkit.inventory.ItemStack", unchanged.get("=="));
    }

    @Test
    void unsupportedNativeValuesAreRejectedInsteadOfStringified() {
        final Object unsupported = new Object();

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BukkitConfigurationValueCodec.toYamlValue(unsupported));

        assertTrue(exception.getMessage().contains(Object.class.getName()));
    }
}
